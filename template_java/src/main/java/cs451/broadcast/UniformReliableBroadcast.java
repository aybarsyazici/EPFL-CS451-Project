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
    private final int lastDeliveredMessageId[];
    private final int slidingWindowSize;
    private final Message[][] pending;

    public UniformReliableBroadcast(byte id, int port, List<Host> hostList, int slidingWindowSize, Logger logger, Deliverer deliverer, int messageCount){
        this.deliverer = deliverer;
        HashMap<Byte, Host> hostMap = new HashMap<>();
        this.pending = new Message[hostList.size()][slidingWindowSize];
        this.lastDeliveredMessageId = new int[hostList.size()];
        this.slidingWindowSize = slidingWindowSize;
        for(Host host : hostList){
            hostMap.put((byte)host.getId(), host);
            this.lastDeliveredMessageId[host.getId()] = 1;
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
        if(lastDeliveredMessageId[message.getOriginalSender()] + 1 == message.getId()){
            deliverer.deliver(message);
            lastDeliveredMessageId[message.getOriginalSender()]++;
            var pendingFromThisHost = pending[message.getOriginalSender()];
            for(int i = 0; i < pendingFromThisHost.length; i++){
                if(pendingFromThisHost[i] != null && pendingFromThisHost[i].getId() == lastDeliveredMessageId[message.getOriginalSender()] + 1){
                    deliverer.deliver(pendingFromThisHost[i]);
                    lastDeliveredMessageId[message.getOriginalSender()]++;
                    pendingFromThisHost[i] = null;
                }
            }
        }
        else{
            this.pending[message.getOriginalSender()][(message.getId() - 1)%slidingWindowSize] = message;
        }
    }

}
