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
    private final HashSet<Byte> delivered;
    private final HashMap<Byte, Host> hosts;
    private final byte myId;
    private int ackCount;
    private int nackCount;

    public PerfectLinks(int port,
                        byte myId,
                        LatticeDeliverer deliverer,
                        HashMap<Byte, Host> hosts,
                        int proposalSetSize) {
        this.stubbornLinks = new StubbornLinks(port, hosts, this, proposalSetSize);
        this.hosts = hosts;
        this.ackCount = 0;
        this.nackCount = 0;
        this.myId = myId;
        this.deliverer = deliverer;
        delivered = new HashSet<>();
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
    public int getCurrentRound(){
        return deliverer.getLatticeRound();
    }

    @Override
    public void deliver(Message message) {
        if(message.getLatticeRound() == deliverer.getLatticeRound()){ // The message I got is from the same round
            if(message.isAckMessage() && message.getId() == deliverer.getActiveProposalNumber()){
                if(delivered.add(message.getSenderId())) {
                    if (message.getAck() == 1) {
                        ackCount++;
                    }
                    else if (message.getAck() == 2) {
                        nackCount++;
                        deliverer.updateCurrentProposal(message.getProposals());
                    }
                    if(ackCount + nackCount >= hosts.size()/2){
                        delivered.clear();
                        stubbornLinks.clearSendArray();
                        if(nackCount == 0){
                            deliverer.decide();
                        }
                        else {
                            deliverer.broadcastNewProposal();
                        }
                        ackCount = 0;
                        nackCount = 0;
                    }
                }
            }
            else{
                // Compare the message's proposal set to the current proposal set
                // If the message's proposal set is a subset of the current proposal set, then send an ACK
                for(int proposal : message.getProposals()){
                    if(!deliverer.getCurrentProposal().contains(proposal)){
                        // Send a NACK
                        deliverer.updateCurrentProposal(message.getProposals());
                        send(new Message(message.getId(), myId, message.getSenderId(), message.getLatticeRound(), (byte)2, deliverer.getCurrentProposal()), hosts.get(message.getSenderId()));
                        return;
                    }
                }
                // Send an ACK
                deliverer.updateCurrentProposal(message.getProposals());
                send(new Message(message.getId(), myId, message.getSenderId(), message.getLatticeRound(), (byte)1, deliverer.getCurrentProposal()), hosts.get(message.getSenderId()));
            }
        }
        else{
            if(message.isAckMessage()) {
                if(message.getLatticeRound() > deliverer.getLatticeRound()){
                    // Because I am behind, I haven't yet sent proposals for this round, thus it should be impossible for me to receive an ACK message
                    System.out.println("SHOULD NEVER HAPPEN" + message);
                }
                // Because I am ahead, I have already decided on a proposal for this round, thus I don't care for the ACK or NACK messages I receive after I have decided.
                return;
            }
            // If we get here the message I receive is a proposal and is from a different round
            if(message.getLatticeRound() > deliverer.getLatticeRound()) { // It's from a future round, so we save it for now and wait for the round to come, don't forget to send an ACK message
                deliverer.getProposal(message.getLatticeRound()).addAll(message.getProposals());
                send(new Message(message.getId(), myId, message.getSenderId(), message.getLatticeRound(), (byte)1, deliverer.getProposal(message.getLatticeRound())), hosts.get(message.getSenderId()));
            }
            if(message.getLatticeRound() < deliverer.getLatticeRound()){ // It's from a previous round,
                for(int proposal : message.getProposals()){
                    if(!deliverer.getProposal(message.getLatticeRound()).contains(proposal)){
                        // Send a NACK
                        deliverer.getProposal(message.getLatticeRound()).addAll(message.getProposals());
                        send(new Message(message.getId(), myId, message.getSenderId(), message.getLatticeRound(), (byte)2, deliverer.getProposal(message.getLatticeRound())), hosts.get(message.getSenderId()));
                        return;
                    }
                }
                // Send an ACK
                deliverer.getProposal(message.getLatticeRound()).addAll(message.getProposals());
                send(new Message(message.getId(), myId, message.getSenderId(), message.getLatticeRound(), (byte)1, deliverer.getProposal(message.getLatticeRound())), hosts.get(message.getSenderId()));
            }
        }
    }
}
