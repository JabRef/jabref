package org.jabref.gui.collab;

import javafx.scene.layout.AnchorPane;

import org.jabref.gui.collab.entrychange.EntryChangeDetailsView;
import org.jabref.gui.collab.entrychange.EntryWithPreviewAndSourceDetailsView;
import org.jabref.gui.collab.groupchange.GroupChangeDetailsView;
import org.jabref.gui.collab.metedatachange.MetadataChangeDetailsView;
import org.jabref.gui.collab.preamblechange.PreambleChangeDetailsView;
import org.jabref.gui.collab.stringadd.BibTexStringAddDetailsView;
import org.jabref.gui.collab.stringchange.BibTexStringChangeDetailsView;
import org.jabref.gui.collab.stringdelete.BibTexStringDeleteDetailsView;
import org.jabref.gui.collab.stringrename.BibTexStringRenameDetailsView;

public sealed abstract class DatabaseChangeDetailsView extends AnchorPane permits EntryWithPreviewAndSourceDetailsView, GroupChangeDetailsView, MetadataChangeDetailsView, PreambleChangeDetailsView, BibTexStringAddDetailsView, BibTexStringChangeDetailsView, BibTexStringDeleteDetailsView, BibTexStringRenameDetailsView, EntryChangeDetailsView {
}
