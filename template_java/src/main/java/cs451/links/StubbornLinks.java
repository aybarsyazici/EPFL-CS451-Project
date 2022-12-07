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
    private final List<Message>[] messageToBeSent;
    private final ConcurrentHashMap<Byte, ConcurrentHashMap<Message, Boolean>> ackMessagesToBeSent;
    private int count;
    private final Runnable msgSendThread;
    private final Runnable ackSendThread;
    private final AtomicBoolean isRunning;
    private final HashMap<Byte, Host> hosts;
    private final Lock lock;

    public StubbornLinks(int port, HashMap<Byte, Host> hosts,
                         PerfectLinks perfectLinks,
                         int hostSize,
                         boolean extraMemory,
                         int proposalSetSize) {
        this.fairLoss = new FairLossLinks(port, this, hostSize, proposalSetSize);
        this.perfectLinks = perfectLinks;
        this.lock = new ReentrantLock();
        this.hosts = hosts;
        this.messageToBeSent = new List[hostSize];
        this.ackMessagesToBeSent = new ConcurrentHashMap<>();
        for(int i = 0; i < hostSize; i++){
            this.messageToBeSent[i] = new ArrayList<>();
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
        // Get memory usage
        (messages).
                forEach(m -> {
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
        if (message.isAckMessage()) {
            ackMessagesToBeSent.computeIfAbsent(message.getReceiverId(), k -> new ConcurrentHashMap<>());
            ackMessagesToBeSent.get(message.getReceiverId()).put(message, true);
        }
        else{
            lock.lock();
            messageToBeSent[message.getReceiverId()].add(message);
            lock.unlock();
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
        if (message.isAckMessage()) { // I have sent this message and received it back.
            Message originalMessage = message.swapSenderReceiver();
            lock.lock();
            boolean removal = messageToBeSent[originalMessage.getReceiverId()].remove(originalMessage);
            lock.unlock();
            if (removal) {
                count += 1;
                if (count % 1000 == 0) {
                    System.out.println("Sent " + count + " messages.");
                }
            }
        }
        perfectLinks.deliver(message);
    }
}
