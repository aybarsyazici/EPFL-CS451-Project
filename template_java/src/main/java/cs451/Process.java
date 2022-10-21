package cs451;

import cs451.links.PerfectLinks;
import cs451.udp.Message;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Process implements Deliverer {
    private final byte id;
    private final HashMap<Byte, Host> hosts;
    private final PerfectLinks links;
    private final String output;
    private int lastSentMessageId;
    private long messageCount;
    private final ConcurrentLinkedQueue<String> logs;
    Queue<String> intermediateLogs;
    private long count;
    private final AtomicBoolean writing;

    Lock lock = new ReentrantLock();


    public Process(byte id, int port,
                   List<Host> hostList, String output) {
        this.id = id;
        this.hosts = new HashMap<>();
        for(Host host : hostList){
            hosts.put((byte)host.getId(), host);
        }
        this.links = new PerfectLinks(port, this, this.hosts);
        this.output = output;
        this.count = 0;
        this.lastSentMessageId = 1;
        logs = new ConcurrentLinkedQueue<>();
        this.writing = new AtomicBoolean(false);
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    if(logs.size() > 300000 && !writing.get()){
                        writing.compareAndSet(false, true);
                        // Copy logs to a new queue
                        lock.lock();
                        intermediateLogs = new LinkedList<>(logs);
                        logs.clear();
                        lock.unlock();
                        try (var outputStream = new FileOutputStream(output, true)) {
                            // Dequeue from logs and write to file
                            while(!intermediateLogs.isEmpty()){
                                outputStream.write(intermediateLogs.peek().getBytes());
                                intermediateLogs.remove();
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        finally {
                            writing.compareAndSet(true, false);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, 6000, 5000);
    }

    public void send(int messageCount, byte destinationId){
        this.messageCount = messageCount;
        Host host = this.hosts.get(destinationId);
        if(host == null) return;
        // var maxMemory = 2000000000 / hosts.size(); // 2GB Memory divided per process, for some reason this didn't work...
        var maxMemory = 900000000 / hosts.size(); // 1.2GB Memory divided per process
        while(lastSentMessageId < messageCount + 1){
             var usedMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
             // System.out.println("Used memory: " + usedMemory);
             // System.out.println("Max memory: " + maxMemory);
             // System.out.println("Last sent message id: " + lastSentMessageId);
             if(usedMemory > maxMemory){
                // System.out.println("Waiting for memory to be freed");
                try {
                    Runtime.getRuntime().gc();
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                continue;
            }
            links.send(new Message(lastSentMessageId, id, destinationId, id), host);
            logs.add("b " + lastSentMessageId + "\n");
            lastSentMessageId++;
        }
    }

    public int getId() {
        return id;
    }

    public void setMessageCount(long messageCount) {
        this.messageCount = messageCount;
    }

    public PerfectLinks getLinks() {
        return links;
    }

    public void stopProcessing(){
        links.stop();
    }

    public void startProcessing(){
        links.start();
    }

    // Write to output file
    public void writeOutput() {
        try (var outputStream = new FileOutputStream(output, true)) {
            logs.forEach(s -> {
                try {
                    outputStream.write(s.getBytes());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            if(intermediateLogs != null){
                intermediateLogs.forEach(s -> {
                    try {
                        outputStream.write(s.getBytes());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Boolean isWriting(){
        return writing.get();
    }

    @Override
    public void deliver(Message message) {
        // System.out.println("HELLO! I'm process " + id + " and I received message: " + message);
        lock.lock();
        logs.add("d " + message.getSenderId() + " " + message.getId() + "\n");
        lock.unlock();
        count += 1;
         if(count % 5000 == 0){
             System.out.println("Process " + id + " received " + count + " messages");
         }
        if(count == (this.hosts.size()-1)*this.messageCount){ System.out.println("Process " + this.id + " received all messages!"); }
    }

}
