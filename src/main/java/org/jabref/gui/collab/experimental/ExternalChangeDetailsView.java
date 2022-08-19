package org.jabref.gui.collab.experimental;

import javafx.scene.layout.AnchorPane;

import org.jabref.gui.collab.experimental.entryadd.EntryAddDetailsView;
import org.jabref.gui.collab.experimental.entrychange.EntryChangeDetailsView;
import org.jabref.gui.collab.experimental.entrydelete.EntryDeleteDetailsView;

public sealed abstract class ExternalChangeDetailsView extends AnchorPane permits EntryAddDetailsView, EntryChangeDetailsView, EntryDeleteDetailsView {
}
