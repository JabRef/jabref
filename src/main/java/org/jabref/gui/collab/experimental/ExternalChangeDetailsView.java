package org.jabref.gui.collab.experimental;

import javafx.scene.layout.AnchorPane;

import org.jabref.gui.collab.experimental.entryadd.EntryAddDetailsView;
import org.jabref.gui.collab.experimental.entrychange.EntryChangeDetailsView;
import org.jabref.gui.collab.experimental.entrydelete.EntryDeleteDetailsView;
import org.jabref.gui.collab.experimental.stringadd.StringAddDetailsView;

public sealed abstract class ExternalChangeDetailsView extends AnchorPane permits EntryAddDetailsView, EntryChangeDetailsView, EntryDeleteDetailsView, StringAddDetailsView {
}
