package space.n4krug.JACKxer.gui;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import javafx.geometry.Dimension2D;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import space.n4krug.JACKxer.control.ControlParameter;
import space.n4krug.JACKxer.control.ParameterRegistry;
import space.n4krug.JACKxer.jackManager.Client;
import space.n4krug.JACKxer.jackManager.ClientRegistry;
import space.n4krug.JACKxer.jackManager.Compressor;
import space.n4krug.JACKxer.jackManager.Gain;
import space.n4krug.JACKxer.jackManager.ParametricEQ;

public class ChannelStrip extends Region {

    private static final double GAP = 8.0;
    private static final double METERS_HEIGHT_PCT = 0.38; // percentage of total strip height
    private static final double BUTTON_WIDTH_PCT = 0.90;  // percentage of strip width, then clamped to fit height
    private static final double FFT_ASPECT_W_OVER_H = 5.0 / 4.0; // 3:2 aspect ratio

    private Gain gainClient;
    private Compressor compClient;
    private ParametricEQ eqClient;
    private final Client lastClient;
    private final Client firstClient;

    private final MainWindow mainWin;

    private final Label name;
    private final LevelMeter preMeter;
    private final LevelMeter postMeter;
    private final GainFaderScale fader;
    private final FFTGraph visGraph;

    private final List<Button> buttons = new ArrayList<>();

    public ChannelStrip(String chainName, ClientRegistry clients, ParameterRegistry params, MainWindow mainWin) throws Exception {
        this.mainWin = mainWin;
        setPadding(new Insets(10));
        getStyleClass().add("channel-strip");

        setMinSize(0, 0);
        setPrefSize(220, 420);
        setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        Set<String> allClients = clients.getClientNames();
        ArrayList<String> chainClients = new ArrayList<>();
        for (String client : allClients) {
            if (client.startsWith(chainName)) {
                chainClients.add(client);
            }
        }
        if (chainClients.isEmpty()) {
            throw new Exception("No clients in chain: " + chainName);
        }
        chainClients.sort(Comparator.naturalOrder());

        for (String client : chainClients) {
            if (client.endsWith("gain")) {
                gainClient = (Gain) clients.get(client);
            }
            if (client.endsWith("compressor")) {
                compClient = (Compressor) clients.get(client);
            }
            if (client.endsWith("eq")) {
                eqClient = (ParametricEQ) clients.get(client);
            }
        }
        lastClient = clients.get(chainClients.getLast());
        firstClient = clients.get(chainClients.getFirst());

        name = new Label(chainName.substring(chainName.indexOf(".") + 1));
        name.setAlignment(Pos.TOP_CENTER);
        name.setMaxWidth(Double.MAX_VALUE);

        preMeter = new LevelMeter(firstClient, LevelMeter.Type.PRE);
        postMeter = new LevelMeter(lastClient);
        fader = createFader(params);

        // 300x200 sets 3:2 pref aspect for any parent doing pref-size computations.
        visGraph = new FFTGraph(lastClient, new Dimension2D(300, 200), 32);

        Button solo = createSoloButton(params);
        buttons.add(solo);

        Button mute = createMuteButton(params);
        buttons.add(mute);

        if (compClient != null) {
            Button comp = createCompButton(params);
            buttons.add(comp);
        }

        if (eqClient != null) {
            Button eq = createEQButton(params);
            buttons.add(eq);
        }

        // Allow manual layout to shrink controls.
        for (Button b : buttons) {
            b.setMinSize(0, 0);
            b.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        }
        fader.setMinSize(0, 0);
        fader.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        getChildren().addAll(name, preMeter, postMeter, fader, visGraph);
        getChildren().addAll(buttons);
    }

    private GainFaderScale createFader(ParameterRegistry params) {
        ControlParameter<Float> gainParam = params.get(gainClient.toString() + ".gain");
        return new GainFaderScale(gainClient, gainParam);
    }

    private Button createMuteButton(ParameterRegistry params) {
        Button button = new Button("OFF");
        button.getStyleClass().add("on-button");

        ControlParameter<Boolean> on = params.get(lastClient.toString() + ".on");
        on.addListener(state -> {
            if (state) {
                button.getStyleClass().add("active");
                button.setText("ON");
            } else {
                button.getStyleClass().remove("active");
                button.setText("OFF");
            }
        });

        button.setOnAction(_ -> on.setNormalized(on.getValue() ? 0 : 1));
        return button;
    }

