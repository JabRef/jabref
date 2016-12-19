package net.sf.jabref.gui.groups;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import net.sf.jabref.gui.AbstractViewModel;
import net.sf.jabref.gui.IconTheme;

public class GroupTreeViewModel extends AbstractViewModel {

    private final ObjectProperty<GroupNodeViewModel> rootGroup = new SimpleObjectProperty<>();

    public ObjectProperty<GroupNodeViewModel> rootGroupProperty() {
        return rootGroup;
    }

    public GroupTreeViewModel() {
        GroupNodeViewModel root = new GroupNodeViewModel("All Entries", true, 30000,
                IconTheme.JabRefIcon.CLOSE, false);
        //root.setExpanded(true);

        GroupNodeViewModel authors = new GroupNodeViewModel("Authors", false, 300,
                IconTheme.JabRefIcon.PRIORITY, false);
        //authors.setExpanded(true);
        authors.getChildren().addAll(
                new GroupNodeViewModel("Ethan", false, 100, true),
                new GroupNodeViewModel("Isabella", false, 40, true),
                new GroupNodeViewModel("Emma", false, 50, IconTheme.JabRefIcon.HELP, true),
                new GroupNodeViewModel("Michael", false, 30, true)
        );

        GroupNodeViewModel journals = new GroupNodeViewModel("Journals", false, 300,
                IconTheme.JabRefIcon.MAKE_KEY, false);
        //journals.setExpanded(true);
        journals.getChildren().addAll(
                new GroupNodeViewModel("JabRef", false, 295, true),
                new GroupNodeViewModel("Java", false, 1, IconTheme.JabRefIcon.PREFERENCES, true),
                new GroupNodeViewModel("JavaFX", false, 1, true),
                new GroupNodeViewModel("FXML", false, 1, true)
        );

        GroupNodeViewModel keywords = new GroupNodeViewModel("keywords", false, 300, IconTheme.JabRefIcon.MAKE_KEY, false);
        //keywords.setExpanded(true);
        GroupNodeViewModel keywordSub = new GroupNodeViewModel("deeper", false, 20, IconTheme.JabRefIcon.SOURCE, false);
        //keywordSub.setExpanded(true);
        keywordSub.getChildren().addAll(
                new GroupNodeViewModel("JabRef", false, 295, true),
                new GroupNodeViewModel("Java", false, 1, IconTheme.JabRefIcon.PREFERENCES, true)
        );
        keywords.getChildren().addAll(
                new GroupNodeViewModel("JabRef", false, 295, true),
                new GroupNodeViewModel("Java", false, 1, IconTheme.JabRefIcon.PREFERENCES, true),
                keywordSub,
                new GroupNodeViewModel("JavaFX", false, 1, true),
                new GroupNodeViewModel("FXML", false, 1, true)
        );

        root.getChildren().addAll(authors, journals, keywords);
        rootGroup.setValue(root);
    }
}
