package cs451.broadcast;

import cs451.Deliverer;
import cs451.Host;
import cs451.Logger;
import cs451.Message.Message;

import java.util.HashMap;
import java.util.List;

public class UniformReliableBroadcast implements Deliverer {
    private final byte id;
    private int messageCount;
    private final BestEffortBroadcast beb;

    private final Logger logger;
    private final Deliverer deliverer;


    public UniformReliableBroadcast(byte id, int port, List<Host> hostList, int slidingWindowSize, Logger logger, Deliverer deliverer){
        this.id = id;
        this.logger = logger;
        this.deliverer = deliverer;
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
        // TODO
    }
}
