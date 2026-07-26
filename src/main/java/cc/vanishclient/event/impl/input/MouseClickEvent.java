package cc.vanishclient.event.impl.input;

import cc.vanishclient.event.types.Event;

public record MouseClickEvent(int button, int action, int mods) implements Event {

}
