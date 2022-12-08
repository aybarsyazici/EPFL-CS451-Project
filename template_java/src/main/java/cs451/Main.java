package cs451;

import java.io.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

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

        BufferedReader brTest = null;
        String text = null;
        try {
            brTest = new BufferedReader(new FileReader(parser.config()));
            text = brTest.readLine();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Test will contain 3 numbers seperated with spaces
        // assign the first number to latticeRoundCount and the 3rd number to proposalSetSize
        String[] test = text.split(" ");
        int latticeRoundCount = Integer.parseInt(test[0]);
        int lineCount = Integer.parseInt(test[1]);
        int proposalSetSize = Integer.parseInt(test[2]);

        // example
        long pid = ProcessHandle.current().pid();
        System.out.println("My PID: " + pid + "\n");
        System.out.println("From a new terminal type `kill -SIGINT " + pid + "` or `kill -SIGTERM " + pid + "` to stop processing packets\n");
        System.out.println("OUTPUT:  " + parser.output());
        System.out.println("My ID: " + parser.myId() + "\n");
        System.out.println("List of resolved hosts is:");
        System.out.println("==========================");
        List<Host> hostList = parser.hosts();
        for(Host host: hostList){
            host.setId(host.getId() - 1); // To prevent overflow in Id, as the max Id is 128 and the max byte is 127.
        }
        for (Host host: hostList) {
            System.out.println(host.getId());
            System.out.println("Human-readable IP: " + host.getIp());
            System.out.println("Human-readable Port: " + host.getPort());
            System.out.println();
            if(host.getId() == (parser.myId()-1)){
                System.out.println("ProposalSetSize: " + proposalSetSize);
                pr = new Process((byte)host.getId(), host.getPort(), hostList, parser.output(), 8, proposalSetSize,latticeRoundCount);
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

        System.out.println("===============");
        System.out.println("Doing some initialization\n");
        pr.startProcessing();
        System.out.println("Broadcasting and delivering messages...\n");

        System.out.println("Process ID: " + (pr.getId()+1));


        // After a process finishes broadcasting,
        // it waits forever for the delivery of messages.
        int currentRound = 0;
        var run = true;
        while (run) {
            while(currentRound < pr.getMaxLatticeRound()){
                try{
                    if(currentRound == latticeRoundCount){
                        run = false;
                        break;
                    }
                    System.out.println("Starting Round " + currentRound );
                    text = brTest.readLine();
                    // The text will contain lineCount many numbers seperated by spaces
                    // Add them all into a set of integers and call pr.send on that set
                    String[] numbers = text.split(" ");
                    Set<Integer> set = new HashSet<>();
                    for(String number: numbers){
                        set.add(Integer.parseInt(number));
                    }
                    pr.send(currentRound, set);
                    currentRound += 1;
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
            Thread.sleep(200);
        }
        while(true){
            Thread.sleep(60 * 60 * 1000);
        }
    }
}