package space.n4krug.JACKxer.gui;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import space.n4krug.JACKxer.jackManager.Biquad;

class EQGraphBasic extends Canvas {

    Biquad[] bands;

    private double[] freqs;
    private double[] cos;
    private double[] sin;

    EQGraphBasic(Biquad[] bands) {
        super(400,200);
        this.bands = bands;

        int width = (int)getWidth();

        freqs = new double[width];
        cos  = new double[width];
        sin  = new double[width];

        double sr = 48000;

        for (int x = 0; x < width; x++) {

            double f = xToFreq(x);

            freqs[x] = f;

            double w = 2 * Math.PI * f / sr;

            cos[x] = Math.cos(w);
            sin[x] = Math.sin(w);
        }

        rerender();
    }

    void rerender() {

        GraphicsContext g = getGraphicsContext2D();

        g.clearRect(0,0,getWidth(),getHeight());

        double prevX = 0;
        double prevY = response(20);

        int[] drawFreqs = {20,50,100,200,500,1000,2000,5000,10000,20000};

        for (int f : drawFreqs) {

            double x = freqToX(f);

            if (f == 100 || f == 1000 || f == 10000) {
                g.setStroke(Color.gray(0.7, 0.35));
                g.setLineWidth(1);
            } else {
                g.setStroke(Color.gray(0.7, 0.15));
                g.setLineWidth(0.5);
            }

            g.strokeLine(x, 0, x, getHeight());
        }

        g.setStroke(Color.BLACK);
        g.setLineWidth(1);

        drawBands(g);
        drawTotal(g);

//        for (int x=1;x<getWidth();x++) {
//
//            double freq = freqs[x];
//
//            double y = response(freq);
//
//            g.strokeLine(prevX, prevY, x, y);
//
//            prevX = x;
//            prevY = y;
//        }
    }

    private double xToFreq(double x) {

        double min = Math.log10(20);
        double max = Math.log10(20000);

        double t = x / getWidth();

        return Math.pow(10, min + t * (max - min));
    }

    private double freqToX(double freq) {
        double min = Math.log10(20);
        double max = Math.log10(20000);

        double t = (Math.log10(freq) - min) / (max - min);

        return t * getWidth();
    }

    private double response(double freq) {

        double sr = 48000;

        double w = 2 * Math.PI * freq / sr;

        double real = 1;
        double imag = 0;

        for (Biquad b : bands) {

            double cos = Math.cos(w);
            double sin = Math.sin(w);

            double numReal =
                    b.getA0()
                            + b.getA1()*cos
                            + b.getA2()*(2*cos*cos-1);

            double numImag =
                    -b.getA1()*sin
                            -b.getA2()*2*sin*cos;

            double denReal =
                    1
                            + b.getB1()*cos
                            + b.getB2()*(2*cos*cos-1);

            double denImag =
                    -b.getB1()*sin
                            -b.getB2()*2*sin*cos;

            double numMag = Math.hypot(numReal,numImag);
            double denMag = Math.hypot(denReal,denImag);

            real *= numMag / denMag;
        }

        double db = 20*Math.log10(real);

        return dbToY(db);
    }

    private double dbToY(double db) {

        double min = -24;
        double max = 24;

        db = Math.max(min,Math.min(max,db));

        double norm = (db-min)/(max-min);

        return getHeight()*(1-norm);
    }

    private double bandResponse(Biquad b, int i) {

        double c = cos[i];
        double s = sin[i];

        double c2 = 2*c*c - 1;
        double sc = 2*s*c;

        double numRe =
                b.getA0()
                        + b.getA1()*c
                        + b.getA2()*c2;

        double numIm =
                -b.getA1()*s
                        -b.getA2()*sc;

        double denRe =
                1
                        + b.getB1()*c
                        + b.getB2()*c2;

        double denIm =
                -b.getB1()*s
                        -b.getB2()*sc;

        double numMag = Math.hypot(numRe,numIm);
        double denMag = Math.hypot(denRe,denIm);

        return numMag / denMag;
    }

    private void drawBands(GraphicsContext g) {

        int hueInc = 360/bands.length;
        int hue = 0;
        for (Biquad band : bands) {

            g.setStroke(Color.hsb(hue, 1.0, 0.9, 0.3));

            double prevY = dbToY(20*Math.log10(bandResponse(band,0)));

            for (int x=1;x<freqs.length;x++) {

                double mag = bandResponse(band,x);

                double db = 20*Math.log10(mag);

                double y = dbToY(db);

                g.strokeLine(x-1, prevY, x, y);

                prevY = y;
            }
            hue += hueInc;
        }
    }

    private void drawTotal(GraphicsContext g) {

        g.setStroke(Color.WHITE);
        g.setLineWidth(2);

        double prevY = dbToY(totalResponse(0));

        for (int x=1;x<freqs.length;x++) {

            double y = dbToY(totalResponse(x));

            g.strokeLine(x-1, prevY, x, y);

            prevY = y;
        }

        g.setLineWidth(1);
    }

    private double totalResponse(int i) {

        double mag = 1;

        for (Biquad b : bands)
            mag *= bandResponse(b,i);

        return 20*Math.log10(mag);
    }
}