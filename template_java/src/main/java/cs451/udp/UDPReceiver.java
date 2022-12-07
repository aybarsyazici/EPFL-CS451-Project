package cs451.udp;

import cs451.interfaces.Deliverer;
import cs451.Message.Message;
import cs451.Message.MessagePackage;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

public class UDPReceiver extends Thread{
    public final int proposalSetSize;
    private boolean running;
    private byte[] buf;
    private DatagramSocket socket;
    private final Deliverer deliverer;
    private final DatagramPacket packet;

    public UDPReceiver(int port, Deliverer deliverer, int proposalSetSize){
        this.proposalSetSize = proposalSetSize;
        this.deliverer = deliverer;
        this.buf = new byte[88 + proposalSetSize*4*8];
        this.packet = new DatagramPacket(buf, buf.length);
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
                socket.receive(packet);
                MessagePackage messagePackage = MessagePackage.fromBytes(packet.getData(),proposalSetSize);
                for(Message message : messagePackage.getMessages()){
                    if(message.getId() == 0) continue;
                    deliverer.deliver(message);
                }
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
