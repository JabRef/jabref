package org.jabref.logic.cleanup;

import java.util.ArrayList;
import java.util.List;

import javafx.collections.ObservableList;

import org.jabref.model.FieldChange;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.Keyword;

import com.dlsc.gemsfx.TagsField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConvertMSCCodesCleanup implements CleanupJob {
    /*
     * Converts MSC codes found in keywords editor to their descriptions
     */
    // TODO: Retrieve Keywords, Load Hashmap, Compare, Set Keywords to Description
    private static final Logger logger = LoggerFactory.getLogger(ConvertMSCCodesCleanup.class);
    private TagsField<Keyword> keywordTagsField;

    @Override
    public List<FieldChange> cleanup(BibEntry entry) {
        List<FieldChange> changes = new ArrayList<>();
        ObservableList<Keyword> tags = keywordTagsField.getTags();
        for (Keyword tag : tags) {
            logger.info("{} \n", tag.toString());
        }
        logger.info("Here are the keywords: {}", tags.toString());
        return changes;
    }
}
