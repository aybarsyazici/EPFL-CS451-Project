package cs451.links;

import cs451.interfaces.Acknowledger;
import cs451.interfaces.Deliverer;
import cs451.Host;
import cs451.Message.Message;
import cs451.Message.MessagePackage;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class StubbornLinks implements Deliverer {
    private final FairLossLinks fairLoss;
    private final PerfectLinks perfectLinks;
    private final AtomicIntegerArray slidingWindows;
    private final AtomicIntegerArray messagesDelivered;

    private final int slidingWindowSize;
    private final List<Message>[] messageToBeSent;
    private final List<Message>[] rebroadcastMessages;

    private final ConcurrentHashMap<Byte, ConcurrentHashMap<Message, Boolean>> ackMessagesToBeSent;
    private int count;
    private int rebroadcastCount;
    private final Runnable msgSendThread;
    private final Runnable ackSendThread;
    private final Runnable rebroadcastThread;
    private final AtomicBoolean isRunning;
    private final HashMap<Byte, Host> hosts;
    private final Lock lock;
    private final Lock rebroadcastLock;
    private final int messageCount;

    Acknowledger acknowledger;

    public StubbornLinks(int port, HashMap<Byte, Host> hosts, PerfectLinks perfectLinks, int hostSize, int slidingWindowSize, boolean extraMemory, Acknowledger acknowledger, int messageCount) {
        this.fairLoss = new FairLossLinks(port, this, hostSize, slidingWindowSize);
        this.perfectLinks = perfectLinks;
        this.lock = new ReentrantLock();
        this.rebroadcastLock = new ReentrantLock();
        this.hosts = hosts;
        this.messageCount = messageCount;
        this.rebroadcastCount = 0;
        this.acknowledger = acknowledger;
        this.messageToBeSent = new List[hostSize];
        this.rebroadcastMessages = new List[hostSize];
        this.ackMessagesToBeSent = new ConcurrentHashMap<>();
        this.slidingWindows = new AtomicIntegerArray(hostSize); // Keep the sliding window of each host
        this.messagesDelivered = new AtomicIntegerArray(hostSize); // Keep the number of messages delivered to each host
        this.slidingWindowSize = slidingWindowSize;
        for(int i = 0; i < hostSize; i++){
            slidingWindows.set(i,slidingWindowSize);
            messagesDelivered.set(i,0);
            this.messageToBeSent[i] = new ArrayList<>();
            this.rebroadcastMessages[i] = new ArrayList<>();
        }
        // this.runnerTasks = new ConcurrentHashMap<>();
        this.count = 0;
        this.isRunning = new AtomicBoolean(true);
        this.msgSendThread = () -> {
            while(isRunning.get()){
                try {
                    for (var host = 0; host < hostSize; host++) {
                        if(messageToBeSent[host].size() > 0){
                            var temp = new ArrayList<>(messageToBeSent[host]);
                            sendMessagesToBeSent(temp, this.hosts.get((byte)host));
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                try{
                    Thread.sleep(1000);
                }
                catch (Exception e){
                    e.printStackTrace();
                }
            }
        };
        this.ackSendThread = () -> {
            while(isRunning.get()){
                try {
                    for (byte hostId : ackMessagesToBeSent.keySet()) {
                        if(ackMessagesToBeSent.get(hostId).size() > 0){
                            var temp = Collections.list(ackMessagesToBeSent.get(hostId).keys());
                            sendAckMessagesToBeSent(temp, this.hosts.get(hostId));
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                try{
                    Thread.sleep(300);
                }
                catch (Exception e){
                    e.printStackTrace();
                }
            }
        };
        this.rebroadcastThread = () -> {
            while(isRunning.get()){
                try {
                    for (var host = 0; host < hostSize; host++) {
                        if(rebroadcastMessages[host].size() > 0){
                            var temp = new ArrayList<>(rebroadcastMessages[host]);
                            sendMessagesToBeSent(temp, this.hosts.get((byte)host));
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                try{
                    Thread.sleep(300);
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
        // Get memory usage
        AtomicLong memory = new AtomicLong(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());
        (messages).
                forEach(m -> {
                    if (!fairLoss.isinQueue(m)) {
                        if(memory.get() > 50*1024*1024) return;
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
                    memory.set(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());
                });
        if(messagesToSend.size() > 0){
            fairLoss.send(new MessagePackage(messagesToSend), host);
        }
    }

    private void sendAckMessagesToBeSent(List<Message> messages, Host host) {
        List<Message> messagesToSend = new ArrayList<>();
        AtomicLong memory = new AtomicLong(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());
        (messages).
                forEach(m -> {
                    if(memory.get() > 55*1024*1024) return;
                    if (!fairLoss.isinQueue(m)) {
                        messagesToSend.add(m);
                        ackMessagesToBeSent.get(m.getReceiverId()).remove(m);
                        if(messagesToSend.size() == 8){
                            fairLoss.send(new MessagePackage(messagesToSend), host);
                            messagesToSend.clear();
                        }
                    }
                    memory.set(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());
                });
        if(messagesToSend.size() > 0){
            fairLoss.send(new MessagePackage(messagesToSend), host);
        }
    }

    public void start() {
        fairLoss.start();
        var msgSendThread = new Thread(this.msgSendThread);
        msgSendThread.setPriority(Thread.MAX_PRIORITY-2);
        msgSendThread.start();

        var ackSendThread = new Thread(this.ackSendThread);
        ackSendThread.setPriority(Thread.MAX_PRIORITY);
        ackSendThread.start();

        var rebroadcastThread = new Thread(this.rebroadcastThread);
        rebroadcastThread.setPriority(Thread.MAX_PRIORITY-1);
        rebroadcastThread.start();
    }

    public void send(Message message, Host host) {
        if (message.isAckMessage()) {
            ackMessagesToBeSent.computeIfAbsent(message.getReceiverId(), k -> new ConcurrentHashMap<>());
            ackMessagesToBeSent.get(message.getReceiverId()).put(message, true);
        }
        else if(message.getOriginalSender() == message.getSenderId()){
            lock.lock();
            messageToBeSent[message.getReceiverId()].add(message);
            lock.unlock();
        }
        else {
            rebroadcastLock.lock();
            rebroadcastMessages[message.getReceiverId()].add(message);
            rebroadcastLock.unlock();
        }

    }

    public void stop() {
        this.isRunning.compareAndSet(true, false);
        fairLoss.stop();
    }

    public void stopSenders(){
        this.isRunning.compareAndSet(true, false);
    }

    public int getSlidingWindow(byte receiverId){
        return slidingWindows.get(receiverId);
    }

    public int getRebroadcastCount(byte receiverId){
        return rebroadcastMessages[receiverId].size();
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
            boolean removal;
            if(originalMessage.getSenderId() == originalMessage.getOriginalSender()){
                lock.lock();
                removal = messageToBeSent[originalMessage.getReceiverId()].remove(originalMessage);
                lock.unlock();
            }
            else {
                rebroadcastLock.lock();
                removal = rebroadcastMessages[originalMessage.getReceiverId()].remove(originalMessage);
                rebroadcastLock.unlock();
            }
            if (removal) {
                if(originalMessage.getOriginalSender() == originalMessage.getSenderId()){ // I am the original sender,
                    // this check works before this message is constructed by swapping receiver and sender. Because I this is the message I've received
                    // the message.getReceiverId() is equal to my id, so after swapping the sender and receiver the originalMessage.getSenderId() is equal to my id
                    // So if the original sender is equal to originalMessage.getSenderId() then I am the original sender
                    messagesDelivered.incrementAndGet(originalMessage.getReceiverId()); // Successfully delivered this message.
                    if(messagesDelivered.get(originalMessage.getReceiverId()) >= slidingWindows.get(originalMessage.getReceiverId())){
                        readyToSlide();
                    }
                    count += 1;
                    if (count % 1000 == 0) {
                        System.out.println("Sent " + count + " messages.");
                    }
                }
                else{
                    // rebroadcastCount++;
                    // if(rebroadcastCount % 10000 == 0){
                        //System.out.println("Rebroadcasted " + rebroadcastCount + " messages.");
                    // }
                    perfectLinks.slideWindow(originalMessage.getReceiverId());
                }
            }
        } else {
            perfectLinks.deliver(message);
        }
    }
}
