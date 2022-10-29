package cs451;

import cs451.links.PerfectLinks;
import cs451.Message.Message;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Process implements Deliverer, Acknowledger{
    private final byte id;
    private final HashMap<Byte, Host> hosts;
    private final PerfectLinks links;
    private final String output;
    private int lastSentMessageId;
    private long messageCount;
    private final ConcurrentLinkedQueue<String> logs;
    private ConcurrentLinkedQueue<String> intermediateLogs;
    private long count;
    private final AtomicBoolean writing;

    private final Timer logChecker;

    private AtomicInteger sendWindow;

    private AtomicBoolean sendersStopped;
    private final int slidingWindowSize;
    Lock lock = new ReentrantLock();


    public Process(byte id, int port,
                   List<Host> hostList, String output, boolean extraMemory, int messageCount) {
        this.id = id;
        this.hosts = new HashMap<>();
        for(Host host : hostList){
            hosts.put((byte)host.getId(), host);
        }
        // 1.9 GB divided by number of hosts is the memory available to this process then each node is 32 bytes
        var availableMemory = (1900000000 / hosts.size());
        var numberOfMessages = availableMemory / 128;
        this.slidingWindowSize = numberOfMessages/(hosts.size()-1);
        this.sendWindow = new AtomicInteger(slidingWindowSize);
        System.out.println("Sliding window size: " + slidingWindowSize);
        this.links = new PerfectLinks(port, this, this.hosts, slidingWindowSize,extraMemory, this, messageCount);
        this.output = output;
        this.count = 0;
        this.messageCount = messageCount;
        this.sendersStopped = new AtomicBoolean(false);
        this.lastSentMessageId = 1;
        logs = new ConcurrentLinkedQueue<>();
        this.writing = new AtomicBoolean(false);
        // Copy logs to a new queue
        // Dequeue from logs and write to file
        logChecker = new Timer();
        logChecker.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    if (logs.size() > 3000000/ hosts.size() && !writing.get()) {
                        writing.compareAndSet(false, true);
                        // Copy logs to a new queue
                        lock.lock();
                        intermediateLogs = new ConcurrentLinkedQueue<>(logs);
                        logs.clear();
                        lock.unlock();
                        try (var outputStream = new FileOutputStream(output, true)) {
                            // Dequeue from logs and write to file
                            while (!intermediateLogs.isEmpty()) {
                                outputStream.write(intermediateLogs.peek().getBytes());
                                intermediateLogs.remove();
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        } finally {
                            writing.compareAndSet(true, false);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                finally {
                    if(sendersStopped.get()){
                        System.gc();
                    }
                }
            }
        }, 5000, 6000);
    }

    public void send(int messageCount, byte destinationId){
        this.messageCount = messageCount;
        Host host = this.hosts.get(destinationId);
        if(host == null) return;
        while(lastSentMessageId < messageCount + 1){
            if(lastSentMessageId > sendWindow.get()){
                try {
                    Runtime.getRuntime().gc();
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            else{
                links.send(new Message(lastSentMessageId, id, destinationId, id), host);
                logs.add("b " + lastSentMessageId + "\n");
                lastSentMessageId++;
            }
        }
        System.out.println("Broadcasted all messages!");
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
        logChecker.cancel();
    }

    public void startProcessing(){
        links.start();
    }

    // Write to output file
    public void writeOutput() {
        try (var outputStream = new FileOutputStream(output, true)) {
            while(!logs.isEmpty()){
                outputStream.write(logs.poll().getBytes());
            }
            if(intermediateLogs == null){
                return;
            }
            while(!intermediateLogs.isEmpty()){
                outputStream.write(intermediateLogs.poll().getBytes());
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
        logs.add("d " + (message.getSenderId()+1) + " " + message.getId() + "\n");
        lock.unlock();
        count += 1;
        if(count % 5000 == 0){
            System.out.println("Process " + id + " received " + count + " messages");
        }
        if(count == (this.hosts.size()-1)*this.messageCount){
            System.out.println("Process " + this.id + " received all messages!");
            System.gc();
        }
    }

    @Override
    public void slideSendWindow() {
        sendWindow.addAndGet(slidingWindowSize);
        System.gc();
    }

    @Override
    public void stopSenders(){
        links.stopSenders();
        System.gc();
        this.sendersStopped.compareAndSet(false, true);
        // System.out.println("Process " + (id+1) + " stopped sending messages.");
    }

}
