package cs451.broadcast;

import cs451.Acknowledger;
import cs451.Deliverer;
import cs451.Host;
import cs451.Logger;
import cs451.Message.Message;
import cs451.links.PerfectLinks;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicIntegerArray;

public class BestEffortBroadcast  implements Deliverer, Acknowledger {
    private PerfectLinks perfectLinks;

    private Deliverer deliverer;
    private Logger logger;

    private byte id;
    private int[] lastSentMessageId;

    private int messageCount;
    private AtomicIntegerArray sendWindow;
    private final int slidingWindowSize;

    private final HashMap<Byte, Host> hosts;


    public BestEffortBroadcast(byte id, int port, List<Host> hostList, boolean extraMemory, int slidingWindowSize, Logger logger, Deliverer deliverer){
        this.hosts = new HashMap<>();
        this.sendWindow = new AtomicIntegerArray(hostList.size());
        this.deliverer = deliverer;
        this.lastSentMessageId = new int[hostList.size()];
        this.slidingWindowSize = slidingWindowSize;
        this.id = id;

        for(Host host : hostList){
            hosts.put((byte)host.getId(), host);
            sendWindow.set(host.getId(), slidingWindowSize);
            this.lastSentMessageId[host.getId()] = 1;
        }

        this.perfectLinks = new PerfectLinks(port, this, this.hosts, slidingWindowSize,false, this, messageCount);


    }

    public void send(int messageCount){
        this.messageCount = messageCount;
        // Iterate over all hosts
        while(true){
            for(byte hostId : hosts.keySet()){
                // Send message to all hosts
                if(hostId == id) continue;
                if(lastSentMessageId[hostId] < messageCount + 1){
                    if(!(lastSentMessageId[hostId] > sendWindow.get(hostId))){
                        perfectLinks.send(new Message(lastSentMessageId[hostId], id, hostId, id), hosts.get(hostId));
                        lastSentMessageId[hostId]++;
                    }
                    else{
                        try {
                            Runtime.getRuntime().gc();
                            Thread.sleep(200);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }

    }

    @Override
    public void slideSendWindow(byte destinationId) {
        int oldWindow = sendWindow.getAndAdd(destinationId,slidingWindowSize);
        // iterate over all sliding windows
        for(int i = 0; i < sendWindow.length(); i++){
            if(sendWindow.get(i) <= oldWindow){
                return;
            }
        }
        // If we get here, all sliding windows have been updated
        for(int i = (oldWindow-slidingWindowSize)+1; i <= oldWindow; i++){
            logger.logBroadcast(i);
        }
    }

    public void start(){
        perfectLinks.start();
    }

    public void stop(){
        perfectLinks.stop();
    }

    @Override
    public void stopSenders() {}

    @Override
    public void deliver(Message message) {
        deliverer.deliver(message);
    }
}
