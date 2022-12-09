package cs451.Message;

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
    // 3 will mean that this processing is telling that he has delivered the given lattice round
    // 4 will be the ack message associated with 3
    private Set<Integer> proposals;
    private List<Integer> receivedProposals;
    private final int hashCode;

    public Message(int id, byte senderId, byte receiverId, int latticeRound, boolean ack){
        this.id = id;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.latticeRound = latticeRound;
        this.ack = ack ? (byte)4 : (byte)3;
        this.hashCode = Objects.hash(this.id, this.latticeRound, this.senderId, this.receiverId, this.ack);
    }

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

    public Message(int id, byte senderId, byte receiverId, int latticeRound, byte ack, List<Integer> receivedProposals) {
        this.id = id;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.latticeRound = latticeRound;
        this.ack = ack;
        this.receivedProposals = receivedProposals;
        this.hashCode = Objects.hash(this.id, this.latticeRound, this.senderId, this.receiverId, this.ack);
    }

    public Set<Integer> getProposals() {
        return proposals;
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
        return this.ack == 1 || this.ack == 2;
    }

    public Boolean isDeliveredMessage(){
        return this.ack == 3;
    }
    public Boolean isDeliveredAck(){
        return this.ack == 4;
    }

    public byte getAck(){
        return this.ack;
    }

    public List<Integer> getReceivedProposals() {
        return receivedProposals;
    }

    public void setReceivedProposals(List<Integer> receivedProposals) {
        this.receivedProposals = receivedProposals;
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
                ", proposals="  + ((proposals != null) ? proposals : receivedProposals ) +
                '}';
    }

    public String printWithoutSet(){
        return "Message{" +
                "proposalCount=" + id +
                ", senderId=" + senderId +
                ", receiverId=" + receiverId +
                ", latticeRound=" + latticeRound +
                ", isAck=" + ack +
                '}';
    }

    // Convert object to byteArray
    public byte[] toByteArray(int proposalSetSize) {
        ByteBuffer buffer = ByteBuffer.allocate(4 + 4 + 1 + 1 + 1 + proposalSetSize * 4);
        buffer.order(ByteOrder.BIG_ENDIAN);
        buffer.putInt(id);
        buffer.putInt(latticeRound);
        buffer.put(senderId);
        buffer.put(receiverId);
        buffer.put(ack);
        if(proposals != null){
            for (Integer proposal : proposals) {
                buffer.putInt(proposal);
            }
        }
        else if(receivedProposals != null){
            for (Integer proposal : receivedProposals) {
                buffer.putInt(proposal);
            }
        }
        return buffer.array();
    }

    // Convert byteArray to object
    public static Message fromByteArray(byte[] bytes, int proposalSetSize) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        buffer.order(ByteOrder.BIG_ENDIAN);
        int id = buffer.getInt();
        int latticeRound = buffer.getInt();
        byte senderId = buffer.get();
        byte receiverId = buffer.get();
        byte ack = buffer.get();
        List<Integer> proposals = new ArrayList<>();
        for (int i = 0; i < proposalSetSize; i++) {
            int value = buffer.getInt();
            if(value != 0){
                proposals.add(value);
            }
        }
        return new Message(id, senderId, receiverId, latticeRound, ack, proposals);
    }

    public Message swapSenderReceiver(){
        var ack = this.isAckMessage() ? (byte)0 : (byte)3;
        return new Message(this.id, this.receiverId, this.senderId, this.latticeRound, ack, this.getProposals());
    }

    public String printSet(){
        StringBuilder sb = new StringBuilder();
        for (int proposal : proposals) {
            sb.append(proposal).append(" ");
        }
        return sb.toString();
    }
}
