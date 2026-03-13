package space.n4krug.JACKxer.jackManager;

public class Biquad {

    public enum Type {
        PEAK,
        LOW_SHELF,
        HIGH_SHELF,
        LOW_PASS,
        HIGH_PASS
    }

    private float a0, a1, a2, b1, b2;
    private float z1 = 0, z2 = 0;

    public float getA0() { return a0; }
    public float getA1() { return a1; }
    public float getA2() { return a2; }
    public float getB1() { return b1; }
    public float getB2() { return b2; }

    public void set(Type type, float freq, float q, float gainDb, float sr) {

        freq = Math.min(freq, sr * 0.49f);

        float w0 = (float) (2 * Math.PI * freq / sr);
        float cos = (float) Math.cos(w0);
        float sin = (float) Math.sin(w0);

        float alpha = sin / (2 * q);
        float A = (float) Math.pow(10, gainDb / 40);

        float b0, b1, b2, a0, a1, a2;

        switch (type) {

            case PEAK:

                b0 = 1 + alpha * A;
                b1 = -2 * cos;
                b2 = 1 - alpha * A;

                a0 = 1 + alpha / A;
                a1 = -2 * cos;
                a2 = 1 - alpha / A;
                break;

            case LOW_PASS:

                b0 = (1 - cos) / 2;
                b1 = 1 - cos;
                b2 = (1 - cos) / 2;

                a0 = 1 + alpha;
                a1 = -2 * cos;
                a2 = 1 - alpha;
                break;

            case HIGH_PASS:

                b0 = (1 + cos) / 2;
                b1 = -(1 + cos);
                b2 = (1 + cos) / 2;

                a0 = 1 + alpha;
                a1 = -2 * cos;
                a2 = 1 - alpha;
                break;

            case LOW_SHELF: {

                float sqrtA = (float) Math.sqrt(A);

                b0 = A * ((A + 1) - (A - 1) * cos + 2 * sqrtA * alpha);
                b1 = 2 * A * ((A - 1) - (A + 1) * cos);
                b2 = A * ((A + 1) - (A - 1) * cos - 2 * sqrtA * alpha);

                a0 = (A + 1) + (A - 1) * cos + 2 * sqrtA * alpha;
                a1 = -2 * ((A - 1) + (A + 1) * cos);
                a2 = (A + 1) + (A - 1) * cos - 2 * sqrtA * alpha;
                break;
            }

            case HIGH_SHELF: {

                float sqrtA = (float) Math.sqrt(A);

                b0 = A * ((A + 1) + (A - 1) * cos + 2 * sqrtA * alpha);
                b1 = -2 * A * ((A - 1) + (A + 1) * cos);
                b2 = A * ((A + 1) + (A - 1) * cos - 2 * sqrtA * alpha);

                a0 = (A + 1) - (A - 1) * cos + 2 * sqrtA * alpha;
                a1 = 2 * ((A - 1) - (A + 1) * cos);
                a2 = (A + 1) - (A - 1) * cos - 2 * sqrtA * alpha;
                break;
            }

            default:
                throw new IllegalArgumentException();
        }

        this.a0 = b0 / a0;
        this.a1 = b1 / a0;
        this.a2 = b2 / a0;
        this.b1 = a1 / a0;
        this.b2 = a2 / a0;
    }

    public float process(float in) {

        float out = in * a0 + z1;

        z1 = in * a1 + z2 - b1 * out;
        z2 = in * a2 - b2 * out;

        return out;
    }

    public void reset() {
        z1 = z2 = 0;
    }
}