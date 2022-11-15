package cs451.interfaces;

import cs451.Message.Message;

public interface UniformDeliverer extends Deliverer {
    void uniformDeliver(Message message);
}
