package cs451.links;

import cs451.Deliverer;
import cs451.Host;
import cs451.udp.Message;

import java.util.HashMap;
import java.util.Map;

public class PerfectLinks implements Deliverer {
    private final StubbornLinks stubbornLinks;
    private final Deliverer deliverer;
    private final Map<Byte, Map<Integer, Boolean>> delivered;

    private int[] slidingWindowStart;
    private final int slidingWindowSize;
    private int[] deliveredMessageCount;
    private final HashMap<Byte, Host> hosts;

    public PerfectLinks(int port, Deliverer deliverer, HashMap<Byte, Host> hosts) {
        // 1.9 GB divided by number of hosts is the memory available to this process then each node is 32 bytes
        var availableMemory = (1900000000 / hosts.size());
        var numberOfMessages = availableMemory / 128;
        this.slidingWindowSize = numberOfMessages/(hosts.size()-1);
        System.out.println("Sliding window size: " + slidingWindowSize);
        this.stubbornLinks = new StubbornLinks(port, this, hosts.size(), slidingWindowSize);
        this.hosts = hosts;
        this.deliverer = deliverer;
        delivered = new HashMap<>();
        this.slidingWindowStart = new int[hosts.size()];
        this.deliveredMessageCount = new int[hosts.size()];
        for(int i = 0; i < hosts.size(); i++){
            this.slidingWindowStart[i] = 0;
            this.deliveredMessageCount[i] = 0;
        }
        // just passed to stubbornLinks for acknowledgment.
    }

    public void send(Message message, Host host){
        stubbornLinks.send(message, host);
    }

    public void stop(){
        stubbornLinks.stop();
    }

    public void start(){
        stubbornLinks.start();
    }

    @Override
    public void deliver(Message message) {
        if(!delivered.containsKey(message.getSenderId())) {
            delivered.put(message.getSenderId(), new HashMap<>());
        }
        if(message.getId() <= slidingWindowStart[message.getSenderId()-1]){
            send(new Message(message, message.getReceiverId(), message.getOriginalSender()), hosts.get(message.getSenderId())); // Send ACK message
        }
        if(message.getId() > slidingWindowStart[message.getSenderId()-1] && message.getId() <= slidingWindowStart[message.getSenderId()-1] + slidingWindowSize){
            send(new Message(message, message.getReceiverId(), message.getOriginalSender()), hosts.get(message.getSenderId())); // Send ACK message
            if(!delivered.get(message.getSenderId()).containsKey(message.getId())){
                deliverer.deliver(message);
                delivered.get(message.getSenderId()).put(message.getId(), true);
                deliveredMessageCount[message.getSenderId()-1] += 1;
                if(deliveredMessageCount[message.getSenderId()-1] < slidingWindowSize){
                    return;
                }
                // Check if this process has delivered all the messages in the sliding window
                if(delivered.get(message.getSenderId()).size() == slidingWindowSize){
                    // If yes, then increment the sliding window start
                    slidingWindowStart[message.getSenderId()-1] += slidingWindowSize;
                    // printSlidingWindows();
                    // Remove all the messages from the delivered map
                    delivered.get(message.getSenderId()).clear();
                    deliveredMessageCount[message.getSenderId()-1] = 0;
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
