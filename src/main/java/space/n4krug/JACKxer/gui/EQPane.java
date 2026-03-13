package space.n4krug.JACKxer.gui;

import javafx.geometry.Dimension2D;
import javafx.geometry.Insets;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Slider;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import space.n4krug.JACKxer.control.ControlParameter;
import space.n4krug.JACKxer.control.ParameterRegistry;
import space.n4krug.JACKxer.jackManager.Biquad;
import space.n4krug.JACKxer.jackManager.ParametricEQ;

public class EQPane extends GridPane {

    private final ParametricEQ eq;
    private final Biquad[] bands;

    public EQPane(ParametricEQ eq, ParameterRegistry params) {

        this.setPadding(new Insets(20));
        this.eq = eq;
        this.bands = eq.getBands();

        EQGraphBasic graph = new EQGraphBasic(bands);
        FFTGraph visGraph = new FFTGraph(eq, new Dimension2D(400, 200), 96);
        StackPane graphs = new StackPane();
        graphs.getChildren().addAll(graph, visGraph);

        VBox controls = new VBox();

        for (int i = 0; i < bands.length; i++) {

            int band = i;

            ControlParameter<Float> freq = params.get(eq + ".band" + i + ".freq");
            ControlParameter<Float> q = params.get(eq + ".band" + i + ".q");
            ControlParameter<Biquad.Type> type = params.get(eq + ".band" + i + ".type");
            ControlParameter<Float> gain = params.get(eq + ".band" + i + ".gain");

            ComboBox<Biquad.Type> typeComboBox = new ComboBox<>();
            typeComboBox.getItems().setAll(Biquad.Type.values());
            typeComboBox.getSelectionModel().select(type.getValue());
            Slider freqSlider = new Slider(0, 1, freqToNorm(freq.getValue()));
            Slider gainSlider = new Slider(-24, 24, gain.getValue());
            Slider qSlider = new Slider(0.3f, 10f, q.getValue());

            type.addListener(v -> {
                graph.rerender();
                typeComboBox.getSelectionModel().select(v);
            });
            freq.addListener(v -> {
                graph.rerender();
                freqSlider.setValue(freqToNorm(v));
            });
            gain.addListener(v -> {
                graph.rerender();
                gainSlider.setValue(v);
            });
            q.addListener(v -> {
                graph.rerender();
                qSlider.setValue(v);
            });

            typeComboBox.setOnAction(e -> {
                float norm = (float) typeComboBox.getSelectionModel().getSelectedIndex() / (float) (typeComboBox.getItems().size() - 1);
                type.setNormalized(norm);
            });

            freqSlider.valueProperty().addListener((o, a, b) -> {
                freq.setNormalized(b.floatValue());
            });

            gainSlider.valueProperty().addListener((o, a, b) -> {
                float min = (float) gainSlider.getMin();
                float max = (float) gainSlider.getMax();
                gain.setNormalized((b.floatValue() - min) / (max - min));
            });

            qSlider.valueProperty().addListener((o, a, b) -> {
                float min = (float) qSlider.getMin();
                float max = (float) qSlider.getMax();
                q.setNormalized((b.floatValue() - min) / (max - min));
            });

            controls.getChildren().addAll(typeComboBox, freqSlider, gainSlider, qSlider);
        }

        add(graphs, 0, 0);
        add(controls, 1, 0);
        this.getStyleClass().add("eq-pane");
    }

    private static double freqToNorm(double f) {

        double fMin = 20;
        double fMax = 20000;

        return Math.log(f / fMin) / Math.log(fMax / fMin);
    }
}