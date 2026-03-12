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
	public void processAudio(FloatBuffer[] inBufs, FloatBuffer[] outBufs, int nframes) {
		FloatBuffer in1 = inBufs[0];
		FloatBuffer in2 = inBufs[1];
		FloatBuffer out = outBufs[0];

		for (int i = 0; i < nframes; i++) {
			float sample1 = in1.get(i);
			float sample2 = in2.get(i);
			out.put(i, (sample1 + sample2) / 2);
		}
	}
}
