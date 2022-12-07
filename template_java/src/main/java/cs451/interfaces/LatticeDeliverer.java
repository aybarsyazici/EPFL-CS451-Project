package cs451.interfaces;

import java.util.Set;

public interface LatticeDeliverer {
    void decide();
    int getActiveProposalNumber();
    Set<Integer> getCurrentProposal();
    Set<Integer> getProposal(int latticeRound);
    void updateProposals(Set<Integer> proposals);
    void broadcastNewProposal();
    int getLatticeRound();

}
