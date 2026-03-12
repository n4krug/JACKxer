package space.n4krug.JACKxer.gui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Set;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import space.n4krug.JACKxer.control.ControlParameter;
import space.n4krug.JACKxer.control.ParameterRegistry;
//import javafx.event.ActionEvent;
//import javafx.event.EventHandler;
import space.n4krug.JACKxer.jackManager.Client;
import space.n4krug.JACKxer.jackManager.ClientRegistry;
import space.n4krug.JACKxer.jackManager.Compressor;
import space.n4krug.JACKxer.jackManager.Gain;

public class ChannelStrip extends VBox {

	private static final int METER_HEIGHT = 200;
	private static final int METER_WIDTH = 20;

	private Gain gainClient;
	private Compressor compClient;
	private final Client lastClient;
	private final Client firstClient;

	public ChannelStrip(String chainName, ClientRegistry clients, ParameterRegistry params) throws Exception {

		this.setPadding(new Insets(10));

		Set<String> allClients = clients.getClientNames();
		ArrayList<String> chainClients = new ArrayList<>();
		for (String client : allClients) {
			//System.out.println("Client: " + client);
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
		}
		lastClient = clients.get(chainClients.getLast());
		firstClient = clients.get(chainClients.getFirst());

		setSpacing(8);
		setAlignment(Pos.CENTER);

		Label name = new Label(chainName.substring(chainName.indexOf(".") + 1));

		LevelMeter preMeter = new LevelMeter(firstClient, LevelMeter.Type.PRE);
		LevelMeter meter = new LevelMeter(lastClient);
		Slider fader = createFader(params);

		HBox meters = new HBox();
		meters.getChildren().addAll(preMeter, meter, fader);
		meters.setAlignment(Pos.CENTER);

		Button mute = createMuteButton(params);
		
		getChildren().addAll(name, meters, mute);

		if (compClient != null) {
			Button comp = createCompButton(params);
			getChildren().add(comp);
		}
	}

	private Slider createFader(ParameterRegistry params) {
		
		final float min = -60;
		final float max = 6;
		

		ControlParameter<Float> gainParam = params.get(gainClient.toString() + ".gain");
		
		Slider slider = new Slider(min, max, gainParam.getValue());
		slider.setPrefHeight(200);
		slider.setPrefWidth(20);
		slider.setOrientation(Orientation.VERTICAL);
		gainParam.addListener(dB -> {
			slider.adjustValue(dB);
		});
		
		slider.valueProperty().addListener((obs, o, n) -> {

			float normalized = (n.floatValue() - min) / (max - min);
			
			gainParam.setNormalized(normalized);

		});

		return slider;
	}

	private Button createMuteButton(ParameterRegistry params) {
		
		Button button = new Button("ON");

		button.setPrefSize(60, 60);
		String baseStyle = "-fx-background-color: red; -fx-background-radius: 8;";
		button.setStyle(baseStyle);

		ControlParameter<Boolean> on = params.get(lastClient.toString() + ".on");
		on.addListener(state -> {
			if (state) {
				button.setStyle(baseStyle);
			} else {
				button.setStyle(baseStyle + "-fx-background-color: lightgray;");
			}
		});
		
		button.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent e) {
				on.setNormalized(on.getValue() ? 0 : 1);
			}
			
		});

		return button;
	}
	
	private Button createCompButton(ParameterRegistry params) {
		Button button = new Button("Comp");
		
		button.setPrefSize(60, 60);
		button.setStyle("-fx-background-color: lightgray; -fx-background-radius: 8;");
		
		button.setOnAction(e -> {

		    Stage stage = new Stage();

		    CompressorPane pane =
		        new CompressorPane(compClient, params);

		    stage.setScene(new Scene(pane));
		    stage.setTitle("Compressor");
		    stage.show();
		});
		
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