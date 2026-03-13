package space.n4krug.JACKxer.control;

public class SelectedParameter extends ControlParameter<Float> {

    public SelectedParameter() {
        super(v -> v, 0f);
    }

    @Override
    public void setNormalized(float v) {

        ControlParameter<?> target = ParameterSelection.get();

        if (target != null) {
            target.setNormalized(v);
        }
    }

    @Override
    public float getNormalized() {

        ControlParameter<?> target = ParameterSelection.get();

        if (target != null) {
            return target.getNormalized();
        }

        return 0;
    }
}