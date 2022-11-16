package cs451.broadcast;

import cs451.Process;
import cs451.interfaces.Deliverer;
import cs451.Host;
import cs451.interfaces.Logger;
import cs451.Message.Message;
import cs451.interfaces.UniformDeliverer;

import java.util.*;

public class UniformReliableBroadcast implements UniformDeliverer {
    private final BestEffortBroadcast beb;
    private final Deliverer deliverer;

    public UniformReliableBroadcast(byte id, int port, List<Host> hostList, int slidingWindowSize, Logger logger, Deliverer deliverer, int messageCount){
        this.deliverer = deliverer;
        HashMap<Byte, Host> hostMap = new HashMap<>();
        for(Host host : hostList){
            hostMap.put((byte)host.getId(), host);
        }
        this.beb = new BestEffortBroadcast(id, port, hostMap, false, slidingWindowSize, this, messageCount);
    }

    public void send(){
        beb.send();
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
