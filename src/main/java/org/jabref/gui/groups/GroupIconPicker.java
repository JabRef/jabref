package org.jabref.gui.groups;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.util.Callback;
import org.jabref.gui.icon.InternalMaterialDesignIcon;
import org.jabref.gui.icon.JabRefIcon;
import org.jabref.gui.util.BaseDialog;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.groups.AbstractGroup;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.IkonProvider;

import java.util.*;

import static java.util.EnumSet.allOf;


public class GroupIconPicker extends BaseDialog<AbstractGroup> {

    private static final List<Ikon> ICONS = new ArrayList<>();

    public String selectedIconName;
    private Pagination pagination;
    private static int iconInOnePage = 504;
    private static int iconInOneRow = 6;

    public ScrollPane createPage(int PageIndex) {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setMaxWidth(Double.MAX_VALUE);
        grid.setAlignment(Pos.CENTER_LEFT);
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setContent(grid);
        scrollPane.setPrefWidth(300);
        scrollPane.setPrefHeight(300);
        int colPointer = 0;
        int rowPointer = 0;
        for (int i = PageIndex * iconInOnePage; i < ICONS.size() - 1 && i < (PageIndex + 1) * iconInOnePage; i++) {
            Ikon ikon = ICONS.get(i);
            Button button = new Button();
            JabRefIcon jabRefIcon = new InternalMaterialDesignIcon(ikon);
            button.setGraphic(new InternalMaterialDesignIcon(ikon).getGraphicNode());
            grid.add(button, colPointer++, rowPointer);
            if (colPointer % iconInOneRow == 0) {
                rowPointer++;
                colPointer = colPointer % iconInOneRow;
            }
            button.setOnAction(event -> {
                selectedIconName = jabRefIcon.getIkon().toString();
            });
        }
        return scrollPane;
    }

    public GroupIconPicker() {
        loadAllIkons();
        this.setTitle(Localization.lang("Icon Picker"));
        int iconNumber = ICONS.size();
        pagination = new Pagination((int) Math.ceil(iconNumber / (iconInOnePage * 1.0)));
        pagination.setPageFactory(new Callback<Integer, Node>() {
            @Override
            public Node call(Integer pageIndex) {
                return createPage(pageIndex);
            }
        });
        getDialogPane().getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);
        getDialogPane().setContent(pagination);
    }

    private static void loadAllIkons() {
        ServiceLoader<IkonProvider> providers = ServiceLoader.load(IkonProvider.class);
        for (IkonProvider provider : providers) {
            ICONS.addAll(allOf(provider.getIkon()));
        }
    }
}
