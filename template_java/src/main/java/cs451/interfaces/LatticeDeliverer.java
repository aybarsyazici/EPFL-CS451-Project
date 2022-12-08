package cs451.interfaces;

import java.util.Set;

public interface LatticeDeliverer {
    void decide(int latticeRound);
    int getActiveProposalNumber(int latticeRound);
    Set<Integer> getCopyOfProposal(int latticeRound);
    Set<Integer> getProposal(int latticeRound);
    void setProposals(Set<Integer> proposals, int latticeRound);
    void broadcastNewProposal(int round);
    int getMaxLatticeRound();
    int getMinLatticeRound();
    boolean isDelivered(int latticeRound);
    void incrementDeliveredAck(int latticeRound, byte senderId);
}