    private Button createSoloButton(ParameterRegistry params) {
        Button button = new Button("SOLO");
        button.getStyleClass().add("on-button");

        ControlParameter<Boolean> solo = params.get(lastClient.toString() + ".solo");
        solo.addListener(state -> {
            if (state) {
                button.getStyleClass().add("active");
            } else {
                button.getStyleClass().remove("active");
            }
        });

        button.setOnAction(_ -> solo.setNormalized(solo.getValue() ? 0 : 1));
        return button;
    }

    private Button createCompButton(ParameterRegistry params) {
        Button button = new Button("Comp");
        button.getStyleClass().add("comp-button");
        button.setOnAction(_ -> mainWin.addOverlay(new CompressorPane(compClient, params)));
        return button;
    }

    private Button createEQButton(ParameterRegistry params) {
        Button button = new Button("EQ");
        button.getStyleClass().add("comp-button");
        button.setOnAction(_ -> mainWin.addOverlay(new EQPane(eqClient, params)));
        return button;
    }

    @Override
    protected void layoutChildren() {
        Insets in = getInsets();
        double x0 = in.getLeft();
        double y = in.getTop();
        double innerW = Math.max(0, getWidth() - in.getLeft() - in.getRight());
        double innerH = Math.max(0, getHeight() - in.getTop() - in.getBottom());

        // Top aligned content, laid out sequentially.
        double labelH = snapSizeY(name.prefHeight(innerW));
        name.resizeRelocate(x0, y, innerW, labelH);
        y += labelH + GAP;

        double metersH = clamp(innerH * METERS_HEIGHT_PCT, 0, Math.max(0, innerH - (labelH + GAP)));
        layoutMeters(x0, y, innerW, metersH);
        y += metersH + GAP;

        double remainingH = Math.max(0, in.getTop() + innerH - y);
        if (remainingH <= 0) {
            visGraph.resizeRelocate(x0, y, 0, 0);
            layoutButtons(x0, y, innerW, 0);
            return;
        }

        int nButtons = buttons.size();
        double buttonGapTotal = nButtons > 0 ? GAP * (nButtons - 1) : 0;
        double interGap = nButtons > 0 ? GAP : 0;

        double buttonMaxH = nButtons > 0 ? Math.max(0, (remainingH - interGap - buttonGapTotal) / nButtons) : 0;
        double desiredButton = innerW * BUTTON_WIDTH_PCT;
        double buttonSize = clamp(Math.min(desiredButton, buttonMaxH), 0, innerW);

        double fftAvailH = Math.max(0, remainingH - interGap - (nButtons * buttonSize + buttonGapTotal));
        layoutFft(x0, y, innerW, fftAvailH);
        double fftH = visGraph.getHeight();
        y += fftH;
        if (nButtons > 0 && fftH > 0) {
            y += GAP;
        }

        layoutButtons(x0, y, innerW, buttonSize);
    }

    private void layoutMeters(double x0, double y0, double w, double h) {
        if (w <= 0 || h <= 0) {
            preMeter.resizeRelocate(x0, y0, 0, 0);
            postMeter.resizeRelocate(x0, y0, 0, 0);
            fader.resizeRelocate(x0, y0, 0, 0);
            return;
        }

        double faderW = clamp(fader.computeBlockWidth(w), 80, w * 0.60);
        double metersW = Math.max(0, w - faderW - 2 * GAP);
        double meterW = metersW / 2.0;

        preMeter.resizeRelocate(x0, y0, meterW, h);
        postMeter.resizeRelocate(x0 + meterW + GAP, y0, meterW, h);
        fader.resizeRelocate(x0 + 2 * meterW + 2 * GAP, y0, faderW, h);
    }

    private void layoutFft(double x0, double y0, double w, double h) {
        if (w <= 0 || h <= 0) {
            visGraph.resizeRelocate(x0, y0, 0, 0);
            return;
        }

        // Largest 3:2 rectangle that fits in (w,h).
        double maxWByH = h * FFT_ASPECT_W_OVER_H;
        double fftW = Math.min(w, maxWByH);
        double fftH = fftW / FFT_ASPECT_W_OVER_H;
        double fftX = x0 + (w - fftW) / 2.0;

        visGraph.resizeRelocate(fftX, y0, fftW, fftH);
    }

    private void layoutButtons(double x0, double y0, double w, double size) {
        double y = y0;
        for (Button b : buttons) {
            if (w <= 0 || size <= 0) {
                b.resizeRelocate(x0, y, 0, 0);
                continue;
            }
            double x = x0 + (w - size) / 2.0;
            b.resizeRelocate(x, y, size, size);
            y += size + GAP;
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

    private static double clamp(double v, double min, double max) {
        if (v < min) {
            return min;
        }
        if (v > max) {
            return max;
        }
        return v;
    }
}
