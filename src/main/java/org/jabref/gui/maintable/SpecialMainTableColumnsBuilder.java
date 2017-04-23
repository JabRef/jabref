package org.jabref.gui.maintable;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.swing.JLabel;

import org.jabref.gui.GUIGlobals;
import org.jabref.gui.IconTheme;
import org.jabref.gui.externalfiletype.ExternalFileType;
import org.jabref.gui.filelist.FileListTableModel;
import org.jabref.gui.specialfields.SpecialFieldValueViewModel;
import org.jabref.gui.specialfields.SpecialFieldViewModel;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.FieldName;
import org.jabref.model.entry.specialfields.SpecialField;

class SpecialMainTableColumnsBuilder {

    MainTableColumn buildNumberColumn() {

        return new MainTableColumn(FieldName.NUMBER_COL) {

            @Override
            public Object getColumnValue(BibEntry entry) {
                return "#";
            }

            @Override
            public String getDisplayName() {
                return "#";
            }
        }

                ;
    }

    MainTableColumn buildRankingColumn() {

        return new MainTableColumn(SpecialField.RANKING.getFieldName(),
                Collections.singletonList(SpecialField.RANKING.getFieldName()),
                new JLabel(SpecialField.RANKING.getFieldName())) {

            @Override
            public Object getColumnValue(BibEntry entry) {

                return entry.getField(SpecialField.RANKING.getFieldName())
                        .flatMap(SpecialField.RANKING::parse).map(rank -> new SpecialFieldValueViewModel(rank).createSpecialFieldValueLabel()).orElse(null);
            }
        };
    }

    MainTableColumn buildPriorityColumn() {
        return new MainTableColumn(SpecialField.PRIORITY.getFieldName(),
                Collections.singletonList(SpecialField.PRIORITY.getFieldName()),
                new JLabel(new SpecialFieldViewModel(SpecialField.PRIORITY).getRepresentingIcon())) {

            @Override
            public Object getColumnValue(BibEntry entry) {

                return entry.getField(SpecialField.PRIORITY.getFieldName())
                        .flatMap(SpecialField.PRIORITY::parse).map(prio -> new SpecialFieldValueViewModel(prio).createSpecialFieldValueLabel()).orElse(null);
            }
        };
    }

    MainTableColumn buildReadStatusColumn() {
        return new MainTableColumn(SpecialField.READ_STATUS.getFieldName(),
                Collections.singletonList(SpecialField.READ_STATUS.getFieldName()),
                new JLabel(new SpecialFieldViewModel(SpecialField.READ_STATUS).getRepresentingIcon())) {

            @Override
            public Object getColumnValue(BibEntry entry) {

                return entry.getField(SpecialField.READ_STATUS.getFieldName())
                        .flatMap(SpecialField.READ_STATUS::parse).map(status -> new SpecialFieldValueViewModel(status).createSpecialFieldValueLabel()).orElse(null);
            }
        };
    }

    MainTableColumn buildRelevanceColumn() {
        return createIconColumn(SpecialField.RELEVANCE.getFieldName(),
                Collections.singletonList(SpecialField.RELEVANCE.getFieldName()),
                new JLabel(new SpecialFieldViewModel(SpecialField.RELEVANCE).getRepresentingIcon()));
    }

    MainTableColumn buildPrintedColumn() {
        return createIconColumn(SpecialField.PRINTED.getFieldName(),
                Collections.singletonList(SpecialField.PRINTED.getFieldName()),
                new JLabel(new SpecialFieldViewModel(SpecialField.PRINTED).getRepresentingIcon()));
    }

    MainTableColumn buildQualityColumn() {
        return createIconColumn(SpecialField.QUALITY.getFieldName(),
                Collections.singletonList(SpecialField.QUALITY.getFieldName()),
                new JLabel(new SpecialFieldViewModel(SpecialField.QUALITY).getRepresentingIcon()));
    }

    MainTableColumn buildFileColumn() {

        return new MainTableColumn(FieldName.FILE,
                Collections.singletonList(FieldName.FILE), new JLabel(IconTheme.JabRefIcon.FILE.getSmallIcon())) {

            @Override
            public Object getColumnValue(BibEntry entry) {
                // We use a FileListTableModel to parse the field content:
                FileListTableModel fileList = new FileListTableModel();
                entry.getField(FieldName.FILE).ifPresent(fileList::setContent);
                if (fileList.getRowCount() > 1) {
                    return new JLabel(IconTheme.JabRefIcon.FILE_MULTIPLE.getSmallIcon());
                } else if (fileList.getRowCount() == 1) {
                    Optional<ExternalFileType> type = fileList.getEntry(0).getType();
                    if (type.isPresent()) {
                        return type.get().getIconLabel();
                    } else {
                        return new JLabel(IconTheme.JabRefIcon.FILE.getSmallIcon());
                    }
                }

                return null;
            }
        };
    }

    /**
     * Creates a MainTableColumn which shows an icon instead textual content
     *
     * @param columnName the name of the column
     * @param fields     the entry fields which should be shown
     * @return the crated MainTableColumn
     */
    MainTableColumn createIconColumn(String columnName, List<String> fields, JLabel iconLabel) {
        return new MainTableColumn(columnName, fields, iconLabel) {

            @Override
            public Object getColumnValue(BibEntry entry) {
                JLabel iconLabel = null;
                boolean iconFound = false;

                // check for each field whether content is available
                for (String field : fields) {
                    if (entry.hasField(field)) {
                        if (iconFound) {
                            return new JLabel(IconTheme.JabRefIcon.FILE_MULTIPLE.getSmallIcon());
                        } else {
                            iconLabel = GUIGlobals.getTableIcon(field);
                            iconFound = true;
                        }

                    }
                }
                return iconLabel;
            }
        };
    }

    /**
     * create a MainTableColumn for specific file types.
     * <p>
     * Shows the icon for the given type (or the FILE_MULTIPLE icon)
     *
     * @param externalFileTypeName the name of the externalFileType
     * @return the created MainTableColumn
     */
    MainTableColumn createFileIconColumn(String externalFileTypeName) {

        return new MainTableColumn(externalFileTypeName, Collections.singletonList(FieldName.FILE), new JLabel()) {

            @Override
            public boolean isFileFilter() {
                return true;
            }

            @Override
            public String getDisplayName() {
                return externalFileTypeName;
            }

            @Override
            public Object getColumnValue(BibEntry entry) {

                boolean iconFound = false;
                JLabel iconLabel = null;
                FileListTableModel fileList = new FileListTableModel();
                entry.getField(FieldName.FILE).ifPresent(fileList::setContent);
                for (int i = 0; i < fileList.getRowCount(); i++) {
                    if ((fileList.getEntry(i).getType().isPresent())
                            && externalFileTypeName.equalsIgnoreCase(fileList.getEntry(i).getType().get().getName())) {
                        if (iconFound) {
                            // already found another file of the desired type - show FILE_MULTIPLE Icon
                            return new JLabel(IconTheme.JabRefIcon.FILE_MULTIPLE.getSmallIcon());
                        } else {
                            iconLabel = fileList.getEntry(i).getType().get().getIconLabel();
                            iconFound = true;
                        }
                    }
                }
                return iconLabel;
            }
        };
    }
}
