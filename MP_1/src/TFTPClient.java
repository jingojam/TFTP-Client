import java.io.*;
import java.net.*;
import java.util.*;
import java.nio.file.*;

/*
    TFTPClient class handles the core functionality of the TFTP client
    appplication. This class handles key operations such as:
    - Communication with the TFTP Server
    - File Operations
    - Packet Initialization
    - TFTP Process
    - Error Handling
*/

public class TFTPClient{
    private DatagramSocket clientSocket; // the socket of the client
    private InetAddress serverIP; // the IP address of the server
    private final int port = 69; //run on port 69
    
    /*
        TFTPClient() constructor to initialize the client socket
    */
    public TFTPClient(){
        try{
            clientSocket = new DatagramSocket(null); // set the socket to `null` first
        } catch(SocketException dgramErr){ // catch an error (if there happens to be after initializing the socket)
            System.err.println(dgramErr);
        }
    }

    /*
    Description:
        - ObtainServerIP() method initializes the IP address of the TFTP server 
    
    Parameters:
        * void 
    */
    public void ObtainServerIP(){
        try{
            Scanner input = new Scanner(System.in); // instantiate a scanner
            System.out.print("Enter Server IP address: "); // prompt the user
            String IP = input.nextLine(); //get the IP address
            this.serverIP = InetAddress.getByName(IP); // set the IP address of the server
        } catch(UnknownHostException e){ // catch an error where the server doesn't exist or is not found
            System.err.println(e.getMessage()); 
        }
    }
    
    /*
        
    Description:
        - GetOpCode() method returns the op code of the packet received from
        the server
    
    Parameters:
        - `DatagramPacket` packet
    
    Additional Notes:
            all packet types have the same format for opcode (first field) 
            
            Size:     2 bytes    X
                      ----------------
            Fields:  | Opcode |  n... |
                      ----------------
        
            example,
        
            consider this ACK packet sent by the server:
                
                2B  2B     = 4 bytes
               +---+---+
               | 4 | 1 |   (opcode = 4, block = 1)
               +---+---+
        
            raw format would look something like (in 2 nibbles; byte) in big endian format:
            
            data  = [0x00, 0x04, 0x00, 0x01] (in this case, the opcode is stored at data[1], which is 0x04)
            index ->  0     1     2     3  
    
            IN DETAIL (a little bit of base conversion):
            - the upper bytes won't be affected.
            - since tftp packet op codes ranges from:
                op     packet
                1 - 2: RRQ/WRQ, respectively
                3    : DATA
                4    : ACK
                5    : ERROR
                6    : OACK
                
         packet     op    2-byte hex   16-bit binary            3-bit changes only (range from 1-6 opcode values)
            RRQ ---> 1 -> 0x00 0x01 -> 0b0000_0000_0000_0001 -> 001 
            WRQ ---> 2 -> 0x00 0x02 -> 0b0000_0000_0000_0010 -> 010
            DATA --> 3 -> 0x00 0x03 -> 0b0000_0000_0000_0011 -> 011
            ACK ---> 4 -> 0x00 0x04 -> 0b0000_0000_0000_0100 -> 100
            ERROR -> 5 -> 0x00 0x05 -> 0b0000_0000_0000_0101 -> 101
            OACK --> 6 -> 0x00 0x06 -> 0b0000_0000_0000_0110 -> 110  
        
            here, you can see only the low 3-bits are changing (just to scale how little difference the opcodes really have)
            due to complexity, we can just consider the low byte as follows:
 
         packet     op       hex       16-bit binary
            RRQ ---> 1 -> 0x00 0x01 -> 0b0000_0000_0000_0001
            WRQ ---> 2 -> 0x00 0x02 -> 0b0000_0000_0000_0010
            DATA --> 3 -> 0x00 0x03 -> 0b0000_0000_0000_0011
            ACK ---> 4 -> 0x00 0x04 -> 0b0000_0000_0000_0100
            ERROR -> 5 -> 0x00 0x05 -> 0b0000_0000_0000_0101
            OACK --> 6 -> 0x00 0x06 -> 0b0000_0000_0000_0110
                                       |----|----| |---|---|  
                                            |          |
                                     uninitialized   low byte
    
            which values are stored in data[1]
    */
    private short GetOpCode(DatagramPacket packet){
        byte[] data = packet.getData(); //get the actual data sent by the server
        return (short) (data[1]); // get the 2nd byte data which stores the actual range of opcodes (1-6)
    }

