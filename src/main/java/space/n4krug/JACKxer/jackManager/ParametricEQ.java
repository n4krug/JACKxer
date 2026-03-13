package space.n4krug.JACKxer.jackManager;

import org.jaudiolibs.jnajack.JackException;
import space.n4krug.JACKxer.control.ControlParameter;
import space.n4krug.JACKxer.control.ParameterRegistry;

import java.nio.FloatBuffer;

public class ParametricEQ extends Client {
    private final Biquad[] biquads;

    public Biquad[] getBands() { return biquads; }

    public ParametricEQ(String name, ParameterRegistry registry, Biquad.Type[] bands)
            throws JackException {
        super(name, new String[]{"in"}, new String[]{"out"}, registry);

        int samplingRate = client.getSampleRate();

        biquads = new Biquad[bands.length];

        for (int i = 0; i < bands.length; i++) {
            Biquad biquad = new Biquad();
            biquads[i] = biquad;
            ControlParameter<Float> q = ControlParameter.range(0.3f, 10f, 1f);
            ControlParameter<Biquad.Type> type = new ControlParameter<Biquad.Type>(v ->
                    Biquad.Type.values()[Math.round(v * (Biquad.Type.values().length - 1))]
                    , bands[i]);
            ControlParameter<Float> gainDb = ControlParameter.range(-24, 24, 0);
            ControlParameter<Float> freq = new ControlParameter<>(
                    v -> (float) normToFreq(v),
                    freq(bands.length, i)
            );

            q.addListener(_ -> biquad.set(type.getValue(), freq.getValue(), q.getValue(), gainDb.getValue(),
                    samplingRate));
            type.addListener(_ -> biquad.set(type.getValue(), freq.getValue(), q.getValue(), gainDb.getValue(),
                    samplingRate));
            gainDb.addListener(_ -> biquad.set(type.getValue(), freq.getValue(), q.getValue(), gainDb.getValue(),
                    samplingRate));
            freq.addListener(_ -> biquad.set(type.getValue(), freq.getValue(), q.getValue(), gainDb.getValue(),
                    samplingRate));

            biquad.set(type.getValue(), freq.getValue(), q.getValue(), gainDb.getValue(), samplingRate);

            registry.register(name + ".band" + i + ".freq", freq);
            registry.register(name + ".band" + i + ".gain", gainDb);
            registry.register(name + ".band" + i + ".q", q);
            registry.register(name + ".band" + i + ".type", type);
        }
    }

    private static double normToFreq(double t) {

        double fMin = 20;
        double fMax = 20000;

        return fMin * Math.pow(fMax / fMin, t);
    }

    private float freq(int amount, int num) {
        float min = 20f;
        float max = 20000f;

        float logMin = (float)Math.log10(min);
        float logMax = (float)Math.log10(max);

        float t = (num + 1f) / (amount + 1f);

        return (float)Math.pow(10, logMin + t * (logMax - logMin));
    }

    @Override
    protected void processAudio(FloatBuffer[] in, FloatBuffer[] out, int nframes) {

        FloatBuffer input = in[0];
        FloatBuffer output = out[0];

        for (int i = 0; i < nframes; i++) {

            float s = input.get(i);

            for (Biquad b : biquads) {
                s = b.process(s);
            }

            output.put(i, s);
        }
    }
}
