package cs451.udp;

import cs451.Message.Message;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class UDPSender extends Thread{
    private DatagramSocket socket;
    private InetAddress address;
    private int port;
    private byte[] buf;

    private UDPObserver observer;
    private int messageId;
    private byte receiverId;

    public UDPSender(String ip, int port, Message message, DatagramSocket socket, UDPObserver udpObserver) {
        try {
            this.port = port;
            this.address = InetAddress.getByName(ip);
            this.socket = socket;
            // ByteArrayOutputStream baos = new ByteArrayOutputStream(256);
            // ObjectOutputStream oos = new ObjectOutputStream(baos);
            // oos.writeObject(message);
            // this.buf = baos.toByteArray();
            // oos.close();
            this.buf = message.toByteArray();
            this.messageId = message.getId();
            this.receiverId = message.getReceiverId();
            this.observer = udpObserver;
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    public void run(){
        DatagramPacket packet = new DatagramPacket(buf, buf.length, address, port);
        try {
            socket.send(packet);
        }
        catch (Exception e){
            e.printStackTrace();
        }
        observer.onUDPSenderExecuted(this.receiverId, this.messageId);
        buf = null;
    }

    public void close(){
        socket.close();
    }
}
