package space.n4krug.JACKxer.control;

public class ParameterSelection {
    private static ControlParameter<?> selected;

    public static void select(ControlParameter<?> param) {
        if (selected != null) {
            selected.setSelected(false);
        }
        selected = param;
        selected.setSelected(true);
    }

    public static ControlParameter<?> get() {
        return selected;
    }
}
