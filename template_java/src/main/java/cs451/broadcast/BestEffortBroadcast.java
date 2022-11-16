package cs451.broadcast;

import cs451.interfaces.Acknowledger;
import cs451.interfaces.Deliverer;
import cs451.Host;
import cs451.Message.Message;
import cs451.interfaces.UniformDeliverer;
import cs451.links.PerfectLinks;

import java.util.HashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicIntegerArray;

public class BestEffortBroadcast  implements Acknowledger {
    private PerfectLinks perfectLinks;
    private byte id;
    private int[] lastSentMessageId;
    private final int messageCount;
    private AtomicIntegerArray sendWindow;
    private final int slidingWindowSize;
    private final ConcurrentLinkedQueue<Message> rebroadcastQueue;
    private final HashMap<Byte, Host> hosts;

    public BestEffortBroadcast(byte id, int port, HashMap<Byte, Host> hosts, boolean extraMemory, int slidingWindowSize, UniformDeliverer deliverer, int messageCount) {
        this.sendWindow = new AtomicIntegerArray(hosts.size());
        this.lastSentMessageId = new int[hosts.size()];
        this.slidingWindowSize = slidingWindowSize;
        this.messageCount = messageCount;
        this.id = id;
        this.hosts = hosts;
        this.rebroadcastQueue = new ConcurrentLinkedQueue<>();
        for (Host host : hosts.values()){
            sendWindow.set(host.getId(), slidingWindowSize);
            this.lastSentMessageId[host.getId()] = 1;
        }
        this.perfectLinks = new PerfectLinks(port, id, deliverer,
                this.hosts, slidingWindowSize,false,
                this, messageCount);
        new Thread(()->{
            while(true)
            {
                while(!rebroadcastQueue.isEmpty()){
                    Message message = rebroadcastQueue.poll();
                    for(byte hostId : hosts.keySet()){
                        if(hostId == id) continue;
                        if(hostId == message.getOriginalSender()) continue;
                        Message newMessage = new Message(message, id, hostId, false);
                        perfectLinks.send(newMessage, hosts.get(hostId));
                    }
                }
                try{
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, "Rebroadcaster").start();
    }

    public void send(){
        // Iterate over all hosts
        for(byte hostId : hosts.keySet()){
            // Send message to all hosts
            if(hostId == id) continue;
            while((lastSentMessageId[hostId] < messageCount + 1) && lastSentMessageId[hostId] <= sendWindow.get(hostId)){
                perfectLinks.send(new Message(lastSentMessageId[hostId], id, hostId, id), hosts.get(hostId));
                lastSentMessageId[hostId]++;
            }
        }
    }
    public void rebroadcast(Message message){
        this.rebroadcastQueue.add(message);
    }

    @Override
    public void slideSendWindow(byte hostId) {
        var size = sendWindow.addAndGet(hostId,slidingWindowSize);
        while((lastSentMessageId[hostId] < messageCount + 1) && lastSentMessageId[hostId] <= size){
            perfectLinks.send(new Message(lastSentMessageId[hostId], id, hostId, id), hosts.get(hostId));
            lastSentMessageId[hostId]++;
        }
        // iterate over all sliding windows
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
