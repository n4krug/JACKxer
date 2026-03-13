package space.n4krug.JACKxer.gui;

import javafx.geometry.Dimension2D;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import space.n4krug.JACKxer.control.ParameterRegistry;
import space.n4krug.JACKxer.jackManager.Client;
import space.n4krug.JACKxer.jackManager.ClientRegistry;
import space.n4krug.JACKxer.tools.StringPair;

import java.util.*;

public class ChannelPreview extends VBox {

    public ChannelPreview(String chainName, ClientRegistry clients, ParameterRegistry params) {
        this.setPadding(new Insets(10));
        this.getStyleClass().add("channel-preview");


        Set<String> allClients = clients.getClientNames();
        ArrayList<String> chainClients = new ArrayList<>();
        for (String client : allClients) {
            if (client.startsWith(chainName)) {
                chainClients.add(client);
            }
        }
        if (chainClients.isEmpty()) {
            throw new RuntimeException("No clients in chain: " + chainName);
        }
        chainClients.sort(Comparator.naturalOrder());

        Client lastClient = clients.get(chainClients.getLast());

        Label name = new Label(chainName.substring(chainName.indexOf(".") + 1));
        FFTGraph visGraph = new FFTGraph(lastClient, new Dimension2D(100, 75), 50);

        getChildren().addAll(name, visGraph);
    }

    public static GridPane createChannelPreviews(String page, List<StringPair> chains, ClientRegistry clients,
                                                 ParameterRegistry params) {
        GridPane gp = new GridPane();
        gp.getStyleClass().add("preview-page");

        ColumnConstraints constraints = new ColumnConstraints(100, 100, Double.MAX_VALUE);
        //constraints.setHgrow(Priority.ALWAYS);

        gp.setPadding(new Insets(10));
        gp.setHgap(10);

        int i = 0;
        for (StringPair chain : chains) {

            ChannelPreview preview = new ChannelPreview(page + "." + chain.getKey(), clients, params);

            gp.add(preview, i, 0);

            i++;
        }
        return gp;
    }
}
