package cs451;

public interface Acknowledger {
    void slideSendWindow(byte destinationId);

    void stopSenders();
}
