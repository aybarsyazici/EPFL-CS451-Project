package cs451.udp;

import cs451.Deliverer;
import cs451.Message;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class UDPReceiver extends Thread{
    private boolean running;
    private byte[] buf = new byte[256];
    private DatagramSocket socket;
    private final Deliverer deliverer;

    public UDPReceiver(int port, Deliverer deliverer){
        this.deliverer = deliverer;
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
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            try {
                socket.receive(packet);
                // Get message from packet as string
                // String mesg = new String(packet.getData(), 0, packet.getLength());
                try{
                    // Convert the packet data to Message
                    ByteArrayInputStream in = new ByteArrayInputStream(packet.getData());
                    ObjectInputStream is = new ObjectInputStream(in);
                    Message message = (Message) is.readObject();
                    deliverer.deliver(message);
                }
                catch (Exception e){
                    e.printStackTrace();
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