    /*
    
    Description:
        - SendACK() is used to construct, convert an ACK packet to a DatagramPacket (for compatibility with .send() method) 
          and send to the server
    
    Parameters
        * `int` portUsed: port number used by the server
        * `short` block: block number being communicated
    */
    public void SendACK(int portUsed, short block){
        try{
            TFTPPacket ack = new ACKPacket(serverIP, portUsed, block); //create an ACK packet
            clientSocket.send(ack.TFTPDatagramPacket()); // send the ACK to the server
        } catch(UnknownHostException e){ //if the server ip is invalid
            System.err.println(e.getMessage());
        } catch (IOException e){ //if the client experiences an issue when sending the packet
            System.err.println(e.getMessage());
        }
    }
    
    /*
    Description:
        - ReadFile() is responsible for the main downloading of a file from the server
    
    Parameters:
        * `String` filename: name of the file to write (to store in the local directory)
        * `int` portUsed: initial port used by the server when receiving initial ACK
        * `int` blksize: size of bytes to read from file, send to server
                - if there is a negotiated blksize, this is set to n>=8,n<=65464 (with options)
                - if no options appended to the packet, default set to 512 
        * `int` tsize: size of the actual file to send
                - if there is a negotiated tsize, this is set to the size of the file agreed upon
                - ifno options appended to the packet, default set to 0
    */
    public void ReadFile(String filename, int blksize, int tsize){
        try{
            FileOutputStream fileOut = new FileOutputStream(filename);// open the file for writing data
            boolean lastPacket = false; //flag to signify the last packet
            boolean errorPacketReceived = false; //flag to signify that an ERROR packet is received
            short expectedBlock = 1; // we initally expect block 1 (as per RFC 1350)
            long totalBytesReceived = 0; //count of total bytes received initially set to 0
            
            while(!lastPacket && !errorPacketReceived){ //as long as it isn't the last packet received and an ACK is received
                byte[] receiveData = new byte[4 + blksize]; // custom blksize (4 bytes from opcode+block + custom blksize bytes specified)
                DatagramPacket serverResponse = new DatagramPacket(receiveData, receiveData.length); //initialize the byte array from the server datagramsent
                clientSocket.receive(serverResponse); //Block until a packet is received
                
                int portUsed = serverResponse.getPort(); // get the port used by the server 
                
                if(GetOpCode(serverResponse) == 3){ //meaning the packet received is an ACK (opcode 3)
                    // detailed info about the DATAPacket() constructors can be found in the DATAPacket.java file
                    DATAPacket dataPacket = new DATAPacket(serverIP, portUsed, serverResponse); //create a new DATA packet (using the constructor with a DatagramPakcet parameter)
                    
                    if(dataPacket.GetBlock() == expectedBlock){ //if the block received is correct
                        fileOut.write(dataPacket.GetDataField()); //write the raw data bytes from the data field from the DATA packet, to the fileoutput
                        expectedBlock++; //move to the next expected block
                        totalBytesReceived += dataPacket.GetDataField().length;  // add to the total amount of bytes received
                        
                        if(tsize > 0){ //if the tsize option is negotiated (defaults 0)
                            System.out.println("[Progress] Bytes received: " + totalBytesReceived + "/" + tsize); //output the progress
                        }
                    } else{
                        System.out.println("Received unexpected block " + dataPacket.GetBlock() + ", expected " + expectedBlock); //blocks are out of order
                    }
                    SendACK(portUsed, dataPacket.GetBlock()); // send an ACK packet with the block
                    
                    //stop when the last packet  is received (case where the received packet is less than the expected number of bytes to read)
                    if(dataPacket.GetDataField().length < blksize) {
                        lastPacket = true; //set the flag to terminate the loop
                    }
                } else if(GetOpCode(serverResponse) == 5){// if the client received an ERROR packet instead
                    ERRORPacket errorPacket = new ERRORPacket(serverIP, portUsed, serverResponse); //construct the ERROR packet
                    System.err.println("ERROR Packet received: [Error Code = " + errorPacket.GetErrorCode() + "] "
                                        + "[Error Message = " + errorPacket.GetErrorMessage() + "]"); //output the error
                    System.out.println("Transaction terminated.");
                    errorPacketReceived = true;  // set the flag to end the transaction
                }
            }
            System.out.println("File '" + filename + "' downloaded successfully.");
        } catch(IOException e){ //if the file wasn't read
            System.err.println("Error reading file: " + e.getMessage());
        }
    }
   
