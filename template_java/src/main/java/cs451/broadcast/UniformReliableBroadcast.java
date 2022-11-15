package cs451.broadcast;

import cs451.Process;
import cs451.interfaces.Deliverer;
import cs451.Host;
import cs451.interfaces.Logger;
import cs451.Message.Message;
import cs451.interfaces.UniformDeliverer;

import java.util.*;

public class UniformReliableBroadcast implements UniformDeliverer {
    private final byte id;
    private int messageCount;
    private final BestEffortBroadcast beb;
    private final HashMap<Map.Entry<Byte, Integer>, HashSet<Byte>> pending;
    private final HashSet<Map.Entry<Byte,Integer>> delivered;
    private final Logger logger;
    private final Deliverer deliverer;

    private final int hostSize;


    public UniformReliableBroadcast(byte id, int port, List<Host> hostList, int slidingWindowSize, Logger logger, Deliverer deliverer){
        this.id = id;
        this.logger = logger;
        this.hostSize = hostList.size();
        this.deliverer = deliverer;
        this.delivered = new HashSet<>();
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
        beb.rebroadcast(message); // I deliver the message so I need to broadcast it
    }

    @Override
    public void uniformDeliver(Message message) {
        deliverer.deliver(message);
    }
}
