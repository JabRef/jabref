package org.jabref.gui.documentviewer;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Optional;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// [impl->feat~ai.chat.jump-to-entry-pdf~1]
@NullMarked
public class JumpToEntryPdfAction extends SimpleCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(JumpToEntryPdfAction.class);

    private final String url;
    private final StateManager stateManager;
    private final DialogService dialogService;

    public JumpToEntryPdfAction(String url, StateManager stateManager, DialogService dialogService) {
        this.url = url;
        this.stateManager = stateManager;
        this.dialogService = dialogService;
    }

    @Override
    public void execute() {
        Optional<EntryCitationUrl> parsedOpt = parseUrl(url);
        if (parsedOpt.isEmpty()) {
            LOGGER.warn("Could not parse citation entry URL: {}", url);
            dialogService.notify(Localization.lang("Invalid URL"));
            return;
        }

        EntryCitationUrl parsed = parsedOpt.get();
        Optional<BibDatabaseContext> activeDatabaseOpt = stateManager.getActiveDatabase();
        if (activeDatabaseOpt.isEmpty()) {
            dialogService.notify(Localization.lang("No library open"));
            return;
        }

        BibDatabaseContext databaseContext = activeDatabaseOpt.get();
        Optional<BibEntry> entryOpt = databaseContext.getDatabase().getEntryByCitationKey(parsed.citationKey());
        if (entryOpt.isEmpty()) {
            dialogService.notify(Localization.lang("Citation key '%0' to select not found in open libraries.", parsed.citationKey()));
            return;
        }

        BibEntry entry = entryOpt.get();
        Optional<LinkedFile> pdfFileOpt = entry.getFiles().stream()
                .filter(file -> {
                    try {
                        return FileUtil.isPDFFile(Path.of(file.getLink()));
                    } catch (InvalidPathException e) {
                        return false;
                    }
                })
                .findFirst();

        if (pdfFileOpt.isEmpty()) {
            dialogService.notify(Localization.lang("No PDF files available"));
            return;
        }

        openInDocumentViewer(pdfFileOpt.get(), parsed.pageNumber());
    }

    private void openInDocumentViewer(LinkedFile pdfFile, Optional<Integer> pageNumber) {
        int targetPage = pageNumber.filter(p -> p > 0).orElse(1);
        DocumentViewerView viewerView = new DocumentViewerView();
        viewerView.switchToFile(pdfFile);
        viewerView.gotoPage(targetPage);
        dialogService.showCustomDialog(viewerView);
    }

    public static Optional<EntryCitationUrl> parseUrl(@Nullable String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            return Optional.empty();
        }

        URI uri;
        try {
            uri = URI.create(rawUrl.trim());
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }

        if (!"entry".equalsIgnoreCase(uri.getScheme())) {
            return Optional.empty();
        }

        String citationKey = uri.getHost() != null ? uri.getHost() : uri.getAuthority();
        if (citationKey == null || citationKey.isBlank()) {
            return Optional.empty();
        }

        try {
            citationKey = URLDecoder.decode(citationKey, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            LOGGER.debug("Could not URL-decode citation key '{}'", citationKey, e);
        }

        Optional<Integer> pageNumber = Optional.empty();
        String path = uri.getPath();
        if (path != null && path.startsWith("/")) {
            String pageStr = path.substring(1).trim();
            if (!pageStr.isEmpty()) {
                try {
                    int parsedPage = Integer.parseInt(pageStr);
                    if (parsedPage > 0) {
                        pageNumber = Optional.of(parsedPage);
                    }
                } catch (NumberFormatException e) {
                    LOGGER.debug("Could not parse page number '{}'", pageStr, e);
                }
            }
        }

        return Optional.of(new EntryCitationUrl(citationKey, pageNumber));
    }

    public record EntryCitationUrl(String citationKey, Optional<Integer> pageNumber) {
    }
}
