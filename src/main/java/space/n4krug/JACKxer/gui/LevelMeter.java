package space.n4krug.JACKxer.gui;

import javafx.animation.AnimationTimer;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;
import space.n4krug.JACKxer.jackManager.Client;

public class LevelMeter extends StackPane {

	public static enum Type {
		PRE, POST
	}

	private static final int WIDTH = 20;
	private static final int HEIGHT = 200;

	private final Rectangle mask = new Rectangle(WIDTH, HEIGHT);
	private final Rectangle peakLine = new Rectangle(WIDTH, 4);

	public LevelMeter(Client client, Type type) {

		Rectangle background = new Rectangle(WIDTH, HEIGHT);

		background.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE, new Stop(0, Color.RED),
				new Stop(0.2, Color.YELLOW), new Stop(1, Color.LIMEGREEN)));

		mask.setFill(Color.BLACK);
		peakLine.setFill(Color.GRAY);

		getChildren().addAll(background, mask, peakLine);

		AnimationTimer timer = new AnimationTimer() {

			double peakHold = 0;

			@Override
			public void handle(long now) {

				double db;
				if (type == Type.PRE) {
					db = client.getPrePeakdB();
				} else {
					db = client.getPeakdB();
				}

				double h = dbToHeight(db);

				peakHold = Math.max(h, peakHold * 0.999);

				mask.setHeight(HEIGHT - h);
				mask.setTranslateY(-h / 2);

				peakLine.setTranslateY(-(peakHold - HEIGHT / 2));
			}
		};

		timer.start();
	}

	public LevelMeter(Client client) {
		this(client, Type.POST);

	}

	private static double dbToHeight(double db) {

		double min = -60;
		double max = 0;

		db = Math.max(min, Math.min(max, db));

		double norm = (db - min) / (max - min);

		return norm * HEIGHT;
	}
}