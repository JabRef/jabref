package org.jabref.gui.maintable.columns;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableValue;

import org.jabref.gui.maintable.BibEntryTableViewModel;
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
public class NameColumn extends MainTableColumn<NameColumn.Value> {
    public record Value(String display, String source) {
    }

    private final OrFields fields;

    public NameColumn(MainTableColumnModel model, MainTableTooltip tooltip) {
        super(model);
        fields = FieldFactory.parseOrFields(model.getQualifier());
        setText(getDisplayName());
        setCellValueFactory(param -> getValue(param.getValue()));
        NameComparator comparator = new NameComparator();
        setComparator((left, right) -> comparator.compare(left.source(), right.source()));
        new ValueTableCellFactory<BibEntryTableViewModel, Value>()
                .withText(Value::display)
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

    private ObservableValue<Value> getValue(BibEntryTableViewModel row) {
        ObservableValue<String> display = row.getFields(fields);
        // Observe the entry directly: toggling useprefix may leave the displayed text unchanged.
        Observable[] dependencies = Stream.concat(Arrays.stream(row.getEntry().getObservables()), Stream.of(display))
                                          .toArray(Observable[]::new);
        return Bindings.createObjectBinding(() -> {
            String source = fields.getFields().stream()
                                  .map(field -> row.getEntry().getResolvedFieldOrAlias(field, row.getBibDatabaseContext().getDatabase()))
                                  .flatMap(Optional::stream)
                                  .findFirst().orElse("");
            return new Value(Optional.ofNullable(display.getValue()).orElse(""), source);
        }, dependencies);
    }
}
