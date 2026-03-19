package space.n4krug.JACKxer.jackManager;

import java.nio.FloatBuffer;

import org.jaudiolibs.jnajack.JackException;

import space.n4krug.JACKxer.control.ControlParameter;
import space.n4krug.JACKxer.control.ParameterRegistry;


public class Gain extends Client {

	/**
	 * Simple linear gain stage.
	 * <p>
	 * Exposes {@code &lt;name&gt;.gain} as a dB parameter in the range {@code [-60..6]}.
	 */
	private float gain;

	private final ControlParameter<Float> gainParam;
	
	public Gain(String name, ParameterRegistry registry) throws JackException {
		super(name, new String[] { "in" }, new String[] { "out" }, registry);
		gainParam = ControlParameter.range(0, 1, 0);
		setNormalizedGain(gainParam.getValue());
		gainParam.addDirectListener(this::setNormalizedGain);
		registry.register(name + ".gain", gainParam);
	}

	@Override
	public void processAudio(FloatBuffer[] inBufs, FloatBuffer[] outBufs, int nframes) {
		FloatBuffer in = inBufs[0];
		FloatBuffer out = outBufs[0];

		for (int i = 0; i < nframes; i++) {
			out.put(i, in.get(i) * gain);
		}
	}

	private void setNormalizedGain(float norm) {
		gain = (float) Math.pow(1.25*norm, 3.5);
	}

	public float getDBGain() {

		if (gain < Math.pow(10, (double) -60 /20)){
			return -Float.MAX_VALUE;
		}

		return (float) (20f*Math.log10(gain));
	}

	public float dbToNormalized(float db) {
		float norm = (float) Math.pow(10, db / 20);

		return (float) (1/1.25) * (float) Math.pow(norm, 1/3.5);
	}
}
