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

	public final JackClient client;
	private final ArrayList<JackPort> inputs = new ArrayList<JackPort>();
	private final ArrayList<JackPort> outputs = new ArrayList<JackPort>();
	private final Jack jack;
	
	private final String name;

	private volatile boolean muted = false;
	private volatile boolean bypassed = false;

	private volatile float peakLevel = 0f;
	private volatile float rmsLevel = 0f;

	private volatile float prePeakLevel = 0f;
	private volatile float preRmsLevel = 0f;

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
		FloatBuffer in = getInputs().getFirst().getFloatBuffer();
		FloatBuffer out = getOutputs().getFirst().getFloatBuffer();

		in = preProcess(in, nframes);

		if (bypassed) {
			for (int i = 0; i < nframes; i++) {
				float sample = in.get(i);
				out.put(i, sample);
			}
		} else {
			out = process(in, nframes);
		}

		this.postProcess(out, nframes);

		return true;
	}

	abstract protected FloatBuffer process(FloatBuffer in, int nframes);

	protected void postProcess(FloatBuffer buf, int nframes) {
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

		peakLevel = Math.max(peak, peakLevel * 0.95f);
		rmsLevel = (nframes > 0) ? (float) Math.sqrt(sum / nframes) : 0f;

		if (muted) {
//			for (int i = 0; i < getOutputs().size(); i++) {
//				FloatBuffer out = getOutputs().get(i).getFloatBuffer();
			for (int j = 0; j < nframes; j++) {
				buf.put(j, 0);
			}
		}
//		}
	}

	private void setMute(boolean mute) {
		muted = mute;
	}

	private void setBypass(boolean bypass) {
		bypassed = bypass;
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
		bypassedParam.addListener(state -> setBypass(state));

        ControlParameter<Boolean> on = ControlParameter.toggle(!muted);
		registry.register(name + ".on", on);
		on.addListener(state -> setMute(!state));
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
