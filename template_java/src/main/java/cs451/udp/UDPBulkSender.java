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
    private byte receiverId;
    private List<Integer> messageIds;

    public UDPBulkSender(String ip, int port, byte receiverId, byte[] buffer, List<Integer> messageIds, DatagramSocket socket, UDPObserver udpObserver) {
        try {
            this.port = port;
            this.address = InetAddress.getByName(ip);
            this.socket = socket;
            this.buffer = buffer;
            this.observer = udpObserver;
            this.receiverId = receiverId;
            this.messageIds = messageIds;
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
        for(int msgId: messageIds) {
            observer.onUDPSenderExecuted(receiverId, msgId);
        }
        buffer = null;
    }

    public void close(){
        socket.close();
    }
}
