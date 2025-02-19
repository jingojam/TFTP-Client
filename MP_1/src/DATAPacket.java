import java.net.*;
import java.nio.*;


/*
    DATAPacket is the main blueprint for creating a DATA packet type.
    It extends the base abstract class TFTPPacket.
    It overrides TFTPPacket's CreatePacket() method to tailor the
    allocation of sizes when constructing DATA packet, specifically 
    on the fields that are unique to DATA packets (block, data) fields.

    Following the RFC 1350 DATA packet field format:

    Size:    2 bytes     2 bytes      n bytes
             ----------------------------------
    Fields: |  op 3  |   Block #  |   Data     |
             ----------------------------------

*/
public class DATAPacket extends TFTPPacket{
    private short block; // data block attribute
    private byte[] dataField; // data field attribute

    /*
    Description:
        - constructor for DATA packet (used for client-to-server DATA packet sending)
    
    Parameters:
        * `InetAddress` address: IP address of the server
        * `int` port: port used by the server
        * `short` block: data block being communicated
        * `byte[]` data: data sent by the server
    */
    public DATAPacket(InetAddress address, int port, short block, byte[] data){
        super(address, port, (short)3); // call on parent constructor to set the address, port, and opcode
        this.block = block; // set the block
        this.data = data; // set the data
        CreatePacket(); //create the packet
    }
    
    /*
        to calculate the fields:
        for example:
            - let's say we are receiving a data packet for block 170
                        
            receivedData = [ 0x00, 0x03, 0x02, 0x01, 0xAB, 0xCD, 0xEF, 0x09] -> 8-byte data array
                  index  ->   0     1     2     3     4     5     6     7
                                
            actual 2-byte block data = 0x0201 (170 in decimal) 
                
            as you can see here, we need 0x02 and 0x01 as one whole which is 0x0201 
            (0x02 and 0x01 when read individually is just 2 and 1 which is an incorrect block)
                    
            (and since 2 bytes is the original format as sent by the server,
            as well as it is the valid format for tftp).
                
            but since the `byte[]` array stores 8-bit (byte) values, the field are divided.
                    
            so we do these steps (to form the original block):
                
            receivedData = [ 0x00, 0x03, 0x02, 0x01, 0xAB, 0xCD, 0xEF, 0x09]
                  index  ->   0     1     2     3     4     5     6     7
                             |---op--|   |-block-|   |-------data--------|
                
            index 0-1 (2 bytes): opcode (as mentioned before)
            index 2-3 (2 bytes): block
            index 4-7 (4 bytes): data
                
            receivedData[2] * 256 -> 0x02 * 256 -> 0x200
            receivedData[3] = 0x01
            receivedData[2] + receivedData[3] = 0x200 + 0x01 = 0x201
            therefore, block = 0x201 = 170 (in decimal)
    */
    
    /*
    Description:
        - overloaded constructor to create a DATA packet (server-to-client data packets)
    
    Parameters:
        * `InetAddress` address: IP address of the server
        * `int` port: port used by the server
        * `short` block: data block being communicated
        * `DatagramPacket` packet: datagram sent by the server
    */
    public DATAPacket(InetAddress address, int port, DatagramPacket packet){
        super(address, port, (short)3); // call the parent constructor to set the address, port, opcode
        byte[] packetData = packet.getData(); // get the raw byte data of the datagram
        int length = packet.getLength() - 4; // length of the data (without the 2-byte opcode and 2-byte block)
        byte[] actualData = new byte[length]; //allocate the data field size only
        
        this.block = (short) (((packet.getData()[2] & 255) * 256) + (packet.getData()[3] & 255)); // combine the separated bytes of the block field from the server
        int dataIndex = 4; // actual data starts at index[4]
        for(int i = 0; i < length; i++){ //set the actual data to the data field of the packet sent by the server
            actualData[i] = packetData[dataIndex];
            dataIndex++;//move to the next index starting from the initial data index
        }
        this.dataField = actualData; //set the data field to the actual data itself
    }
    
    @Override
    public void CreatePacket(){       
        int packetSize = 2 + 2 + data.length; // 2 byte opcode + 2byte block + data 
        ByteBuffer packet = ByteBuffer.allocate(packetSize); // allocate the packet size

        packet.putShort((short)3); //append a short (2bytes) for the opcode (which is 3 for DATA packet as per RFC 1350)
        packet.putShort(block);// append a short (2 bytes) for the block
        packet.put(data); //append the data

        this.data = packet.array();//set the data attribute to the packet data
    }
    
    /*
    Description:
        - setter method for setting the block attribute
    
    Parameters:
        * `short` block: block being communicated
    */
    public void SetBlock(short block){
        this.block = block;
        CreatePacket();
    }
    
    /*
    Description:
        - getter method for obtaining the block attribute
    
    Parameters:
        * void
    */
    public short GetBlock(){
        return block;
    }
    
    /*
    Description:
        - getter method to obtain the data field attribute
    
    Parameters:
        * void
    */
    public byte[] GetDataField(){
        return dataField;
    }
}