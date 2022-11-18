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
    private MessagePackage messagePackage;
    public UDPBulkSender(String ip, int port, MessagePackage messagePackage, DatagramSocket socket, UDPObserver udpObserver) {
        try {
            this.port = port;
            this.address = InetAddress.getByName(ip);
            this.socket = socket;
            this.observer = udpObserver;
            this.messagePackage = messagePackage;
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    public void run(){
        var buffer = messagePackage.toBytes();
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, port);
        try {
            socket.send(packet);
        }
        catch (Exception e){
            e.printStackTrace();
        }
        observer.onUDPBulkSenderExecuted();
        for(Message message: messagePackage.getMessages()){
            observer.onUDPSenderExecuted(message);
        }
    }

    public void close(){
        socket.close();
    }
}
