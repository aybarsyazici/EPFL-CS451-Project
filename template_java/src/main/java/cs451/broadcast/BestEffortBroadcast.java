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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;

public class BestEffortBroadcast implements LatticeDeliverer {
    private final PerfectLinks perfectLinks;
    private final byte id;
    private final List<Host> hosts;
    private final Process process;
    private AtomicInteger activeProposalNumber;
    private AtomicInteger currentlatticeRound;
    private Set<Integer>[] proposals;

    public BestEffortBroadcast(byte id,
                               int port,
                               List<Host> hostList,
                               Process process,
                               int proposalSetSize,
                               int latticeRoundCount) {
        this.process = process;
        this.id = id;
        this.activeProposalNumber = new AtomicInteger(0);
        this.currentlatticeRound = new AtomicInteger(0);
        this.hosts = hostList;
        this.proposals = new Set[latticeRoundCount];
        for(int i = 0; i < latticeRoundCount; i++){
            this.proposals[i] = ConcurrentHashMap.newKeySet();
        }
        HashMap<Byte,Host> hostMap = new HashMap<>();
        for(Host host : hostList){
            hostMap.put((byte)host.getId(), host);
        }
        this.perfectLinks = new PerfectLinks(port, id, this,
                hostMap,
                proposalSetSize);
    }

    public void broadcast(Set<Integer> proposals){
        // Iterate over all hosts
        var proposalNumber = activeProposalNumber.incrementAndGet();
        var latticeRound = currentlatticeRound.get();
        this.proposals[latticeRound].addAll(proposals);
        System.out.println("Broadcasting proposal " + proposalNumber + " round " + latticeRound + " with " + this.proposals[latticeRound].size() + " elements");
        System.out.println("__________________________");
        var currentProposals = this.getProposal(latticeRound);
        for(Host host : this.hosts){
            // Send message to all hosts
            if(host.getId() == id) continue; // Don't send it to yourself
            perfectLinks.send(new Message(proposalNumber, id, (byte)host.getId(), latticeRound, currentProposals), host);
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
        activeProposalNumber.set(0);
        var round = currentlatticeRound.getAndIncrement();         // Move to the next round
        process.deliver(round, this.proposals[round]);
    }
    @Override
    public int getActiveProposalNumber() {
        return this.activeProposalNumber.get();
    }
    @Override
    public Set<Integer> getCurrentProposal() {
        return this.proposals[currentlatticeRound.get()];
    }

    @Override
    public Set<Integer> getCopyOfCurrentProposal(){
        return Set.copyOf(this.proposals[currentlatticeRound.get()]);
    }

    @Override
    public Set<Integer> getCopyOfProposal(int latticeRound){
        return Set.copyOf(this.proposals[latticeRound]);
    }

    @Override
    public Set<Integer> getProposal(int latticeRound) {
        return this.proposals[latticeRound];
    }
    @Override
    public void updateCurrentProposal(Set<Integer> proposals) {
        this.proposals[currentlatticeRound.get()].addAll(proposals);
    }
    @Override
    public void setProposals(Set<Integer> proposals, int latticeRound) {
        this.proposals[latticeRound] = proposals;
    }
    @Override
    public void broadcastNewProposal() {
        // Iterate over all hosts
        var proposalNumber = activeProposalNumber.incrementAndGet();
        var latticeRound = currentlatticeRound.get();
        System.out.println("Broadcasting NEW proposal " + proposalNumber + " round " + latticeRound + " with " + this.proposals[proposalNumber].size() + " elements");
        System.out.println("__________________________");
        var currentProposals = this.getProposal(latticeRound);
        for(Host host : this.hosts){
            // Send message to all hosts
            if(host.getId() == id) continue; // Don't send it to yourself
            perfectLinks.send(new Message(proposalNumber, id, (byte)host.getId(), latticeRound, currentProposals), host);
        }
    }

    @Override
    public int getLatticeRound() {
        return this.currentlatticeRound.get();
    }
}
