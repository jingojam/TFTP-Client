import java.net.*;
import java.util.*;
import java.nio.*;

/*
    WRRQPacket is the main blueprint of creating a WRQ/RRQ Packet type.
    It extends the base abstract class TFTPPacket.
    It overrides TFTPPacket's CreatePacket() method to tailor the
    allocation of sizes when constructing either a WRQ or a RRQ
    packet, specifically taking into consideration the fields that are 
    unique to WRQ/RRQ packets (ilename, mode) fields.

    Following the RFC 1350 WRQ/RRQ packet field format:

    Size:     2 bytes     string    1 byte     string   1 byte
              ------------------------------------------------
    Fields:  | op 1/2 |  Filename  |   0  |    Mode    |   0  |
              ------------------------------------------------

    For implementation of TFTP WRQ/RRQ option extension, follows
    RFC 2347 options field(s):
     
            +-------+--------+---+--------+---+--------+---+--------+---+        +-------+---+--------+---+
    Fields: |  op   |filename| 0 |  mode  | 0 |  opt1  | 0 | value1 | 0 |.. -> ..| optN  | 0 | valueN | 0 |
            +-------+--------+---+--------+---+--------+---+--------+---+        +-------+---+--------+---+

    NOTES: 
    - the `string` fields are NOT strings (e.g. an array of characters, string objects).
    - although defined as formal class attributes of WRRQPacket as type `String`,
      these variables are converted into byte format prior to packet creation.
*/

public class WRRQPacket extends TFTPPacket{
    private String file; //attribute for filename string
    private String mode; //attribute for mode string
    
    /*
    Description:
        - Constuctor for standard WRQ/RRQ packets
    
    Parameters:
        * `InetAddress` address: IP address of the server
        * `short` op: op code of the packet
        * `String` filename: name of the file
        * `String` mode: mode of transfer
    */
    public WRRQPacket(InetAddress address, short op, String filename, String mode){
        super(address, 69, op); //call on the parent constructor to initialize IP address, port = 69 initially, op code
        this.file= filename; // set the file name 
        this.mode = mode; // set the mode
        CreatePacket(); // create the packet
    }

    /*
    Description:
        - Overloaded constructor for WRQ/RRQ packets appended with options
    
    Parameters:
        * `InetAddress` address: IP address
        * `short` op: op code of the packet
        * `String` filename: name of the file
        * `String` mode: mode of transfer
        * `Map<String,Integer>` optsAndVals: key-value pairs for options ("blksize"-512, "tsize"-0 defaults) 
    */
    public WRRQPacket(InetAddress address, short op, String filename, String mode, Map<String, Integer> optsAndVals){
        super(address, 69, op);//call on the parent constructor to initialize IP address, port = 69 initially, op code
        this.file= filename;// set the file name 
        this.mode = mode; // set the mode
        CreateOptionsPacket(optsAndVals); // create the packet
    }
    /*
    Description:
        - creates the packet based on the format:
        Size:     2 bytes     string    1 byte     string   1 byte
                  ------------------------------------------------
        Fields:  |   1/2  |  Filename  |   0  |    Mode    |   0  |
                  ------------------------------------------------   
    
    Parameters:
        * void
    */
   @Override
    public void CreatePacket(){
        byte[] byteFile = file.getBytes(); //convert string to bytes
        byte[] byteMode = mode.getBytes(); //conver the mode string into bytes
        
        int packetSize = 2 + byteFile.length + 1 + byteMode.length + 1; // 2-byte opcode + filename + 1-byte 0 terminator + mode + 1-byte 0 terminator
        
        ByteBuffer packet = ByteBuffer.allocate(packetSize); // allocate theb packet size 
 
        packet.putShort(op); //append a short (2-byte) opcode
        packet.put(byteFile); //append the filename
        packet.put((byte)0); //append null terminator
        packet.put(byteMode); //append mode
        packet.put((byte)0); //null terminate

        this.data = packet.array(); //set the data attribute to the packet
    }
    
    /*
    Description:
        - creates the packet based on the format (for WRQ/RRQ packets with options): 
            +-------+--------+---+--------+---+--------+---+--------+---+        +-------+---+--------+---+
    Fields: |  op   |filename| 0 |  mode  | 0 |  opt1  | 0 | value1 | 0 |.. -> ..| optN  | 0 | valueN | 0 |
            +-------+--------+---+--------+---+--------+---+--------+---+        +-------+---+--------+---+ 
    
    Parameters:
        * `Map<String,Integer>` optsAndVals: key-value pairs for options ("blksize"-512, "tsize"-0 defaults) 
    */
    public void CreateOptionsPacket(Map<String, Integer> optsAndVals){
        //ensure base packet is created first
        CreatePacket();  

        //calculate the size of the options (key-value pairs)
        int optionsSize = 0;
        for(Map.Entry<String, Integer> entry : optsAndVals.entrySet()){ //iterate over key-value pairs in the optsAndVals
            optionsSize += entry.getKey().length() + 1;  // key + null terminator
            optionsSize += entry.getValue().toString().length() + 1;  // value + null terminator
        }

        //allocate buffer for the full packet (base + options)
        int totalSize = this.data.length + optionsSize;
        ByteBuffer packet = ByteBuffer.allocate(totalSize);

        packet.put(this.data);  //copy base WRQ/RRQ packet into buffer

        //add options to the buffer
        for(Map.Entry<String, Integer> entry : optsAndVals.entrySet()){ //iterate over key-value pairs in the optsAndVals
            packet.put(entry.getKey().getBytes());  //option name
            packet.put((byte) 0);  //null terminator after option
            packet.put(entry.getValue().toString().getBytes());  //option value
            packet.put((byte) 0);  //null terminator after value
        }

        this.data = packet.array();  // Set the final packet data
    }

    /*
    Description:
        - setter method to set the file name attribute
    
    Parameters:
        - `String` file: name of the file
    */
    public void SetFilename(String file){
        this.file = file;
        CreatePacket();
    }

    /*
    Description:
        - setter method to set the file mode attribute
    
    Parameters:
        - `String` mode: mode of the file transfer
    */
    public void SetMode(String mode){
        this.mode = mode;
        CreatePacket();
    }

    /*
    Description:
        - getter method to get the file name attribute
    
    Parameters:
        * void
    */
    public String GetFilename(){
        return file;
    }

    /*
    Description:
        - getter method to get the mode name attribute
    
    Parameters:
        * void
    */
    public String GetMode(){
        return mode;
    }
}