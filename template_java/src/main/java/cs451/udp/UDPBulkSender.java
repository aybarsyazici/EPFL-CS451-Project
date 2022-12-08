package cs451.udp;

import cs451.Message.Message;
import cs451.Message.MessagePackage;
import cs451.links.FairLossLinks;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.List;

public class UDPBulkSender implements Runnable{
    private DatagramSocket socket;
    private InetAddress address;
    private int port;
    private final byte[] buffer;
    public UDPBulkSender(String ip, int port, byte[] buf, DatagramSocket socket) {
        this.buffer = buf;
        try {
            this.port = port;
            this.address = InetAddress.getByName(ip);
            this.socket = socket;
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    public void run(){
        try {
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, port);
            socket.send(packet);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    public void close(){
        socket.close();
    }
}
