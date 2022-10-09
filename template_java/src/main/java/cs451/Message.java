package cs451;

import java.io.Serializable;

public class Message implements Serializable {
    private final int id;
    private final int senderId;
    private final int receiverId;

    private final int originalSender;

    private final String content;

    public Message(int id, int senderId, int receiverId, int originalSender, String content) {
        this.id = id;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.originalSender = originalSender;
        this.content = content;
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

    public String getContent() {
        return content;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Message message = (Message) o;

        if (id == message.getId() && originalSender == message.getOriginalSender()) return true;
        return false;
    }

    @Override
    public String toString() {
        return "Message{" +
                "id=" + id +
                ", senderId=" + senderId +
                ", receiverId=" + receiverId +
                ", originalSender=" + originalSender +
                ", payload=" + content +
                '}';
    }
}
