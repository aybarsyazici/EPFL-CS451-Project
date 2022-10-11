package cs451.links;

import cs451.Deliverer;
import cs451.Host;
import cs451.udp.Message;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;

public class PerfectLinks implements Deliverer {
    private final StubbornLinks stubbornLinks;
    private final Deliverer deliverer;
    private final HashMap<Integer, Host> hosts;
    private final List<Message> delivered;

    public PerfectLinks(int port, Deliverer deliverer, HashMap<Integer, Host> hosts) {
        this.stubbornLinks = new StubbornLinks(port, this, hosts);
        this.deliverer = deliverer;
        delivered = new ArrayList<>();
        this.hosts = hosts;
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
        if(!delivered.contains(message)){
            delivered.add(message);
            deliverer.deliver(message);
        }
    }
}
