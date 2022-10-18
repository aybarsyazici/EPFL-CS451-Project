package cs451.udp;

import javax.sound.midi.Receiver;
import java.io.Serializable;

public class Message implements Serializable {
    private final int id;
    private final int senderId;
    private final int receiverId;
    private final int originalSender;
    private final Boolean ack;

    public Message(int id, int senderId, int receiverId, int originalSender) {
        this.id = id;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.originalSender = originalSender;
        this.ack = false;
    }

    public Message(Message message, int newSender, int newReceiver){
        this.id = message.getId();
        this.senderId = newSender;
        this.receiverId = newReceiver;
        this.originalSender = message.getOriginalSender();
        this.ack = true;
    }

    public int getId() {
        return id;
    }

    public int getSenderId() {
        return senderId;
    }

    public int getReceiverId() {
        return receiverId;
    }

    public int getOriginalSender() {
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

        return id == message.getId() && originalSender == message.getOriginalSender();
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
}
