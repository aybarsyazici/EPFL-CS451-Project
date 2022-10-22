package cs451.udp;

public interface UDPObserver {
    void onUDPSenderExecuted(byte receiverId, int messageId);
}