    /*
    
    Description:
        - WriteFile() method handles the main file sending transactions from the client to the server
    
    Parameters:
        * `String` filename: name of the file to write (to store in the local directory)
        * `int` portUsed: initial port used by the server when receiving initial ACK
        * `int` blksize: size of bytes to read from file, send to server
                - if there is a negotiated blksize, this is set to n>=8,n<=65464 (with options)
                - if no options appended to the packet, default set to 512 
        * `int` tsize: size of the actual file to send
                - if there is a negotiated tsize, this is set to the size of the file agreed upon
                - ifno options appended to the packet, default set to 0
    */
    public void WriteFile(String filename, int portUsed, int blksize, int tsize){
        File fileToSend = new File(filename); //open file for file size checking
        boolean fileIsDivisible = (fileToSend.length() % blksize) == 0; //flag to set if file size has no remainder when sending bytes of size blksize
        int blocks = (int) fileToSend.length() / blksize; //calculate number of blocks needed for transfer (used for divisible blocks)
        int blockCounter = 0;//block sent counter 
        long totalBytesSent = 0; //counter for total bytes sent
        
        try{
            FileInputStream file = new FileInputStream(filename); //open the file for sending
            short dataBlock = 1; //expected block initially is 1
            boolean end = false; // flag to set if the end of the file send is reached
            
            while(!end){ // as long as flag isn't set
                byte[] fileData = file.readNBytes(blksize); // read blksize number of bytes from the file 
                
                //construct and send DATA packet
                TFTPPacket dataPacket = new DATAPacket(serverIP, portUsed, dataBlock, fileData); //append the data read from the file, into the data field of the DATA packet
                // portUsed and dataBlock are appended to the DATA packet to send in order to properly sync both client and server
                clientSocket.send(dataPacket.TFTPDatagramPacket());//construct the datagram packet and send to the server
                
                totalBytesSent += fileData.length; // add the total number of bytes so far

                // wait for ACK for the sent DATA packet
                byte[] receiveACK = new byte[4]; //allocate 4 bytes for ACK packet
                DatagramPacket serverACK = new DatagramPacket(receiveACK, receiveACK.length); // get the the datagram packet
                clientSocket.receive(serverACK); //receive the packet from the server
                
                if(GetOpCode(serverACK) == 4){ // the packet is an ACK (opcode = 4)
                    ACKPacket ackPacket = new ACKPacket(serverIP, portUsed, serverACK); // create a new ACK packet
                    
                    if(ackPacket.GetBlock() == dataBlock){//if the received block is correctly sequenced
                        dataBlock++; //move the next block
                        blockCounter++; //count the blocks sent
                        portUsed = serverACK.getPort(); //get the port used by the server
                        
                        if(tsize > 0){ //if tsize is negotiated
                            System.out.println("[Progress] Bytes sent: " + totalBytesSent + "/" + tsize);
                        }
                    }
                } else if(GetOpCode(serverACK) == 5){ // ERROR packet received (op code = 5)
                    ERRORPacket errorPacket = new ERRORPacket(serverIP, portUsed, serverACK); //create the ERROR packet 
                    System.err.println("ERROR Packet received: [Error Code = " + errorPacket.GetErrorCode() + "] " 
                                        + "[Error Message = " + errorPacket.GetErrorMessage() + "]"); // output the error
                    System.out.println("Transaction terminated.");
                    end = true; //terminate the transaction
                }
                
                if(fileIsDivisible && blockCounter == blocks){ // if file size is divisible by the bytes being sent and the amount of blocks sent has reached the last expected block to send
                    byte[] emptyData = new byte[0];//initialize an empty data (as per RFC 1350)
                    TFTPPacket finalDataPacket = new DATAPacket(serverIP, portUsed, dataBlock, emptyData); //create a DATA packet with empty data field
                    clientSocket.send(finalDataPacket.TFTPDatagramPacket());// send the DATA packet

                    //wait for final ACK
                    byte[] receive = new byte[4];
                    DatagramPacket finalACK = new DatagramPacket(receive, receive.length);  //construct the ACK packet
                    clientSocket.receive(finalACK); // receive the ACK
                    end = true;// terminate the transaction
                } else if(fileData.length < blksize){ //if the block isn't divisible by the file size (there is a remainder of bytes and blocks)
                    end = true; //terminate the transaction
                }
                
            }

            System.out.println("File '" + filename + "' uploaded successfully."); //signal the end of the transaction

        } catch(IOException e){ //if it reaches an error when transacting the file
            System.err.println("Error during file transfer: " + e.getMessage());
        }
    }
    
