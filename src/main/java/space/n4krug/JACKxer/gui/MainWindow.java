package space.n4krug.JACKxer.gui;

import java.util.ArrayList;
import java.util.List;

import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;

public class MainWindow extends BorderPane {
	
	private final List<Pane> pages = new ArrayList<>();
	private final HBox pageNav = new HBox();
	
	public MainWindow() {
		this.setBottom(pageNav);
	}
	
	public void addPage(Pane page, String name) {
		int pageId = pages.size();
		pages.add(page);
		Button button = new Button(name);
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
	}
}
