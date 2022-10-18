package cs451.links;

import cs451.Deliverer;
import cs451.Host;
import cs451.udp.Message;
import cs451.udp.UDPReceiver;
import cs451.udp.UDPSender;

import java.net.DatagramSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;

// Implementation of Fair Loss Links using UDP sockets
public class FairLossLinks implements Deliverer {
    private static final int THREAD_NUMBER =  Math.max(Runtime.getRuntime().availableProcessors(),20);
    private final UDPReceiver receiver;
    private final Deliverer deliverer;
    private final ExecutorService pool = Executors.newFixedThreadPool(THREAD_NUMBER);
    // These sockets will be used by udp senders to send messages, each udp sender runs in a separate thread
    // and each thread has its own socket
    private final DatagramSocket[] sockets;
    // idea: Maybe instead of having sockets here we can have an array of UDP Senders
    // will this improve performance ? I am not sure.



    FairLossLinks(int port, Deliverer deliverer){ // Create a new receiver
        this.receiver = new UDPReceiver(port, this);
        this.deliverer = deliverer;
        System.out.println("THREAD NUMBER: " + THREAD_NUMBER);
        // initialize sockets
        sockets = new DatagramSocket[THREAD_NUMBER];
        for(int i = 0; i < THREAD_NUMBER; i++){
            try{
                sockets[i] = new DatagramSocket();
            }
            catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    void send(Message message, Host host){ // Create a new sender and send message
        int socketId = ThreadLocalRandom.current().nextInt(sockets.length); // Choose a socket to send the message
        pool.submit(new UDPSender(host.getIp(), host.getPort(), message, sockets[socketId])); // Send the message on the given socket
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
