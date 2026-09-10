package org.jabref.gui.documentviewer;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

/// Opens the PDF of an entry addressed by an in-app link.
///
/// The link mirrors the REST API path of the entry (`/libraries/{id}/entries/{key}`) so that the same address
/// works in jabsrv, cite-as-you-write and JabMap:
///
/// - `jabref://libraries/{id}/entries/{key}` (absolute, `{id}` as in [BibDatabaseContext#getLibraryId])
/// - `entries/{key}` (relative to the active library, what the AI chat emits)
///
/// Both forms accept an optional `/files/{n}` segment selecting the `{n}`-th linked file (1-based, default: first PDF)
/// and a `#page={page}` fragment as in the PDF Open Parameters understood by Acrobat.
// [impl->feat~ai.chat.jump-to-entry-pdf~1]
@NullMarked
public class JumpToEntryPdfAction extends SimpleCommand {
    public static final String SCHEME = "jabref";

    private static final Logger LOGGER = LoggerFactory.getLogger(JumpToEntryPdfAction.class);
    private static final Pattern PAGE_FRAGMENT = Pattern.compile("(?:^|&)(?:page=|p=)?(\\d+)(?:&|$)");

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
        Optional<EntryLink> linkOpt = parseUrl(url);
        if (linkOpt.isEmpty()) {
            LOGGER.warn("Could not parse entry link: {}", url);
            dialogService.notify(Localization.lang("Invalid URL"));
            return;
        }
        EntryLink link = linkOpt.get();

        Optional<BibDatabaseContext> databaseOpt = link.libraryId()
                                                       .map(id -> stateManager.getOpenDatabases().stream()
                                                                              .filter(context -> context.getLibraryId().filter(id::equals).isPresent())
                                                                              .findFirst())
                                                       .orElseGet(stateManager::getActiveDatabase);
        if (databaseOpt.isEmpty()) {
            dialogService.notify(Localization.lang("No library open"));
            return;
        }

        Optional<BibEntry> entryOpt = databaseOpt.get().getDatabase().getEntryByCitationKey(link.citationKey());
        if (entryOpt.isEmpty()) {
            dialogService.notify(Localization.lang("Citation key '%0' to select not found in open libraries.", link.citationKey()));
            return;
        }

        BibEntry entry = entryOpt.get();
        if (stateManager.activeTabProperty() != null && stateManager.activeTabProperty().get() != null) {
            stateManager.activeTabProperty().get().ifPresent(tab -> tab.clearAndSelect(entry));
        }
        stateManager.setSelectedEntries(List.of(entry));

        List<LinkedFile> files = entry.getFiles();
        Optional<LinkedFile> pdfFileOpt = link.fileIndex()
                                              .filter(index -> index <= files.size())
                                              .map(index -> files.get(index - 1))
                                              .or(() -> files.stream().filter(JumpToEntryPdfAction::isPdf).findFirst());
        if (pdfFileOpt.isEmpty()) {
            dialogService.notify(Localization.lang("No PDF files available"));
            return;
        }

        openInDocumentViewer(pdfFileOpt.get(), link.page().orElse(1));
    }

    private static boolean isPdf(LinkedFile file) {
        try {
            return FileUtil.isPDFFile(Path.of(file.getLink()));
        } catch (InvalidPathException e) {
            LOGGER.debug("Skipping file link that is not a path: {}", file.getLink(), e);
            return false;
        }
    }

    private void openInDocumentViewer(LinkedFile pdfFile, int page) {
        DocumentViewerView viewerView = new DocumentViewerView();
        viewerView.disableLiveMode();
        viewerView.gotoPage(page);
        viewerView.switchToFile(pdfFile);
        dialogService.showCustomDialog(viewerView);
    }

    /// @return empty if the url is not an entry link (callers then fall back to opening it in the browser)
    public static Optional<EntryLink> parseUrl(@Nullable String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            return Optional.empty();
        }
        URI uri;
        try {
            uri = new URI(rawUrl.trim());
        } catch (URISyntaxException e) {
            return Optional.empty();
        }

        List<String> segments = new ArrayList<>();
        boolean absolute = uri.getScheme() != null;
        if (absolute) {
            if (!SCHEME.equalsIgnoreCase(uri.getScheme())) {
                return Optional.empty();
            }
            // "jabref://libraries/..." parses "libraries" as authority
            segments.add(uri.getAuthority());
        }
        String path = uri.getPath();
        if (path != null) {
            segments.addAll(Arrays.stream(path.split("/")).filter(s -> !s.isEmpty()).toList());
        }

        Optional<String> libraryId = Optional.empty();
        if (absolute) {
            if (segments.size() < 2 || !"libraries".equals(segments.getFirst())) {
                return Optional.empty();
            }
            libraryId = Optional.of(segments.get(1));
            segments = segments.subList(2, segments.size());
        }
        if (segments.size() < 2 || !"entries".equals(segments.getFirst())) {
            return Optional.empty();
        }
        String citationKey = segments.get(1);

        Optional<Integer> fileIndex = Optional.empty();
        Optional<Integer> pathPage = Optional.empty();

        if (segments.size() == 2) {
            // entries/{citationKey}
        } else if (segments.size() == 3) {
            // entries/{citationKey}/{page} e.g. entries/Queiroz2026/5
            pathPage = parsePositiveInt(segments.get(2));
            if (pathPage.isEmpty()) {
                return Optional.empty();
            }
        } else if (segments.size() == 4 && "files".equals(segments.get(2))) {
            // entries/{citationKey}/files/{fileIndex}
            fileIndex = parsePositiveInt(segments.get(3));
        } else if (segments.size() == 4 && "page".equalsIgnoreCase(segments.get(2))) {
            // entries/{citationKey}/page/{page}
            pathPage = parsePositiveInt(segments.get(3));
            if (pathPage.isEmpty()) {
                return Optional.empty();
            }
        } else if (segments.size() == 5 && "files".equals(segments.get(2))) {
            // entries/{citationKey}/files/{fileIndex}/{page}
            fileIndex = parsePositiveInt(segments.get(3));
            pathPage = parsePositiveInt(segments.get(4));
            if (pathPage.isEmpty()) {
                return Optional.empty();
            }
        } else {
            return Optional.empty();
        }

        Optional<Integer> fragmentPage = Optional.ofNullable(uri.getFragment())
                                                 .map(PAGE_FRAGMENT::matcher)
                                                 .filter(Matcher::find)
                                                 .flatMap(matcher -> parsePositiveInt(matcher.group(1)));
        Optional<Integer> page = fragmentPage.or(() -> pathPage);

        return Optional.of(new EntryLink(libraryId, citationKey, fileIndex, page));
    }

    public static boolean isEntryUrl(@Nullable String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            return false;
        }
        String trimmed = rawUrl.trim();
        return trimmed.startsWith("entries/")
                || trimmed.startsWith("/entries/")
                || trimmed.startsWith(SCHEME + "://")
                || trimmed.startsWith("entry://");
    }

    private static Optional<Integer> parsePositiveInt(String value) {
        try {
            return Optional.of(Integer.parseInt(value)).filter(number -> number > 0);
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    /// @param libraryId empty for links relative to the active library
    /// @param fileIndex 1-based index into the entry's linked files, empty for "first PDF"
    /// @param page      1-based page number
    public record EntryLink(Optional<String> libraryId, String citationKey, Optional<Integer> fileIndex, Optional<Integer> page) {
    }
}
