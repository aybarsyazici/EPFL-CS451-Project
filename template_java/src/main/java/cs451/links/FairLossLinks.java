package cs451.links;

import cs451.Deliverer;
import cs451.Host;
import cs451.Message.Message;
import cs451.Message.MessagePackage;
import cs451.udp.UDPBulkSender;
import cs451.udp.UDPObserver;
import cs451.udp.UDPReceiver;

import java.net.DatagramSocket;
import java.util.Map;
import java.util.concurrent.*;

// Implementation of Fair Loss Links using UDP sockets
public class FairLossLinks implements Deliverer, UDPObserver {
    private final int THREAD_NUMBER;
    private final UDPReceiver receiver;
    private final Deliverer deliverer;
    private final ExecutorService pool;
    private final ConcurrentHashMap<Message, Boolean> messagesInTheQueue;
    private final DatagramSocket[] sockets;
    // These sockets will be used by udp senders to send messages, each udp sender runs in a separate thread
    // and each thread has its own socket

    FairLossLinks(int port, Deliverer deliverer, int hostSize, int maxMemory, boolean extraMemory){
        this.receiver = new UDPReceiver(port, this, maxMemory, hostSize, extraMemory);
        this.deliverer = deliverer;
        int maxThreads = (900-(5*hostSize))/hostSize; /* 1024 is the total max amount of threads
        Each process has 5 threads (main, logChecker, messageSender, Receiver and finally the signal handler )
        so for each running process we by default have 5 threads. We also leave some thread count for threads that Java might be spawning
        So in total the size of the thread pool at max can be 900 - 5*hostSize, and we divide this between the hosts that we have
        so that each host has a thread pool of size (900 - 5*hostSize) / hostSize
        */
        this.THREAD_NUMBER = Math.min(maxThreads, Runtime.getRuntime().availableProcessors());
        this.pool = Executors.newFixedThreadPool(THREAD_NUMBER);
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
        messagesInTheQueue = new ConcurrentHashMap<>();
    }

    void send(MessagePackage messagePackage, Host host){ // Create a new sender and send message
        int socketId = ThreadLocalRandom.current().nextInt(sockets.length); // Choose a socket to send the message
        for (Message message : messagePackage.getMessages()){
            messagesInTheQueue.put(message, true);
        }
        pool.submit(
                new UDPBulkSender(
                        host.getIp(),
                        host.getPort(),
                        messagePackage.copy(),
                        sockets[socketId],
                        this));
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

    boolean isQueueFull(){
        ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) this.pool;
        int activeCount = threadPoolExecutor.getActiveCount();
        long taskCount = threadPoolExecutor.getTaskCount();
        long completedTaskCount = threadPoolExecutor.getCompletedTaskCount();
        long tasksToDo = taskCount - completedTaskCount - activeCount;
        return tasksToDo >= THREAD_NUMBER*5;
    }

    Boolean isInQueue(Message message){
        return messagesInTheQueue.containsKey(message);
    }

    @Override
    public void deliver(Message message) {
        deliverer.deliver(message);
    }
    @Override
    public void confirmDeliver(Message message){}
    @Override
    public void onUDPSenderExecuted(Message message) {
        // System.out.println("Message with id: " + messageId + " has been sent.");
        messagesInTheQueue.remove(message);

    }
}
