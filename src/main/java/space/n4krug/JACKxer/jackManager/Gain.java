package space.n4krug.JACKxer.jackManager;

import java.nio.FloatBuffer;

import org.jaudiolibs.jnajack.JackClient;
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
		gainParam.addListener(dB -> setGaindB(dB));
		registry.register(name + ".gain", gainParam);
	}

	@Override
	public boolean process(JackClient client2, int nframes) {
		FloatBuffer in = getInputs().get(0).getFloatBuffer();
		FloatBuffer out = getOutputs().get(0).getFloatBuffer();

        in = preProcess(in, nframes);
        
		for (int i = 0; i < nframes; i++) {
			float sample = in.get(i);
			out.put(i, sample * gain);
		}

		this.postProcess(out, nframes);

		return true;
	}

	private void setGaindB(float dB) {
		gain = (float) Math.pow(10, dB/20);
	}
}
