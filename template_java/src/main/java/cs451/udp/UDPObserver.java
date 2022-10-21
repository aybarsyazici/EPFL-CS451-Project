package cs451.udp;

public interface UDPObserver {
    void onUDPSenderExecuted(byte senderId, int messageId);
}