       /*
    Description:
        - SendRRQ() is the entry point for reading files from the server, and it handles the user inputs that are
          necessary for file transaction (like negotiating the blksize/tsize) and the actual file to determine/consider
    
    Parameters:
        * void
    */
    public void SendRRQ(){
        Scanner scanner = new Scanner(System.in);//open a new scanner for input
        Map<String, Integer> optsAndVals = new HashMap<>(); //use a map for key-value pairs
        // in this case, the pair is a string ("blksize" or "tsize" names), and a corresponding number (which is their specified value)
        
        //default values in case OACK does not include them
        int blksize = optsAndVals.getOrDefault("blksize", 512); //default blksize is 512 bytes
        int tsize = optsAndVals.getOrDefault("tsize", 0); // default tsize 0
        
        try{
            TFTPPacket rrq_packet = null; //initialize the RRQ packet to null
            boolean validInput = true; // flag for invalid input ranges
           
            System.out.print("Enter File to Read: "); // prompt the user to enter the file name to read
            String filename = scanner.nextLine();

            System.out.print("Enter Filename to use on the local directory when downloading: ");//prompt for the name to use
            String chosenFilename = scanner.nextLine();

            String mode = "octet"; // default octet mode
            int userOption;

            do{
                validInput = true; //set the flag intailly
                System.out.println("Options:"); // options available
                System.out.println("[1] Send a RRQ packet without options (standard 512-bytes transfer)"); //standard 512 bytes transfer (standard RRQ/WRQ packets)
                System.out.println("[2] Send a RRQ packet with options (transfer size negotiable)"); //transfer with options (RRQ/WRQ packets with options)
                System.out.print("Option: ");
                userOption = scanner.nextInt();
                scanner.nextLine();  //consume newline
                if(userOption < 1 || userOption > 2){
                    System.err.println("Invalid input"); //prompt invalid input
                    validInput = false;
                }
            } while(!validInput);//retry as long as input is invalid
            
            switch(userOption){ //all inputs are now filtered, enter switch-case block
                case 1:
                    rrq_packet = new WRRQPacket(serverIP, (short)1, filename, mode); //if user chooses the standard packet (option 1)
                    break;
                // create an RRQ packet (without options)
                case 2: //if user chooses to negotiate options 
                    do{
                        validInput = true;
                        System.out.println("Options: ");  // prompt the user with the avialable options
                        System.out.println("[1] blksize"); // 1 for blksize
                        System.out.println("[2] tsize"); // 2 for tsize
                        System.out.println("[3] blksize and tsize"); //3 for both
                        System.out.print("Option: ");
                        int option = scanner.nextInt();

                        switch(option){
                            case 1:// if user chooses to negotiate the blksize
                                int chosenSize;
                                do{
                                    System.out.print("Enter size [8 - 65464]: "); // valid range is from 8-65464 bytes as per RFC 2348
                                    chosenSize = scanner.nextInt();
                                    if(chosenSize < 8 || chosenSize > 65464){
                                        System.err.println("Please select a valid range only."); //invalid range specified
                                    }
                                } while(chosenSize < 8 || chosenSize > 65464); //retry the prompt as long as an invalid range is specified
                                optsAndVals.put("blksize", chosenSize);//append the size as a pair to "blksize"
                                break;
                                
                            case 2: //user wants to include tsize option
                                optsAndVals.put("tsize", 0); // as per RFC 2349 default size is 0 if used (for RRQ)
                                break;
                                
                            case 3: //if user chooses both blksize and tsize
                                int chosenBlksize; 
                                do{
                                    System.out.print("Enter blksize [8 - 65464]: "); //obtain the byte size transfer for blksize
                                    chosenBlksize = scanner.nextInt();
                                    if(chosenBlksize < 8 || chosenBlksize > 65464){ //prompt if the range is invalid
                                        System.err.println("Please select a valid range for blksize.");
                                    }
                                } while (chosenBlksize < 8 || chosenBlksize > 65464);  //retry as long as range is invalid

                                optsAndVals.put("blksize", chosenBlksize); //append both options
                                optsAndVals.put("tsize", 0);
                                break;
                            default:
                                System.out.println("Invalid option."); //prompt invalid input
                                validInput = false; // otherwise, invalid input
                                break;
                        }
                    } while(!validInput); // retry if input is invalid
                    rrq_packet = new WRRQPacket(serverIP, (short) 1, filename, mode, optsAndVals); //construct the RRQ packet
                    break;
            }

            //send RRQ packet to server
            clientSocket.send(rrq_packet.TFTPDatagramPacket());

            // receive either an ACK or OACK
            byte[] receiveACK = new byte[516]; //size covers both ACK and OACK
            DatagramPacket serverResponse = new DatagramPacket(receiveACK, receiveACK.length);

            clientSocket.receive(serverResponse); //receive an O/ACK packet
            
            if(GetOpCode(serverResponse) == 6){//OACK packet received (opcode = 6)
                OACKPacket oackPacket = new OACKPacket(serverIP, serverResponse.getPort(), serverResponse); //construct the OACK packet
     
                SendACK(serverResponse.getPort(), (short)0); //acknowledge the 0 block initially
                System.out.println("OACK received with options: " + oackPacket.GetOptions()); //display the options made so far

                // get the option key-value pairs
                Map<String, Integer> receivedOptions = oackPacket.GetOptions();
                if (receivedOptions.containsKey("blksize")){
                    blksize = receivedOptions.get("blksize"); //if "blksize" option is appended, then get the value it's paired up with 
                }
                if(receivedOptions.containsKey("tsize")){ 
                    tsize = receivedOptions.get("tsize");//if the "tsize" option is appended, then get the valuee it is paired up with
                }
            } else if(GetOpCode(serverResponse) == 5){//ERROR packet received (opcode = 5)
                ERRORPacket errorPacket = new ERRORPacket(serverIP, serverResponse.getPort(), serverResponse); //create the ERROR packet
                System.err.println("ERROR Packet received: [Error Code = " + errorPacket.GetErrorCode() + "] " 
                                    + "[Error Message = " + errorPacket.GetErrorMessage() + "]"); //display the errors
                System.out.println("Transaction terminated.");
            }
            if(GetOpCode(serverResponse) != 5){ //only enter file reading if there wasn't any ERROR packet received (good to go)
                ReadFile(chosenFilename, blksize, tsize);
            }
        } catch(IOException e){//if there was an error with the file transaction
            System.err.println("Error during RRQ: " + e.getMessage());
        }
    }
    
