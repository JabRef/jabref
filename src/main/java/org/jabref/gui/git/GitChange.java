package org.jabref.gui.git;

 import java.util.Optional;

 import javafx.beans.property.BooleanProperty;
 import javafx.beans.property.SimpleBooleanProperty;
 import javafx.beans.property.SimpleStringProperty;
 import javafx.beans.property.StringProperty;

 import org.jabref.gui.git.entryadd.EntryAdd;
 import org.jabref.gui.git.entrychange.EntryChange;
 import org.jabref.gui.git.entrydelete.EntryDelete;

 import org.jabref.gui.undo.NamedCompound;
 import org.jabref.gui.util.OptionalObjectProperty;
 import org.jabref.model.git.BibGitContext;

 public sealed abstract class GitChange permits EntryAdd, EntryChange, EntryDelete {
     protected final BibGitContext BibGitContext;
     protected final OptionalObjectProperty<GitChangeResolver> externalChangeResolver = OptionalObjectProperty.empty();
     private final BooleanProperty accepted = new SimpleBooleanProperty();
     private final StringProperty name = new SimpleStringProperty();

     protected GitChange(BibGitContext BibGitContext, GitChangeResolverFactory gitChangeResolverFactory) {
         this.BibGitContext = BibGitContext;
         setChangeName("Unnamed Change!");

         if (gitChangeResolverFactory != null) {
             externalChangeResolver.set(gitChangeResolverFactory.create(this));
         }
     }

     public boolean isAccepted() {
         return accepted.get();
     }

     public BooleanProperty acceptedProperty() {
         return accepted;
     }

     public void setAccepted(boolean accepted) {
         this.accepted.set(accepted);
     }

     /**
      * Convenience method for accepting changes
      * */
     public void accept() {
         setAccepted(true);
     }

     public String getName() {
         return name.get();
     }

     protected void setChangeName(String changeName) {
         name.set(changeName);
     }

     public Optional<GitChangeResolver> getExternalChangeResolver() {
         return externalChangeResolver.get();
     }

     public abstract void applyChange(NamedCompound undoEdit);
 }