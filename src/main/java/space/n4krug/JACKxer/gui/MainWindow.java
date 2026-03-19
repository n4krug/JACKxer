package space.n4krug.JACKxer.gui;

import java.util.ArrayList;
import java.util.List;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import space.n4krug.JACKxer.control.ControlParameter;
import space.n4krug.JACKxer.control.ParameterRegistry;

public class MainWindow extends BorderPane {
	
	private final List<Pane> pages = new ArrayList<>();
	private final HBox pageNav = new HBox(10);
	private final StackPane center = new StackPane();

	private final ParameterRegistry params;
	//private final ArrayList<ControlParameter<Boolean>> pageParams = new ArrayList<>();

	/**
	 * Main UI container for channel pages.
	 * <p>
	 * Each page is controlled by a {@code active-page.<n>} toggle parameter in the
	 * {@link ParameterRegistry}.
	 */
	public MainWindow(ParameterRegistry params) {
		this.params = params;
		this.setCenter(center);
		this.setBottom(pageNav);
		pageNav.getStyleClass().add("page-nav");
		pageNav.setPadding(new Insets(10));
	}
	
	public void addPage(Pane page, String name) {
		int pageId = pages.size();
		pages.add(page);
		ControlParameter<Boolean> pageParam =  ControlParameter.toggle(false);
		pageParam.addListener(on -> {
			System.out.println(on);
			if (on) {
				for (int i = 0; i < pages.size(); i++) {
					if (i!=pageId) {
						params.get("active-page." + i).setNormalized(0);
					}
				}
				center.getChildren().clear();
				center.getChildren().add(pages.get(pageId));
				pageNav.getChildren().get(pageId).getStyleClass().add("active");
			} else {
				pageNav.getChildren().get(pageId).getStyleClass().remove("active");
			}
		});
		params.register("active-page." + pageId, pageParam);
		Button button = new Button(name);
		button.getStyleClass().add("page-nav-button");
		button.setPadding(new Insets(20));
		button.setOnAction(_ -> params.get("active-page." + pageId).setNormalized(1));
		pageNav.getChildren().add(button);
	}

	public void addOverlay(Pane content) {
		StackPane pane = new StackPane();
		pane.getStyleClass().add("overlay");
		content.getStyleClass().add("overlay-content");
		Button close = new Button("X");
		pane.getChildren().addAll(content, close);
		StackPane.setAlignment(close, Pos.TOP_RIGHT);
		StackPane.setMargin(pane, new Insets(60));
		StackPane.setMargin(content, new Insets(10));
		StackPane.setMargin(close, new Insets(-10));
		close.setPrefSize(60, 60);
		close.setOnAction(_ -> center.getChildren().remove(pane));
		center.getChildren().add(pane);
	}
}
