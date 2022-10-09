package cs451.links;

import cs451.Deliverer;
import cs451.Host;
import cs451.Message;
import cs451.udp.UDPReceiver;
import cs451.udp.UDPSender;

import java.io.ObjectOutputStream;

// Implementation of Fair Loss Links using UDP sockets
public class FairLossLinks implements Deliverer {
    private final UDPReceiver receiver;
    private final Deliverer deliverer;

    FairLossLinks(int port, Deliverer deliverer){ // Create a new receiver
        this.receiver = new UDPReceiver(port, this);
        this.deliverer = deliverer;
    }

    // TODO: Maybe implement multiple sockets for sending?
    void send(Message message, Host host){ // Create a new sender and send message
        UDPSender sender = new UDPSender(host.getIp(), host.getPort(), message);
        sender.start();
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
