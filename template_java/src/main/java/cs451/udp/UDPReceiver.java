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
    private byte[] buf = new byte[256];
    private DatagramSocket socket;
    private final Deliverer deliverer;
    private final int maxMemory;
    AtomicLong usedMemory;


    public UDPReceiver(int port, Deliverer deliverer, int maxMemory){
        this.maxMemory = maxMemory;
        this.deliverer = deliverer;
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
                if (usedMemory.get() >= maxMemory + 5000000){
                    Runtime.getRuntime().gc();
                    continue;
                }
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
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
