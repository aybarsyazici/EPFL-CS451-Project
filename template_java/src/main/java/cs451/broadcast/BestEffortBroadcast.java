package cs451.broadcast;

import cs451.interfaces.Acknowledger;
import cs451.interfaces.Deliverer;
import cs451.Host;
import cs451.Message.Message;
import cs451.interfaces.UniformDeliverer;
import cs451.links.PerfectLinks;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicIntegerArray;

public class BestEffortBroadcast  implements Acknowledger {
    private PerfectLinks perfectLinks;

    private UniformDeliverer uniformDeliverer;
    private byte id;
    private int[] lastSentMessageId;

    private int messageCount;
    private AtomicIntegerArray sendWindow;
    private final int slidingWindowSize;

    private final HashMap<Byte, Host> hosts;

    public BestEffortBroadcast(byte id, int port, HashMap<Byte, Host> hosts, boolean extraMemory, int slidingWindowSize, UniformDeliverer deliverer){
        this.sendWindow = new AtomicIntegerArray(hosts.size());
        this.lastSentMessageId = new int[hosts.size()];
        this.slidingWindowSize = slidingWindowSize;
        this.id = id;
        this.hosts = hosts;
        for (Host host : hosts.values()){
            sendWindow.set(host.getId(), slidingWindowSize);
            this.lastSentMessageId[host.getId()] = 1;
        }
        this.perfectLinks = new PerfectLinks(port, id, deliverer,
                this.hosts, slidingWindowSize,false,
                this, messageCount);
    }

    public void send(int messageCount){
        this.messageCount = messageCount;
        // Iterate over all hosts
        while(true){
            boolean sleep = true;
            boolean finished = true;
            for(byte hostId : hosts.keySet()){
                // Send message to all hosts
                if(hostId == id) continue;
                if(lastSentMessageId[hostId] < messageCount + 1){
                    if(!(lastSentMessageId[hostId] > sendWindow.get(hostId))){
                        perfectLinks.send(new Message(lastSentMessageId[hostId], id, hostId, id), hosts.get(hostId));
                        lastSentMessageId[hostId]++;
                        sleep = false;
                    }
                    finished = false;
                }
            }
            if(finished) break;
            if(sleep){
                try {
                    Runtime.getRuntime().gc();
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    public void rebroadcast(Message message){
        for(byte hostId : hosts.keySet()){
            if(hostId == id) continue;
            // if(hostId == message.getOriginalSender()) continue; // Don't send to the original sender
            Message newMessage = new Message(message, id, hostId, false);
            perfectLinks.send(newMessage, hosts.get(hostId));
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
    }

    public void start(){
        perfectLinks.start();
    }

    public void stop(){
        perfectLinks.stop();
    }

    @Override
    public void stopSenders() {}

}
