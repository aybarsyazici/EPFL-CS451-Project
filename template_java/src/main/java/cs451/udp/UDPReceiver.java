package cs451.udp;

import cs451.Deliverer;
import cs451.Message.Message;
import cs451.Message.MessagePackage;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.concurrent.atomic.AtomicLong;

public class UDPReceiver extends Thread{
    private boolean running;
    private byte[] buf = new byte[64];
    private DatagramSocket socket;
    private final Deliverer deliverer;
    private final long maxMemory;
    private final AtomicLong usedMemory;
    private final DatagramPacket packet;
    private int count;

    public UDPReceiver(int port, Deliverer deliverer, int maxMemory, int hostSize, boolean extraMemory){
        this.maxMemory = maxMemory + ( extraMemory ? calculateExtraMemory(hostSize) : 0);
        this.count = 0;
        this.deliverer = deliverer;
        this.packet = new DatagramPacket(buf, buf.length);
        usedMemory = new AtomicLong(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());
        try{
            this.socket = new DatagramSocket(port);
            System.out.println("UDPReceiver: Created socket on port " + port);
        }
        catch (SocketException e){
            e.printStackTrace();
        }
    }

    public void run(){
        running = true;
        while(running){
            try {
//                usedMemory.set(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());
//                if (usedMemory.get() >= maxMemory){
//                    count++;
//                    if(count==5){
//                        count = 0;
//                        Runtime.getRuntime().gc();
//                    }
//                    continue;
//                }
                socket.receive(packet);
                MessagePackage messagePackage = MessagePackage.fromBytes(packet.getData());
                for(Message message : messagePackage.getMessages()){
                    if(message.getId() == 0) continue;
                    deliverer.deliver(message);
                }
                messagePackage = null;
            }
            catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    public void haltReceiving(){
        running = false;
        socket.close();
    }

    private long calculateExtraMemory(int hostSize){
        if(hostSize <= 5){
            return 200000000; //200MB
        }
        else if(hostSize <= 8){
            return 250000000; //250MB
        }
        else if(hostSize <= 30){
            return 300000000; //300MB
        }
        else if(hostSize <= 50){
            return 400000000; //400MB
        }
        else if(hostSize <= 60){
            return 300000000; //300MB
        }
        else if(hostSize <= 70){
            return 250000000; //250MB
        }
        else if(hostSize <= 80){
            return 200000000; //200MB
        }
        else{
            return Math.min(8960000000L/hostSize,100000000);
        }
    }
}
