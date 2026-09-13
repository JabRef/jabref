package org.jabref.gui.importer.actions;

import java.nio.file.Path;

import org.jabref.gui.DialogService;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.model.groups.ExplicitGroup;

/// Warns about static groups that still list their entries inside the group (format of old JabRef versions).
/// Library migrations are not run anymore, so these memberships are not shown and are lost on save.
public class LegacyGroupMembershipWarningAction implements GUIPostOpenAction {

    @Override
    public boolean isActionNecessary(ParserResult parserResult, DialogService dialogService, CliPreferences preferences) {
        return parserResult.getMetaData().getGroups().stream()
                           .flatMap(root -> root.iterateOverTree())
                           .anyMatch(node -> node.getGroup() instanceof ExplicitGroup group && !group.getLegacyEntryKeys().isEmpty());
    }

    @Override
    public void performAction(ParserResult parserResult, DialogService dialogService, CliPreferences preferences) {
        dialogService.showWarningDialogAndWait(
                Localization.lang("Group memberships of %0 cannot be read", parserResult.getPath().map(Path::toString).orElse("")),
                Localization.lang("This library was written by an old JabRef version. Its static groups appear empty, and saving the library removes their entries permanently.") + "\n\n" +
                        Localization.lang("Create a backup copy of the library. To keep the group memberships, open and save the library once with JabRef 5.15 before using it with this version."));
    }
}
