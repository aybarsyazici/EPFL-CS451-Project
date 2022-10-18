package cs451.udp;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class UDPSender implements Runnable{
    private DatagramSocket socket;
    private InetAddress address;
    private int port;
    private byte[] buf;

    public UDPSender(String ip, int port, Message message, DatagramSocket socket) {
        try {
            this.port = port;
            this.address = InetAddress.getByName(ip);
            this.socket = socket;
            ByteArrayOutputStream baos = new ByteArrayOutputStream(256);
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(message);
            this.buf = baos.toByteArray();
            oos.close();
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
    }

    public void close(){
        socket.close();
    }
}
