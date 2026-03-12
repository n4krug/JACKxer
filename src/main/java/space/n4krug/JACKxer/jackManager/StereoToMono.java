package space.n4krug.JACKxer.jackManager;

import java.nio.FloatBuffer;

import org.jaudiolibs.jnajack.JackClient;
import org.jaudiolibs.jnajack.JackException;

import space.n4krug.JACKxer.control.ParameterRegistry;

public class StereoToMono extends Client {

	public StereoToMono(String name, ParameterRegistry registry) throws JackException {
		super(name, new String[] { "inL", "inR" }, new String[] { "out" }, registry);
		// TODO Auto-generated constructor stub
	}

	@Override
	public boolean process(JackClient client2, int nframes) {
		FloatBuffer in1 = getInputs().get(0).getFloatBuffer();
		FloatBuffer in2 = getInputs().get(1).getFloatBuffer();
		FloatBuffer out = getOutputs().get(0).getFloatBuffer();

        in1 = preProcess(in1, nframes);
        in2 = preProcess(in2, nframes);
        
		for (int i = 0; i < nframes; i++) {
			float sample1 = in1.get(i);
			float sample2 = in2.get(i);
			out.put(i, (sample1 + sample2) / 2 );
		}

		this.postProcess(out, nframes);

		return true;
	}

	@Override
	protected FloatBuffer process(FloatBuffer in, int nframes) {
		return null;
	}

}
