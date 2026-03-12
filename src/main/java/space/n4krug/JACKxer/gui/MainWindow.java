package space.n4krug.JACKxer.gui;

import java.util.ArrayList;
import java.util.List;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;

public class MainWindow extends BorderPane {
	
	private final List<Pane> pages = new ArrayList<>();
	private final HBox pageNav = new HBox(10);
	private final String buttonStyle = "-fx-background-color: lightgray; -fx-background-radius: 8;";

	public MainWindow() {

		this.setBottom(pageNav);
		this.setPadding(new Insets(10));
	}
	
	public void addPage(Pane page, String name) {
		int pageId = pages.size();
		pages.add(page);
		Button button = new Button(name);
		button.setStyle(buttonStyle);
		button.setPadding(new Insets(10, 20, 10, 20));
		button.setOnAction(e -> {
			setPage(pageId);
		});
		pageNav.getChildren().add(button);
		if (pageId == 0) {
			setPage(pageId);
		}
	}
	
	public void setPage(int pageId) {
		if (pageId >= pages.size()) {
			return;
		}
		this.setCenter(pages.get(pageId));
		for (Node button : pageNav.getChildren()) {
			button.setStyle(buttonStyle);
		}
		pageNav.getChildren().get(pageId).setStyle(buttonStyle + "-fx-background-color: lightgreen;");
	}
}
