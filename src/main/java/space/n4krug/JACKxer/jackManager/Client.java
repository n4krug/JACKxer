package space.n4krug.JACKxer.jackManager;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

import org.jaudiolibs.jnajack.Jack;
import org.jaudiolibs.jnajack.JackClient;
import org.jaudiolibs.jnajack.JackException;
import org.jaudiolibs.jnajack.JackOptions;
import org.jaudiolibs.jnajack.JackPort;
import org.jaudiolibs.jnajack.JackPortFlags;
import org.jaudiolibs.jnajack.JackPortType;
import org.jaudiolibs.jnajack.JackProcessCallback;
import org.jaudiolibs.jnajack.JackStatus;

import space.n4krug.JACKxer.control.ControlParameter;
import space.n4krug.JACKxer.control.ParameterRegistry;

public abstract class Client implements JackProcessCallback {

	/**
	 * The underlying JACK client handle.
	 * <p>
	 * This is exposed for port name resolution and sample rate queries.
	 */
	public final JackClient client;
	private final ArrayList<JackPort> inputs = new ArrayList<JackPort>();
	private final ArrayList<JackPort> outputs = new ArrayList<JackPort>();
	private final Jack jack;
	
	private final String name;

	private volatile boolean muted = false;
	private volatile boolean bypassed = false;
	private volatile boolean warnedFrameMismatch = false;

	private volatile float peakLevel = 0f;
	private volatile float rmsLevel = 0f;

	private volatile float prePeakLevel = 0f;
	private volatile float preRmsLevel = 0f;

	private static final int FFT_BUFFER_SIZE = 4096;

	private final float[] fftBuffer = new float[FFT_BUFFER_SIZE];
	private volatile int fftWritePos = 0;

	/**
	 * Normalizes FloatBuffer state for absolute indexed access and clamps to a safe frame count.
	 * <p>
	 * Note: {@link FloatBuffer#get(int)} and {@link FloatBuffer#put(int, float)} are bounds-checked
	 * against {@code limit}, not {@code capacity}. On the JACK process thread, we always want a
	 * consistent {@code [0..frames)} view.
	 */
	private static int normalizeAndClamp(FloatBuffer buf, int nframes) {
		if (buf == null) {
			return 0;
		}
		buf.clear(); // position=0, limit=capacity
		int frames = Math.min(nframes, buf.capacity());
		buf.limit(frames);
		return frames;
	}

	/**
	 * Returns the configured instance name of this client.
	 */
    public String toString() {
		return name;
	}
	
	public float getPeakLevel() {
		return peakLevel;
	}

	public float getRmsLevel() {
		return rmsLevel;
	}
	public float getPeakdB() {
		return 20f * (float) Math.log10(peakLevel + 1e-9f);
	}

	public float getRmsdB() {
		return 20f * (float) Math.log10(rmsLevel + 1e-9f);
	}

	public float getPrePeakLevel() {
		return prePeakLevel;
	}

	public float getPreRmsLevel() {
		return preRmsLevel;
	}

	public float getPrePeakdB() {
		return 20f * (float) Math.log10(prePeakLevel + 1e-9f);
	}

	public float getPreRmsdB() {
		return 20f * (float) Math.log10(preRmsLevel + 1e-9f);
	}

	protected FloatBuffer preProcess(FloatBuffer buf, int nframes) {
		if (buf == null || nframes <= 0) {
			return buf;
		}
		float peak = 0;
		float sum = 0;

		for (int i = 0; i < nframes; i++) {
			float sample = buf.get(i);

			float abs = Math.abs(sample);

			if (abs > peak) {
				peak = abs;
			}

			sum += sample * sample;
		}

		prePeakLevel = Math.max(peak, prePeakLevel * 0.95f);
		preRmsLevel = (nframes > 0) ? (float) Math.sqrt(sum / nframes) : 0f;

		return buf;
	}

	@Override
	public boolean process(JackClient client, int nframes) {
		FloatBuffer[] inBufs = new FloatBuffer[inputs.size()];
		FloatBuffer[] outBufs = new FloatBuffer[outputs.size()];

		int frames = nframes;
		int minInCap = Integer.MAX_VALUE;
		int minOutCap = Integer.MAX_VALUE;

		for (int i = 0; i < inputs.size(); i++) {
			inBufs[i] = inputs.get(i).getFloatBuffer();
			if (inBufs[i] != null) {
				minInCap = Math.min(minInCap, inBufs[i].capacity());
			} else {
				minInCap = 0;
			}
			frames = Math.min(frames, normalizeAndClamp(inBufs[i], nframes));
		}

		for (int i = 0; i < outputs.size(); i++) {
			outBufs[i] = outputs.get(i).getFloatBuffer();
			if (outBufs[i] != null) {
				minOutCap = Math.min(minOutCap, outBufs[i].capacity());
			} else {
				minOutCap = 0;
			}
			frames = Math.min(frames, normalizeAndClamp(outBufs[i], nframes));
		}

		if (frames <= 0) {
			return true;
		}

		if (frames < nframes && !warnedFrameMismatch) {
			warnedFrameMismatch = true;
			String inCap = inputs.isEmpty() ? "-" : Integer.toString(minInCap == Integer.MAX_VALUE ? 0 : minInCap);
			String outCap = outputs.isEmpty() ? "-" : Integer.toString(minOutCap == Integer.MAX_VALUE ? 0 : minOutCap);
			System.err.println(
					"[JACKxer] Frame clamp for \"" + name + "\": nframes=" + nframes +
					", frames=" + frames +
					", inPorts=" + inputs.size() +
					", outPorts=" + outputs.size() +
					", minInCap=" + inCap +
					", minOutCap=" + outCap
			);
		}

		for (int i = 0; i < inBufs.length; i++) {
			preProcess(inBufs[i], frames);
		}

			 if (bypassed) {

			int n = Math.min(inBufs.length, outBufs.length);

			for (int ch = 0; ch < n; ch++) {
				FloatBuffer in = inBufs[ch];
				FloatBuffer out = outBufs[ch];

				for (int i = 0; i < frames; i++) {
					out.put(i, in.get(i));
				}
			}

		} else {

			processAudio(inBufs, outBufs, frames);

		}

		for (FloatBuffer out : outBufs) {
			postProcess(out, frames);
		}

		return true;
	}

