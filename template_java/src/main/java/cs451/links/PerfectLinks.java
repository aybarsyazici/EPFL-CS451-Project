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

    private int slidingWindowStart;
    private final int slidingWindowSize;
    private int slidingWindowEnd;
    private int deliveredMessageCount;
    private final HashMap<Byte, Host> hosts;

    public PerfectLinks(int port, Deliverer deliverer, HashMap<Byte, Host> hosts) {
        // 1.9 GB divided by number of hosts is the memory available to this process then each node is 32 bytes
        var availableMemory = (1900000000 / hosts.size());
        var numberOfMessages = availableMemory / 128;
        this.slidingWindowSize = numberOfMessages/(hosts.size()-1);
        this.slidingWindowEnd = slidingWindowStart + slidingWindowSize;
        System.out.println("Sliding window size: " + slidingWindowSize);
        this.stubbornLinks = new StubbornLinks(port, this, hosts.size(), slidingWindowSize);
        this.hosts = hosts;
        this.deliverer = deliverer;
        delivered = new HashMap<>();
        this.slidingWindowStart = 0;
        this.deliveredMessageCount = 0;
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
        if(message.getId() <= slidingWindowStart){
            send(new Message(message, message.getReceiverId(), message.getOriginalSender()), hosts.get(message.getSenderId())); // Send ACK message
        }
        if(message.getId() > slidingWindowStart && message.getId() <= slidingWindowEnd) {
            send(new Message(message, message.getReceiverId(), message.getOriginalSender()), hosts.get(message.getSenderId())); // Send ACK message
            if(!delivered.get(message.getSenderId()).containsKey(message.getId())){
                deliverer.deliver(message);
                delivered.get(message.getSenderId()).put(message.getId(), true);
                deliveredMessageCount += 1;
                if(deliveredMessageCount < slidingWindowSize*(hosts.size()-1)){
                    return;
                }
                // Check we have received all the messages from all processes
                boolean allReceived = true;
                for(Map<Integer, Boolean> map : delivered.values()){
                    if(map.size() != slidingWindowSize){
                        allReceived = false;
                        break;
                    }
                }
                if(allReceived){
                    // Move the sliding window
                    // System.out.println("Moving sliding window to " + slidingWindowEnd);
                    slidingWindowStart += slidingWindowSize;
                    slidingWindowEnd += slidingWindowSize;
                    // Remove the messages from the delivered map
                    for(Map<Integer, Boolean> map : delivered.values()){
                        map.clear();
                    }
                    deliveredMessageCount = 0;
                    delivered.clear();
                }
            }
        }
    }
}
