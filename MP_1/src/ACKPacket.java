import java.net.*;
import java.nio.*;

/*
    ACKPacket is the main blueprint for creating an ACK packet type.
    It extends the base abstract class TFTPPacket.
    It overrides TFTPPacket's CreatePacket() method to tailor the
    allocation of sizes when constructing ACK packet

    in accordance to the RFC 1350, ACKPacket follows this packet 
    format:

    Size:     2 bytes    2 bytes
             ---------------------
    Fields: |   op 4 |   Block #  |
             ---------------------
*/
public class ACKPacket extends TFTPPacket {
    private short block; // block attribute
    
    /*
    Description:
        - constructor to create a ACK packet
    
    Parameters:
        * `InetAddress` address: IP address of the server
        * `int` port: port used by the server
        * `short` block: data block being communicated
    */
    public ACKPacket(InetAddress address, int port, short block){
        super(address, port, (short)4); //call on parent constructor to initialize the IP address, port, and opcode = 4
        this.block = block; // set the block
        CreatePacket(); // create the packet
    }
    
    /*
    Description:
        - constructor to create a ACK packet
    
    Parameters:
        * `InetAddress` address: IP address of the server
        * `int` port: port used by the server
        * `short` block: data block being communicated
    */  
    public ACKPacket(InetAddress address, int port, DatagramPacket packet){
        super(address, port, (short)4);
        this.block = (short) (((packet.getData()[2] & 255) * 256) + (packet.getData()[3] & 255));
    }
    
    @Override
    public void CreatePacket(){
        ByteBuffer packet = ByteBuffer.allocate(4);

        packet.putShort((short)4);
        packet.putShort(block);

        this.data = packet.array();
    }
    
    public void SetBlock(short block){
        this.block = block;
        CreatePacket();
    }
    
    public short GetBlock(){
        return block;
    }
}