package cs451.links;

import cs451.Deliverer;
import cs451.Host;
import cs451.Message;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class PerfectLinks implements Deliverer {
    private final StubbornLinks stubbornLinks;
    private final Deliverer deliverer;

    private List<Message> delivered;

    public PerfectLinks(int port, Deliverer deliverer) {
        this.stubbornLinks = new StubbornLinks(port, this);
        this.deliverer = deliverer;
        delivered = new ArrayList<>();
    }

    public void send(Message message, Host host){
        stubbornLinks.send(message,host);
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
