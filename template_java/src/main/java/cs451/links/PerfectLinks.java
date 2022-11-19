package cs451.links;

import cs451.interfaces.Acknowledger;
import cs451.interfaces.Deliverer;
import cs451.Host;
import cs451.Message.Message;
import cs451.interfaces.UniformDeliverer;

import java.util.*;

public class PerfectLinks implements Deliverer {
    private final StubbornLinks stubbornLinks;
    private final UniformDeliverer uniformDeliverer;
    private final HashMap<Integer, Set<Byte>>[] delivered;
    private int[] slidingWindowStart;
    private final int slidingWindowSize;
    private final HashMap<Byte, Host> hosts;
    private final byte myId;

    public PerfectLinks(int port,
                        byte myId,
                        UniformDeliverer deliverer,
                        HashMap<Byte, Host> hosts,
                        int slidingWindowSize,
                        boolean extraMemory,
                        Acknowledger acknowledger,
                        int messageCount) {
        this.stubbornLinks = new StubbornLinks(port, hosts, this, hosts.size(), slidingWindowSize, extraMemory, acknowledger, messageCount);
        this.slidingWindowSize = slidingWindowSize;
        this.hosts = hosts;
        this.myId = myId;
        this.uniformDeliverer = deliverer;
        delivered = new HashMap[hosts.size()];
        this.slidingWindowStart = new int[hosts.size()];
        for (int i = 0; i < hosts.size(); i++) {
            this.slidingWindowStart[i] = 0;
            this.delivered[i] = new HashMap<>();
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
    public void deliver(Message message) {
        if (message.getId() <= slidingWindowStart[message.getOriginalSender()]) { // Message has already been delivered by this process
            // Inform the sender that the message has been delivered but don't deliver it again.
            send(new Message(message, message.getReceiverId(), message.getSenderId()), hosts.get(message.getSenderId())); // Send ACK message
            if(message.getSenderId() != message.getOriginalSender() && message.getOriginalSender() != myId){
                send(new Message(message, message.getReceiverId(), message.getOriginalSender()), hosts.get(message.getOriginalSender())); // Send ACK message
            }
        }
        if (message.getId() > slidingWindowStart[message.getOriginalSender()] && message.getId() <= slidingWindowStart[message.getOriginalSender()] + slidingWindowSize) {
            send(new Message(message, message.getReceiverId(), message.getSenderId()), hosts.get(message.getSenderId())); // Send ACK message
            if(message.getSenderId() != message.getOriginalSender() && message.getOriginalSender() != myId){
                send(new Message(message, message.getReceiverId(), message.getOriginalSender()), hosts.get(message.getOriginalSender())); // Send ACK message
            }
            delivered[message.getOriginalSender()].computeIfAbsent(message.getId(), k -> new HashSet<>());
            // For each process(original sender) there is one HashMap, where the key is the message id and the value is a set of hosts that have seen this message.
            if (delivered[message.getOriginalSender()].get(message.getId()).add(message.getSenderId())) {
                if (delivered[message.getOriginalSender()].get(message.getId()).size() == 1) {
                    if(message.getOriginalSender() != myId){
                        uniformDeliverer.deliver(message); // First time getting the message
                    }
                    if(message.getOriginalSender() != message.getSenderId()){
                        delivered[message.getOriginalSender()].get(message.getId()).add(message.getOriginalSender());
                    }
                    delivered[message.getOriginalSender()].get(message.getId()).add(myId); // I also have the message now
                }
                if (delivered[message.getOriginalSender()].get(message.getId()).size() == (hosts.size() / 2) + 1) {
                    // If the number of hosts that have seen the message is greater than the number of hosts/2 + 1
                    // Then it's safe to deliver the message
                    uniformDeliverer.uniformDeliver(message);
                }
                slideWindow(message.getOriginalSender());
            }
        }
    }

    private void printSlidingWindows() {
        System.out.print("{ ");
        for (int i = 0; i < hosts.size(); i++) {
            System.out.print(i + ": " + slidingWindowStart[i] + " | ");
        }
        System.out.print(" }\n");
    }

    public void slideWindow(byte originalSender){
        if (readyToSlide(originalSender)) { // Can we slide the window for this process?
            // If yes, then increment the sliding window start
            slidingWindowStart[originalSender] += slidingWindowSize;
            // printSlidingWindows();
            // Remove all the messages from the delivered map
            for (int messageId : delivered[originalSender].keySet()) {
                delivered[originalSender].get(messageId).clear();
            }
            delivered[originalSender].clear();
            System.gc();
        }
    }

    private boolean readyToSlide(byte originalSender) {
        if (delivered[originalSender].size() < slidingWindowSize) { // We haven't received all the messages in the sliding window
            return false;
        }
        if(originalSender != myId && stubbornLinks.getRebroadcastCount(originalSender) >= hosts.size()*slidingWindowSize ){
            return false; // We still have a lot of rebroadcast messages to deliver to this process, wait for them to be delivered to save memory
        }
        // We have received all the messages. But are they all delivered? (i.e. have we received an ACK from at least half the hosts)
        for (int messageId : delivered[originalSender].keySet()) {
            if (delivered[originalSender].get(messageId).size() < (hosts.size() / 2) + 1) {
                return false; // This message is yet to be delivered!
            }
        }
        return true;
    }

    public HashMap<Integer, Set<Byte>> getDelivered(byte origSender){
        return this.delivered[origSender];
    }
}
