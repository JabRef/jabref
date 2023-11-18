package org.jabref.gui.git;

 import java.util.Optional;

 import javafx.beans.property.BooleanProperty;
 import javafx.beans.property.SimpleBooleanProperty;
 import javafx.beans.property.SimpleStringProperty;
 import javafx.beans.property.StringProperty;

 import org.jabref.gui.git.entryadd.EntryAdd;
 import org.jabref.gui.git.entrychange.EntryChange;
 import org.jabref.gui.git.entrydelete.EntryDelete;
 import org.jabref.gui.git.groupchange.GroupChange;
 import org.jabref.gui.git.metedatachange.MetadataChange;
 import org.jabref.gui.git.preamblechange.PreambleChange;
 import org.jabref.gui.git.stringadd.BibTexStringAdd;
 import org.jabref.gui.git.stringchange.BibTexStringChange;
 import org.jabref.gui.git.stringdelete.BibTexStringDelete;
 import org.jabref.gui.git.stringrename.BibTexStringRename;
 import org.jabref.gui.undo.NamedCompound;
 import org.jabref.gui.util.OptionalObjectProperty;
 import org.jabref.model.git.GitContext;

 public sealed abstract class GitChange permits EntryAdd, EntryChange, EntryDelete, GroupChange, MetadataChange, PreambleChange, BibTexStringAdd, BibTexStringChange, BibTexStringDelete, BibTexStringRename {
     protected final GitContext gitContext;
     protected final OptionalObjectProperty<GitChangesResolver> externalChangeResolver = OptionalObjectProperty.empty();
     private final BooleanProperty accepted = new SimpleBooleanProperty();
     private final StringProperty name = new SimpleStringProperty();

     protected GitChange(GitContext gitContext, GitChangeResolverFactory gitChangeResolverFactory) {
         this.gitContext = gitContext;
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

     public Optional<DatabaseChangeResolver> getExternalChangeResolver() {
         return externalChangeResolver.get();
     }

     public abstract void applyChange(NamedCompound undoEdit);
 }