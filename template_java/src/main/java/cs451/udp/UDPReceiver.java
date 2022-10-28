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


    public UDPReceiver(int port, Deliverer deliverer, int maxMemory, int hostSize, boolean extraMemory){
        this.maxMemory = maxMemory + (extraMemory ? 2560000000L/hostSize : 0);
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
                usedMemory.set(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());
                if (usedMemory.get() >= maxMemory){
                    Runtime.getRuntime().gc();
                    continue;
                }
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
}
