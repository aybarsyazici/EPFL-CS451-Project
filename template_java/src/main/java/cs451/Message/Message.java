package cs451.Message;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;

public class Message implements Serializable {
    public static final int BYTE_SIZE = 8;
    private final int id;
    private final byte senderId;
    private final byte receiverId;
    private final byte originalSender;
    private final Boolean ack;

    public Message(int id, byte senderId, byte receiverId, byte originalSender) {
        this.id = id;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.originalSender = originalSender;
        this.ack = false;
    }

    public Message(int id, byte senderId, byte receiverId, byte originalSender, Boolean ack) {
        this.id = id;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.originalSender = originalSender;
        this.ack = ack;
    }

    public Message(Message message, byte newSender, byte newReceiver){
        this.id = message.getId();
        this.senderId = newSender;
        this.receiverId = newReceiver;
        this.originalSender = message.getOriginalSender();
        this.ack = true;
    }

    public Message(Message message, byte newSender, byte newReceiver, Boolean ack){
        this.id = message.getId();
        this.senderId = newSender;
        this.receiverId = newReceiver;
        this.originalSender = message.getOriginalSender();
        this.ack = ack;
    }

    public int getId() {
        return id;
    }

    public byte getSenderId() {
        return senderId;
    }

    public byte getReceiverId() {
        return receiverId;
    }

    public byte getOriginalSender() {
        return originalSender;
    }
    public Boolean isAckMessage(){
        return ack;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Message message = (Message) o;

        return id == message.getId()
                && originalSender == message.getOriginalSender()
                && senderId == message.getSenderId()
                && receiverId == message.getReceiverId()
                && ack == message.isAckMessage();
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id, this.originalSender, this.senderId, this.receiverId, this.ack);
    }

    @Override
    public String toString() {
        return "Message{" +
                "id=" + id +
                ", senderId=" + senderId +
                ", receiverId=" + receiverId +
                ", originalSender=" + originalSender +
                ", isAck=" + ack +
                '}';
    }

    // Convert object to byteArray
    public byte[] toByteArray() {
        byte[] bytes = new byte[8];
        ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN).putInt(id).put(senderId).put(receiverId).put(originalSender).put(ack ? (byte) 1 : (byte) 0);
        return bytes;
    }

    // Convert byteArray to object
    public static Message fromByteArray(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN);
        int id = buffer.getInt();
        byte senderId = buffer.get();
        byte receiverId = buffer.get();
        byte originalSender = buffer.get();
        byte ack = buffer.get();
        return new Message(id, senderId, receiverId, originalSender, ack == 1);
    }

    public Message swapSenderReceiver(){
        return new Message(this.id, this.receiverId, this.senderId, this.originalSender, false);
    }

    // Copy message
    public Message copy(){
        return new Message(this.id, this.senderId, this.receiverId, this.originalSender, this.ack);
    }

}
