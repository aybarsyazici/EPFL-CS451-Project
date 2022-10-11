package cs451.links;

import cs451.Deliverer;
import cs451.Host;
import cs451.udp.Message;
import cs451.udp.MessageExtension;
import cs451.udp.MessageType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class StubbornLinks implements Deliverer {
    private final FairLossLinks fairLoss;
    private final Deliverer deliverer;
    private final Timer timer;
    private final HashMap<Integer, Host> hosts;
    private final ConcurrentLinkedQueue<MessageExtension> alreadySent;
    // We need to keep the list of the messages we have already sent
    // Pass through the array -> check if they have been received by the other end.
    // if they are not received we send them again
    // Repeat continuously till they have been acknowledged, i.e., received and delivered by the other end.

    public StubbornLinks(int port, Deliverer deliverer, HashMap<Integer, Host> hosts) {
        this.fairLoss = new FairLossLinks(port, this);
        this.deliverer = deliverer;
        this.alreadySent = new ConcurrentLinkedQueue<>();
        this.hosts = hosts;
        this.timer = new Timer();
    }

    public void start(){
        fairLoss.start();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                try{
                    (new ArrayList<>(alreadySent)).parallelStream().forEach(m -> fairLoss.send(m.getMessage(),m.getHost()));
                }
                catch (Exception e) {e.printStackTrace();}
            }
        }, 100, 50);
    }

    public void send(Message message, Host host){
        if(message.isAckMessage()) {
            fairLoss.send(message, host);
            return;
        }
        System.out.println("Sending new payload message" + message);
        alreadySent.add(new MessageExtension(message, host));
    }

    public void stop(){
        timer.cancel();
        fairLoss.stop();
    }

    @Override
    public void deliver(Message message) {
        Host senderHost = hosts.get(message.getOriginalSender());
        if(message.getOriginalSender() == message.getReceiverId()){ // I have sent this message and received it back.
            alreadySent.remove(new MessageExtension(message, senderHost));
            System.out.println("Stubborn links successfully sent message with id: " + message.getId());
        }
        else{
            System.out.println("NEW MESSAGE WOO");
            deliverer.deliver(message);
            send(new Message(message, message.getReceiverId(), message.getOriginalSender()), senderHost);
            // To inform the original sender of this message I need a way to access the hosts array.
        }
    }
}
