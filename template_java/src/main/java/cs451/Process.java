package cs451;

import cs451.broadcast.BestEffortBroadcast;
import cs451.broadcast.UniformReliableBroadcast;
import cs451.links.PerfectLinks;
import cs451.Message.Message;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Process implements Deliverer, Logger{
    private final byte id;
    private final UniformReliableBroadcast broadcast;
    private final String output;

    private final ConcurrentLinkedQueue<String> logs;
    private ConcurrentLinkedQueue<String> intermediateLogs;
    private long count;
    private final AtomicBoolean writing;

    private final Timer logChecker;
    private final int hostSize;
    private AtomicBoolean sendersStopped;

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
        this.hostSize = hostList.size();
        this.writing = new AtomicBoolean(false);
        this.broadcast = new UniformReliableBroadcast(id, port, hostList, slidingWindowSize, this, this);
        // Copy logs to a new queue
        // Dequeue from logs and write to file
        logChecker = new Timer();
        logChecker.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    if (logs.size() > 3000000/ hostSize && !writing.get()) {
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

    public void send(int messageCount){
        broadcast.send(messageCount);
        System.out.println("Broadcasted all messages!");
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

    @Override
    public void deliver(Message message) {
        // System.out.println("HELLO! I'm process " + id + " and I received message: " + message);
        lock.lock();
        logs.add("d " + (message.getOriginalSender()+1) + " " + message.getId() + "\n");
        lock.unlock();
        count += 1;
        if(count % 5000 == 0){
            System.out.println("Process " + id + " received " + count + " messages");
        }
    }


    private int calcWindowSize(int hostSize){
        //return Math.min(255000/hostSize, 2000);
        if(hostSize <= 3){
            return 100000;
        }
        else if(hostSize <= 5){
            return 50000;
        }
        else if(hostSize <= 8){
            return 30000;
        }
        else if(hostSize <= 10){
            return 25000;
        }
        else if(hostSize <= 20){
            return 15000;
        }
        else if(hostSize <= 30){
            return 10000;
        }
        else if(hostSize <= 40){
            return 8000;
        }
        else if(hostSize <= 50){
            return 7000;
        }
        else if(hostSize <= 60){
            return 6000;
        }
        else if(hostSize <= 70){
            return 5000;
        }
        else if(hostSize <= 80){
            return 4000;
        }
        else if(hostSize <= 90){
            return 3000;
        }
        else{
            return 2000;
        }

//            var availableMemory = (1900000000 / hosts.size());
//            var hostSizeForDivision = Math.max((hosts.size()-1),1);
//            var temp = Math.min(((availableMemory)/(256*hostSizeForDivision)), 20000);
//            return Math.max(temp, 2000);

    }

    @Override
    public void logBroadcast(int messageId) {
        logs.add("b " + messageId + "\n");
    }
}
