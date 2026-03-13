package space.n4krug.JACKxer.gui;

import java.util.ArrayList;
import java.util.List;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import space.n4krug.JACKxer.control.ControlParameter;
import space.n4krug.JACKxer.control.ParameterRegistry;

public class MainWindow extends BorderPane {
	
	private final List<Pane> pages = new ArrayList<>();
	private final HBox pageNav = new HBox(10);

	private final ParameterRegistry params;
	//private final ArrayList<ControlParameter<Boolean>> pageParams = new ArrayList<>();

	public MainWindow(ParameterRegistry params) {
		this.params = params;
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
				this.setCenter(pages.get(pageId));
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
}
