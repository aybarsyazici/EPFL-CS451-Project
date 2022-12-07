package cs451.links;

import cs451.interfaces.Deliverer;
import cs451.Host;
import cs451.Message.Message;
import cs451.Message.MessagePackage;
import cs451.udp.UDPBulkSender;
import cs451.udp.UDPObserver;
import cs451.udp.UDPReceiver;

import java.net.DatagramSocket;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

// Implementation of Fair Loss Links using UDP sockets
public class FairLossLinks implements Deliverer, UDPObserver {
    private final int THREAD_NUMBER;
    private final int proposalSetSize;
    private final UDPReceiver receiver;
    private final Deliverer deliverer;
    private final ExecutorService pool;
    private final AtomicInteger jobCount;
    private final ConcurrentHashMap<Message, Boolean> inQueue;
    private final DatagramSocket[] sockets;
    // These sockets will be used by udp senders to send messages, each udp sender runs in a separate thread
    // and each thread has its own socket

    FairLossLinks(int port, Deliverer deliverer, int hostSize, int proposalSetSize){
        this.receiver = new UDPReceiver(port, this, proposalSetSize);
        this.deliverer = deliverer;
        this.proposalSetSize = proposalSetSize;
        this.inQueue = new ConcurrentHashMap<>();
        this.THREAD_NUMBER = 2; /* 8 process at max per host
        Each process has 6 threads (main, logChecker, ackSender, messageSender, Receiver and finally the signal handler )
        so for each running process we by default have 6 threads. We also leave some thread count for threads that Java might be spawning
        */
        this.pool = Executors.newFixedThreadPool(THREAD_NUMBER);
        this.jobCount = new AtomicInteger(0);
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

    void send(MessagePackage messagePackage, Host host){ // Create a new sender and send message
        int socketId = ThreadLocalRandom.current().nextInt(sockets.length); // Choose a socket to send the message
        this.jobCount.addAndGet(1);
        for(Message message: messagePackage.getMessages()){
            this.inQueue.put(message, true);
        }
        pool.submit(
                new UDPBulkSender(
                        host.getIp(),
                        host.getPort(),
                        messagePackage.copy(),
                        sockets[socketId],
                        this,
                        proposalSetSize)
        );
    }

    void start(){
        receiver.start();
    }

    void stop(){
        receiver.haltReceiving();
        pool.shutdownNow();
        for(int i = 0; i < sockets.length; i++){
            sockets[i].close();
        }
    }

    public boolean isQueueFull(){
        //return this.jobCount.get() > THREAD_NUMBER * 500;
        return false;
    }

    public boolean isinQueue(Message message){
        return this.inQueue.containsKey(message);
    }

    @Override
    public void deliver(Message message) {
        deliverer.deliver(message);
    }

    @Override
    public void onUDPSenderExecuted(Message message) {
        this.inQueue.remove(message);
    }

    @Override
    public void onUDPBulkSenderExecuted(){
        this.jobCount.decrementAndGet();
    }
}
