package cs451.links;

import cs451.Deliverer;
import cs451.Host;
import cs451.udp.Message;

import java.util.HashMap;
import java.util.Map;

public class PerfectLinks implements Deliverer {
    private final StubbornLinks stubbornLinks;
    private final Deliverer deliverer;
    private final Map<Map.Entry<Integer, Integer>, Message> delivered;

    public PerfectLinks(int port, Deliverer deliverer, HashMap<Integer, Host> hosts) {
        this.stubbornLinks = new StubbornLinks(port, this, hosts);
        this.deliverer = deliverer;
        delivered = new HashMap<>();
        // just passed to stubbornLinks for acknowledgment.
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
        if(!delivered.containsKey(Map.entry(message.getOriginalSender(), message.getId()))){
            delivered.put(Map.entry(message.getOriginalSender(), message.getId()), message);
            deliverer.deliver(message);
        }
    }
}
