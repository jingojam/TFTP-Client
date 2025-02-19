import java.util.*;
import java.net.*;
import java.nio.*;
import java.nio.charset.*;


/* 

    +-------+---~~---+---+---~~---+---+---~~---+---+---~~---+---+
    | op 6  |  opt1  | 0 | value1 | 0 |  optN  | 0 | valueN | 0 |
    +-------+---~~---+---+---~~---+---+---~~---+---+---~~---+---+
 
*/

public class OACKPacket extends TFTPPacket {
    private Map<String, Integer> optsAndVals;
    
    public OACKPacket(InetAddress address, int port, Map<String, Integer> optsAndVals){
        super(address, port, (short)6);
        this.optsAndVals = optsAndVals;
        CreatePacket();
    }
    
    public OACKPacket(InetAddress address, int port, DatagramPacket packet){
        super(address, port, (short)6);
        Map<String, Integer> optionsMap = new HashMap<>();
        byte[] receivedData = packet.getData();
        int index = 2; // skip the opcode part of the packet
        Integer val = null;
        while(index < packet.getLength()){
            int startIndexOfOp = index;

            //get the option name until null terminator (0)
            while(index < receivedData.length && receivedData[index] != 0){
                index++;
            }

            String opt = new String(receivedData, startIndexOfOp, index - startIndexOfOp, StandardCharsets.US_ASCII);
            index++; //skip the null terminator

            int startIndexOfVal = index;

            // get the value until null terminator (0)
            while (index < receivedData.length && receivedData[index] != 0) {
                index++;
            }
                    
            //convert the extracted byte data to a String, then parse it to an Integer
                    
            String valStr = new String(receivedData, startIndexOfVal, index - startIndexOfVal, StandardCharsets.US_ASCII);
            if(!valStr.isEmpty()){ // check if not empty before parsing
                val = Integer.valueOf(valStr); // convert string to integer
                optionsMap.put(opt, val);
            }
            index++; //skip the null terminator

            //add the option and value as a pair
            optionsMap.put(opt, val);
        }
        this.optsAndVals = optionsMap;
    }
    
    @Override
    public void CreatePacket(){
        ByteBuffer buffer = ByteBuffer.allocate(512); // Max TFTP packet size

        buffer.putShort((short)6); // OACK opcode

        // Convert each option into key-value byte pairs
        for (Map.Entry<String, Integer> entry : optsAndVals.entrySet()) {
            String option = entry.getKey();
            Integer value = entry.getValue();

            buffer.put(option.getBytes()); // Option name
            buffer.put((byte) 0);  // Null terminator after option
            buffer.put(value.toString().getBytes()); // Option value as bytes
            buffer.put((byte) 0);  // Null terminator after value
        }

        this.data = buffer.array();
    }
    
    public Map<String, Integer> GetOptions(){
        return optsAndVals;
    }
}