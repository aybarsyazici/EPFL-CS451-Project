package cs451.interfaces;

public interface UniformDeliverer extends Deliverer {
    void uniformDeliver(byte originalSender, int messageId);
}
