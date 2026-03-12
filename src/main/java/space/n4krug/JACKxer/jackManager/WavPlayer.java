package space.n4krug.JACKxer.jackManager;

import java.nio.FloatBuffer;
import java.io.File;
import java.io.IOException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.jaudiolibs.jnajack.JackClient;
import org.jaudiolibs.jnajack.JackException;

import space.n4krug.JACKxer.control.ParameterRegistry;

public class WavPlayer extends Client {

	private float[] audio;
	private int position = 0;

	public WavPlayer(String clientName, String filename, ParameterRegistry registry)
			throws JackException, UnsupportedAudioFileException, IOException {
		super(clientName, null, new String[] { "out" }, registry);
		loadWav(filename);
	}

	public boolean process(JackClient client2, int nframes) {
		FloatBuffer out = getOutputs().get(0).getFloatBuffer();
		
		out = preProcess(out, nframes);

		for (int i = 0; i < nframes; i++) {
			if (audio != null) {
				out.put(i, audio[position]);

				position++;

				if (position >= audio.length) {
					position = 0;
				}
			} else {
				out.put(i, 0);
			}
		}

		this.postProcess(out, nframes);
		
		return true;
	}

	@Override
	protected FloatBuffer process(FloatBuffer in, int nframes) {
		return null;
	}

	public void loadWav(String file) throws UnsupportedAudioFileException, IOException {

		AudioInputStream stream = AudioSystem.getAudioInputStream(new File(file));

		AudioFormat format = stream.getFormat();

		byte[] bytes = stream.readAllBytes();

		int samples = bytes.length / 2;
		audio = new float[samples];

		for (int i = 0; i < samples; i++) {
			int low = bytes[i * 2] & 0xff;
			int high = bytes[i * 2 + 1];
			int sample = (high << 8) | low;

			audio[i] = sample / 32768f;
		}
	}
}
