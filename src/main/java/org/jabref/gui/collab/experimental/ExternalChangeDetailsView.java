package org.jabref.gui.collab.experimental;

import javafx.scene.layout.AnchorPane;

import org.jabref.gui.collab.experimental.entryadd.EntryAddDetailsView;
import org.jabref.gui.collab.experimental.entrychange.EntryChangeDetailsView;
import org.jabref.gui.collab.experimental.entrydelete.EntryDeleteDetailsView;
import org.jabref.gui.collab.experimental.metedatachange.MetadataChangeDetailsView;
import org.jabref.gui.collab.experimental.stringadd.StringAddDetailsView;
import org.jabref.gui.collab.experimental.stringchange.StringChangeDetailsView;
import org.jabref.gui.collab.experimental.stringdelete.StringDeleteDetailsView;
import org.jabref.gui.collab.experimental.stringrename.StringRenameDetailsView;

public sealed abstract class ExternalChangeDetailsView extends AnchorPane permits EntryAddDetailsView, EntryChangeDetailsView, EntryDeleteDetailsView, MetadataChangeDetailsView, StringAddDetailsView, StringChangeDetailsView, StringDeleteDetailsView, StringRenameDetailsView {
}
