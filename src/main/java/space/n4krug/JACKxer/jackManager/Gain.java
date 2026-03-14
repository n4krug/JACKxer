package space.n4krug.JACKxer.jackManager;

import java.nio.FloatBuffer;

import org.jaudiolibs.jnajack.JackException;

import space.n4krug.JACKxer.control.ControlParameter;
import space.n4krug.JACKxer.control.ParameterRegistry;


public class Gain extends Client {

	private float gain;

	private final ControlParameter<Float> gainParam;
	
	public Gain(String name, ParameterRegistry registry) throws JackException {
		super(name, new String[] { "in" }, new String[] { "out" }, registry);
		gainParam = ControlParameter.range(-60, 6, -60);
		setGaindB(gainParam.getValue());
		gainParam.addDirectListener(this::setGaindB);
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

	private void setGaindB(float dB) {
		gain = (float) Math.pow(10, dB/20);
	}
}
