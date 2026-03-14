package space.n4krug.JACKxer.gui;

import javafx.geometry.Insets;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import space.n4krug.JACKxer.control.ControlParameter;
import space.n4krug.JACKxer.control.ParameterRegistry;

import java.util.ArrayList;
import java.util.List;

public class PreviewWindow extends BorderPane {

    private final List<Pane> pages = new ArrayList<>();
    ParameterRegistry params;

    /**
     * Secondary window that mirrors the active page selection from {@link MainWindow}.
     */
    public PreviewWindow(ParameterRegistry params) {
        this.params = params;
    }

    public void addPage(Pane page, String name) {
        int pageId = pages.size();
        pages.add(page);
        ControlParameter<Boolean> pageParam = params.get("active-page." + pageId);
        pageParam.addListener(v -> {
            System.out.println("Page" + pageId + ": " + v);
            if (v) {
                System.out.println("Set page to: " + pageId);
                this.setCenter(pages.get(pageId));
            }
        });
    }

}
