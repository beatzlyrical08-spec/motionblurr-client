package cc.vanishclient.event.impl.chat;

import cc.vanishclient.event.types.CancellableEvent;
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
