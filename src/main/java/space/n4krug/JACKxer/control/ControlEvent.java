package space.n4krug.JACKxer.control;

public class ControlEvent {

    public enum Type {
        ABSOLUTE,
        RELATIVE,
        BUTTON
    }

    public final String controller;
    public final String id;
    public final Type type;
    public final float value;

    public ControlEvent(String controller, String id, Type type, float value) {
        this.controller = controller;
        this.id = id;
        this.type = type;
        this.value = value;
    }
}