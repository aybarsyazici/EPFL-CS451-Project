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
    private final int parallelRoundCount;
    private final List<Host> hosts;
    private final Process process;
    private final AtomicIntegerArray activeProposalNumber;
    private final AtomicInteger maxLatticeRound;
    private final AtomicInteger lastDecidedLatticeRound;
    private final boolean readyToDeliver[];
    private final boolean selfDelivered[];
    private final Set<Integer>[] proposals;
    private final Set<Integer>[] acceptedValues;
    private final Set<Byte>[] proposalDeliveredAcks;
    private final int latticeRoundCount;


    public BestEffortBroadcast(byte id,
                               int port,
                               List<Host> hostList,
                               Process process,
                               int parallelRoundCount,
                               int proposalSetSize,
                               int latticeRoundCount) {
        this.process = process;
        this.id = id;
        this.latticeRoundCount = latticeRoundCount;
        this.activeProposalNumber = new AtomicIntegerArray(parallelRoundCount);
        this.maxLatticeRound = new AtomicInteger(parallelRoundCount);
        this.parallelRoundCount = parallelRoundCount;
        this.lastDecidedLatticeRound = new AtomicInteger(0);
        this.hosts = hostList;
        this.readyToDeliver = new boolean[latticeRoundCount];
        this.selfDelivered = new boolean[latticeRoundCount];
        this.proposalDeliveredAcks = new HashSet[latticeRoundCount];
        this.acceptedValues = new Set[latticeRoundCount];
        this.proposals = new Set[parallelRoundCount];
        for(int i = 0; i < parallelRoundCount; i++){
            this.proposals[i] = ConcurrentHashMap.newKeySet();
        }
        for(int i = 0; i < latticeRoundCount; i++){
            this.readyToDeliver[i] = false;
            this.proposalDeliveredAcks[i] = new HashSet<>();
            this.acceptedValues[i] = ConcurrentHashMap.newKeySet();
        }
        HashMap<Byte,Host> hostMap = new HashMap<>();
        for(Host host : hostList){
            hostMap.put((byte)host.getId(), host);
        }
        this.perfectLinks = new PerfectLinks(port, id, this,
                hostMap,
                parallelRoundCount,
                proposalSetSize);
    }

    public void broadcast(int round, Set<Integer> proposals){
        // Iterate over all hosts
        var proposalNumber = activeProposalNumber.incrementAndGet(round%parallelRoundCount);
        this.proposals[round%parallelRoundCount].addAll(proposals);
        this.proposals[round%parallelRoundCount].addAll(this.acceptedValues[round]);
        var currentProposals = this.getProposal(round);
        this.acceptedValues[round].addAll(currentProposals);
        System.out.println("Broadcasting proposal " + proposalNumber + " round " + round + " with " + this.proposals[round%parallelRoundCount].size() + " elements");
        System.out.println("__________________________");
        for(Host host : this.hosts){
            // Send message to all hosts
            if(host.getId() == id) continue; // Don't send it to yourself
            perfectLinks.send(new Message(proposalNumber, id, (byte)host.getId(), round, currentProposals), host);
        }
    }

    public void broadcastDelivered(int round){
        System.out.println("Broadcasting DELIVERY OF: " + round);
        for(Host host: this.hosts){
            if(host.getId() == id) continue;
            perfectLinks.send(new Message(1,id,(byte)host.getId(),round,false),host);
        }
    }

    public void start(){
        perfectLinks.start();
    }
    public void stop(){
        perfectLinks.stop();
    }
    @Override
    public void decide(int latticeRound) {
        activeProposalNumber.set(latticeRound%parallelRoundCount,0);
        this.readyToDeliver[latticeRound] = true;
        while(lastDecidedLatticeRound.get() < latticeRoundCount && readyToDeliver[lastDecidedLatticeRound.get()]){
            var round = lastDecidedLatticeRound.getAndIncrement();
            process.deliver(round, getProposal(round));
            this.selfDelivered[round] = true;
            broadcastDelivered(round);
            this.proposals[round%parallelRoundCount].clear();
            checkForDeletion();
            maxLatticeRound.incrementAndGet(); // Allow for next round to be broadcast.
        }
    }
    @Override
    public int getActiveProposalNumber(int latticeRound) {
        return this.activeProposalNumber.get(latticeRound % parallelRoundCount);
    }

    @Override
    public Set<Integer> getCopyOfProposal(int latticeRound){
        return Set.copyOf(this.proposals[latticeRound%parallelRoundCount]);
    }

    @Override
    public Set<Integer> getProposal(int latticeRound) {
        return this.proposals[latticeRound%parallelRoundCount];
    }
    @Override
    public void setProposals(Set<Integer> proposals, int latticeRound) {
        this.proposals[latticeRound%parallelRoundCount] = proposals;
    }
    @Override
    public void broadcastNewProposal(int latticeRound) {
        // Iterate over all hosts
        var proposalNumber = activeProposalNumber.incrementAndGet(latticeRound%parallelRoundCount);
        this.proposals[latticeRound%parallelRoundCount].addAll(this.acceptedValues[latticeRound]);
        var currentProposals = this.getProposal(latticeRound);
        this.acceptedValues[latticeRound].addAll(currentProposals);
        System.out.println("Broadcasting NEW proposal " + proposalNumber + " round " + latticeRound + " with " + currentProposals.size() + " elements");
        System.out.println("__________________________");
        for(Host host : this.hosts){
            // Send message to all hosts
            if(host.getId() == id) continue; // Don't send it to yourself
            perfectLinks.send(new Message(proposalNumber, id, (byte)host.getId(), latticeRound, currentProposals), host);
        }
    }

    @Override
    public int getMaxLatticeRound() {
        return this.maxLatticeRound.get();
    }
    @Override
    public int getMinLatticeRound() {
        return this.lastDecidedLatticeRound.get();
    }

    @Override
    public boolean isDelivered(int latticeRound){
        return this.readyToDeliver[latticeRound];
    }

    @Override
    public void incrementDeliveredAck(int latticeRound, byte senderId){
        if(this.proposalDeliveredAcks[latticeRound] != null && this.proposalDeliveredAcks[latticeRound].add(senderId)){
            checkForDeletion();
        }
    }
    @Override
    public Set<Integer> getAcceptedValue(int latticeRound){
        return this.acceptedValues[latticeRound];
    }

    private void checkForDeletion(){
        var round = 0;
        while(round < lastDecidedLatticeRound.get()){
            if(selfDelivered[round] && this.proposalDeliveredAcks[round] != null && this.proposalDeliveredAcks[round].size() == hosts.size() - 1){
                System.out.println("Cleared accepted value set for round " + round);
                this.acceptedValues[round].clear();
                this.acceptedValues[round] = null;
                this.proposalDeliveredAcks[round].clear();
                this.proposalDeliveredAcks[round] = null;
            }
            round++;
        }
    }
}
