package space.n4krug.JACKxer.gui;

import java.util.Collections;
import java.util.List;

import javafx.scene.layout.GridPane;
import space.n4krug.JACKxer.control.ControlParameter;
import space.n4krug.JACKxer.control.ParameterRegistry;
import space.n4krug.JACKxer.jackManager.ClientRegistry;
import space.n4krug.JACKxer.tools.ConfigParser;
import space.n4krug.JACKxer.tools.StringPair;

public class ChannelPage extends GridPane {
	public ChannelPage(List<StringPair> chains, List<StringPair> nodes, ClientRegistry clients, ParameterRegistry params) {
		int i = 0;
		for (StringPair chain : chains) {
			ChannelStrip strip = new ChannelStrip(chain.getKey(), clients, params);
			this.add(strip, i, 0);

			List<String> chainClients = ConfigParser.getChainNodes(nodes, chain.getKey(), Collections.reverseOrder());
			ControlParameter<Boolean> on = params.get(chainClients.get(0) + ".on");
			on.setNormalized(0);

			i++;
		}

	}
}
