package space.n4krug.JACKxer.jackManager;

import java.nio.FloatBuffer;

import org.jaudiolibs.jnajack.JackException;

import space.n4krug.JACKxer.control.ControlParameter;
import space.n4krug.JACKxer.control.ParameterRegistry;

public class Compressor extends Client {

    /**
     * A basic peak detector + static curve compressor.
     * <p>
     * Parameters are registered under the client name:
     * {@code .threshold} (dB), {@code .ratio}, {@code .attack} (s), {@code .release} (s),
     * {@code .makeup} (dB).
     */
    private float thresholdDb = -18f;
    private final ControlParameter<Float> thresholdParam;
    private float ratio = 4f;
    private final ControlParameter<Float> ratioParam;

    private float attack = 0.01f;
    private final ControlParameter<Float> attackParam;
    private float release = 0.1f;
    private final ControlParameter<Float> releaseParam;

    private float makeupGain = 1f; // linear
    private final ControlParameter<Float> makeupParam;

    private float envelope = 0f;
    private float gain = 1f;

    private final float sampleRate;

    private float attackCoef;
    private float releaseCoef;

    public Compressor(String name, ParameterRegistry registry) throws JackException {
        super(name, new String[]{"in"}, new String[]{"out"}, registry);

        sampleRate = this.client.getSampleRate();

        thresholdParam = ControlParameter.range(-60f, 0f, thresholdDb);
        thresholdParam.addDirectListener(dB -> setThresholdDb(dB));
        registry.register(name + ".threshold", thresholdParam);

        ratioParam = ControlParameter.range(1, 10, ratio);
        ratioParam.addDirectListener(ratio -> setRatio(ratio));
        registry.register(name + ".ratio", ratioParam);
        
        attackParam = ControlParameter.range(0.001f, 0.2f, attack);
        attackParam.addDirectListener(s -> setAttack(s));
        registry.register(name + ".attack", attackParam);
        
        releaseParam = ControlParameter.range(0.01f, 1f, release);
        releaseParam.addDirectListener(s -> setRelease(s));
        registry.register(name + ".release", releaseParam);
        
        makeupParam = ControlParameter.range(-6, 24, 0f);
        makeupParam.addDirectListener(this::setMakeupGaindB);
        registry.register(name + ".makeup", makeupParam);
        
        attackCoef = (float)Math.exp(-1.0 / (sampleRate * attack));
        releaseCoef = (float)Math.exp(-1.0 / (sampleRate * release));
    }

    public float getThresholdDb() {
		return thresholdDb;
	}

	private void setThresholdDb(float thresholdDb) {
		this.thresholdDb = thresholdDb;
	}

	public float getRatio() {
		return ratio;
	}

	private void setRatio(float ratio) {
		this.ratio = ratio;
	}

	public float getAttack() {
		return attack;
	}

	private void setAttack(float attack) {
		this.attack = attack;
        attackCoef = (float)Math.exp(-1.0 / (sampleRate * attack));
	}

	public float getRelease() {
		return release;
	}

	private void setRelease(float release) {
		this.release = release;
        releaseCoef = (float)Math.exp(-1.0 / (sampleRate * release));
	}

    public float getMakeupGain() {
		return makeupGain;
	}

	private void setMakeupGaindB(float makeupGainDb) {
		this.makeupGain = (float) Math.pow(10, makeupGainDb / 20f);
	}

	private volatile float gainReductionDb = 0f;

	public float getGainReductionDb() {
	    return gainReductionDb;
	}
	
	@Override
    public void processAudio(FloatBuffer[] inBufs, FloatBuffer[] outBufs, int nframes) {
        FloatBuffer in = inBufs[0];
        FloatBuffer out = outBufs[0];

        for (int i = 0; i < nframes; i++) {

            float sample = in.get(i);

            float x = Math.abs(sample);

            if (x > envelope)
                envelope = attackCoef * envelope + (1 - attackCoef) * x;
            else
                envelope = releaseCoef * envelope + (1 - releaseCoef) * x;

            float levelDb = 20f * (float)Math.log10(envelope + 1e-9f);

            float gainDb = 0;

            if (levelDb > thresholdDb) {

                float over = levelDb - thresholdDb;

                gainDb = -(over - over / ratio);
            }

            float targetGain = (float)Math.pow(10, gainDb / 20f);

            gain += (targetGain - gain) * 0.01f;

            float y = sample * gain * makeupGain;

            gainReductionDb = -gainDb; 
            
            out.put(i, y);
        }
    }
}
