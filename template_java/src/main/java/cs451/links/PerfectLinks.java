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

    public PerfectLinks(int port,
                        UniformDeliverer deliverer,
                        HashMap<Byte, Host> hosts,
                        int slidingWindowSize,
                        boolean extraMemory,
                        Acknowledger acknowledger,
                        int messageCount) {
        this.stubbornLinks = new StubbornLinks(port, hosts, this, hosts.size(), slidingWindowSize, extraMemory, acknowledger, messageCount);
        this.slidingWindowSize = slidingWindowSize;
        this.hosts = hosts;
        this.uniformDeliverer = deliverer;
        delivered = new HashMap[hosts.size()];
        this.slidingWindowStart = new int[hosts.size()];
        for(int i = 0; i < hosts.size(); i++){
            this.slidingWindowStart[i] = 0;
            this.delivered[i] = new HashMap<>();
        }
    }

    public void send(Message message, Host host){
        stubbornLinks.send(message, host);
    }

    public void stop(){
        stubbornLinks.stop();
    }

    public void stopSenders() {
        stubbornLinks.stopSenders();
    }

    public void start(){
        stubbornLinks.start();
    }

    @Override
    public void deliver(Message message) {
        if(message.getId() <= slidingWindowStart[message.getOriginalSender()]){
            send(new Message(message, message.getReceiverId(), message.getSenderId()), hosts.get(message.getSenderId())); // Send ACK message
            if(message.getOriginalSender() != message.getSenderId()){
                send(new Message(message, message.getReceiverId(), message.getOriginalSender()), hosts.get(message.getOriginalSender())); // Send ACK message
            }
        }
        else if(message.getId() > slidingWindowStart[message.getOriginalSender()] && message.getId() <= slidingWindowStart[message.getOriginalSender()] + slidingWindowSize){
            send(new Message(message, message.getReceiverId(), message.getSenderId()), hosts.get(message.getSenderId())); // Send ACK message
            if(message.getOriginalSender() != message.getSenderId()){
                send(new Message(message, message.getReceiverId(), message.getOriginalSender()), hosts.get(message.getOriginalSender())); // Send ACK message
            }
            delivered[message.getOriginalSender()].computeIfAbsent(message.getId(), k -> new HashSet<>());
            if(delivered[message.getOriginalSender()].get(message.getId()).add(message.getSenderId())){
                if(delivered[message.getOriginalSender()].get(message.getId()).size() == 1){
                    uniformDeliverer.deliver(message); // First time getting the message
                }
                // Check if this process has delivered all the messages in the sliding window
                else if(delivered[message.getOriginalSender()].get(message.getId()).size() < (hosts.size()/2)+1) return;
                if(readyToSlide(message.getOriginalSender())){
                    System.out.println("Sliding window for process " + message.getOriginalSender());
                    // If yes, then increment the sliding window start
                    slidingWindowStart[message.getOriginalSender()] += slidingWindowSize;
                    // Remove all the messages from the delivered map
                    for(int messageId : delivered[message.getOriginalSender()].keySet()){
                        uniformDeliverer.uniformDeliver(message.getOriginalSender(),messageId);
                        delivered[message.getOriginalSender()].get(messageId).clear();
                    }
                    delivered[message.getOriginalSender()].clear();
                    System.gc();
                }
            }
        }
    }

    private void printSlidingWindows(){
        System.out.print("{ ");
        for(int i = 0; i < hosts.size(); i++){
            System.out.print(i + ": " + slidingWindowStart[i] + " | ");
        }
        System.out.print(" }\n");
    }

    private boolean readyToSlide(byte originalSender){
        if(delivered[originalSender].size() < slidingWindowSize){ // We haven't received all the messages in the sliding window
            return false;
        }
        // Check if for all messages I've received in the sliding window, I've received at least hostSize/2 ACKs.
        for(int messageId: delivered[originalSender].keySet()){
            if(delivered[originalSender].get(messageId).size() < (hosts.size()/2)+1){
                return false;
            }
        }
        return true;
    }
}
