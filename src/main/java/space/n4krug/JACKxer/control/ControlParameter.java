package space.n4krug.JACKxer.control;

import java.util.ArrayList;
import java.util.function.Consumer;

import javafx.application.Platform;

public class ControlParameter<T> {

	private final ParameterMapper<T> mapper;
	
	private final ArrayList<Consumer<T>> listeners = new ArrayList<>();
    private final ArrayList<Consumer<Boolean>> selectionListeners = new ArrayList<>();

	private T value;
    private float normalized;

	public ControlParameter(ParameterMapper<T> mapper, T baseValue) {
		this.mapper = mapper;
		value = baseValue;
        normalized = 0;
	}
	
	public void addListener(Consumer<T> listener) {
		listeners.add(listener);
	}
	
    public void setNormalized(float v) {

        normalized = Math.max(0, Math.min(1, v));

        value = mapper.map(normalized);

        for (Consumer<T> listener : listeners) {

            if (Platform.isFxApplicationThread()) {
                listener.accept(value);
            } else {
                Platform.runLater(() -> listener.accept(value));
            }
        }
    }

    public void addSelectionListener(Consumer<Boolean> listener) { selectionListeners.add(listener); }

    public void setSelected(boolean selected) {
        for (Consumer<Boolean> listener : selectionListeners) {
            listener.accept(selected);
        }
    }

    public T getValue() {
        return value;
    }

    public float getNormalized() { return normalized; }
    
    public static ControlParameter<Float> range(float min, float max, float start) {
    	return new ControlParameter<>(v -> min + v * (max - min), start);
    }
    
    public static ControlParameter<Boolean> toggle(boolean start) {
    	return new ControlParameter<>(v -> v > 0.5f, start);
    }
}