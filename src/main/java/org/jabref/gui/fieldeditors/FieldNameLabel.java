package org.jabref.gui.fieldeditors;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;

import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.InternalField;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.strings.StringUtil;

public class FieldNameLabel extends Label {

    public FieldNameLabel(Field field) {
        super(field.getDisplayName());

        setPadding(new Insets(4, 0, 0, 0));
        setAlignment(Pos.CENTER);
        setPrefHeight(Double.POSITIVE_INFINITY);

        String description = getDescription(field);
        if (StringUtil.isNotBlank(description)) {
            Tooltip tooltip = new Tooltip();
            tooltip.setText(description);
            this.setTooltip(tooltip);
        }
    }

    public String getDescription(Field field) {
        if (field.isStandardField()) {
            StandardField standardField = (StandardField) field;
            switch (standardField) {
                case ADDENDUM:
                    return Localization.lang("Miscellaneous bibliographic data usually printed at the end of the entry.");
                case AFTERWORD:
                    return Localization.lang("Author(s) of an afterword to the work.");
                case ANNOTATOR:
                    return Localization.lang("Author(s) of annotations to the work.");
                case AUTHOR:
                    return Localization.lang("Author(s) of the work.");
                case BOOKTITLE:
                    return Localization.lang("Title of the main publication this work is part of.");
                case CHAPTER:
                    return Localization.lang("Chapter or section or any other unit of a work.");
                case COMMENTATOR:
                    return Localization.lang("Author(s) of a commentary to the work.") + "\n" +
                            Localization.lang("Note that this field is intended for commented editions which have a commentator in addition to the author. If the work is a stand-alone commentary, the commentator should be given in the author field.");
                case DATE:
                    return Localization.lang("Publication date of the work.");
                case DOI:
                    return Localization.lang("Digital Object Identifier of the work.");
                case EDITION:
                    return Localization.lang("Edition of a printed publication.");
                case EDITOR:
                    return Localization.lang("Editor(s) of the work or the main publication, depending on the type of the entry.");
                case EID:
                    return Localization.lang("Electronic identifier of a work.") + "\n" +
                            Localization.lang("This field may replace the pages field for journals deviating from the classic pagination scheme of printed journals by only enumerating articles or papers and not pages.");
                case EPRINT:
                    return Localization.lang("Electronic identifier of an online publication.") + "\n" +
                            Localization.lang("This is roughly comparable to a DOI but specific to a certain archive, repository, service, or system.");
                case EPRINTCLASS:
                case PRIMARYCLASS:
                    return Localization.lang("Additional information related to the resource indicated by the eprint field.") + "\n" +
                            Localization.lang("This could be a section of an archive, a path indicating a service, a classification of some sort.");
                case EPRINTTYPE:
                case ARCHIVEPREFIX:
                    return Localization.lang("Type of the eprint identifier, e. g., the name of the archive, repository, service, or system the eprint field refers to.");
                case EVENTDATE:
                    return Localization.lang("Date of a conference, a symposium, or some other event.");
                case EVENTTITLE:
                    return Localization.lang("Title of a conference, a symposium, or some other event.") + "\n"
                            + Localization.lang("Note that this field holds the plain title of the event. Things like \"Proceedings of the Fifth XYZ Conference\" go into the titleaddon or booktitleaddon field.");
                case EVENTTITLEADDON:
                    return Localization.lang("Annex to the eventtitle field.") + "\n" +
                            Localization.lang("Can be used for known event acronyms.");
                case FILE:
                case PDF:
                    return Localization.lang("Link(s) to a local PDF or other document of the work.");
                case HOWPUBLISHED:
                    return Localization.lang("Publication notice for unusual publications which do not fit into any of the common categories.");
                case INSTITUTION:
                case SCHOOL:
                    return Localization.lang("Name of a university or some other institution.");
                case INTRODUCTION:
                    return Localization.lang("Author(s) of an introduction to the work.");
                case ISBN:
                    return Localization.lang("International Standard Book Number of a book.");
                case ISSUE:
                    return Localization.lang("Issue of a journal.") + "\n" +
                            Localization.lang("This field is intended for journals whose individual issues are identified by a designation such as \"Spring\" or \"Summer\" rather than the month or a number. Integer ranges and short designators are better written to the number field.");
                case ISSUESUBTITLE:
                    return Localization.lang("Subtitle of a specific issue of a journal or other periodical.");
                case ISSUETITLE:
                    return Localization.lang("Title of a specific issue of a journal or other periodical.");
                case JOURNALTITLE:
                case JOURNAL:
                    return Localization.lang("Name of a journal, a newspaper, or some other periodical.");
                case LABEL:
                    return Localization.lang("Designation to be used by the citation style as a substitute for the regular label if any data required to generate the regular label is missing.");
                case LIBRARY:
                    return Localization.lang("Information such as a library name and a call number.");
                case LOCATION:
                case ADDRESS:
                    return Localization.lang("The place(s) of publication, i. e., the location of the publisher or institution, depending on the entry type.");
                case MAINTITLE:
                    return Localization.lang("Main title of a multi-volume book, such as \"Collected Works\".");
                case MONTH:
                    return Localization.lang("Publication month.");
                case NOTE:
                    return Localization.lang("Miscellaneous bibliographic data which does not fit into any other field.");
                case NUMBER:
                    return Localization.lang("Number of a journal or the volume/number of a book in a series.");
                case ORGANIZATION:
                    return Localization.lang("Organization(s) that published a manual or an online resource, or sponsored a conference.");
                case PAGES:
                    return Localization.lang("One or more page numbers or page ranges.") + "\n" +
                            Localization.lang("If the work is published as part of another one, such as an article in a journal or a collection, this field holds the relevant page range in that other work. It may also be used to limit the reference to a specific part of a work (a chapter in a book, for example). For papers in electronic journals with anon-classical pagination setup the eid field may be more suitable.");
                case PAGETOTAL:
                    return Localization.lang("Total number of pages of the work.");
                case PUBLISHER:
                    return Localization.lang("Name(s) of the publisher(s).");
                case PUBSTATE:
                    return Localization.lang("Publication state of the work, e. g., \"in press\".");
                case SERIES:
                    return Localization.lang("Name of a publication series, such as \"Studies in...\", or the number of a journal series.");
                case SUBTITLE:
                    return Localization.lang("Subtitle of the work.");
                case TITLE:
                    return Localization.lang("Title of the work.");
                case URL:
                    return Localization.lang("URL of an online publication.");
                case URLDATE:
                    return Localization.lang("Access date of the address specified in the url field.");
                case VENUE:
                    return Localization.lang("Location of a conference, a symposium, or some other event.");
                case VERSION:
                    return Localization.lang("Revision number of a piece of software, a manual, etc.");
                case VOLUME:
                    return Localization.lang("Volume of a multi-volume book or a periodical.");
                case VOLUMES:
                    return Localization.lang("Total number of volumes of a multi-volume work.");
                case YEAR:
                    return Localization.lang("Year of publication.");
                case KEYWORDS:
                    return Localization.lang("Separated list of keywords.");
            }
        } else if (field instanceof InternalField) {
            InternalField internalField = (InternalField) field;
            switch (internalField) {
                case KEY_FIELD:
                    return Localization.lang("Key by which the work may be cited.");
                case GROUPS:
                    return Localization.lang("Name(s) of the (manual) groups the entry belongs to.");
            }
        }
        return "";
    }
}