	abstract protected void processAudio(FloatBuffer[] in, FloatBuffer[] out, int nframes);

	/**
	 * Called after {@link #processAudio(FloatBuffer[], FloatBuffer[], int)} to update meters,
	 * the FFT ring buffer, and apply post effects such as muting.
	 * <p>
	 * Runs on the JACK realtime process thread.
	 */
	protected void postProcess(FloatBuffer buf, int nframes) {
		if (buf == null || nframes <= 0) {
			return;
		}
		float peak = 0;
		float sum = 0;

		for (int i = 0; i < nframes; i++) {
			float sample = buf.get(i);

			float abs = Math.abs(sample);

			if (abs > peak) {
				peak = abs;
			}

			sum += sample * sample;
		}

		for (int i = 0; i < nframes; i++) {

			float s = buf.get(i);

			fftBuffer[fftWritePos++] = s;

			if (fftWritePos >= fftBuffer.length)
				fftWritePos = 0;
		}

		peakLevel = Math.max(peak, peakLevel * 0.95f);
		rmsLevel = (nframes > 0) ? (float) Math.sqrt(sum / nframes) : 0f;

		if (muted) {

			for (int i = 0; i < nframes; i++) {
				buf.put(i, 0f);
			}

		}

	}

	private void setMute(boolean mute) {
		muted = mute;
	}

	private void setBypass(boolean bypass) {
		bypassed = bypass;
	}

	public void copyFFT(float[] dest) {

		int pos = fftWritePos;

		int len = dest.length;

		for (int i = 0; i < len; i++) {

			int idx = pos - len + i;

			if (idx < 0)
				idx += FFT_BUFFER_SIZE;

			dest[i] = fftBuffer[idx];
		}
	}

//	private boolean getMuted() {
//		return muted;
//	}

	public Client(String name, String[] inputs, String[] outputs, ParameterRegistry registry) throws JackException {
		this.name = name;
		jack = Jack.getInstance();

		client = jack.openClient(name, EnumSet.of(JackOptions.JackNoStartServer), EnumSet.noneOf(JackStatus.class));

		if (inputs != null) {
			for (String input : inputs) {
				this.inputs
						.add(client.registerPort(input, JackPortType.AUDIO, EnumSet.of(JackPortFlags.JackPortIsInput)));
			}
		}
		if (outputs != null) {
			for (String output : outputs) {
				this.outputs.add(
						client.registerPort(output, JackPortType.AUDIO, EnumSet.of(JackPortFlags.JackPortIsOutput)));
			}
		}

		client.setProcessCallback(this);
		client.activate();

		ControlParameter<Boolean> bypassedParam = ControlParameter.toggle(bypassed);
		registry.register(name + ".bypass", bypassedParam);
		bypassedParam.addDirectListener(state -> setBypass(state));

        ControlParameter<Boolean> on = ControlParameter.toggle(!muted);
		registry.register(name + ".on", on);
		on.addDirectListener(state -> setMute(!state));
	}

	public String[] getInputNames() {
		String[] inputNames = new String[inputs.size()];

		for (int i = 0; i < inputs.size(); i++) {
			inputNames[i] = inputs.get(i).getName();
		}

		return inputNames;
	}

	public String[] getOutputNames() {
		String[] outputNames = new String[outputs.size()];

		for (int i = 0; i < outputs.size(); i++) {
			outputNames[i] = outputs.get(i).getName();
		}

		return outputNames;
	}

	public final ArrayList<JackPort> getInputs() {

		return inputs;
	}

	public final ArrayList<JackPort> getOutputs() {

		return outputs;
	}

	public final JackPort in(int i) {
		return getInputs().get(i);
	}

	public final JackPort out(int i) {
		return getOutputs().get(i);
	}

	public void connect(JackPort source, JackPort dest) throws JackException {
		jack.connect(client, source.getName(), dest.getName());
	}

	public void connect(JackPort source, String dest) throws JackException {
		jack.connect(client, source.getName(), dest);
	}

	public void connect(String source, JackPort dest) throws JackException {
		jack.connect(client, source, dest.getName());
	}
	
	public void connect(String source, String dest) throws JackException {
		jack.connect(client, source, dest);
	}

}
