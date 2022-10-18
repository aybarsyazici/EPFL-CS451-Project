package cs451.links;

import cs451.Deliverer;
import cs451.Host;
import cs451.udp.Message;
import cs451.udp.UDPReceiver;
import cs451.udp.UDPSender;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// Implementation of Fair Loss Links using UDP sockets
public class FairLossLinks implements Deliverer {
    private static final int THREAD_NUMBER =  Math.max(Runtime.getRuntime().availableProcessors(),2);
    private final UDPReceiver receiver;
    private final Deliverer deliverer;
    private final ExecutorService pool = Executors.newFixedThreadPool(THREAD_NUMBER);

    FairLossLinks(int port, Deliverer deliverer){ // Create a new receiver
        this.receiver = new UDPReceiver(port, this);
        this.deliverer = deliverer;
        System.out.println("THREAD NUMBER: " + THREAD_NUMBER);
    }

    void send(Message message, Host host){ // Create a new sender and send message
        UDPSender sender = new UDPSender(host.getIp(), host.getPort(), message);
        pool.submit(sender);
    }

    void start(){
        receiver.start();
    }

    void stop(){
        receiver.haltReceiving();
    }

    @Override
    public void deliver(Message message) {
        deliverer.deliver(message);
    }
}
