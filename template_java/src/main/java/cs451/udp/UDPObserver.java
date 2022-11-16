package cs451.udp;

import cs451.Message.Message;

public interface UDPObserver {
    void onUDPSenderExecuted(Message message);

    void onUDPBulkSenderExecuted();
}
