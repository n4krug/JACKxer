package space.n4krug.JACKxer.gui;

import javafx.geometry.Dimension2D;
import javafx.geometry.Insets;
import javafx.geometry.VPos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Slider;
import javafx.scene.layout.*;
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

        EQGraphBasic graph = new EQGraphBasic(bands, 400, 200);
        FFTGraph visGraph = new FFTGraph(eq, new Dimension2D(400, 200), 96);
        StackPane graphs = new StackPane();
        graphs.getChildren().addAll(graph, visGraph);

        HBox controls = new HBox();

        setVgrow(graphs, Priority.ALWAYS);
        setHgrow(graphs, Priority.ALWAYS);

        ControlParameter<Boolean> bypass = params.get(eq + ".bypass");
        Button bypassButton = new Button("OFF");
        if (!bypass.getValue()) {
            bypassButton.getStyleClass().add("active");
            bypassButton.setText("ON");
        }
        bypass.addListener(v -> {
            if (!v) {
                bypassButton.getStyleClass().add("active");
                bypassButton.setText("ON");
            } else {
                bypassButton.getStyleClass().remove("active");
                bypassButton.setText("OFF");
            }
        });
        bypassButton.setOnAction(e -> {
            bypass.setNormalized(bypass.getValue() ? 0 : 1);
        });
        bypassButton.setPrefSize(60, 60);

        controls.getChildren().add(bypassButton);
        GridPane.setMargin(bypassButton, new Insets(10));
        GridPane.setValignment(bypassButton, VPos.BOTTOM);

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
            freqSlider.setOrientation(javafx.geometry.Orientation.VERTICAL);
            gainSlider.setOrientation(javafx.geometry.Orientation.VERTICAL);
            qSlider.setOrientation(javafx.geometry.Orientation.VERTICAL);

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

            HBox sliders = new HBox();
            sliders.getChildren().addAll(freqSlider,gainSlider,qSlider);

            VBox control = new VBox();
            control.getChildren().addAll(sliders, typeComboBox);

            controls.getChildren().add(control);
        }


        add(graphs, 0, 0);
        add(controls, 0, 1);
        getStyleClass().add("eq-pane");
    }

    private static double freqToNorm(double f) {

        double fMin = 20;
        double fMax = 20000;

        return Math.log(f / fMin) / Math.log(fMax / fMin);
    }
}