package space.n4krug.JACKxer.gui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Set;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Dimension2D;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import space.n4krug.JACKxer.control.ControlParameter;
import space.n4krug.JACKxer.control.ParameterRegistry;
//import javafx.event.ActionEvent;
//import javafx.event.EventHandler;
import space.n4krug.JACKxer.jackManager.*;

public class ChannelStrip extends VBox {

	private static final int METER_HEIGHT = 200;
	private static final int METER_WIDTH = 20;

	private Gain gainClient;
	private Compressor compClient;
	private ParametricEQ eqClient;
	private final Client lastClient;
	private final Client firstClient;

	private final MainWindow mainWin;

	public ChannelStrip(String chainName, ClientRegistry clients, ParameterRegistry params, MainWindow mainWin) throws Exception {

		this.setPadding(new Insets(10));
		this.getStyleClass().add("channel-strip");
		this.mainWin = mainWin;
		setFillWidth(true);
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

		setSpacing(8);
		setAlignment(Pos.CENTER);

		Label name = new Label(chainName.substring(chainName.indexOf(".") + 1));

		LevelMeter preMeter = new LevelMeter(firstClient, LevelMeter.Type.PRE);
		preMeter.prefHeightProperty().bind(heightProperty().multiply(0.35));
		preMeter.prefWidthProperty().bind(widthProperty().divide(3));
		LevelMeter meter = new LevelMeter(lastClient);
		meter.prefHeightProperty().bind(heightProperty().multiply(0.35));
		meter.prefWidthProperty().bind(widthProperty().divide(3));
		Slider fader = createFader(params);

		HBox meters = new HBox();
		meters.getChildren().addAll(preMeter, meter, fader);
		meters.setAlignment(Pos.CENTER);

		Button mute = createMuteButton(params);

		FFTGraph visGraph = new FFTGraph(lastClient, new Dimension2D(60, 60), 32);
		visGraph.widthProperty().bind(widthProperty().subtract(20));
		visGraph.heightProperty().bind(widthProperty().multiply(0.75f));

		visGraph.widthProperty().addListener((obs, oldV, newV) -> visGraph.render());
		//visGraph.heightProperty().addListener((obs, oldV, newV) -> visGraph.render());

		getChildren().addAll(name, meters, visGraph, mute);

		if (compClient != null) {
			Button comp = createCompButton(params);
			getChildren().add(comp);
		}

		if (eqClient != null) {
			Button eq = createEQButton(params);
			getChildren().add(eq);
		}
	}

	private Slider createFader(ParameterRegistry params) {
		
		final float min = -60;
		final float max = 6;
		

		ControlParameter<Float> gainParam = params.get(gainClient.toString() + ".gain");
		
		Slider slider = new Slider(min, max, gainParam.getValue());
		//slider.setPrefHeight(200);
		//slider.setPrefWidth(20);
		slider.setOrientation(Orientation.VERTICAL);
		gainParam.addListener(dB -> {
			slider.adjustValue(dB);
		});

		slider.prefHeightProperty().bind(heightProperty().multiply(0.35));

		slider.valueProperty().addListener((obs, o, n) -> {

			float normalized = (n.floatValue() - min) / (max - min);
			
			gainParam.setNormalized(normalized);

		});

		return slider;
	}

	private Button createMuteButton(ParameterRegistry params) {
		
		Button button = new Button("OFF");

		button.setPrefSize(60, 60);
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

		button.prefWidthProperty().bind(widthProperty().multiply(0.7));
		button.prefHeightProperty().bind(button.prefWidthProperty());

		button.setOnAction(_ -> on.setNormalized(on.getValue() ? 0 : 1));

		return button;
	}
	
	private Button createCompButton(ParameterRegistry params) {
		Button button = new Button("Comp");
		
		button.setPrefSize(60, 60);
		button.getStyleClass().add("comp-button");

		button.setOnAction(e -> {

		    Stage stage = new Stage();

		    CompressorPane pane =
		        new CompressorPane(compClient, params);

			Scene scene = new Scene(pane);
			stage.setScene(scene);
		    stage.setTitle("Compressor");
		    stage.show();
			scene.getStylesheets().add("style.css");
		});

		button.prefWidthProperty().bind(widthProperty().multiply(0.7));
		button.prefHeightProperty().bind(button.prefWidthProperty());

		return button;
	}

	private Button createEQButton(ParameterRegistry params) {
		Button button = new Button("EQ");

		button.setPrefSize(60, 60);
		button.getStyleClass().add("comp-button");

		button.setOnAction(e -> {

			Stage stage = new Stage();

			EQPane pane = new EQPane(eqClient, params);

			mainWin.addOverlay(pane);

			//Scene scene = new Scene(pane);
			//stage.setScene(scene);
			//stage.setTitle("EQ");
			//stage.show();
			//scene.getStylesheets().add("style.css");
		});

		button.prefWidthProperty().bind(widthProperty().multiply(0.7));
		button.prefHeightProperty().bind(button.prefWidthProperty());

		return button;
	}

	private static double dbToHeight(double db) {

		double min = -60;
		double max = 0;

		db = Math.max(min, Math.min(max, db));

		double norm = (db - min) / (max - min);

		return norm * METER_HEIGHT;
	}
}