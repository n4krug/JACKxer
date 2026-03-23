package space.n4krug.JACKxer.tools;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.Map.Entry;

import org.jaudiolibs.jnajack.JackException;

import com.ezylang.evalex.EvaluationException;
import com.ezylang.evalex.parser.ParseException;

import space.n4krug.JACKxer.control.ParameterRegistry;
import space.n4krug.JACKxer.gui.*;
import space.n4krug.JACKxer.jackManager.Client;
import space.n4krug.JACKxer.jackManager.ClientFactory;
import space.n4krug.JACKxer.jackManager.ClientRegistry;

public class ChannelConfigLoader {
	private static final String CONFIG_LOCATION = "config/";

//	private final ParameterRegistry params = new ParameterRegistry();

	/**
	 * Loads a channel graph configuration, registers parameters, creates clients, connects ports,
	 * and builds the GUI pages.
	 * <p>
	 * The config file is read from {@code config/<file>}.
	 */
	public static void load(String file, ClientRegistry registry, ParameterRegistry params, MainWindow mainWin, PreviewWindow prevWin)
			throws FileNotFoundException, IOException, EvaluationException, ParseException {

		List<String> allLines = Files.readAllLines(Path.of(CONFIG_LOCATION + file));

		LinkedHashMap<String, List<String>> pageMap = ConfigParser.splitPages(allLines);

		Map<String, Integer> globVars = ConfigParser.parseParams("var", pageMap.get("global"));
		Map<String, Integer> globCounters = ConfigParser.parseParams("counter", pageMap.get("global"));

		//List<StringPair> nodes = new ArrayList<>();
		Map<String, String> nodes = new HashMap<>();
		List<StringPair> connections = new ArrayList<>();
		Map<String, List<StringPair>> chains = new HashMap<>();

		for (Entry<String, List<String>> page : pageMap.entrySet()) {

			List<String> lines = page.getValue();

			Map<String, Integer> vars = new HashMap<>();
			vars.putAll(globVars);
			vars.putAll(ConfigParser.parseParams("var", lines));
			Map<String, Integer> counters = new HashMap<>();
			counters.putAll(globCounters);
			counters.putAll(ConfigParser.parseParams("counter", lines));

			chains.put(page.getKey(), ConfigParser.parseKeyword("chain", vars, counters, lines));
			List<StringPair> pageNodes = ConfigParser.parseKeyword("node", vars, counters, lines);
			for (StringPair node : pageNodes) {
				nodes.put(page.getKey() + "." + node.getKey(), node.getValue());
			}
			connections.addAll(ConfigParser.parseKeyword("connect", vars, counters, lines));

			/*
			 * Split Chains
			 */
			for (StringPair chain : chains.get(page.getKey())) {
				String[] elems = chain.getValue().split(" -> ");

				List<String> elemNames = new ArrayList<>();
				for (int elemId = 0; elemId < elems.length; elemId++) {
					String elem = elems[elemId];

					String name = elem;

					if (elem.contains("(")) {
						String type = elem.split("\\(")[0];
						name = String.format("%s.%s.%s", chain.getKey(), elemId, type);
						nodes.put(page.getKey() + "." + name, elem);
					}
					if (!name.startsWith("global.")) {
						name = page.getKey() + "." + name;
					}
					elemNames.add(name);
				}

				for (int i = 0; i < elemNames.size() - 1; i++) {
					String from = elemNames.get(i);
					String to = elemNames.get(i + 1);

					connections.add(new StringPair("", from + " -> " + to));
				}
			}

		}

		/*
		 * Create Nodes (Clients)
		 */
		for (Entry<String, String> node : nodes.entrySet()) {
			String type = node.getValue();
			
			Client client = ClientFactory.create(NodeSpec.parseNode(node.getKey(), type, params));
			System.out.println("Registering node: " + node.getKey());
			registry.register(node.getKey(), client);
		}

		/*
		 * Create Connections
		 */
		for (StringPair connectString : connections) {
			String[] connect = connectString.getValue().split(" -> ");

			for (int i = 0; i < connect.length - 1; i++) {
				String from = connect[i];
				String to = connect[i + 1];

				List<String> fromChain = new ArrayList<>();
				List<String> toChain = new ArrayList<>();
				for (String page : pageMap.keySet()) {
					for (StringPair chain : chains.get(page)) {
						if ((page + "." + chain.getKey()).startsWith(from)) {
							for (Entry<String, String> node : nodes.entrySet()) {
								if (node.getKey().startsWith(page + "." + chain.getKey())) {
									fromChain.add(node.getKey());
								}
							}
						}
						if ((page + "." + chain.getKey()).startsWith(to)) {
							for (Entry<String, String> node : nodes.entrySet()) {
								if (node.getKey().startsWith(page + "." + chain.getKey())) {
									toChain.add(node.getKey());
								}
							}
						}
					}
				}
				Collections.sort(fromChain);
				Collections.sort(toChain);
				if (!fromChain.isEmpty()) {
					from = fromChain.getLast();
				}
				if (!toChain.isEmpty()) {
					to = toChain.getFirst();
				}

				if (!from.contains(":")) {
					from += ":out";
				}

				if (!to.contains(":")) {
					to += ":in";
				}

				connect(from, to, registry);
			}
		}

		/*
		 * GUI Stuff
		 */
		for (String page : pageMap.keySet()) {
			if (page.equals("global")) {
				continue;
			}
			mainWin.addPage(new ChannelPage(page, chains.get(page), nodes, registry, params, mainWin), page);
			if (prevWin != null) {
				prevWin.addPage(ChannelPreview.createChannelPreviews(page, chains.get(page), registry, params), page);
			}
		}
		params.get("active-page.0").setNormalized(1);
	}

	private static String resolvePort(String port, ClientRegistry registry) {

		String[] parts = port.split(":");

		String clientName = parts[0];

		Client client = registry.get(clientName);

		if (client != null) {
			// internal client
			return client.client.getName() + ":" + parts[1];
		}

		// external JACK port
		return port;
	}

	private static void connect(String from, String to, ClientRegistry registry) {

		String src = resolvePort(from, registry);
		String dst = resolvePort(to, registry);

		System.out.println("Connecting " + src + " -> " + dst);

		Client any = registry.getAnyClient();

		try {
			any.connect(src, dst);
		} catch (JackException e) {
			System.out.println("Can't connect node \"" + src + "\" to \"" + dst + "\"");
		}
	}
}
