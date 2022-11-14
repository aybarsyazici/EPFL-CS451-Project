package cs451.interfaces;

public interface Acknowledger {
    void slideSendWindow(byte destinationId);

    void stopSenders();
}
