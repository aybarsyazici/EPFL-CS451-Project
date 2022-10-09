package cs451;

import cs451.links.FairLossLinks;
import cs451.links.PerfectLinks;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Process implements Deliverer {
    private int id;
    private int port;
    private List<Host> hosts;
    private PerfectLinks links;
    private String output;
    private final ConcurrentLinkedQueue<String> logs = new ConcurrentLinkedQueue<>();


    public Process(int id, int port,
                   List<Host> hosts, String output) {
        this.id = id;
        this.port = port;
        this.hosts = hosts;
        this.links = new PerfectLinks(port, this);
        this.output = output;
    }

    public void send(Message message){
        Host host = null;
        for(Host temp: hosts){
            if(temp.getId() == message.getReceiverId()){
                host = temp;
            }
        }
        if(host == null) return;
        links.send(message, host);
        logs.add("b " + message.getId() + "\n");
    }

    public int getId() {
        return id;
    }

    public PerfectLinks getLinks() {
        return links;
    }

    public void stopProcessing(){
        links.stop();
    }

    public void startProcessing(){
        links.start();
    }

    // Write to output file
    public void writeOutput() {
        try (var outputStream = new FileOutputStream(output)) {
            logs.forEach(s -> {
                try {
                    outputStream.write(s.getBytes());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void deliver(Message message) {
        System.out.println("HELLO! I'm process " + id + " and I received message: " + message);
        logs.add("d " + message.getSenderId() + " " + message.getId() + "\n");
    }

}
