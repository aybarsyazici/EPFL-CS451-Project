package cs451.links;

import cs451.Acknowledger;
import cs451.Deliverer;
import cs451.Host;
import cs451.Message.Message;
import cs451.Message.MessageExtension;
import cs451.Message.MessagePackage;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class StubbornLinks implements Deliverer {
    private final FairLossLinks fairLoss;
    private final Deliverer deliverer;
    private final int[] slidingWindows;
    private final int[] messagesDelivered;

    private final int slidingWindowSize;
    private final ConcurrentHashMap<Byte, ConcurrentHashMap<Message, Boolean>> messageToBeSent;
    private final ConcurrentHashMap<Byte, ConcurrentHashMap<Message, Boolean>> ackMessagesToBeSent;
    private int count;
    private final Runnable msgSendThread;
    private final Runnable ackMsgSendThread;
    private final int maxMemory;
    private AtomicBoolean isRunning;

    private final HashMap<Byte, Host> hosts;

    Acknowledger acknowledger;

    public StubbornLinks(int port, HashMap<Byte, Host> hosts, Deliverer deliverer, int hostSize, int slidingWindowSize, boolean extraMemory, Acknowledger acknowledger, int messageCount) {
        this.maxMemory = 1800000000 / hostSize; // 200Mb is left for the non heap memories of the programs
        this.fairLoss = new FairLossLinks(port, this, hostSize, maxMemory, extraMemory);
        this.deliverer = deliverer;
        this.hosts = hosts;
        this.acknowledger = acknowledger;
        this.messageToBeSent = new ConcurrentHashMap<>();
        this.ackMessagesToBeSent = new ConcurrentHashMap<>();
        this.slidingWindows = new int[hostSize]; // Keep the sliding window of each host
        this.messagesDelivered = new int[hostSize]; // Keep the number of messages delivered to each host
        this.slidingWindowSize = slidingWindowSize;
        for(int i = 0; i < hostSize; i++){
            slidingWindows[i] = slidingWindowSize;
        }
        for(int i = 0; i < hostSize; i++){
            messagesDelivered[i] = 0;
        }
        // this.runnerTasks = new ConcurrentHashMap<>();
        this.count = 0;
        this.isRunning = new AtomicBoolean(true);
        this.ackMsgSendThread = () -> {
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
                    Thread.sleep(200);
                }
                catch (Exception e){
                    e.printStackTrace();
                }
            }
        };
        this.msgSendThread = () -> {
            while(isRunning.get()){
                try {
                    for (byte hostId : messageToBeSent.keySet()) {
                        if(messageToBeSent.get(hostId).size() > 0){
                            var temp = Collections.list(messageToBeSent.get(hostId).keys());
                            sendMessagesToBeSent(temp, this.hosts.get(hostId));
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
    }

    private void sendMessagesToBeSent(List<Message> messages, Host host){
        if(messages.size() == 0) return;
        List<Message> messagesToSend = new ArrayList<>();
        (messages).
                forEach(m -> {
                    if (!fairLoss.isInQueue(m)) {
                        var slidingWindowSize = slidingWindows[m.getReceiverId()];
                        if(m.getOriginalSender() == m.getSenderId() &&
                                m.getId() > slidingWindowSize){
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
                    if (!fairLoss.isInQueue(m)){
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
        new Thread(ackMsgSendThread, "Ack message send thread").start();
    }

    public void send(Message message, Host host) {
        if (message.isAckMessage()) {
            ackMessagesToBeSent.computeIfAbsent(message.getReceiverId(), k -> new ConcurrentHashMap<>());
            ackMessagesToBeSent.get(message.getReceiverId()).put(message, true);
            return;
        }
        messageToBeSent.computeIfAbsent(message.getReceiverId(), k -> new ConcurrentHashMap<>());
        messageToBeSent.get(message.getReceiverId()).put(message, true);
    }

    public void stop() {
        this.isRunning.compareAndSet(true, false);
        fairLoss.stop();
    }

    public void stopSenders(){
        this.isRunning.compareAndSet(true, false);
    }

    @Override
    public void deliver(Message message) {
        if (message.isAckMessage()) { // I have sent this message and received it back.
            Message originalMessage = message.swapSenderReceiver();
            if (messageToBeSent.get(originalMessage.getReceiverId()).containsKey(originalMessage)) {
                messageToBeSent.get(originalMessage.getReceiverId()).remove(originalMessage);
                if(originalMessage.getOriginalSender() == originalMessage.getSenderId()){ // I am the original sender
                    messagesDelivered[originalMessage.getReceiverId()]++; // Successfully delivered this message.
                    if(messagesDelivered[originalMessage.getReceiverId()] >= slidingWindows[originalMessage.getReceiverId()]){
                        System.out.println("Sliding window of " + originalMessage.getReceiverId() + " is increased by 1.");
                        slidingWindows[originalMessage.getReceiverId()] += this.slidingWindowSize;
                        acknowledger.slideSendWindow(originalMessage.getReceiverId());
                    }
                    count += 1;
                    if (count % 5000 == 0) {
                        System.out.println("Sent " + count + " messages.");
                    }
                }
            }
        } else {
            deliverer.deliver(message);
        }
    }
    @Override
    public void confirmDeliver(Message message){}
}
