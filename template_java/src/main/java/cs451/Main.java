package cs451;

import cs451.udp.Message;

import java.io.*;

public class Main {

    static Process pr;

    private static void handleSignal() {
        //immediately stop network packet processing
        System.out.println("Immediately stopping network packet processing.");
        pr.stopProcessing();

        //write/flush output file if necessary
        System.out.println("Writing output.");
        while(pr.isWriting()){} // Wait for the current writing to finish
        pr.writeOutput();
    }

    private static void initSignalHandlers() {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                handleSignal();
            }
        });
    }

    public static void main(String[] args) throws InterruptedException {
        Parser parser = new Parser(args);
        parser.parse();

        initSignalHandlers();

        // example
        long pid = ProcessHandle.current().pid();
        System.out.println("My PID: " + pid + "\n");
        System.out.println("From a new terminal type `kill -SIGINT " + pid + "` or `kill -SIGTERM " + pid + "` to stop processing packets\n");
        System.out.println("OUTPUT:  " + parser.output());
        System.out.println("My ID: " + parser.myId() + "\n");
        System.out.println("List of resolved hosts is:");
        System.out.println("==========================");
        for (Host host: parser.hosts()) {
            System.out.println(host.getId());
            System.out.println("Human-readable IP: " + host.getIp());
            System.out.println("Human-readable Port: " + host.getPort());
            System.out.println();
            if(host.getId() == parser.myId()){
                pr = new Process((byte)host.getId(), host.getPort(), parser.hosts(), parser.output());
            }
        }
        System.out.println();

        System.out.println("Path to output:");
        System.out.println("===============");
        System.out.println(parser.output() + "\n");
        boolean deletedSuccess = new File(parser.output()).delete();
        System.out.println("Deleted old output file: " + deletedSuccess + "\n");

        System.out.println("Path to config:");
        System.out.println("===============");
        System.out.println(parser.config() + "\n");

        try {
            var a = System.getProperty("com.sun.management.jmxremote.localConnectorAddress");
            System.out.println(a);
        }   catch (Exception e){
            e.printStackTrace();
        }
        System.out.println("===============");

        System.out.println("Doing some initialization\n");
        pr.startProcessing();
        BufferedReader brTest = null;
        String text = null;
        try {
            brTest = new BufferedReader(new FileReader(parser.config()));
            text = brTest.readLine();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println("Broadcasting and delivering messages...\n");
        int nmOfMessages = Integer.parseInt(text.substring(0, text.indexOf(" ")));
        int deliverTarget = Integer.parseInt(text.substring(text.indexOf(" ") + 1));
        System.out.println("Number of messages: " + nmOfMessages);
        System.out.println("Deliver target: " + deliverTarget);
        if(pr.getId() != deliverTarget){
            System.out.println("Process ID: " + pr.getId() + " Deliver Target: " + deliverTarget);
            pr.send(nmOfMessages, (byte)deliverTarget);
        }
        else {
            pr.setMessageCount(nmOfMessages);
        }
        // After a process finishes broadcasting,
        // it waits forever for the delivery of messages.
        while (true) {
            // Sleep for 1 hour
            Thread.sleep(60 * 60 * 1000);
        }
    }
}