package cs451;

import cs451.udp.Message;

public interface Deliverer {
    void deliver(Message message);
}
