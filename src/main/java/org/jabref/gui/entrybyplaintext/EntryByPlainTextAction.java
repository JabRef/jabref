package org.jabref.gui.entrybyplaintext;

import static org.jabref.gui.actions.ActionHelper.needsDatabase;

import org.jabref.gui.StateManager;
import org.jabref.gui.actions.SimpleCommand;

public class EntryByPlainTextAction extends SimpleCommand {

  public EntryByPlainTextAction(StateManager stateManager){
    this.executable.bind(needsDatabase(stateManager));

  }

  @Override
  public void execute() {
    EntryValidateDialog dialog = new EntryValidateDialog();
    dialog.showAndWait();
  }
}
