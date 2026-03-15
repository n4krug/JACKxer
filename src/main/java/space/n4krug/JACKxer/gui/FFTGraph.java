package space.n4krug.JACKxer.gui;

import edu.emory.mathcs.jtransforms.fft.FloatFFT_1D;
import javafx.animation.AnimationTimer;
import javafx.geometry.Dimension2D;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import org.jaudiolibs.jnajack.JackException;
import space.n4krug.JACKxer.jackManager.Client;

/**
 * Resizable FFT visualization.
 * <p>
 * This is implemented as a {@link Region} hosting a {@link Canvas} so parents can lay it out
 * normally (Canvas itself does not participate in Region sizing APIs). This avoids feedback
 * loops where a child's size is bound to the parent's width, making the parent's min/pref
 * width "ratchet" upwards and preventing shrinking.
 */
public class FFTGraph extends Region {

    private static final int FFT_SIZE = 2048;
    private final int bands; // = 96;

    private final float[] samples = new float[FFT_SIZE];
    private final float[] spectrum = new float[FFT_SIZE / 2];
    private FloatFFT_1D fft = new FloatFFT_1D(FFT_SIZE);

    private final double[] bandFreq;
    private final float[] bandMag;
    private final float[] prevBandMag;

    private final int sampleRate;

    private final Client source;

    private final Canvas canvas;
    private final double prefW;
    private final double prefH;
    private final double aspect;

    public FFTGraph(Client source, Dimension2D size, int bands) {

        this.prefW = Math.max(1, size.getWidth());
        this.prefH = Math.max(1, size.getHeight());
        this.aspect = prefH / prefW;
        this.canvas = new Canvas(prefW, prefH);
        getChildren().add(canvas);

        setMinSize(0, 0);
        setPrefSize(prefW, prefH);
        setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        this.bands = bands;
        bandFreq = new double[this.bands];
        bandMag = new float[this.bands];
        prevBandMag = new float[this.bands];

        this.source = source;
        try {
            sampleRate = source.client.getSampleRate();
        } catch (JackException e) {
            throw new RuntimeException(e);
        }

        int[] bandStart = new int[this.bands];
        int[] bandEnd = new int[this.bands];

        double minF = 20;
        double maxF = 20000;

        for (int b = 0; b < this.bands; b++) {

            double t1 = (double)b / this.bands;
            double t2 = (double)(b+1) / this.bands;

            double f1 = minF * Math.pow(maxF/minF, t1);
            double f2 = minF * Math.pow(maxF/minF, t2);

            bandFreq[b] = (f1 + f2) * 0.5;

            bandStart[b] = clampBin((int) (f1 * FFT_SIZE / sampleRate));
            bandEnd[b] = clampBin((int) (f2 * FFT_SIZE / sampleRate));
        }


        AnimationTimer timer = new AnimationTimer() {
            public void handle(long now) {
                updateFFT();
                updateBands(bandStart, bandEnd);
                render();

                for (int b = 0; b < bands; b++) {
                    prevBandMag[b] = bandMag[b];
                }
            }
        };

        timer.start();
    }

    @Override
    protected void layoutChildren() {
        double w = Math.max(0, getWidth());
        double h = Math.max(0, getHeight());

        // Keep the Canvas sized to the allocated Region size.
        if (Math.abs(canvas.getWidth() - w) > 0.5) {
            canvas.setWidth(w);
        }
        if (Math.abs(canvas.getHeight() - h) > 0.5) {
            canvas.setHeight(h);
        }
    }

    @Override
    public boolean isResizable() {
        return true;
    }

    @Override
    protected double computeMinWidth(double height) {
        return 0;
    }

    @Override
    protected double computeMinHeight(double width) {
        return 0;
    }

    @Override
    protected double computePrefWidth(double height) {
        return prefW;
    }

    @Override
    protected double computePrefHeight(double width) {
        double w = width > 0 ? width : prefW;
        return Math.max(0, w * aspect);
    }

