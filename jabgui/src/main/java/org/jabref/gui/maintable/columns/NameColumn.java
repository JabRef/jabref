package org.jabref.gui.maintable.columns;

import org.jabref.gui.maintable.BibEntryTableViewModel;
import org.jabref.gui.maintable.BibEntryTableViewModel.NameValue;
import org.jabref.gui.maintable.MainTableColumnModel;
import org.jabref.gui.maintable.MainTableTooltip;
import org.jabref.gui.util.ValueTableCellFactory;
import org.jabref.logic.bibtex.comparator.NameComparator;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.field.FieldTextMapper;
import org.jabref.model.entry.field.OrFields;

import org.jspecify.annotations.NullMarked;

/// Displays formatted names but sorts their resolved source values with the shared name comparator.
@NullMarked
public class NameColumn extends MainTableColumn<NameValue> {
    private final OrFields fields;

    public NameColumn(MainTableColumnModel model, MainTableTooltip tooltip) {
        super(model);
        fields = FieldFactory.parseOrFields(model.getQualifier());
        setText(getDisplayName());
        setCellValueFactory(param -> param.getValue().getNameFields(fields));
        NameComparator comparator = new NameComparator();
        setComparator((left, right) -> comparator.compare(left.source(), right.source()));
        new ValueTableCellFactory<BibEntryTableViewModel, NameValue>()
                .withText(NameValue::display)
                .graphicTooltip((entry, value) -> tooltip.createTooltip(
                        entry.getBibDatabaseContext(), entry.getEntry(), value.display()))
                .showTooltipRegardlessOfTruncation(tooltip::shouldAlwaysShowTooltip)
                .install(this);
        setSortable(true);
    }

    @Override
    public String getDisplayName() {
        return FieldTextMapper.getDisplayName(fields);
    }
}
