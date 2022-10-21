package cs451.links;

import cs451.Deliverer;
import cs451.Host;
import cs451.udp.Message;
import cs451.udp.MessageExtension;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class StubbornLinks implements Deliverer {
    private final FairLossLinks fairLoss;
    private final Deliverer deliverer;
    private final Timer timer;
    private final int hostSize;
    private final int[] slidingWindows;
    private final int[] messagesDelivered;

    private final int slidingWindowSize;
    private final ConcurrentHashMap<Integer, MessageExtension> messageToBeSent;
    private final ConcurrentHashMap<Map.Entry<Byte, Integer>, MessageExtension> ackMessagesToBeSent;
    // private final ConcurrentHashMap<Integer, ConcurrentLinkedQueue<Future>> runnerTasks;
    private int count;
    // We need to keep the list of the messages we have already sent
    // Pass through the array -> check if they have been received by the other end.
    // if they are not received we send them again
    // Repeat continuously till they have been acknowledged, i.e., received and delivered by the other end.

    public StubbornLinks(int port, Deliverer deliverer, int hostSize, int slidingWindowSize) {
        this.fairLoss = new FairLossLinks(port, this, hostSize);
        this.deliverer = deliverer;
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
        this.hostSize = hostSize;
        this.count = 0;
        this.timer = new Timer();
    }

    private void sendMessagesToBeSent(ArrayList<Object> messages, Boolean isAck) {
       if (isAck && fairLoss.isQueueFull()) {
           // System.out.println("Queue IS full.");
           return;
       }
        // System.out.println("Queue not full.");
        AtomicLong usedMemory = new AtomicLong(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());
        // System.out.println("Used memory: " + usedMemory + " bytes and messages left to send: " + arrayToLoop.size());
        var maxMemory = 1800000000 / hostSize; // 200Mb is left for the non heap memories of the programs
        if (!isAck && usedMemory.get() > maxMemory) {
            try {
                Runtime.getRuntime().gc();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return;
        }
        // if(arrayToLoop.size() > 0)System.out.println("Message amount to send: " + arrayToLoop.size());
        // Collections.shuffle(messages);
        (messages).
                forEach(m -> {
                    MessageExtension me = (MessageExtension) m;
                    if (!fairLoss.isInQueue(me.getMessage().getSenderId(),me.getMessage().getId())) {
                        usedMemory.set(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());
                        if (!isAck && usedMemory.get() > maxMemory) {
                            try {
                                Runtime.getRuntime().gc();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            return;
                        }
                        if(!isAck){
                            var slidingWindowSize = slidingWindows[me.getMessage().getReceiverId()];
                            if(me.getMessage().getId() > slidingWindowSize){
                                // System.out.println("Message " + me.getMessage().getId() + " is not in the sliding window of " + me.getMessage().getReceiverId() + " which is " + slidingWindowSize);
                                return;
                            }
                        }
                        fairLoss.send(me.getMessage(), me.getHost());
                        if(isAck) {
                            ackMessagesToBeSent.remove(Map.entry(me.getMessage().getSenderId(), me.getMessage().getId()));
                        }
                        // System.out.println("Sending " + me.getMessage().getId());
                    } // else {
                        // System.out.println("Message " + me.getMessage().getId() + " is already in the queue.");
                    // }
                });
        // System.out.println("_____________________________________________________________");
    }

    public void start() {
        fairLoss.start();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    sendMessagesToBeSent(new ArrayList<>(messageToBeSent.values()), false);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, 100, 300);
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    sendMessagesToBeSent(new ArrayList<>(ackMessagesToBeSent.values()), true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, 100, 500);
    }

    public void send(Message message, Host host) {
        if (message.isAckMessage()) {
            ackMessagesToBeSent.put(Map.entry(message.getSenderId(),message.getId()), new MessageExtension(message, host));
            return;
        }
        messageToBeSent.put(message.getId(), new MessageExtension(message, host));
    }

    public void stop() {
        timer.cancel();
        fairLoss.stop();
    }

    @Override
    public void deliver(Message message) {
        if (message.getOriginalSender() == message.getReceiverId()) { // I have sent this message and received it back.
            if (messageToBeSent.containsKey(message.getId())) {
                try {
                    // runnerTasks.get(message.getId()).forEach(f -> f.cancel(false));
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    messageToBeSent.remove(message.getId());
                    messagesDelivered[message.getSenderId()]++; // Successfully delivered this message.
                    if(messagesDelivered[message.getSenderId()] >= slidingWindows[message.getSenderId()]){
                        slidingWindows[message.getSenderId()] += this.slidingWindowSize;
                        System.out.println("Sliding window of " + message.getSenderId() + " is now " + slidingWindows[message.getSenderId()]);
                    }
                    // runnerTasks.remove(message.getId());
                }
                count += 1;
                if (count % 5000 == 0) {
                    System.out.println("Sent " + count + " messages.");
                }
            }
        } else {
            deliverer.deliver(message);
        }
    }
}
