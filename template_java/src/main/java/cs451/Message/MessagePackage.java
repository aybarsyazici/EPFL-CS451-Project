package cs451.Message;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class MessagePackage implements Serializable {
    private final List<Message> messages;

    public MessagePackage(){
        this.messages = new ArrayList<>();
    }

    public MessagePackage(List<Message> messages){
        this.messages = messages;
    }

    // Convert to bytes to send through DatagramPacket
    public byte[] toBytes(){
        byte[] bytes = new byte[0];
        for(Message message : messages){
            byte[] messageBytes = message.toByteArray();
            byte[] newBytes = new byte[bytes.length + messageBytes.length];
            System.arraycopy(bytes, 0, newBytes, 0, bytes.length);
            System.arraycopy(messageBytes, 0, newBytes, bytes.length, messageBytes.length);
            bytes = newBytes;
        }
        return bytes;
    }

    // Convert from byte array received through datagram packet
    public static MessagePackage fromBytes(byte[] bytes){
        MessagePackage messagePackage = new MessagePackage();
        int i = 0;
        while(i < bytes.length){
            byte[] messageBytes = new byte[Message.BYTE_SIZE];
            System.arraycopy(bytes, i, messageBytes, 0, Message.BYTE_SIZE);
            messagePackage.addMessage(Message.fromByteArray(messageBytes));
            i += Message.BYTE_SIZE;
        }
        return messagePackage;
    }

    public void addMessage(Message message){
        messages.add(message);
    }

    public List<Message> getMessages(){
        return this.messages;
    }

    // get all message ids
    public List<Integer> getMessageIds(){
        List<Integer> messageIds = new ArrayList<>();
        for(Message message : messages){
            messageIds.add(message.getId());
        }
        return messageIds;
    }

    public int size(){
        return messages.size();
    }

    // Create copy of message package
    public MessagePackage copy(){
        List<Message> messagesCopy = new ArrayList<>();
        for(Message message : messages){
            messagesCopy.add(message.copy());
        }
        return new MessagePackage(messagesCopy);
    }
}
