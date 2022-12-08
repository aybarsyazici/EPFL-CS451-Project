package cs451.interfaces;

import java.util.Set;

public interface LatticeDeliverer {
    void decide();
    int getActiveProposalNumber();
    Set<Integer> getCurrentProposal();
    Set<Integer> getCopyOfCurrentProposal();
    Set<Integer> getCopyOfProposal(int latticeRound);
    Set<Integer> getProposal(int latticeRound);
    void updateCurrentProposal(Set<Integer> proposals);
    void setProposals(Set<Integer> proposals, int latticeRound);
    void broadcastNewProposal();
    int getLatticeRound();

}
