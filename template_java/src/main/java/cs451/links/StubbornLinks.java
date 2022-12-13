package cs451.links;

import cs451.interfaces.Acknowledger;
import cs451.interfaces.Deliverer;
import cs451.Host;
import cs451.Message.Message;
import cs451.Message.MessagePackage;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class StubbornLinks implements Deliverer {
    private final FairLossLinks fairLoss;
    private final PerfectLinks perfectLinks;
    private final ConcurrentHashMap<Byte, ConcurrentHashMap<Message, Boolean>> messageToBeSent;
    private final ConcurrentHashMap<Byte, ConcurrentHashMap<Message, Boolean>> ackMessagesToBeSent;
    private int count;
    private final int hostSize;
    private final Runnable msgSendThread;
    private final Runnable ackSendThread;
    private final AtomicBoolean isRunning;
    private final HashMap<Byte, Host> hosts;
    public StubbornLinks(int port, HashMap<Byte, Host> hosts,
                         PerfectLinks perfectLinks,
                         int proposalSetSize) {
        this.fairLoss = new FairLossLinks(port, this, proposalSetSize);
        this.perfectLinks = perfectLinks;
        this.hosts = hosts;
        this.hostSize = hosts.size();
        this.messageToBeSent = new ConcurrentHashMap<>();
        this.ackMessagesToBeSent = new ConcurrentHashMap<>();
        Random rand = new Random(); //instance of random class
        // this.runnerTasks = new ConcurrentHashMap<>();
        this.count = 0;
        this.isRunning = new AtomicBoolean(true);
        this.msgSendThread = () -> {
            while(isRunning.get()){
                try {
                    var startIndex = 0;
                    for (int i = 0; i < hostSize; i++) {
                        byte hostId = (byte)((startIndex + i)%hostSize);
                        if(messageToBeSent.containsKey(hostId) && messageToBeSent.get(hostId).size() > 0){
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
        this.ackSendThread = () -> {
            while(isRunning.get()){
                try {
                    var startIndex = 0;
                    for (int i = 0; i < hostSize; i++) {
                        byte hostId = (byte)((startIndex + i)%hostSize);
                        if(ackMessagesToBeSent.containsKey(hostId) && ackMessagesToBeSent.get(hostId).size() > 0){
                            var temp = Collections.list(ackMessagesToBeSent.get(hostId).keys());
                            sendAckMessagesToBeSent(temp, this.hosts.get(hostId));
                        }
                    }
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

    private boolean shouldBeDeleted(Message message){
        if(!message.isDeliveredMessage()){
            if(perfectLinks.isDelivered(message.getLatticeRound())) return true;
            return perfectLinks.getActiveProposalNumber(message.getLatticeRound()) != message.getId();
        }
        return false;
    }

    private void sendMessagesToBeSent(List<Message> messages, Host host){
        if(messages.size() == 0) return;
        List<Message> messagesToSend = new ArrayList<>();
        (messages).
                forEach(m -> {
                        if(shouldBeDeleted(m)){
                            messageToBeSent.get(m.getReceiverId()).remove(m);
                            return;
                        }
                        if (!fairLoss.isinQueue(m)) {
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
                    if (!fairLoss.isinQueue(m)) {
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
        var msgSendThread = new Thread(this.msgSendThread);
        msgSendThread.setPriority(Thread.MAX_PRIORITY-2);
        msgSendThread.start();

        var ackSendThread = new Thread(this.ackSendThread);
        ackSendThread.setPriority(Thread.MAX_PRIORITY);
        ackSendThread.start();

    }

    public void send(Message message, Host host) {
        if (message.isAckMessage() || message.isDeliveredAck()) {
            ackMessagesToBeSent.computeIfAbsent(message.getReceiverId(), k -> new ConcurrentHashMap<>());
            ackMessagesToBeSent.get(message.getReceiverId()).put(message, true);
        }
        else{
            messageToBeSent.computeIfAbsent(message.getReceiverId(), k -> new ConcurrentHashMap<>());
            messageToBeSent.get(message.getReceiverId()).put(message, true);
        }
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
        try{
            if(message.isDeliveredAck()){
                Message originalMessage = message.swapSenderReceiver();
                messageToBeSent.get(originalMessage.getReceiverId()).remove(originalMessage);
            }
            else if (message.isAckMessage()) { // I have sent this message and received it back
                Message originalMessage = message.swapSenderReceiver();
                var removal = messageToBeSent.get(originalMessage.getReceiverId()).remove(originalMessage);
                if (removal != null && removal) {
                    perfectLinks.deliver(message);
                    count += 1;
                    if (count % 1000 == 0) {
                        System.out.println("Sent " + count + " messages.");
                    }
                }
            }
            else{
                perfectLinks.deliver(message);
            }
        }
        catch (Exception e){
            e.printStackTrace();
            System.out.println("ERROR IN STUBBORN LINKS DELIVER MSG: " + message.printWithoutSet());
            System.out.println("ERROR IN STUBBORN LINKS DELIVER ORGMESSG: " + message.swapSenderReceiver().printWithoutSet());

        }
    }

    @Override
    public int getMaxLatticeRound() {
        return perfectLinks.getMaxLatticeRound();
    }
    @Override
    public int getMinLatticeRound() {
        return perfectLinks.getMinLatticeRound();
    }

}
