package cs451.links;

import cs451.Deliverer;
import cs451.Host;
import cs451.udp.Message;
import cs451.udp.MessageExtension;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class StubbornLinks implements Deliverer {
    private final FairLossLinks fairLoss;
    private final Deliverer deliverer;
    private final Timer timer;
    private final HashMap<Byte, Host> hosts;
    private final ConcurrentHashMap<Integer,MessageExtension> alreadySent;
    private int count;
    // We need to keep the list of the messages we have already sent
    // Pass through the array -> check if they have been received by the other end.
    // if they are not received we send them again
    // Repeat continuously till they have been acknowledged, i.e., received and delivered by the other end.

    public StubbornLinks(int port, Deliverer deliverer, HashMap<Byte, Host> hosts) {
        this.fairLoss = new FairLossLinks(port, this);
        this.deliverer = deliverer;
        this.alreadySent = new ConcurrentHashMap<>();
        this.hosts = hosts;
        this.count = 0;
        this.timer = new Timer();
    }

    public void start(){
        fairLoss.start();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                try{
                    (new ArrayList<>(Arrays.asList(alreadySent.values().toArray()))).
                            forEach(m -> fairLoss.send(((MessageExtension)m).getMessage(),((MessageExtension)m).getHost()));
                }
                catch (Exception e) {e.printStackTrace();}
            }
        }, 100, 200);
    }

    public void send(Message message, Host host){
        if(message.isAckMessage()) {
            fairLoss.send(message, host);
            return;
        }
        alreadySent.put(message.getId(), new MessageExtension(message, host));
    }

    public void stop(){
        timer.cancel();
        fairLoss.stop();
    }

    @Override
    public void deliver(Message message) {
        Host senderHost = hosts.get(message.getOriginalSender());
        if(message.getOriginalSender() == message.getReceiverId()){ // I have sent this message and received it back.
            if(alreadySent.containsKey(message.getId())) {
                alreadySent.remove(message.getId());
                count += 1;
                if(count == 10000){
                    System.out.println("Sent 10k messages.");
                    count = 0;
                }
            }
        }
        else{
            deliverer.deliver(message);
            send(new Message(message, message.getReceiverId(), message.getOriginalSender()), senderHost);
            // To inform the original sender of this message I need a way to access the hosts array.
        }
    }
}
