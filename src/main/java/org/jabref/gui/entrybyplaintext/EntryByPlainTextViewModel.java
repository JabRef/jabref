package org.jabref.gui.entrybyplaintext;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.gui.StateManager;
import org.jabref.model.database.BibDatabaseContext;

public class EntryByPlainTextViewModel {

  private final StringProperty inputText = new SimpleStringProperty("");
  private final BibDatabaseContext bibDatabaseContext;



  public EntryByPlainTextViewModel(BibDatabaseContext bibDatabaseContext){

    this.bibDatabaseContext = bibDatabaseContext;

  }

  public StringProperty inputTextProperty(){

    return this.inputText;

  }

  public void startParsing(){
    //TODO: Add method to start the parsing of the text
      EntryValidateDialog dlg = new EntryValidateDialog();
      dlg.showAndWait();
  }







}
