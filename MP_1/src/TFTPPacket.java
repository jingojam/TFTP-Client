import java.net.*;

/*
    TFTPPacket is the Abstract class representing a TFTP packet. Provides common attributes and methods
    for handling TFTP packets.
*/
public abstract class TFTPPacket {
    protected InetAddress address;
    protected int port;
    protected byte[] data;
    protected short op;
    
    /*
    Description:
        - Constructor to initialize a TFTP packet with an address, port, and operation code.
    
    Parameters:
        * `InetAddress` address: The IP address of the destination.
        * `int` port: The port number to send the packet.
        * `short` op: The operation code for the TFTP packet.
    */
    public TFTPPacket(InetAddress address, int port, short op) {
        this.address = address;
        this.port = port;
        this.op = op;
    }
    
    /*
    Description:
        - Abstract method to be implemented by subclasses for creating specific TFTP packets.
    
    Parameters:
        * void
    */
    public abstract void CreatePacket();
    
    /*
    Description:
        - Creates and returns a DatagramPacket representation of the TFTP packet.
    
    Parameters:
        * void
    
    Returns:
        * `DatagramPacket`: The datagram packet representing this TFTP packet.
    */
    public DatagramPacket TFTPDatagramPacket() {
        return new DatagramPacket(data, data.length, address, port);
    }
    
    /*
    Description:
        - Sets the data payload of the TFTP packet.
    
    Parameters:
        * `byte[]` data: The byte array representing the data payload.
    */
    public void SetData(byte[] data) {
        this.data = data;
    }
    
    /*
    Description:
        - Sets the operation code for the TFTP packet.
    
    Parameters:
        * `short` op: The operation code to set.
    */
    public void SetOp(short op) {
        this.op = op;
    }
    
    /*
    Description:
        - Retrieves the data payload of the TFTP packet.
    
    Parameters:
        * void
    
    Returns:
        * `byte[]`: The byte array representing the data payload.
    */
    public byte[] GetData() {
        return data;
    }
    
    /*
    Description:
        - Retrieves the operation code of the TFTP packet.
    
    Parameters:
         * void
    
    Returns:
        * `short`: The operation code of the packet.
    */
    public short GetOp() {
        return op;
    }
    
    /*
    Description:
        - Retrieves the destination port number of the TFTP packet.
    
    Parameters:
        * void
    
    Returns:
        * `int`: The port number.
    */
    public int GetPort() {
        return port;
    }
}
