import java.net.*;
import java.nio.*;
import java.nio.charset.*;

/*
    Size:    2 bytes     2 bytes      string    1 byte
             -----------------------------------------
    Fields: |    5   |  ErrorCode |   ErrMsg   |   0  |
             -----------------------------------------
*/

public class ERRORPacket extends TFTPPacket{
    private String errMsg;
    private short errCode;

    public ERRORPacket(InetAddress address, int port, short errCode, String errMsg){
        super(address, port, (short)5);
        this.errMsg = errMsg;
        this.errCode = errCode;
        CreatePacket();
    }
    
    public ERRORPacket(InetAddress address, int port, DatagramPacket packet){
        super(address, port, (short)5);
        this.errCode = (short) (((packet.getData()[2] & 255) * 256) + (packet.getData()[3] & 255));
        int errorMsgLength = packet.getLength() - 4;

        if(errorMsgLength > 0){
            this.errMsg = new String(packet.getData(), 4, errorMsgLength, StandardCharsets.US_ASCII);
        } else{
            this.errMsg = null;
        }
    }
    
    @Override
    public void CreatePacket(){
        byte[] byteErrMsg = errMsg.getBytes();
        int packetSize = 2 + 2 + byteErrMsg.length + 1;
        ByteBuffer packet = ByteBuffer.allocate(packetSize);
        
        packet.putShort(op);
        packet.putShort(errCode);
        packet.put(byteErrMsg);
        packet.put((byte)0);
    
        this.data = packet.array();
    }
    
    public void SetErrorMessage(String errMsg){
        this.errMsg = errMsg;
        CreatePacket();
    }
    
    public String GetErrorMessage(){
        return errMsg;
    }
    
    public void SetErrorCode(short errCode){
        this.errCode = errCode;
        CreatePacket();
    }
    
    public short GetErrorCode(){
        return errCode;
    }
}