    private void updateFFT() {

        source.copyFFT(samples);

        window(samples);

        fft.realForward(samples);

        // JTransforms realForward packing:
        // a[0] = Re(0), a[1] = Re(N/2) (Nyquist), and for k=1..N/2-1: a[2k]=Re(k), a[2k+1]=Im(k)
        spectrum[0] = Math.abs(samples[0]);
        for (int i = 1; i < spectrum.length; i++) {
            float re = samples[2 * i];
            float im = samples[2 * i + 1];
            spectrum[i] = (float) Math.sqrt(re * re + im * im);
        }
    }

    private void updateBands(int[] bandStart, int[] bandEnd) {
        for (int b = 0; b < bands; b++) {
            int start = bandStart[b];
            int end = bandEnd[b];

            if (end < start) {
                int tmp = start;
                start = end;
                end = tmp;
            }

            float sum = 0;
            int count = 0;

            for (int i = start; i <= end; i++) {
                float v = spectrum[i];
                sum += v * v;
                count++;
            }

            float rms = (float) Math.sqrt(sum / Math.max(count, 1));
            bandMag[b] = rms * 0.3f + prevBandMag[b] * 0.7f;
        }
    }

    private int clampBin(int bin) {
        if (bin < 0) {
            return 0;
        }
        int max = spectrum.length - 1;
        if (bin > max) {
            return max;
        }
        return bin;
    }

    private void window(float[] s) {

        for (int i = 0; i < s.length; i++) {

            double w = 0.5 * (1 - Math.cos(2*Math.PI*i/(s.length-1)));

            s[i] *= w;
        }
    }

    private void render() {

        double w = canvas.getWidth();
        double h = canvas.getHeight();
        if (w <= 0 || h <= 0) {
            return;
        }

        GraphicsContext g = canvas.getGraphicsContext2D();

        g.clearRect(0, 0, w, h);

        //g.setStroke(Color.LIME);

        //double prevX = 0;
        //double prevY = getHeight();

        //for (int i = 1; i < spectrum.length; i++) {

        //    double freq = (double) (i * sampleRate) / FFT_SIZE;

        //    double x = freqToX(freq);

        //    double db = 20 * Math.log10(spectrum[i] + 1e-9);

        //    double y = dbToY(db);

        //    g.strokeLine(prevX, prevY, x, y);

        //    prevX = x;
        //    prevY = y;
        //}

        g.beginPath();
        g.moveTo(0, h);

        for (int b = 0; b < bands; b++) {

            double x = freqToX(bandFreq[b]);

            double db =
                    20 * Math.log10(bandMag[b] + 1e-9);

            double y = dbToY(db);

            g.lineTo(x, y);
        }

        g.lineTo(w, h);
        g.closePath();

        g.setFill(grad);
        g.fill();

        g.setLineJoin(StrokeLineJoin.ROUND);
        g.setLineCap(StrokeLineCap.ROUND);

        //drawSpectrum(g);
    }

    private final LinearGradient grad = new LinearGradient(
            0, 0, 0, 1, true,
            CycleMethod.NO_CYCLE,
            new Stop(0, Color.rgb(0,255,0,0.8)),
            new Stop(1, Color.rgb(0,255,0,0.1))
    );

    private void drawSpectrum(GraphicsContext g) {

        g.setFill(Color.rgb(0, 255, 0, 0.5)); // 50% opacity
        g.beginPath();

        g.moveTo(0, getHeight());

        for (int i = 1; i < spectrum.length; i++) {

            double freq = i * sampleRate / FFT_SIZE;
            double x = freqToX(freq);

            double db = 20 * Math.log10(spectrum[i] + 1e-9);
            double y = dbToY(db);

            g.lineTo(x, y);
        }

        g.lineTo(getWidth(), getHeight());
        g.closePath();
        g.setFill(grad);
        g.fill();

        // draw outline on top
        g.setStroke(Color.LIME);
        g.stroke();
    }

    private double freqToX(double freq) {
        double min = Math.log10(20);
        double max = Math.log10(20000);

        double t = (Math.log10(freq) - min) / (max - min);

        return t * canvas.getWidth();
    }

    private double dbToY(double db) {

        double min = -24;
        double max = 24;

        db = Math.max(min,Math.min(max,db));

        double norm = (db-min)/(max-min);

        return canvas.getHeight() * (1 - norm);
    }
}
