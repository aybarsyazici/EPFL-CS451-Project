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
    public byte[] toBytes(int proposalSetSize){
        byte[] bytes = new byte[0];
        for(Message message : messages){
            byte[] messageBytes = message.toByteArray(proposalSetSize);
            byte[] newBytes = new byte[bytes.length + messageBytes.length];
            System.arraycopy(bytes, 0, newBytes, 0, bytes.length);
            System.arraycopy(messageBytes, 0, newBytes, bytes.length, messageBytes.length);
            bytes = newBytes;
        }
        return bytes;
    }

    // Convert from byte array received through datagram packet
    public static MessagePackage fromBytes(byte[] bytes, int proposalSetSize){
        MessagePackage messagePackage = new MessagePackage();
        int messageSize = 88 + proposalSetSize*4*8;
        for(int i = 0; i < bytes.length; i += messageSize){
            byte[] messageBytes = new byte[messageSize];
            System.arraycopy(bytes, i, messageBytes, 0, messageSize);
            messagePackage.addMessage(Message.fromByteArray(messageBytes, proposalSetSize));
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
        List<Message> messagesCopy = new ArrayList<>(messages);
        return new MessagePackage(messagesCopy);
    }
}
