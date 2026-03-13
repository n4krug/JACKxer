package space.n4krug.JACKxer;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import space.n4krug.JACKxer.control.MidiRouter;
import space.n4krug.JACKxer.control.ParameterRegistry;
import space.n4krug.JACKxer.gui.MainWindow;
import space.n4krug.JACKxer.gui.PreviewWindow;
import space.n4krug.JACKxer.jackManager.ClientRegistry;
import space.n4krug.JACKxer.jackManager.Compressor;
import space.n4krug.JACKxer.jackManager.Gain;
import space.n4krug.JACKxer.jackManager.WavPlayer;
import space.n4krug.JACKxer.tools.ChannelConfigLoader;
import space.n4krug.JACKxer.tools.MidiConfigLoader;

/**
 * Hello world!
 *
 */
public class App extends Application {

	WavPlayer wp;

	Gain wpGain;

	Compressor compressor;

	@Override
	public void start(Stage stage) throws Exception {
		ClientRegistry clientRegistry = new ClientRegistry();
		ParameterRegistry params = new ParameterRegistry();
		MainWindow mainWin = new MainWindow(params);
		PreviewWindow prevWin = new PreviewWindow(params);
		ChannelConfigLoader.load("main.cfg", clientRegistry, params, mainWin, prevWin);
		MidiRouter midi = new MidiRouter();
		MidiConfigLoader.loadAllAvailable(midi, params);

		Scene scene = new Scene(mainWin);
		scene.getStylesheets().add("style.css");
		stage.setScene(scene);
		stage.show();

		Scene previewScene = new Scene(prevWin);
		previewScene.getStylesheets().add("style.css");
		Stage previewStage = new Stage();
		previewStage.setScene(previewScene);
		previewStage.show();

		stage.setOnCloseRequest(_ -> {
            Platform.exit();
            System.exit(0);
        });

	}

	public static void main(String[] args) {
		launch(args);
	}
}
