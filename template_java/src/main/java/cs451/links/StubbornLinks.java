package cs451.links;

import cs451.Deliverer;
import cs451.Host;
import cs451.Message;

public class StubbornLinks implements Deliverer {
    private final FairLossLinks fairLoss;
    private static final int SEND_DELAY = 200;
    private static final int maxRetries = 5;
    private final Deliverer deliverer;


    public StubbornLinks(int port, Deliverer deliverer) {
        this.fairLoss = new FairLossLinks(port, this);
        this.deliverer = deliverer;
    }

    public void start(){
        fairLoss.start();
    }

    public void send(Message message, Host host){
        int retries = 0;
        while (retries < maxRetries) {
            fairLoss.send(message,host);
            retries++;
        }
    }

    public void stop(){
        fairLoss.stop();
    }

    @Override
    public void deliver(Message message) {
        deliverer.deliver(message);
    }
}
