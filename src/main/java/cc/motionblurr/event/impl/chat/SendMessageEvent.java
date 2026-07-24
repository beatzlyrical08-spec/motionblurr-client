package cc.motionblurr.event.impl.chat;

import cc.motionblurr.event.types.CancellableEvent;

public class SendMessageEvent extends CancellableEvent {
    private final String message;

    public SendMessageEvent(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}

