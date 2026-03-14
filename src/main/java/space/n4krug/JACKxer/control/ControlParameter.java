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
	
    /**
     * Adds a listener that is always invoked on the JavaFX application thread.
     * <p>
     * If {@link #setNormalized(float)} is called off the FX thread, the callback is scheduled
     * via {@link javafx.application.Platform#runLater(Runnable)}.
     */
	public void addListener(Consumer<T> listener) {
		fxListeners.add(listener);
	}
	
    /**
     * Adds a listener that is invoked synchronously on the calling thread of
     * {@link #setNormalized(float)}.
     * <p>
     * Use this for DSP-side parameter updates where scheduling onto the FX thread would add
     * latency (for example when parameters are driven by MIDI input).
     */
    public void addDirectListener(Consumer<T> listener) {
        directListeners.add(listener);
    }

    /**
     * Sets the parameter value using a normalized range of {@code [0..1]}.
     * <p>
     * The mapper passed to the constructor converts the normalized value into the typed
     * parameter value.
     */
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

    /**
     * Returns the last normalized value that was set, clamped to {@code [0..1]}.
     */
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
