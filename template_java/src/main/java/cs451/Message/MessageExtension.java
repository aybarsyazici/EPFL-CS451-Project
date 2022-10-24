package cs451.Message;

import cs451.Host;

public class MessageExtension {
    private final Message message;
    private final Host host;

    public Message getMessage() {
        return message;
    }

    public Host getHost() {
        return host;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        MessageExtension message = (MessageExtension) obj;

        return message.getMessage().equals(this.getMessage());
    }

    public MessageExtension(Message message, Host host) {
        this.message = message;
        this.host = host;
    }
}
