package cs451.broadcast;

import cs451.Process;
import cs451.interfaces.*;
import cs451.Host;
import cs451.Message.Message;
import cs451.links.PerfectLinks;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicIntegerArray;

public class BestEffortBroadcast implements LatticeDeliverer {
    private final PerfectLinks perfectLinks;
    private final byte id;
    private final List<Host> hosts;
    private final Process process;
    private int activeProposalNumber;
    private int currentlatticeRound;
    private Set<Integer>[] proposals;

    public BestEffortBroadcast(byte id,
                               int port,
                               List<Host> hostList,
                               Process process,
                               int proposalSetSize,
                               int latticeRoundCount) {
        this.process = process;
        this.id = id;
        this.activeProposalNumber = 0;
        this.currentlatticeRound = 0;
        this.hosts = hostList;
        this.proposals = new Set[latticeRoundCount];
        for(int i = 0; i < latticeRoundCount; i++){
            this.proposals[i] = new HashSet<>();
        }
        HashMap<Byte,Host> hostMap = new HashMap<>();
        for(Host host : hostList){
            hostMap.put((byte)host.getId(), host);
        }
        this.perfectLinks = new PerfectLinks(port, id, this,
                hostMap,false,
                proposalSetSize);
    }

    public void broadcast(Set<Integer> proposals){
        // Iterate over all hosts
        this.activeProposalNumber++;
        this.proposals[this.currentlatticeRound].addAll(proposals);
        for(Host host : this.hosts){
            // Send message to all hosts
            if(host.getId() == id) continue; // Don't send it to yourself
            perfectLinks.send(new Message(this.activeProposalNumber, id, (byte)host.getId(), this.currentlatticeRound, this.proposals[this.currentlatticeRound]), host);
        }
    }
    public void start(){
        perfectLinks.start();
    }
    public void stop(){
        perfectLinks.stop();
    }
    @Override
    public void decide() {
        process.deliver(currentlatticeRound, proposals[currentlatticeRound]);
        activeProposalNumber = 0;
        currentlatticeRound++; // Move to the next round
    }
    @Override
    public int getActiveProposalNumber() {
        return this.activeProposalNumber;
    }
    @Override
    public Set<Integer> getCurrentProposal() {
        return this.proposals[currentlatticeRound];
    }
    @Override
    public Set<Integer> getProposal(int latticeRound) {
        return this.proposals[latticeRound];
    }
    @Override
    public void updateProposals(Set<Integer> proposals) {
        this.proposals[currentlatticeRound].addAll(proposals);
    }

    @Override
    public void broadcastNewProposal() {
        // Iterate over all hosts
        this.activeProposalNumber++;
        for(Host host : this.hosts){
            // Send message to all hosts
            if(host.getId() == id) continue; // Don't send it to yourself
            perfectLinks.send(new Message(this.activeProposalNumber, id, (byte)host.getId(), this.currentlatticeRound, this.proposals[this.currentlatticeRound]), host);
        }
    }

    @Override
    public int getLatticeRound() {
        return this.currentlatticeRound;
    }
}
