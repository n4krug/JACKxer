package space.n4krug.JACKxer.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Transmitter;

import com.ezylang.evalex.EvaluationException;
import com.ezylang.evalex.parser.ParseException;

import space.n4krug.JACKxer.control.ControlParameter;
import space.n4krug.JACKxer.control.MidiInput;
import space.n4krug.JACKxer.control.MidiRouter;
import space.n4krug.JACKxer.control.ParameterRegistry;

public class MidiConfigLoader {
	private static final String MIDI_CONFIG_LOCATION = "config/midi/";
	private static final ArrayList<MidiDevice> openDevices = new ArrayList<>();

	/**
	 * Loads a MIDI mapping config for a specific MIDI device.
	 * <p>
	 * The file format uses {@code control <id> = <parameterId>} entries, where {@code <id>}
	 * typically looks like {@code ch0.cc74}.
	 */
	public static void load(String file, MidiRouter router, ParameterRegistry registry, MidiDevice.Info deviceInfo)
			throws FileNotFoundException, IOException, MidiUnavailableException, EvaluationException, ParseException {

		MidiDevice device = MidiSystem.getMidiDevice(deviceInfo);
		if (device.getMaxTransmitters() == 0) {
			return;
		}
		device.open();
		openDevices.add(device);
		Transmitter trans = device.getTransmitter();
		trans.setReceiver(new MidiInput(router, deviceInfo.getName()));
		
//		Properties props = new Properties();
//		props.load(new FileInputStream(file));
		List<String> lines = Files.readAllLines(Path.of(file));

		Map<String, Integer> vars = ConfigParser.parseParams("var", lines);
		Map<String, Integer> counters = ConfigParser.parseParams("counter", lines);
		
		List<StringPair> controls = ConfigParser.parseKeyword("control", vars, counters, lines);
		
		for (StringPair control : controls) {
			
			System.out.println("Mapping " + control.getKey() + " to " + control.getValue());

			if (!control.getKey().startsWith("ch") || !control.getKey().contains("cc")) {
				continue;
			}

			String[] parts = control.getKey().split("\\.");

			int channel = Integer.parseInt(parts[0].substring(2));
			int cc = Integer.parseInt(parts[1].substring(2));

			String paramName = control.getValue();

			ControlParameter p = registry.get(paramName);

			if (p != null) {
				router.map(deviceInfo.getName(), "ch" + channel + ".cc" + cc, p);
			}
		}
	}

	public static void loadAllAvailable(MidiRouter router, ParameterRegistry registry)
			throws FileNotFoundException, IOException, MidiUnavailableException, EvaluationException, ParseException {
		final MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();

		for (MidiDevice.Info info : infos) {
			File configFile = new File(MIDI_CONFIG_LOCATION + info.getName() + ".cfg");
			if (configFile.exists() && !configFile.isDirectory() && configFile.canRead()) {
				System.out.println("Loading config for: " + info.getName());
				load(configFile.getAbsolutePath(), router, registry, info);
			}
		}
	}
}
