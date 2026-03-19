package space.n4krug.JACKxer.gui;

import javafx.geometry.Insets;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
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

		HBox graphLevels = new HBox(graph, inLevel, outLevel);

		graphLevels.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

		HBox.setMargin(inLevel, new Insets(5));
		HBox.setMargin(outLevel, new Insets(5));

		setVgrow(graphLevels, Priority.ALWAYS);
		setHgrow(graphLevels, Priority.ALWAYS);

		add(graphLevels, 1, 0);
		//add(inLevel, 2, 0);
		//add(outLevel, 3, 0);
	}

	private class KneeGraph extends Region {
		private final Canvas canvas;
		private final double prefSize;

		KneeGraph() {
			this.prefSize = 200;
			this.canvas = new Canvas(prefSize, prefSize);
			getChildren().add(canvas);

			setMinSize(0, 0);
			setPrefSize(prefSize, prefSize);
			setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

			rerender();
		}

		@Override
		protected void layoutChildren() {
			double w = Math.max(0, getWidth());
			double h = Math.max(0, getHeight());

			double s = Math.min(w, h);
			double x = (w - s) / 2.0;
			double y = (h - s) / 2.0;

			boolean resized = false;
			if (Math.abs(canvas.getWidth() - s) > 0.5) {
				canvas.setWidth(s);
				resized = true;
			}
			if (Math.abs(canvas.getHeight() - s) > 0.5) {
				canvas.setHeight(s);
				resized = true;
			}

			canvas.relocate(x, y);

			if (resized) {
				rerender();
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
			return prefSize;
		}

		@Override
		protected double computePrefHeight(double width) {
			return prefSize;
		}

		void rerender() {
			GraphicsContext g = canvas.getGraphicsContext2D();

			double w = canvas.getWidth();
			double h = canvas.getHeight();
			g.clearRect(0, 0, w, h);

			int wi = (int) Math.floor(w);
			if (wi < 2 || h <= 0) {
				return;
			}

			g.setStroke(Color.WHITE);
			g.setLineWidth(2);
			g.setLineCap(StrokeLineCap.ROUND);
			g.setLineJoin(StrokeLineJoin.ROUND);

			double minDb = -60.0;
			double maxDb = 0.0;

			double prevY = dbToY(compressorCurve(xToDb(0, wi, minDb, maxDb)), h, minDb, maxDb);

			g.beginPath();
			g.moveTo(0, prevY);

			for (int x = 1; x < wi; x++) {
				double inDb = xToDb(x, wi, minDb, maxDb);
				double outDb = compressorCurve(inDb);
				double y = dbToY(outDb, h, minDb, maxDb);
				g.lineTo(x, y);
			}

			g.stroke();
		}

		private double xToDb(int x, int width, double minDb, double maxDb) {
			int denom = Math.max(1, width - 1);
			double t = (double) x / (double) denom;
			return minDb + t * (maxDb - minDb);
		}

		private double dbToY(double db, double height, double minDb, double maxDb) {
			double v = Math.max(minDb, Math.min(maxDb, db));
			double t = (v - minDb) / (maxDb - minDb);
			return height * (1.0 - t);
		}

		private double compressorCurve(double inDb) {
			final double threshold = comp.getThresholdDb();
			final double makeupGain = 20.0 * Math.log10(comp.getMakeupGain());
//			System.out.println(makeupGain);
//			System.out.println(threshold);
			final double ratio = comp.getRatio();
//			System.out.println(ratio);
//			System.out.println(indB);
			if (inDb <= threshold) {
				return inDb + makeupGain;
			}

//			System.out.println("in:" + indB + ", thresh: " + threshold);
			return threshold + (inDb - threshold) / ratio + makeupGain;
		}
	}
}
