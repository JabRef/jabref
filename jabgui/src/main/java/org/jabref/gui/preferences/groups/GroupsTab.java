package org.jabref.gui.preferences.groups;

import org.jabref.gui.preferences.forms.AbstractFormTabView;
import org.jabref.logic.l10n.Localization;

public class GroupsTab extends AbstractFormTabView<GroupsTabViewModel> {

    public GroupsTab() {
        this.viewModel = new GroupsTabViewModel(preferences.getGroupsPreferences());
        buildView();
    }

    @Override
    public String getTabName() {
        return Localization.lang("Groups");
    }

    private void buildView() {
        getChildren().add(form()
                .title(Localization.lang("Groups"))

                .section(Localization.lang("View"))
                .radioGroup(viewMode -> viewMode
                        .radio(Localization.lang("Display only entries belonging to all selected groups"), viewModel.groupViewModeIntersectionProperty())
                        .radio(Localization.lang("Display all entries belonging to one or more of the selected groups"), viewModel.groupViewModeUnionProperty()))

                .checkbox(Localization.lang("Automatically assign new entry to selected groups"), viewModel.autoAssignGroupProperty())
                .checkbox(Localization.lang("Display count of items in group"), viewModel.displayGroupCount())
                .checkbox(Localization.lang("Show 'AI chat' in the context menu"), viewModel.showAiChatButtonProperty())

                .build());
    }
}
