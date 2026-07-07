package cc.motionblurr.event.impl.input;

import cc.motionblurr.event.types.Event;

public record MouseClickEvent(int button, int action, int mods) implements Event {

}
