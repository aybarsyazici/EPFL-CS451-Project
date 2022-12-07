package cs451.Message;

import com.sun.source.tree.Tree;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;

public class Message implements Serializable {
    private final int id;
    private final int latticeRound;

    private final byte senderId;
    private final byte receiverId;
    private final byte ack; // 0 means the message is a proposal, 1 means message is ack, 2 means message is n_ack
    private final Set<Integer> proposals;
    private final int hashCode;

    public Message(int id, byte senderId, byte receiverId, int latticeRound, Set<Integer> proposals) {
        this.id = id;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.latticeRound = latticeRound;
        this.ack = 0;
        this.proposals = proposals;
        this.hashCode = Objects.hash(this.id, this.latticeRound, this.senderId, this.receiverId, this.ack);
    }

    public Message(int id, byte senderId, byte receiverId, int latticeRound, byte ack, Set<Integer> proposals) {
        this.id = id;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.latticeRound = latticeRound;
        this.ack = ack;
        this.proposals = proposals;
        this.hashCode = Objects.hash(this.id, this.latticeRound, this.senderId, this.receiverId, this.ack);
    }

    public Message(Message message, byte newSender, byte newReceiver, byte ack){
        this.id = message.getId();
        this.senderId = newSender;
        this.receiverId = newReceiver;
        this.latticeRound = message.getLatticeRound();
        this.ack = ack;
        this.proposals = message.copyProposals();
        this.hashCode = Objects.hash(this.id, this.latticeRound, this.senderId, this.receiverId, this.ack);
    }

    public Set<Integer> getProposals() {
        return proposals;
    }

    private Set<Integer> copyProposals(){
        return new HashSet<>(this.proposals);
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

    public int getLatticeRound() {
        return latticeRound;
    }
    public Boolean isAckMessage(){
        return this.ack != 0;
    }
    public byte getAck(){
        return this.ack;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Message message = (Message) o;

        return id == message.getId()
                && latticeRound == message.getLatticeRound()
                && senderId == message.getSenderId()
                && receiverId == message.getReceiverId()
                && ack == message.getAck();
    }

    @Override
    public int hashCode() {
        return this.hashCode;
    }

    @Override
    public String toString() {
        return "Message{" +
                "proposalCount=" + id +
                ", senderId=" + senderId +
                ", receiverId=" + receiverId +
                ", latticeRound=" + latticeRound +
                ", isAck=" + ack +
                ", proposals=" + proposals +
                '}';
    }

    // Convert object to byteArray
    public byte[] toByteArray(int proposalSetSize) {
        byte[] byteArray = new byte[11 + proposalSetSize*4];
        ByteBuffer byteBuffer = ByteBuffer.wrap(byteArray);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);
        byteBuffer.putInt(this.id);
        byteBuffer.put(this.senderId);
        byteBuffer.put(this.receiverId);
        byteBuffer.putInt(this.latticeRound);
        byteBuffer.put(this.ack);
        for (Integer proposal : this.proposals) {
            byteBuffer.putInt(proposal);
        }
        return byteArray;
    }

    // Convert byteArray to object
    public static Message fromByteArray(byte[] bytes, int proposalSetSize) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);
        int id = byteBuffer.getInt();
        byte senderId = byteBuffer.get();
        byte receiverId = byteBuffer.get();
        int latticeRound = byteBuffer.getInt();
        byte ack = byteBuffer.get();
        Set<Integer> proposals = new HashSet<>();
        for (int i = 0; i < proposalSetSize; i++) {
            int proposal = byteBuffer.getInt();
            if(proposal != 0){
                proposals.add(proposal);
            }
        }
        return new Message(id, senderId, receiverId, latticeRound, ack, proposals);
    }

    public Message swapSenderReceiver(){
        return new Message(this.id, this.receiverId, this.senderId, this.latticeRound, (byte)0,this.copyProposals());
    }

    // Copy message
    public Message copy(){
        return new Message(this.id, this.senderId, this.receiverId, this.latticeRound, this.ack, this.copyProposals());
    }

    public String printSet(){
        StringBuilder sb = new StringBuilder();
        for (int proposal : proposals) {
            sb.append(proposal).append(" ");
        }
        return sb.toString();
    }
}
