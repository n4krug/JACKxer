package space.n4krug.JACKxer.gui;

import javafx.animation.AnimationTimer;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;
import space.n4krug.JACKxer.jackManager.Client;

public class LevelMeter extends Pane {

	public static enum Type {
		PRE, POST
	}

	private static final int DEFAULT_WIDTH = 20;
	private static final int DEFAULT_HEIGHT = 200;

	private final Rectangle mask = new Rectangle();
	private final Rectangle peakLine = new Rectangle();
	private final Rectangle background = new Rectangle();

	public LevelMeter(Client client, Type type) {

		setPrefSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
		setMinSize(0, 0);
		setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

		background.setManaged(false);
		mask.setManaged(false);
		peakLine.setManaged(false);

		background.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE, new Stop(0, Color.RED),
				new Stop(0.2, Color.YELLOW), new Stop(1, Color.LIMEGREEN)));

		mask.setFill(Color.BLACK);
		peakLine.setFill(Color.GRAY);
		peakLine.setHeight(4);

		getChildren().addAll(background, mask, peakLine);

		AnimationTimer timer = new AnimationTimer() {

			double peakHold = 0;

			@Override
			public void handle(long now) {

				double db = type == Type.PRE
						? client.getPrePeakdB()
						: client.getPeakdB();

				double h = dbToHeight(db, getHeight());
				peakHold = Math.max(h, peakHold * 0.999);

				requestLayout();

				mask.setX(0);
				mask.setY(0);
				mask.setWidth(getWidth());
				mask.setHeight(Math.max(0, getHeight() - h));

				peakLine.setX(0);
				peakLine.setWidth(getWidth());
				peakLine.setY(
						Math.max(0, getHeight() - peakHold - peakLine.getHeight() / 2)
				);
			}
		};

		timer.start();
	}

	public LevelMeter(Client client) {
		this(client, Type.POST);

	}

	@Override
	protected void layoutChildren() {
		background.setX(0);
		background.setY(0);
		background.setWidth(getWidth());
		background.setHeight(getHeight());
	}

	private static double dbToHeight(double db, double height) {

		double min = -60;
		double max = 0;

		db = Math.max(min, Math.min(max, db));

		double norm = (db - min) / (max - min);

		return norm * height;
	}
}