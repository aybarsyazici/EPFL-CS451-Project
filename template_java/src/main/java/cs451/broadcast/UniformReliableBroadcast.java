package cs451.broadcast;

import cs451.Deliverer;
import cs451.Host;
import cs451.Logger;
import cs451.Message.Message;

import java.util.*;

public class UniformReliableBroadcast implements Deliverer {
    private final byte id;
    private int messageCount;
    private final BestEffortBroadcast beb;
    private final HashMap<Map.Entry<Byte, Integer>, HashSet<Byte>> pending;
    private final Logger logger;
    private final Deliverer deliverer;

    private final int hostSize;


    public UniformReliableBroadcast(byte id, int port, List<Host> hostList, int slidingWindowSize, Logger logger, Deliverer deliverer){
        this.id = id;
        this.logger = logger;
        this.hostSize = hostList.size();
        this.deliverer = deliverer;
        this.pending = new HashMap<>();
        HashMap<Byte, Host> hostMap = new HashMap<>();
        for(Host host : hostList){
            hostMap.put((byte)host.getId(), host);
        }
        this.beb = new BestEffortBroadcast(id, port, hostMap, false, slidingWindowSize, this);
    }

    public void send(int messageCount){
        this.messageCount = messageCount;
        beb.send(messageCount);
    }

    public void stop(){
        beb.stop();
    }

    public void start(){
        beb.start();
    }

    @Override
    public void deliver(Message message) {
        deliverer.deliver(message);
        // this.confirmDeliver(message);
        beb.rebroadcast(message); // I deliver the message so I need to broadcast it
    }

    @Override
    public void confirmDeliver(Message message){
        Map.Entry<Byte, Integer> key = new AbstractMap.SimpleEntry<>(message.getOriginalSender(), message.getId());
        this.pending.computeIfAbsent(key, k -> new HashSet<>());
        if(this.pending.get(key).add(message.getSenderId())){
            if(this.pending.get(key).size() >= (hostSize/2)){
                this.deliverer.deliver(message);
                this.pending.get(key).clear();
                this.pending.remove(key);
            }
        }
    }
}