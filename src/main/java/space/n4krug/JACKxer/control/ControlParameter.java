package space.n4krug.JACKxer.control;

import java.util.ArrayList;
import java.util.function.Consumer;

import javafx.application.Platform;

public class ControlParameter<T> {

	private final ParameterMapper<T> mapper;
	
	private final ArrayList<Consumer<T>> listeners = new ArrayList<>();
	
	private T value;
	
	public ControlParameter(ParameterMapper<T> mapper, T baseValue) {
		this.mapper = mapper;
		value = baseValue;
	}
	
	public void addListener(Consumer<T> listener) {
		listeners.add(listener);
	}
	
    public void setNormalized(float v) {

        value = mapper.map(v);

        for (Consumer<T> listener : listeners) {

            if (Platform.isFxApplicationThread()) {
                listener.accept(value);
            } else {
                Platform.runLater(() -> listener.accept(value));
            }
        }
    }
    
    public static ControlParameter<Float> range(float min, float max, float start) {
    	return new ControlParameter<>(v -> min + v * (max - min), start);
    }
    
    public static ControlParameter<Boolean> toggle(boolean start) {
    	return new ControlParameter<>(v -> v > 0.5f, start);
    }
    
    public T getValue() {
    	return value;
    }
}