package space.n4krug.JACKxer.jackManager;

import java.nio.FloatBuffer;
import java.util.Random;

import org.jaudiolibs.jnajack.JackClient;
import org.jaudiolibs.jnajack.JackException;

import space.n4krug.JACKxer.control.ParameterRegistry;

public class NoiseGen extends Client {

	public enum Type {
		WHITE, PINK
	}

//	private Random rand = new Random();
	private Type type;

	public NoiseGen(String name, Type type, ParameterRegistry registry) throws JackException {
		super(name, null, new String[] { "out" }, registry);
		this.type = type;
	}

	@Override
	public void processAudio(FloatBuffer[] inBufs, FloatBuffer[] outBufs, int nframes) {
		FloatBuffer out = outBufs[0];

		for (int i = 0; i < nframes; i++) {
			switch (type) {
			case WHITE:
				out.put(i, white());
				break;
			case PINK:
				out.put(i, pink());
				break;
			}
		}
	}

	private int rng = 0x12345678;

	private float white() {

		rng ^= (rng << 13);
		rng ^= (rng >>> 17);
		rng ^= (rng << 5);

		return rng * (0.35f / 2147483648.0f);
	}

	private double b0, b1, b2, b3, b4, b5, b6;

	private float pink() {
		float white = white();

		b0 = 0.99886 * b0 + white * 0.0555179;
		b1 = 0.99332 * b1 + white * 0.0750759;
		b2 = 0.96900 * b2 + white * 0.1538520;
		b3 = 0.86650 * b3 + white * 0.3104856;
		b4 = 0.55000 * b4 + white * 0.5329522;
		b5 = -0.7616 * b5 - white * 0.0168980;

		double pink = b0 + b1 + b2 + b3 + b4 + b5 + b6 + white * 0.5362;

		b6 = white * 0.115926;

		return (float) pink * 0.11f;
	}

}
