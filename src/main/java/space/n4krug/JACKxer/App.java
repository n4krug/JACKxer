package space.n4krug.JACKxer;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.stage.Stage;
import space.n4krug.JACKxer.control.MidiRouter;
import space.n4krug.JACKxer.control.ParameterRegistry;
import space.n4krug.JACKxer.gui.MainWindow;
import space.n4krug.JACKxer.gui.PreviewWindow;
import space.n4krug.JACKxer.jackManager.ClientRegistry;
import space.n4krug.JACKxer.tools.ChannelConfigLoader;
import space.n4krug.JACKxer.tools.MidiConfigLoader;
import space.n4krug.JACKxer.tools.ParameterStateStore;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * JavaFX entrypoint.
 * <p>
 * Bootstraps the {@link space.n4krug.JACKxer.jackManager.ClientRegistry} and
 * {@link space.n4krug.JACKxer.control.ParameterRegistry}, then loads the audio graph and UI
 * from configuration files under {@code config/}.
 */
public class App extends Application {

	private static final String MAIN_CONFIG = "main.cfg";
	private final AtomicBoolean saved = new AtomicBoolean(false);

	@Override
	public void start(Stage stage) throws Exception {
		ClientRegistry clientRegistry = new ClientRegistry();
		ParameterRegistry params = new ParameterRegistry();
		MainWindow mainWin = new MainWindow(params);
		PreviewWindow prevWin = new PreviewWindow(params);
		ChannelConfigLoader.load(MAIN_CONFIG, clientRegistry, params, mainWin, prevWin);
		ParameterStateStore.loadAndApply(MAIN_CONFIG, params);
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
			if (saved.compareAndSet(false, true)) {
				ParameterStateStore.save(MAIN_CONFIG, params);
			}
			Platform.exit();
			System.exit(0);
        });

	}

	public static void main(String[] args) {
		launch(args);
	}
}
