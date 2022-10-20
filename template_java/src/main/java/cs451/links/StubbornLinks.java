package cs451.links;

import cs451.Deliverer;
import cs451.Host;
import cs451.udp.Message;
import cs451.udp.MessageExtension;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class StubbornLinks implements Deliverer {
    private final FairLossLinks fairLoss;
    private final Deliverer deliverer;
    private final Timer timer;
    private final HashMap<Byte, Host> hosts;
    private final ConcurrentHashMap<Integer, MessageExtension> messageToBeSent;
    private final ConcurrentHashMap<Integer, MessageExtension> ackMessagesToBeSent;
    // private final ConcurrentHashMap<Integer, ConcurrentLinkedQueue<Future>> runnerTasks;
    private int count;
    // We need to keep the list of the messages we have already sent
    // Pass through the array -> check if they have been received by the other end.
    // if they are not received we send them again
    // Repeat continuously till they have been acknowledged, i.e., received and delivered by the other end.

    public StubbornLinks(int port, Deliverer deliverer, HashMap<Byte, Host> hosts) {
        this.fairLoss = new FairLossLinks(port, this, hosts.size());
        this.deliverer = deliverer;
        this.messageToBeSent = new ConcurrentHashMap<>();
        this.ackMessagesToBeSent = new ConcurrentHashMap<>();
        // this.runnerTasks = new ConcurrentHashMap<>();
        this.hosts = hosts;
        this.count = 0;
        this.timer = new Timer();
    }

    private void sendMessagesToBeSent(ArrayList<Object> messages, Boolean isAck) {
       if (isAck && fairLoss.isQueueFull()) {
           // System.out.println("Queue IS full.");
           return;
       }
        // System.out.println("Queue not full.");
        long usedMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        // System.out.println("Used memory: " + usedMemory + " bytes and messages left to send: " + arrayToLoop.size());
        var maxMemory = 1900000000 / hosts.size();
        if (!isAck && usedMemory > maxMemory) {
            try {
                Runtime.getRuntime().gc();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return;
        }
        // if(arrayToLoop.size() > 0)System.out.println("Message amount to send: " + arrayToLoop.size());
        (messages).
                forEach(m -> {
                    MessageExtension me = (MessageExtension) m;
                    if (!fairLoss.isInQueue(me.getMessage().getId())) {
                        fairLoss.send(me.getMessage(), me.getHost());
                        if(isAck) {
                            ackMessagesToBeSent.remove(me.getMessage().getId());
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
                    sendMessagesToBeSent(new ArrayList<>(Arrays.asList(messageToBeSent.values().toArray())), false);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, 100, 300);
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    sendMessagesToBeSent(new ArrayList<>(Arrays.asList(ackMessagesToBeSent.values().toArray())), true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, 100, 300);
    }

    public void send(Message message, Host host) {
        if (message.isAckMessage()) {
            ackMessagesToBeSent.put(message.getId(), new MessageExtension(message, host));
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
        Host senderHost = hosts.get(message.getOriginalSender());
        if (message.getOriginalSender() == message.getReceiverId()) { // I have sent this message and received it back.
            if (messageToBeSent.containsKey(message.getId())) {
                try {
                    // runnerTasks.get(message.getId()).forEach(f -> f.cancel(false));
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    messageToBeSent.remove(message.getId());
                    // runnerTasks.remove(message.getId());
                }
                // count += 1;
                // if (count == 5000) {
                    // System.out.println("Sent 5k messages.");
                    // count = 0;
                // }
            }
        } else {
            deliverer.deliver(message);
            send(new Message(message, message.getReceiverId(), message.getOriginalSender()), senderHost);
            // To inform the original sender of this message I need a way to access the hosts array.
        }
    }
}
