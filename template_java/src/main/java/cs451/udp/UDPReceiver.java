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
    private final ConcurrentLinkedQueue<Message> messages;

    public UDPReceiver(int port, Deliverer deliverer, int proposalSetSize){
        this.proposalSetSize = proposalSetSize;
        this.deliverer = deliverer;
        this.buf = new byte[(11 + proposalSetSize*4)];
        this.messages = new ConcurrentLinkedQueue<>();
        this.packet = new DatagramPacket(buf, buf.length);
        try{
            this.socket = new DatagramSocket(port);
            System.out.println("UDPReceiver: Created socket on port " + port);
        }
        catch (SocketException e){
            e.printStackTrace();
        }
        this.running = true;
        new Thread(()->{
            while(running){
                while(messages.size() > 0){
                    deliverer.deliver(messages.poll());
                }
                try{
                    Thread.sleep(1000);
                }
                catch (Exception e){
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public void run(){
        while(running){
            try {
                socket.receive(packet);
                MessagePackage messagePackage = MessagePackage.fromBytes(packet.getData(),proposalSetSize);
                for(Message message : messagePackage.getMessages()){
                    if(message.getId() == 0) continue;
                    messages.add(message);
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
}
