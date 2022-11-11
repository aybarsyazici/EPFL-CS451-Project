package cs451.udp;

import cs451.Message.Message;
import cs451.Message.MessagePackage;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.List;

public class UDPBulkSender extends Thread{
    private DatagramSocket socket;
    private InetAddress address;
    private int port;
    private UDPObserver observer;
    private byte[] buffer;
    private MessagePackage messagePackage;

    public UDPBulkSender(String ip, int port, MessagePackage messagePackage, DatagramSocket socket, UDPObserver udpObserver) {
        try {
            this.port = port;
            this.address = InetAddress.getByName(ip);
            this.socket = socket;
            this.observer = udpObserver;
            this.messagePackage = messagePackage;
            this.buffer = this.messagePackage.toBytes();

        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    public void run(){
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, port);
        try {
            socket.send(packet);
        }
        catch (Exception e){
            e.printStackTrace();
        }
        // System.out.println("Sent messages length: " + this.messagePackage.getMessages().size());
        for(Message message: this.messagePackage.getMessages()) {
            observer.onUDPSenderExecuted(message);
            // System.out.println("Sent message " + message);
        }
        buffer = null;
        messagePackage = null;
    }

    public void close(){
        socket.close();
    }
}
