package cc.motionblurr.event.impl.chat;

import cc.motionblurr.event.types.CancellableEvent;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChatEvent extends CancellableEvent {
    private String message;

    public ChatEvent(String message) {
        this.message = message;
    }
}
