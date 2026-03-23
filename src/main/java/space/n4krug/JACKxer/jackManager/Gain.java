package space.n4krug.JACKxer.jackManager;

import java.nio.FloatBuffer;

import org.jaudiolibs.jnajack.JackException;

import space.n4krug.JACKxer.control.ControlParameter;
import space.n4krug.JACKxer.control.ParameterRegistry;


public class Gain extends Client {

	private float gain;
	private boolean solo = false;

    public Gain(String name, ParameterRegistry registry) throws JackException {
		super(name, new String[] { "in" }, new String[] { "out", "solo" }, registry);
        ControlParameter<Float> gainParam = ControlParameter.range(0, 1, 0);
		setNormalizedGain(gainParam.getValue());
		gainParam.addDirectListener(this::setNormalizedGain);
		registry.register(name + ".gain", gainParam);

		ControlParameter<Boolean> soloParam = ControlParameter.toggle(false);
		soloParam.addDirectListener(v -> solo = v);
		registry.register(name + ".solo", soloParam);
	}

	@Override
	public void processAudio(FloatBuffer[] inBufs, FloatBuffer[] outBufs, int nframes) {
		FloatBuffer in = inBufs[0];
		FloatBuffer out = outBufs[0];

		for (int i = 0; i < nframes; i++) {
			out.put(i, in.get(i) * gain);
		}

		FloatBuffer soloBuf = outBufs[1];

		for (int i = 0; i < nframes; i++) {
			soloBuf.put(i, solo ? in.get(i) : 0f);
		}
	}

	private void setNormalizedGain(float norm) {
		gain = (float) Math.pow((4f/3)*norm, 2.4);
	}

	public float dbToNormalized(float db) {
		float norm = (float) Math.pow(10, db / 20);

		return (3f/4) * (float) Math.pow(norm, 1/2.4);
	}
}
