/*  Copyright (C) 2016 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/
package net.sf.jabref.gui;

import javafx.animation.FadeTransition;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.TableColumnBase;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableRow;
import javafx.scene.control.TreeTableView;
import javafx.scene.text.Text;
import javafx.util.Duration;

import com.sun.javafx.scene.control.skin.TreeTableRowSkin;

public class GroupTreeViewModel {

    @FXML
    private TreeTableView<NewGroupNodeViewModel> groupTree;

    @FXML
    private TreeTableColumn<NewGroupNodeViewModel,NewGroupNodeViewModel> mainColumn;

    @FXML
    private TreeTableColumn<NewGroupNodeViewModel,Integer> numberColumn;

    @FXML
    private TreeTableColumn<NewGroupNodeViewModel,Integer> disclosureNodeColumn;

    @FXML
    public void initialize() {
        TreeItem<NewGroupNodeViewModel> root = new TreeItem<>(new NewGroupNodeViewModel("All Entries", true, 30000,
                IconTheme.JabRefIcon.CLOSE));
        root.setExpanded(true);

        TreeItem<NewGroupNodeViewModel> authors = new TreeItem<>(new NewGroupNodeViewModel("Authors", false, 300,
                IconTheme.JabRefIcon.PRIORITY));
        authors.setExpanded(true);
        authors.getChildren().addAll(
                new TreeItem<>(new NewGroupNodeViewModel("Ethan", false, 100)),
                new TreeItem<>(new NewGroupNodeViewModel("Isabella", false, 40)),
                new TreeItem<>(new NewGroupNodeViewModel("Emma", false, 50, IconTheme.JabRefIcon.HELP)),
                new TreeItem<>(new NewGroupNodeViewModel("Michael", false, 30)));

        TreeItem<NewGroupNodeViewModel> keywords = new TreeItem<>(new NewGroupNodeViewModel("keywords", false, 300,
                IconTheme.JabRefIcon.MAKE_KEY));
        keywords.setExpanded(true);
        keywords.getChildren().addAll(
                new TreeItem<>(new NewGroupNodeViewModel("JabRef", false, 295)),
                new TreeItem<>(new NewGroupNodeViewModel("Java", false, 1, IconTheme.JabRefIcon.PREFERENCES)),
                new TreeItem<>(new NewGroupNodeViewModel("JavaFX", false, 1)),
                new TreeItem<>(new NewGroupNodeViewModel("FXML", false, 1)));

        root.getChildren().addAll(authors, keywords);

        PseudoClass rootPseudoClass = PseudoClass.getPseudoClass("root");
        PseudoClass subElementPseudoClass = PseudoClass.getPseudoClass("sub");
        mainColumn.setPrefWidth(150);
        mainColumn.setCellValueFactory(cellData -> cellData.getValue().valueProperty());
        mainColumn.setCellFactory(column -> {
            TreeTableCell<NewGroupNodeViewModel, NewGroupNodeViewModel> cell = new TreeTableCell<NewGroupNodeViewModel, NewGroupNodeViewModel>() {

                @Override
                protected void updateItem(NewGroupNodeViewModel item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        setText(item.getName());
                        if (item.isRoot()) {
                            this.pseudoClassStateChanged(rootPseudoClass, true);
                        }
                        setTooltip(new Tooltip(item.getDescription()));
                        Text graphic = new Text(item.getIconCode());
                        graphic.getStyleClass().add("icon");
                        setGraphic(graphic);
                    }
                }
            };

            /*
            cell.tableRowProperty().get().treeItemProperty().addListener((obs, oldTreeItem, newTreeItem) -> {
                cell.pseudoClassStateChanged(subElementPseudoClass,
                        newTreeItem != null && newTreeItem.getParent() != cell.getTreeTableView().getRoot());
            });
            */

            return cell;
        });

        numberColumn.setCellValueFactory(cellData -> cellData.getValue().getValue().getHits());

        //groupTree.setTreeColumn(disclosureNodeColumn);

        groupTree.setRowFactory(treeTable -> {
            TreeTableRow row = new TreeTableRow();
            /*
            TreeTableRow row = new TreeTableRow<NewGroupNodeViewModel>() {
                @Override
                protected Skin<?> createDefaultSkin() {
                    return new CheckBoxTreeTableRowSkin<>(this);
                }
            };
            */
            row.treeItemProperty().addListener((ov, oldTreeItem, newTreeItem) -> {
                boolean active = newTreeItem == treeTable.getRoot();
                row.pseudoClassStateChanged(rootPseudoClass, active);
            });
            return row;
        });

        groupTree.setRoot(root);
    }

    public static class CheckBoxTreeTableRowSkin<S> extends TreeTableRowSkin<S> {

        public CheckBoxTreeTableRowSkin(TreeTableRow<S> control) {
            super(control);
        }

        @Override
        protected void layoutChildren(double x, double y, double w, double h) {
            super.layoutChildren(x, y, w, h);

            TreeTableRow<S> control = getSkinnable();

            // Determine indention
            int indentationLevel = getIndentationLevel(control);
            if (! isShowRoot()) indentationLevel--;
            final double indentationPerLevel = getIndentationPerLevel();
            double leftMargin = indentationLevel * indentationPerLevel;

            for (int column = 0, max = cells.size(); column < max; column++) {
                TreeTableCell<S, ?> tableCell = cells.get(column);

                TableColumnBase<?,?> treeColumn = getTreeColumn();
                int indentationColumnIndex = treeColumn == null ? 0 : getVisibleLeafColumns().indexOf(treeColumn);
                indentationColumnIndex = indentationColumnIndex < 0 ? 0 : indentationColumnIndex;

                // Add left margin to first column
                if(column == 0) {

                }

                // Remove left margin from disclosure node
                if(column == indentationColumnIndex) {
                    Node disclosureNode = getSkinnable().getDisclosureNode();

                    // We need to fade in since the super class may faded the node out if there was not enough space
                    // to display it together with the left margin.
                    disclosureNode.setVisible(true);
                    final FadeTransition fader = new FadeTransition(Duration.millis(200), disclosureNode);
                    fader.setToValue(1.0);
                    fader.play();
                    final double defaultDisclosureWidth = 10;
                    disclosureNode.resize(defaultDisclosureWidth, disclosureNode.prefHeight(defaultDisclosureWidth));

                    // Relocate
                    disclosureNode.relocate(x, y + tableCell.getPadding().getTop());
                    disclosureNode.toFront();
                }

                x += tableCell.getWidth();
            }
        }

    }

    public static class NewGroupNodeViewModel {

        private final String name;
        private final boolean isRoot;
        private ObservableValue<Integer> hits;
        private String iconCode;

        private NewGroupNodeViewModel(String name, boolean isRoot, int hits) {
            this(name, isRoot, hits, IconTheme.JabRefIcon.QUALITY);
        }
        private NewGroupNodeViewModel(String name, boolean isRoot, int hits, IconTheme.JabRefIcon icon) {
            this(name, isRoot, hits, icon.getCode());
        }
        private NewGroupNodeViewModel(String name, boolean isRoot, int hits, String iconCode) {
            this.name = name;
            this.isRoot = isRoot;
            this.iconCode = iconCode;
            this.hits = new SimpleObjectProperty<>(hits);
        }

        public String getName() {
            return name;
        }

        public boolean isRoot() {
            return isRoot;
        }

        public String getDescription() {
            return "Some group named " + getName();
        }

        public ObservableValue<Integer> getHits() {
            return hits;
        }

        public String getIconCode() {
            return iconCode;
        }
    }
}
