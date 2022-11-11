package cs451;

import cs451.Message.Message;

public interface Deliverer {
    void deliver(Message message);

    void confirmDeliver(Message message);
}
