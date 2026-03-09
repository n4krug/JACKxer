package space.n4krug.JACKxer.gui;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import space.n4krug.JACKxer.control.ControlParameter;
import space.n4krug.JACKxer.control.ParameterRegistry;
import space.n4krug.JACKxer.jackManager.Compressor;

public class CompressorPane extends GridPane {

	private Compressor comp;

	private final ControlParameter<Float> threshold;
	private final ControlParameter<Float> ratio;
	private final ControlParameter<Float> attack;
	private final ControlParameter<Float> release;
	private final ControlParameter<Float> makeup;

	public CompressorPane(Compressor comp, ParameterRegistry registry) {

		this.comp = comp;
		String name = comp.toString();

		KneeGraph graph = new KneeGraph();
		
		Slider thresholdSlider = new Slider(-60, 0, comp.getThresholdDb());
		Label thresholdLabel = new Label(String.format("Threshold: %.1f dB", comp.getThresholdDb()));
		threshold = registry.get(name + ".threshold");
		threshold.addListener(dB -> {
			thresholdSlider.adjustValue(dB);
			thresholdLabel.setText(String.format("Threshold: %.1f dB", dB));
			graph.rerender();
		});
		thresholdSlider.valueProperty().addListener((o, a, b) -> {
			float min = (float) thresholdSlider.getMin();
			float max = (float) thresholdSlider.getMax();
			threshold.setNormalized((b.floatValue() - min) / (max - min));
		});

		Slider ratioSlider = new Slider(1, 20, comp.getRatio());
		Label ratioLabel = new Label(String.format("Ratio: 1.0 : %.1f", comp.getRatio()));
		ratio = registry.get(name + ".ratio");
		ratio.addListener(ratio -> {
			ratioLabel.setText(String.format("Ratio: 1.0 : %.1f", ratio));
			graph.rerender();
		});
		ratioSlider.valueProperty().addListener((o, a, b) -> {
			float min = (float) ratioSlider.getMin();
			float max = (float) ratioSlider.getMax();
			ratio.setNormalized((b.floatValue() - min) / (max - min));
		});

		Slider attackSlider = new Slider(0.001, 0.2, comp.getAttack());
		Label attackLabel = new Label(String.format("Attack: %.1f ms", comp.getAttack() * 1000));
		attack = registry.get(name + ".attack");
		attack.addListener(s -> {
			attackSlider.adjustValue(s);
			attackLabel.setText(String.format("Attack: %.1f ms", s * 1000));
		});
		attackSlider.valueProperty().addListener((o, a, b) -> {
			float min = (float) attackSlider.getMin();
			float max = (float) attackSlider.getMax();
			attack.setNormalized((b.floatValue() - min) / (max - min));
		});

		Slider releaseSlider = new Slider(0.01, 1.0, comp.getRelease());
		Label releaseLabel = new Label(String.format("Release: %.1f ms", comp.getRelease() * 1000));
		release = registry.get(name + ".release");
		release.addListener(s -> {
			releaseSlider.adjustValue(s);
			releaseLabel.setText(String.format("Release: %.1f ms", comp.getRelease() * 1000));
		});
		releaseSlider.valueProperty().addListener((o, a, b) -> {
			float min = (float) releaseSlider.getMin();
			float max = (float) releaseSlider.getMax();
			release.setNormalized((b.floatValue() - min) / (max - min));
		});

		Slider makeupSlider = new Slider(-6, 24, 20 * Math.log10(comp.getMakeupGain()));
		Label makeupLabel = new Label(String.format("Makeup: %.1f dB", 20 * Math.log10(comp.getMakeupGain())));
		makeup = registry.get(name + ".makeup");
		makeup.addListener(dB -> {
			makeupLabel.setText(String.format("Makeup: %.1f dB", 20 * Math.log10(dB)));
			graph.rerender();
		});
		makeupSlider.valueProperty().addListener((o, a, b) -> {
			float min = (float) makeupSlider.getMin();
			float max = (float) makeupSlider.getMax();
			makeup.setNormalized((b.floatValue() - min) / (max - min));
		});

		VBox tmp = new VBox();

		tmp.getChildren().addAll(thresholdLabel, thresholdSlider, ratioLabel, ratioSlider, attackLabel, attackSlider,
				releaseLabel, releaseSlider, makeupLabel, makeupSlider
//            new Label("GR"), grMeter
		);

		add(tmp, 0, 0);

		LevelMeter inLevel = new LevelMeter(comp, LevelMeter.Type.PRE);
		LevelMeter outLevel = new LevelMeter(comp, LevelMeter.Type.POST);

		add(graph, 1, 0);
		add(inLevel, 2, 0);
		add(outLevel, 3, 0);
	}

	private class KneeGraph extends Canvas {
		KneeGraph() {
			super(200, 200);
			rerender();
		}

		void rerender() {
			GraphicsContext g = getGraphicsContext2D();

			g.clearRect(0, 0, this.getWidth(), this.getHeight());

			float[] prevPoint = new float[2];
			float indB = mapTodB(0);
			float outdB = compressorCurve(indB);
			float out = mapFromdB(outdB);
			for (int i = 1; i < 200; i++) {
				prevPoint[0] = i - 1;
				prevPoint[1] = out;
				indB = mapTodB(i);
				outdB = compressorCurve(indB);
				out = mapFromdB(outdB);

				g.strokeLine(prevPoint[0], prevPoint[1], i, out);
			}
		}

		private float mapTodB(int in) {
			return -60 * (200 - in) / 200;
		}

		private float mapFromdB(float in) {
			return in * 200 / -60;
		}

		private float compressorCurve(float indB) {
			final float threshold = comp.getThresholdDb();
			final float makeupGain = 20f * (float) Math.log10(comp.getMakeupGain());
//			System.out.println(makeupGain);
//			System.out.println(threshold);
			final float ratio = comp.getRatio();
//			System.out.println(ratio);
//			System.out.println(indB);
			if (indB <= threshold) {
				return indB + makeupGain;
			}

//			System.out.println("in:" + indB + ", thresh: " + threshold);
			return threshold + (indB - threshold) / ratio + makeupGain;
		}
	}
}