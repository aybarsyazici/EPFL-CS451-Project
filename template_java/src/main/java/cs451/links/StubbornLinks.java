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
import java.util.concurrent.atomic.AtomicLong;

public class StubbornLinks implements Deliverer {
    private final FairLossLinks fairLoss;
    private final Deliverer deliverer;
    private final int[] slidingWindows;
    private final int[] messagesDelivered;

    private final int slidingWindowSize;
    private final ConcurrentHashMap<Byte, ConcurrentHashMap<Integer,MessageExtension>> messageToBeSent;
    private final ConcurrentHashMap<Byte, ConcurrentHashMap<Integer,MessageExtension>> ackMessagesToBeSent;
    private int count;
    // We need to keep the list of the messages we have already sent
    // Pass through the array -> check if they have been received by the other end.
    // if they are not received we send them again
    // Repeat continuously till they have been acknowledged, i.e., received and delivered by the other end.
    private final Runnable msgSendThread;
    private final Runnable ackMsgSendThread;
    private final int maxMemory;
    private AtomicBoolean isRunning;
    private final int messageCount;

    Acknowledger acknowledger;

    public StubbornLinks(int port, Deliverer deliverer, int hostSize, int slidingWindowSize, boolean extraMemory, Acknowledger acknowledger, int messageCount) {
        this.maxMemory = 1800000000 / hostSize; // 200Mb is left for the non heap memories of the programs
        this.fairLoss = new FairLossLinks(port, this, hostSize, maxMemory, extraMemory);
        this.deliverer = deliverer;
        this.acknowledger = acknowledger;
        this.messageCount = messageCount;
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
                    for (var host : ackMessagesToBeSent.keySet()) {
                        sendAckMessagesToBeSent(new ArrayList<>(ackMessagesToBeSent.get(host).values()));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                try{
                    Thread.sleep(100);
                }
                catch (Exception e){
                    e.printStackTrace();
                }
            }
        };
        this.msgSendThread = () -> {
            while(isRunning.get()){
                try {
                    for (var host : messageToBeSent.keySet()) {
                        sendMessagesToBeSent(new ArrayList<>(messageToBeSent.get(host).values()));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                try{
                    Thread.sleep(150);
                }
                catch (Exception e){
                    e.printStackTrace();
                }
            }
        };
    }

    private void sendMessagesToBeSent(ArrayList<MessageExtension> messages){
        if(messages.size() == 0) return;
        var usedMemory = new AtomicLong(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());
        if (usedMemory.get() > maxMemory) {
            return;
        }
        List<Message> messagesToSend = new ArrayList<>();
        Host host = messages.get(0).getHost();
        (messages).
                forEach(m -> {
                    if (!fairLoss.isInQueue(m.getMessage().getReceiverId(),m.getMessage().getId())) {
                        usedMemory.set(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());
                        if (usedMemory.get() > maxMemory) {
                            return;
                        }
                        var slidingWindowSize = slidingWindows[m.getMessage().getReceiverId()];
                        if(m.getMessage().getOriginalSender() == m.getMessage().getSenderId() &&
                                m.getMessage().getId() > slidingWindowSize){
                            // System.out.println("Message " + me.getMessage().getId() + " is not in the sliding window of " + me.getMessage().getReceiverId() + " which is " + slidingWindowSize);
                            return;
                        }
                        messagesToSend.add(m.getMessage());
                        if(messagesToSend.size() == 8){
                            fairLoss.send(new MessagePackage(messagesToSend), m.getHost());
                            messagesToSend.clear();
                        }
                    }
                });
        if(messagesToSend.size() > 0){
            fairLoss.send(new MessagePackage(messagesToSend), host);
        }
    }

    private void sendAckMessagesToBeSent(ArrayList<MessageExtension> messages) {
        if (fairLoss.isQueueFull()) {
            // System.out.println("Queue IS full.");
            return;
        }
        List<Message> messagesToSend = new ArrayList<>();
        (messages).
                forEach(m -> {
                    if (!fairLoss.isInQueue(m.getMessage().getReceiverId(),m.getMessage().getId())) {
                        messagesToSend.add(m.getMessage());
                        ackMessagesToBeSent.get(m.getMessage().getReceiverId()).remove(m.getMessage().getId());
                        if(messagesToSend.size() == 8){
                            fairLoss.send(new MessagePackage(messagesToSend), m.getHost());
                            messagesToSend.clear();
                        }
                    }
                });
        if(messagesToSend.size() > 0){
            fairLoss.send(new MessagePackage(messagesToSend), messages.get(0).getHost());
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
            ackMessagesToBeSent.get(message.getReceiverId()).put(message.getId(), new MessageExtension(message, host));
            return;
        }
        messageToBeSent.computeIfAbsent(message.getReceiverId(), k -> new ConcurrentHashMap<>());
        messageToBeSent.get(message.getReceiverId()).put(message.getId(), new MessageExtension(message, host));
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
            messageToBeSent.computeIfAbsent(message.getSenderId(), k -> new ConcurrentHashMap<>());
            if (messageToBeSent.get(message.getSenderId()).containsKey(message.getId())) {
                messageToBeSent.get(message.getSenderId()).remove(message.getId());
                if(message.getOriginalSender() == message.getReceiverId()){
                    messagesDelivered[message.getSenderId()]++; // Successfully delivered this message.
                    if(messagesDelivered[message.getSenderId()] >= slidingWindows[message.getSenderId()]){
                        System.out.println("Sliding window of " + message.getSenderId() + " is increased by 1.");
                        slidingWindows[message.getSenderId()] += this.slidingWindowSize;
                        acknowledger.slideSendWindow(message.getSenderId());
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
}
