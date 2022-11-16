package cs451.links;

import cs451.interfaces.Acknowledger;
import cs451.interfaces.Deliverer;
import cs451.Host;
import cs451.Message.Message;
import cs451.Message.MessagePackage;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class StubbornLinks implements Deliverer {
    private final FairLossLinks fairLoss;
    private final Deliverer deliverer;
    private final AtomicIntegerArray slidingWindows;
    private final AtomicIntegerArray messagesDelivered;

    private final int slidingWindowSize;
    private final List<Message>[] messageToBeSent;
    private final ConcurrentHashMap<Byte, ConcurrentHashMap<Message, Boolean>> ackMessagesToBeSent;
    private int count;
    private final Runnable msgSendThread;
    private final AtomicBoolean isRunning;

    private final HashMap<Byte, Host> hosts;

    private byte startHost;
    private final Lock lock;
    private final int messageCount;

    Acknowledger acknowledger;

    public StubbornLinks(int port, HashMap<Byte, Host> hosts, Deliverer deliverer, int hostSize, int slidingWindowSize, boolean extraMemory, Acknowledger acknowledger, int messageCount) {
        this.fairLoss = new FairLossLinks(port, this, hostSize);
        this.deliverer = deliverer;
        this.lock = new ReentrantLock();
        this.hosts = hosts;
        this.messageCount = messageCount;
        this.startHost = 0;
        this.acknowledger = acknowledger;
        this.messageToBeSent = new List[hostSize];
        this.ackMessagesToBeSent = new ConcurrentHashMap<>();
        this.slidingWindows = new AtomicIntegerArray(hostSize); // Keep the sliding window of each host
        this.messagesDelivered = new AtomicIntegerArray(hostSize); // Keep the number of messages delivered to each host
        this.slidingWindowSize = slidingWindowSize;
        for(int i = 0; i < hostSize; i++){
            slidingWindows.set(i,slidingWindowSize);
            messagesDelivered.set(i,0);
            this.messageToBeSent[i] = new ArrayList<>();
        }
        // this.runnerTasks = new ConcurrentHashMap<>();
        this.count = 0;
        this.isRunning = new AtomicBoolean(true);
        this.msgSendThread = () -> {
            while(isRunning.get()){
                try {
                    for (byte hostId : ackMessagesToBeSent.keySet()) {
                        if(ackMessagesToBeSent.get(hostId).size() > 0){
                            var temp = Collections.list(ackMessagesToBeSent.get(hostId).keys());
                            sendAckMessagesToBeSent(temp, this.hosts.get(hostId));
                        }
                    }
                    for (var host = startHost; host < hostSize; host++) {
                        if(messageToBeSent[host].size() > 0){
                            var temp = new ArrayList<>(messageToBeSent[host]);
                            sendMessagesToBeSent(temp, this.hosts.get((byte)host));
                        }
                    }
                    this.startHost = (byte) ((this.startHost+1)%hostSize);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                try{
                    Thread.sleep(500);
                }
                catch (Exception e){
                    e.printStackTrace();
                }
            }
        };
    }

    private void sendMessagesToBeSent(List<Message> messages, Host host){
        if(messages.size() == 0) return;
        List<Message> messagesToSend = new ArrayList<>();
        (messages).
                forEach(m -> {
                    if (!fairLoss.isQueueFull()) {
                        var slidingWindowSize = slidingWindows.get(m.getReceiverId());
                        if(m.getSenderId() == m.getOriginalSender() && m.getId() > slidingWindowSize){
                            return;
                        }
                        messagesToSend.add(m);
                        if(messagesToSend.size() == 8){
                            fairLoss.send(new MessagePackage(messagesToSend), host);
                            messagesToSend.clear();
                        }
                    }
                });
        if(messagesToSend.size() > 0){
            fairLoss.send(new MessagePackage(messagesToSend), host);
        }
    }

    private void sendAckMessagesToBeSent(List<Message> messages, Host host) {
        List<Message> messagesToSend = new ArrayList<>();
        (messages).
                forEach(m -> {
                    if (!fairLoss.isQueueFull()){
                        messagesToSend.add(m);
                        ackMessagesToBeSent.get(m.getReceiverId()).remove(m);
                        if(messagesToSend.size() == 8){
                            fairLoss.send(new MessagePackage(messagesToSend), host);
                            messagesToSend.clear();
                        }
                    }
                });
        if(messagesToSend.size() > 0){
            fairLoss.send(new MessagePackage(messagesToSend), host);
        }
    }

    public void start() {
        fairLoss.start();
        new Thread(msgSendThread, "Message send thread").start();
        // new Thread(ackMsgSendThread, "Ack message send thread").start();
    }

    public void send(Message message, Host host) {
        if (message.isAckMessage()) {
            ackMessagesToBeSent.computeIfAbsent(message.getReceiverId(), k -> new ConcurrentHashMap<>());
            ackMessagesToBeSent.get(message.getReceiverId()).put(message, true);
            return;
        }
        lock.lock();
        messageToBeSent[message.getReceiverId()].add(message);
        lock.unlock();
    }

    public void stop() {
        this.isRunning.compareAndSet(true, false);
        fairLoss.stop();
    }

    public void stopSenders(){
        this.isRunning.compareAndSet(true, false);
    }

    private void readyToSlide(){
        int count = 0;
        for(int i = 0; i < hosts.size(); i++){
            if( messagesDelivered.get(i) >= slidingWindows.get(i) || messagesDelivered.get(i) == messageCount){
                count++;
            }
        }
        if(count >= hosts.size()/2){
            for(int i= 0; i<hosts.size(); i++){
                if(messagesDelivered.get(i) >= slidingWindows.get(i) ){
                    slidingWindows.addAndGet(i,this.slidingWindowSize);
                    acknowledger.slideSendWindow((byte)i);
                }
            }
            System.gc();
        }
    }


    @Override
    public void deliver(Message message) {
        if (message.isAckMessage()) { // I have sent this message and received it back.
            Message originalMessage = message.swapSenderReceiver();
            lock.lock();
            var removal = messageToBeSent[originalMessage.getReceiverId()].remove(originalMessage);
            lock.unlock();
            if (removal) {
                if(originalMessage.getOriginalSender() == originalMessage.getSenderId()){ // I am the original sender,
                    // this check works before this message is constructed by swapping receiver and sender. Because I this is the message I've received
                    // the message.getReceiverId() is equal to my id, so after swapping the sender and receiver the originalMessage.getSenderId() is equal to my id
                    // So if the original sender is equal to originalMessage.getSenderId() then I am the original sender
                    messagesDelivered.incrementAndGet(originalMessage.getReceiverId()); // Successfully delivered this message.
                    readyToSlide();
                    count += 1;
                    if (count % 1000 == 0) {
                        System.out.println("Sent " + count + " messages.");
                    }
                }
            }
        } else {
            deliverer.deliver(message);
        }
    }
}
