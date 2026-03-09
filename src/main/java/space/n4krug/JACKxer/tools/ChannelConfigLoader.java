package space.n4krug.JACKxer.tools;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.jaudiolibs.jnajack.JackException;

import com.ezylang.evalex.EvaluationException;
import com.ezylang.evalex.Expression;
import com.ezylang.evalex.parser.ParseException;

import javafx.scene.layout.GridPane;
import space.n4krug.JACKxer.control.ControlParameter;
import space.n4krug.JACKxer.control.ParameterRegistry;
import space.n4krug.JACKxer.gui.ChannelPage;
import space.n4krug.JACKxer.gui.ChannelStrip;
import space.n4krug.JACKxer.gui.MainWindow;
import space.n4krug.JACKxer.jackManager.Client;
import space.n4krug.JACKxer.jackManager.ClientFactory;
import space.n4krug.JACKxer.jackManager.ClientRegistry;

public class ChannelConfigLoader {
	private static final String CONFIG_LOCATION = "config/";

//	private final ParameterRegistry params = new ParameterRegistry();

	public static void load(String file, ClientRegistry registry, ParameterRegistry params, MainWindow mainWin)
			throws FileNotFoundException, IOException, JackException, EvaluationException, ParseException {

		List<String> allLines = Files.readAllLines(Path.of(CONFIG_LOCATION + file));


		Map<String, List<String>> pageMap = ConfigParser.splitPages(allLines);

		Map<String, Integer> globVars = ConfigParser.parseParams("var", pageMap.get("global"));
		Map<String, Integer> globCounters = ConfigParser.parseParams("counter", pageMap.get("global"));

		
		List<StringPair> nodes = new ArrayList<>();
		List<StringPair> connections = new ArrayList<>();
		Map<String, List<StringPair>> chains = new HashMap<>();
		
		for (Entry<String, List<String>> page : pageMap.entrySet()) {
			
			List<String> lines = page.getValue();
			
			Map<String, Integer> vars = new HashMap<>();
			vars.putAll(globVars);
			vars.putAll(ConfigParser.parseParams("var", lines));
			Map<String, Integer> counters = new HashMap<>();
			vars.putAll(globCounters);
			vars.putAll(ConfigParser.parseParams("counter", lines));
			
			
			chains.put(page.getKey(), ConfigParser.parseKeyword("chain", vars, counters, lines));
			nodes.addAll(ConfigParser.parseKeyword("node", vars, counters, lines));
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
						nodes.add(new StringPair(name, elem));
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
			for (StringPair node : nodes) {
				String type = node.getValue();

				Client client = ClientFactory.create(NodeSpec.parseNode(node.getKey(), type, params));
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
					for (String page : chains.keySet()) {
						for (StringPair chain : chains.get(page)) {
							if (chain.getKey().startsWith(from)) {
								for (StringPair node : nodes) {
									if (node.getKey().startsWith(chain.getKey())) {
										fromChain.add(node.getKey());
									}
								}
							}
							if (chain.getKey().startsWith(to)) {
								for (StringPair node : nodes) {
									if (node.getKey().startsWith(chain.getKey())) {
										toChain.add(node.getKey());
									}
								}
							}
						}
					}
					Collections.sort(fromChain, Collections.reverseOrder());
					Collections.sort(toChain);
					if (fromChain.size() >= 1) {
						from = fromChain.get(0);
					}
					if (toChain.size() >= 1) {
						to = toChain.get(0);
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
			mainWin.addPage(new ChannelPage(chains, nodes, registry, params), page.getKey());
//		Set<String> allClients = registry.getClientNames();
//		int i = 0;
//		for (StringPair chain : chains) {
//			ChannelStrip strip = new ChannelStrip(chain.getKey(), registry, params);
//			mainPane.add(strip, i, 0);
//
//			// Turn of last nodes in chains
//			ArrayList<String> chainClients = new ArrayList<>();
//			for (String client : allClients) {
//				if (client.startsWith(chain.getKey())) {
//					chainClients.add(client);
//				}
//			}
//			Collections.sort(chainClients, Collections.reverseOrder());
//			ControlParameter<Boolean> on = params.get(chainClients.get(0) + ".on");
//			on.setNormalized(0);
//
//			i++;
//		}
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

	private static void connect(String from, String to, ClientRegistry registry) throws JackException {

		String src = resolvePort(from, registry);
		String dst = resolvePort(to, registry);

		System.out.println("Connecting " + src + " -> " + dst);

		Client any = registry.getAnyClient();

		any.connect(src, dst);
	}
}
