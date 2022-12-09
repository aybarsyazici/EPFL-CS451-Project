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
    private final MessagePackage messagePackage;
    private final int proposalSetSize;
    private final UDPObserver observer;
    public UDPBulkSender(String ip, int port, DatagramSocket socket, MessagePackage messagePackage, UDPObserver observer, int proposalSetSize) {
        this.messagePackage = messagePackage;
        this.proposalSetSize = proposalSetSize;
        this.observer = observer;
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
            byte[] buffer = messagePackage.toBytes(proposalSetSize);
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, port);
            socket.send(packet);
            for(Message message : messagePackage.getMessages()){
                observer.onUDPSenderExecuted(message);
            }
        }
        catch (Exception e){
            System.out.println("ERROR while sending to: "+messagePackage.getMessages().get(0).getReceiverId());
            e.printStackTrace();
        }
    }

    public MessagePackage getMessagePackage(){
        return this.messagePackage;
    }

    public void close(){
        socket.close();
    }
}
