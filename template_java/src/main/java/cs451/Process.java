package cs451;

import cs451.broadcast.UniformReliableBroadcast;
import cs451.interfaces.Deliverer;
import cs451.interfaces.Logger;
import cs451.Message.Message;
import cs451.interfaces.UniformDeliverer;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Process implements Deliverer, Logger {
    private final byte id;
    private final UniformReliableBroadcast broadcast;
    private final String output;

    private final ConcurrentLinkedQueue<String> logs;
    private ConcurrentLinkedQueue<String> intermediateLogs;
    private long count;
    private final AtomicBoolean writing;

    private final Timer logChecker;
    private AtomicBoolean sendersStopped;
    private final int messageCount;
    Lock lock = new ReentrantLock();


    public Process(byte id, int port,
                   List<Host> hostList, String output, boolean extraMemory, int messageCount) {
        this.id = id;
        this.output = output;
        this.count = 0;
        this.sendersStopped = new AtomicBoolean(false);
        int slidingWindowSize = calcWindowSize(hostList.size());
        System.out.println("Sliding window size: " + slidingWindowSize);
        logs = new ConcurrentLinkedQueue<>();
        this.messageCount = messageCount;
        this.writing = new AtomicBoolean(false);
        this.broadcast = new UniformReliableBroadcast(id, port, hostList, slidingWindowSize, this, this, messageCount);
        // Copy logs to a new queue
        // Dequeue from logs and write to file
        logChecker = new Timer();
        logChecker.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    if (logs.size() > 10000 && !writing.get()) {
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
        }, 0, 6000);
    }

    public void send(){
        broadcast.send();
    }

    public int getId() {
        return id;
    }


    public void stopProcessing(){
        broadcast.stop();
        logChecker.cancel();
    }

    public void startProcessing(){
        broadcast.start();
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

    public void deliver (Message message) {
        lock.lock();
        logs.add("d " + (message.getOriginalSender()+1) + " " + message.getId() + "\n");
        lock.unlock();
        count += 1;
        if(count % 5000 == 0){
            System.out.println("Process " + id + " received " + count + " messages");
        }
    }

    private int calcWindowSize(int hostSize){
        if(hostSize == 2){
            return 7000;
        }
        if(hostSize == 3){
            return 3000;
        }
        if(hostSize == 4){
            return 2500;
        }
        if(hostSize == 5){
            return 2000;
        }
        if(hostSize == 6){
            return 1500;
        }
        if(hostSize == 7){
            return 1000;
        }
        if(hostSize == 8){
            return 800;
        }
        if(hostSize == 32){
            return 40;
        }
        return Math.max(40960/(hostSize*hostSize),5);
    }

    @Override
    public void logBroadcast(int messageId) {
        lock.lock();
        logs.add("b " + messageId + "\n");
        lock.unlock();
    }

    private void logAllBroadcast(int messageCount){
        for(int i = 1; i < messageCount+1; i++){
            logs.add("b " + i + "\n");
        }
    }
}
