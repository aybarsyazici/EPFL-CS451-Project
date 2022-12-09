package cs451.links;

import cs451.interfaces.Acknowledger;
import cs451.interfaces.Deliverer;
import cs451.Host;
import cs451.Message.Message;
import cs451.interfaces.LatticeDeliverer;
import cs451.interfaces.UniformDeliverer;

import java.util.*;

public class PerfectLinks implements Deliverer {
    private final StubbornLinks stubbornLinks;
    private final LatticeDeliverer deliverer;
    private final HashSet<Byte>[] delivered;
    private final HashSet<Integer> comparisonSet;
    private final int parallelRoundCount;
    private final HashMap<Byte, Host> hosts;
    private final byte myId;
    private int[] ackCount;
    private int[] nackCount;

    public PerfectLinks(int port,
                        byte myId,
                        LatticeDeliverer deliverer,
                        HashMap<Byte, Host> hosts,
                        int parallelRoundCount,
                        int proposalSetSize) {
        this.stubbornLinks = new StubbornLinks(port, hosts, this, proposalSetSize);
        this.hosts = hosts;
        this.comparisonSet = new HashSet<>();
        this.parallelRoundCount = parallelRoundCount;
        this.ackCount = new int[parallelRoundCount];
        this.nackCount = new int[parallelRoundCount];
        this.myId = myId;
        this.deliverer = deliverer;
        delivered = new HashSet[parallelRoundCount];
        for(int i = 0; i < parallelRoundCount; i++){
            delivered[i] = new HashSet<>();
        }
    }

    public void send(Message message, Host host) {
        stubbornLinks.send(message, host);
    }

    public void stop() {
        stubbornLinks.stop();
    }

    public void stopSenders() {
        stubbornLinks.stopSenders();
    }

    public void start() {
        stubbornLinks.start();
    }

    @Override
    public int getMaxLatticeRound(){
        return deliverer.getMaxLatticeRound();
    }

    public boolean isDelivered(int latticeRound){
        return deliverer.isDelivered(latticeRound);
    }

    @Override
    public void deliver(Message message) {
        try {
            if (message.isDeliveredMessage()) {
                deliverer.incrementDeliveredAck(message.getLatticeRound(), message.getSenderId());
                send(new Message(1, myId, message.getSenderId(), message.getLatticeRound(), true), hosts.get(message.getSenderId()));
            }
            else if (deliverer.getMinLatticeRound() <= message.getLatticeRound() && message.getLatticeRound() < deliverer.getMaxLatticeRound()) {
                if (message.isAckMessage()) {
                    if (message.getId() != deliverer.getActiveProposalNumber(message.getLatticeRound())) {
                        return;
                    }
                    if (isDelivered(message.getLatticeRound())) return;
                    if (delivered[message.getLatticeRound() % parallelRoundCount].add(message.getSenderId())) {
                        if (message.getAck() == 1) {
                            ackCount[message.getLatticeRound() % parallelRoundCount]++;
                        } else if (message.getAck() == 2) {
                            nackCount[message.getLatticeRound() % parallelRoundCount]++;
                            deliverer.getProposal(message.getLatticeRound()).addAll(message.getReceivedProposals());
                        }
                        if (ackCount[message.getLatticeRound() % parallelRoundCount] + nackCount[message.getLatticeRound() % parallelRoundCount] >= hosts.size() / 2) {
                            delivered[message.getLatticeRound() % parallelRoundCount].clear();
                            if (nackCount[message.getLatticeRound() % parallelRoundCount] == 0) {
                                deliverer.decide(message.getLatticeRound());
                            } else {
                                deliverer.broadcastNewProposal(message.getLatticeRound());
                            }
                            ackCount[message.getLatticeRound() % parallelRoundCount] = 0;
                            nackCount[message.getLatticeRound() % parallelRoundCount] = 0;
                        }
                    }
                }
                else{
                    // Compare the message's proposal set to the current proposal set
                    // If the message's proposal set is a subset of the current accepted value set, then send an ACK
                    this.comparisonSet.addAll(message.getReceivedProposals());
                    if(comparisonSet.containsAll(deliverer.getAcceptedValue(message.getLatticeRound()))){
                        // Send an ACK
                        deliverer.getAcceptedValue(message.getLatticeRound()).addAll(message.getReceivedProposals());
                        send(new Message(message.getId(), myId, message.getSenderId(), message.getLatticeRound(), (byte) 1, deliverer.getAcceptedValue(message.getLatticeRound())), hosts.get(message.getSenderId()));
                    }
                    else{
                        // Send a NACK
                        deliverer.getAcceptedValue(message.getLatticeRound()).addAll(message.getReceivedProposals());
                        send(new Message(message.getId(), myId, message.getSenderId(), message.getLatticeRound(), (byte) 2, deliverer.getAcceptedValue(message.getLatticeRound())), hosts.get(message.getSenderId()));
                    }
                    this.comparisonSet.clear();
                }
            }
            else {
                if (message.isAckMessage()) {
                    if (message.getLatticeRound() >= deliverer.getMaxLatticeRound()) {
                        // Because I am behind, I haven't yet sent proposals for this round, thus it should be impossible for me to receive an ACK message
                        System.out.println("SHOULD NEVER HAPPEN" + message);
                    }
                    // Because I am ahead, I have already decided on a proposal for this round, thus I don't care for the ACK or NACK messages I receive after I have decided.
                    return;
                }
                // Compare the message's proposal set to the current proposal set
                // If the message's proposal set is a subset of the current accepted value set, then send an ACK
                this.comparisonSet.addAll(message.getReceivedProposals());
                if(comparisonSet.containsAll(deliverer.getAcceptedValue(message.getLatticeRound()))){
                    // Send an ACK
                    deliverer.getAcceptedValue(message.getLatticeRound()).addAll(message.getReceivedProposals());
                    send(new Message(message.getId(), myId, message.getSenderId(), message.getLatticeRound(), (byte) 1, deliverer.getAcceptedValue(message.getLatticeRound())), hosts.get(message.getSenderId()));
                }
                else{
                    // Send a NACK
                    deliverer.getAcceptedValue(message.getLatticeRound()).addAll(message.getReceivedProposals());
                    send(new Message(message.getId(), myId, message.getSenderId(), message.getLatticeRound(), (byte) 2, deliverer.getAcceptedValue(message.getLatticeRound())), hosts.get(message.getSenderId()));
                }
                this.comparisonSet.clear();
            }
        }
        catch (Exception e){
            e.printStackTrace();
            System.out.println("ERROR IN PERFECT LINKS DELIVER: " + message.printWithoutSet());
        }
    }
}
