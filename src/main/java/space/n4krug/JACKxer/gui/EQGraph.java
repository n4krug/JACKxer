package space.n4krug.JACKxer.gui;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import space.n4krug.JACKxer.jackManager.Biquad;

class EQGraph extends Canvas {

    Biquad[] bands;

    EQNode activeNode;

    EQGraph(Biquad[] bands) {
        super(400,200);
        this.bands = bands;
        rerender();

        setOnMousePressed(e -> {

            activeNode = findNearestNode(e.getX(), e.getY());

        });

        setOnMouseDragged(e -> {

            if (activeNode == null) return;

            float freq = (float) xToFreq(e.getX());
            float gain = (float) yToGain(e.getY());

            activeNode.freq.setNormalized(freqToNorm(freq));
            activeNode.gain.setNormalized((gain + 24) / 48);

            rerender();
        });
    }

    void rerender() {

        GraphicsContext g = getGraphicsContext2D();

        g.clearRect(0,0,getWidth(),getHeight());

        double prevX = 0;
        double prevY = response(20);

        for (int x=1;x<getWidth();x++) {

            double freq = screenToFreq(x);

            double y = response(freq);

            g.strokeLine(prevX, prevY, x, y);

            prevX = x;
            prevY = y;
        }

       // for (EQNode node : nodes) {

       //     double x = freqToX(node.freq.getValue());
       //     double y = gainToY(node.gain.getValue());

       //     g.setFill(Color.WHITE);
       //     g.fillOval(x - 5, y - 5, 10, 10);

       // }
    }

    private double screenToFreq(double x) {

        double min = Math.log10(20);
        double max = Math.log10(20000);

        double t = x / getWidth();

        return Math.pow(10, min + t * (max - min));
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

    private double freqToX(double f) {
        double min = Math.log10(20);
        double max = Math.log10(20000);
        double t = (Math.log10(f) - min) / (max - min);
        return t * getWidth();
    }

    private double xToFreq(double x) {
        double min = Math.log10(20);
        double max = Math.log10(20000);
        double t = x / getWidth();
        return Math.pow(10, min + t * (max - min));
    }

    private double gainToY(double db) {
        double min = -24;
        double max = 24;
        double t = (db - min) / (max - min);
        return getHeight() * (1 - t);
    }

    private double yToGain(double y) {
        double min = -24;
        double max = 24;
        double t = 1 - y / getHeight();
        return min + t * (max - min);
    }

    private EQNode findNearestNode(double x, double y) {

        EQNode best = null;
        double bestDist = 20;

       // for (EQNode n : nodes) {

       //     double nx = freqToX(n.freq.getValue());
       //     double ny = gainToY(n.gain.getValue());

       //     double d = Math.hypot(nx - x, ny - y);

       //     if (d < bestDist) {
       //         best = n;
       //         bestDist = d;
       //     }
       // }

        return best;
    }

    private static float freqToNorm(float f) {

        float fMin = 20;
        float fMax = 20000;

        return (float) (Math.log(f / fMin) / Math.log(fMax / fMin));
    }
}
