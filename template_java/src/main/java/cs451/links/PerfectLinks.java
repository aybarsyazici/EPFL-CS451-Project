package cs451.links;

import cs451.Acknowledger;
import cs451.Deliverer;
import cs451.Host;
import cs451.Message.Message;

import java.util.*;

public class PerfectLinks implements Deliverer {
    private final StubbornLinks stubbornLinks;
    private final Deliverer deliverer;
    private final HashSet<Integer>[] delivered;

    private int[] slidingWindowStart;
    private final int slidingWindowSize;
    private int[] deliveredMessageCount;
    private final HashMap<Byte, Host> hosts;

    public PerfectLinks(int port,
                        Deliverer deliverer,
                        HashMap<Byte, Host> hosts,
                        int slidingWindowSize,
                        boolean extraMemory,
                        Acknowledger acknowledger,
                        int messageCount) {
        this.stubbornLinks = new StubbornLinks(port, this, hosts.size(), slidingWindowSize, extraMemory, acknowledger, messageCount);
        this.slidingWindowSize = slidingWindowSize;
        this.hosts = hosts;
        this.deliverer = deliverer;
        delivered = new HashSet[hosts.size()];
        this.slidingWindowStart = new int[hosts.size()];
        this.deliveredMessageCount = new int[hosts.size()];
        for(int i = 0; i < hosts.size(); i++){
            this.slidingWindowStart[i] = 0;
            this.deliveredMessageCount[i] = 0;
            this.delivered[i] = new HashSet();
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
        if(message.getId() <= slidingWindowStart[message.getSenderId()]){
            send(new Message(message, message.getReceiverId(), message.getOriginalSender()), hosts.get(message.getSenderId())); // Send ACK message
        }
        if(message.getId() > slidingWindowStart[message.getSenderId()] && message.getId() <= slidingWindowStart[message.getSenderId()] + slidingWindowSize){
            send(new Message(message, message.getReceiverId(), message.getOriginalSender()), hosts.get(message.getSenderId())); // Send ACK message
            if(!delivered[message.getSenderId()].contains(message.getId())){
                deliverer.deliver(message);
                delivered[message.getSenderId()].add(message.getId());
                deliveredMessageCount[message.getSenderId()] += 1;
                if(deliveredMessageCount[message.getSenderId()] < slidingWindowSize){
                    return;
                }
                // Check if this process has delivered all the messages in the sliding window
                if(delivered[message.getSenderId()].size() == slidingWindowSize){
                    // If yes, then increment the sliding window start
                    slidingWindowStart[message.getSenderId()] += slidingWindowSize;
                    // printSlidingWindows();
                    // Remove all the messages from the delivered map
                    delivered[message.getSenderId()].clear();
                    deliveredMessageCount[message.getSenderId()] = 0;
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
}
