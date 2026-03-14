package space.n4krug.JACKxer.control;

import java.util.ArrayList;
import java.util.function.Consumer;

import javafx.application.Platform;

public class ControlParameter<T> {

	private final ParameterMapper<T> mapper;
	
	private final ArrayList<Consumer<T>> fxListeners = new ArrayList<>();
	private final ArrayList<Consumer<T>> directListeners = new ArrayList<>();
    private final ArrayList<Consumer<Boolean>> selectionListeners = new ArrayList<>();

	private T value;
    private float normalized;

	public ControlParameter(ParameterMapper<T> mapper, float normalizedStart) {
		this.mapper = mapper;
        setNormalizedInternal(normalizedStart, false);
	}
	
	public void addListener(Consumer<T> listener) {
		fxListeners.add(listener);
	}
	
    public void addDirectListener(Consumer<T> listener) {
        directListeners.add(listener);
    }

    public void setNormalized(float v) {

        setNormalizedInternal(v, true);
    }

    private void setNormalizedInternal(float v, boolean notify) {
        float nextNormalized = Math.max(0, Math.min(1, v));
        if (notify && Math.abs(nextNormalized - normalized) < 1e-6f) {
            return;
        }

        normalized = nextNormalized;
        value = mapper.map(normalized);

        if (!notify) {
            return;
        }

        for (Consumer<T> listener : directListeners) {
            listener.accept(value);
        }

        if (fxListeners.isEmpty()) {
            return;
        }

        if (Platform.isFxApplicationThread()) {
            for (Consumer<T> listener : fxListeners) {
                listener.accept(value);
            }
        } else {
            final T valueSnapshot = value;
            Platform.runLater(() -> {
                for (Consumer<T> listener : fxListeners) {
                    listener.accept(valueSnapshot);
                }
            });
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
        float denom = (max - min);
        float startNorm = denom == 0 ? 0f : (start - min) / denom;
    	return new ControlParameter<>(v -> min + v * (max - min), startNorm);
    }
    
    public static ControlParameter<Boolean> toggle(boolean start) {
    	return new ControlParameter<>(v -> v > 0.5f, start ? 1f : 0f);
    }
}
