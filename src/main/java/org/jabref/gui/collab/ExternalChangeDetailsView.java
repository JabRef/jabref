package org.jabref.gui.collab;

import javafx.scene.layout.AnchorPane;

import org.jabref.gui.collab.entryadd.EntryAddDetailsView;
import org.jabref.gui.collab.entrychange.EntryChangeDetailsView;
import org.jabref.gui.collab.entrydelete.EntryDeleteDetailsView;
import org.jabref.gui.collab.groupchange.GroupChangeDetailsView;
import org.jabref.gui.collab.metedatachange.MetadataChangeDetailsView;
import org.jabref.gui.collab.preamblechange.PreambleChangeDetailsView;
import org.jabref.gui.collab.stringadd.StringAddDetailsView;
import org.jabref.gui.collab.stringchange.StringChangeDetailsView;
import org.jabref.gui.collab.stringdelete.StringDeleteDetailsView;
import org.jabref.gui.collab.stringrename.StringRenameDetailsView;

public sealed abstract class ExternalChangeDetailsView extends AnchorPane permits EntryAddDetailsView, EntryChangeDetailsView, EntryDeleteDetailsView, GroupChangeDetailsView, MetadataChangeDetailsView, PreambleChangeDetailsView, StringAddDetailsView, StringChangeDetailsView, StringDeleteDetailsView, StringRenameDetailsView {
}