    public void SendWRQ(){
        Scanner scanner = new Scanner(System.in); // open a new scanner
        Map<String, Integer> optsAndVals = new HashMap<>(); //use a map for key-value pairs
        // in this case, the pair is a string ("blksize" or "tsize" names), and a corresponding number (which is their specified value)
             
        try{
            TFTPPacket wrq_packet = null; //WRQ packet initially null
            boolean validInput = true; // flag for input validity
            int userOption; //for user option input
            System.out.print("Enter File to Write: "); // prompt for the name of the file to send
            String filename = scanner.nextLine();
            
            System.out.print("Enter Filename to use on the server when uploading: "); //prompt for the file name to use on the server
            String chosenFilename = scanner.nextLine();

            if(!Files.exists(Paths.get(filename))){ //check if the file exists
                System.err.println("File does not exist."); //signal that the file does not exist on the local directory
            } else{ // otherwise, file is valid and found
                String mode = "octet"; // default octet mode
                do{
                    validInput = true;// always reinitialize to true for reprompt and input range correction
                    System.out.println("Options:"); // display the options available for transfer
                    System.out.println("[1] Send a WRQ packet without options (standard 512-bytes transfer)"); //standard prompt
                    System.out.println("[2] Send a WRQ packet with options (transfer size negotiable)"); // WRQ packets with options appended
                    System.out.print("Option: ");
                    userOption = scanner.nextInt();

                    scanner.nextLine();  //consume newline
                
                    if(userOption < 1 || userOption > 2){ // if input isn't within expected range
                        System.err.println("Invalid input"); //prompt invalid input
                        validInput = false; // invalid input
                    }
                } while(!validInput);//retry as long as input is invalid
                
                switch (userOption){ // get the necessary operation to perform after receiving transaction style (standard or with options)
                    case 1:
                        wrq_packet = new WRRQPacket(serverIP, (short)2, chosenFilename, mode); // standard transaction (no options)
                        break;
                        
                    case 2://transaction for WRQ packets append with options
                        do{
                            validInput = true; // reinitialize for reprompting
                            System.out.println("Options: "); // options available
                            System.out.println("[1] blksize"); // 1 for blksize option only
                            System.out.println("[2] tsize"); // 2 for tsize option only
                            System.out.println("[3] blksize and tsize"); // 3 for both blksize and tsize
                            System.out.print("Option: ");
                            int option = scanner.nextInt();

                            switch(option){
                                case 1: // blksize option 
                                    int chosenSize;
                                    do {
                                        System.out.print("Enter size [8 - 65464]: "); // prompt for file transfer size
                                        chosenSize = scanner.nextInt();
                                        if(chosenSize < 8 || chosenSize > 65464){ //ensure within range (8-65464 bytes as per RFC 2348)
                                            System.err.println("Please select a valid range only.");
                                        }
                                    } while (chosenSize < 8 || chosenSize > 65464);
                                    optsAndVals.put("blksize", chosenSize); // set the value of the "blksize" to the input
                                    break;
                                    
                                case 2: //append a tsize option only
                                    FileInputStream file = new FileInputStream(filename); //open the file for file size checking
                                    optsAndVals.put("tsize", file.readAllBytes().length); //append the file size to the "tsize" field
                                    break;
                                    
                                case 3: // both options to add to the packet
                                    int chosenBlksize;
                                    do{
                                        System.out.print("Enter blksize [8 - 65464]: "); //prompt for the byte transfer size
                                        chosenBlksize = scanner.nextInt();
                                        if(chosenBlksize < 8 || chosenBlksize > 65464){ // invalid range
                                            System.err.println("Please select a valid range for blksize.");
                                        }
                                    } while(chosenBlksize < 8 || chosenBlksize > 65464);//ensure within range (8-65464 bytes as per RFC 2348)
                                    optsAndVals.put("blksize", chosenBlksize); // append the byte file size transfer to the "blksize"

                                    // Set tsize after calculating file size
                                    FileInputStream filesend = new FileInputStream(filename); //get the file size
                                    optsAndVals.put("tsize", filesend.readAllBytes().length);//append it to the "tsize" field
                                    break;
                                    
                                default://out of range
                                    System.out.println("Invalid option.");
                                    validInput = false; //reprompt
                                    break;
                            }
                            wrq_packet = new WRRQPacket(serverIP, (short)2, chosenFilename, mode, optsAndVals); //send the WRQ packet with options
                        } while(!validInput); // as long as input is not valid
                        break;
                }

                //send WRQ request to server
                clientSocket.send(wrq_packet.TFTPDatagramPacket());

                //wait for server ACK or OACK
                byte[] receiveACK = new byte[512];
                DatagramPacket serverResponse = new DatagramPacket(receiveACK, receiveACK.length);

                clientSocket.receive(serverResponse);
                // Process received packet
                                                        
                int blksize = optsAndVals.getOrDefault("blksize", 512); // default is 512 bytes for blksize
                int tsize = optsAndVals.getOrDefault("tsize", 0); // defult is 0 for tsize

                int dataPort = serverResponse.getPort(); // Use the correct port

                if(GetOpCode(serverResponse) == 4){  //received an ACK (opcode = 4)
                    ACKPacket ackPacket = new ACKPacket(serverIP, dataPort, serverResponse); //construct the ACK packet
                    if(ackPacket.GetBlock() == 0){  // must be acknowledged with block 0 initially as per RFC 1350
                        WriteFile(filename, dataPort, blksize, tsize); //good to go, write the file to the server
                    }
                } else if(GetOpCode(serverResponse) == 6){ //received an OACK (opcode = 6)
                    OACKPacket oackPacket = new OACKPacket(serverIP, dataPort, serverResponse);// construct the OACK packet
                    Map<String, Integer> receivedOptions = oackPacket.GetOptions(); //get the options from the OACK packet
                    if(receivedOptions.containsKey("blksize")){ //if blksize is appended
                        blksize = receivedOptions.get("blksize"); //set the blksize value from the value pair with key "blksize"
                    }
                    if(receivedOptions.containsKey("tsize")){
                        tsize = receivedOptions.get("tsize"); //set the tsize value from the value pair with key "tsize"
                    }
                    System.out.println("OACK received with options: " + oackPacket.GetOptions()); //display the agreed options
                    WriteFile(filename, dataPort, blksize, tsize); //write the file
                } else if(GetOpCode(serverResponse) == 5){ // ERROR packet received 
                    ERRORPacket errorPacket = new ERRORPacket(serverIP, serverResponse.getPort(), serverResponse); //construct the ERROR packet
                    System.err.println("ERROR Packet received: [Error Code = " + errorPacket.GetErrorCode() + "] " 
                                        + "[Error Message = " + errorPacket.GetErrorMessage() + "]"); //display the error
                    System.out.println("Transaction terminated.");                    
                }
            }
        } catch(IOException e){ // if there was an error for transferring files
            System.err.println("Error during WRQ: " + e.getMessage());
        }
    }   
    
    /*
    Desciption:
        - GetServerIP() is a getter method for obtaining the server IP address
    
    Parameters:
        * void
    */
    public InetAddress GetServerIP(){   
        return serverIP;
    }

    /*
    Desciption:
        - GetPort() is the getter method for obtaining the port (69)
    
    Parameters:
        * void
    */
    public int GetPort(){
        return port;
    }
    
    /*
    Desciption:
        - SetServerIP() is a setter method for setting the IP address of the server
    
    Parameters:
        * void
    */
    public void SetServerIP(InetAddress serverIP){
        this.serverIP = serverIP;
    }
}