package space.n4krug.JACKxer.gui;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javafx.geometry.Insets;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import space.n4krug.JACKxer.control.ControlParameter;
import space.n4krug.JACKxer.control.ParameterRegistry;
import space.n4krug.JACKxer.jackManager.ClientRegistry;
import space.n4krug.JACKxer.tools.ConfigParser;
import space.n4krug.JACKxer.tools.StringPair;

public class ChannelPage extends GridPane {
	public ChannelPage(String page, List<StringPair> chains, Map<String, String> nodes, ClientRegistry clients,
					   ParameterRegistry params) {

		ColumnConstraints constraints = new ColumnConstraints(100, 100, Double.MAX_VALUE);
		//constraints.setHgrow(Priority.ALWAYS);

		this.setPadding(new Insets(10));
		this.setHgap(10);

		int i = 0;
		for (StringPair chain : chains) {
			ChannelStrip strip;
			try {
				strip = new ChannelStrip(page + "." + chain.getKey(), clients, params);
				this.add(strip, i, 0);
				this.getStyleClass().add("channel-page");
				this.getColumnConstraints().add(constraints);
				List<String> chainClients = ConfigParser.getChainNodes(nodes, page + "." + chain.getKey(),
						Collections.reverseOrder());
				ControlParameter<Boolean> on = params.get(chainClients.getFirst() + ".on");
				on.setNormalized(0);
			} catch (Exception e) {
				e.printStackTrace();
			}

			i++;
		}

	}
}
