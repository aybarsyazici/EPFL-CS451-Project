package cs451.links;

import cs451.Deliverer;
import cs451.Host;
import cs451.Message.Message;
import cs451.Message.MessagePackage;
import cs451.udp.UDPBulkSender;
import cs451.udp.UDPObserver;
import cs451.udp.UDPReceiver;
import cs451.udp.UDPSender;

import java.net.DatagramSocket;
import java.util.Map;
import java.util.concurrent.*;

// Implementation of Fair Loss Links using UDP sockets
public class FairLossLinks implements Deliverer, UDPObserver {
    private final int THREAD_NUMBER;
    private final UDPReceiver receiver;

    private final Deliverer deliverer;
    private final ExecutorService pool;
    private final ConcurrentHashMap<Map.Entry<Byte,Integer>, Boolean> messagesInTheQueue;
    // These sockets will be used by udp senders to send messages, each udp sender runs in a separate thread
    // and each thread has its own socket
    private final DatagramSocket[] sockets;
    // idea: Maybe instead of having sockets here we can have an array of UDP Senders
    // will this improve performance ? I am not sure.



    FairLossLinks(int port, Deliverer deliverer, int hostSize, int maxMemory, boolean extraMemory){ // Create a new receiver
        this.receiver = new UDPReceiver(port, this, maxMemory,extraMemory);
        this.deliverer = deliverer;
        // this.THREAD_NUMBER = 2;
        this.THREAD_NUMBER = Math.min(800/hostSize, Runtime.getRuntime().availableProcessors());
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
            messagesInTheQueue.put(Map.entry(message.getReceiverId(), message.getId()), true);
        }
        pool.submit(
                new UDPBulkSender(
                        host.getIp(),
                        host.getPort(),
                        (byte)host.getId(),
                        messagePackage.toBytes(),
                        messagePackage.getMessageIds(),
                        sockets[socketId],
                        this));
    }

    void start(){
        receiver.start();
    }

    void stop(){
        receiver.haltReceiving();
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
        return tasksToDo >= THREAD_NUMBER*2;
    }

    Boolean isInQueue(byte receiverId, int messageId){
        return messagesInTheQueue.containsKey(Map.entry(receiverId, messageId));
    }

    @Override
    public void deliver(Message message) {
        deliverer.deliver(message);
    }

    @Override
    public void onUDPSenderExecuted(byte receiverId, int messageId) {
        // System.out.println("Message with id: " + messageId + " has been sent.");
        messagesInTheQueue.remove(Map.entry(receiverId, messageId));
    }
}
