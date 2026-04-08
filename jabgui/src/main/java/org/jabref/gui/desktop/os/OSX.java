package org.jabref.gui.desktop.os;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.jabref.architecture.AllowedToUseAwt;
import org.jabref.gui.DialogService;
import org.jabref.gui.externalfiletype.ExternalFileType;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.frame.ExternalApplicationsPreferences;

/// This class contains macOS (OSX) specific implementations for file directories and file/application open handling methods.
///
/// We cannot use a static logger instance here in this class as the Logger first needs to be configured in the {@link JabKit#initLogging}.
/// The configuration of tinylog will become immutable as soon as the first log entry is issued.
/// https://tinylog.org/v2/configuration/
@AllowedToUseAwt("Requires AWT to open a file")
public class OSX extends NativeDesktop {

    private static final Path ADOBE_READER_EXECUTABLE = Path.of("/Applications/Adobe Acrobat Reader.app/Contents/MacOS/AdobeReader");
    private static final Path ADOBE_ACROBAT_EXECUTABLE = Path.of("/Applications/Adobe Acrobat DC/Adobe Acrobat.app/Contents/MacOS/Adobe Acrobat");

    @Override
    public void openFile(String filePath, String fileType, ExternalApplicationsPreferences externalApplicationsPreferences, int pageNumber) throws IOException {
        Optional<ExternalFileType> type = ExternalFileTypes.getExternalFileTypeByExt(fileType, externalApplicationsPreferences);

        if (type.isPresent() && !type.get().getOpenWithApplication().isEmpty()) {
            String application = type.get().getOpenWithApplication();
            openFileWithApplication(filePath, application, pageNumber);
            return;
        }

        if (isPdf(fileType) && pageNumber > 1) {
            String fileUrlWithPage = Path.of(filePath).toUri().toString() + "#page=" + pageNumber;

            String chromeBinary = "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome";
            String edgeBinary = "/Applications/Microsoft Edge.app/Contents/MacOS/Microsoft Edge";

            if (Files.exists(Path.of(chromeBinary))) {
                new ProcessBuilder(chromeBinary, fileUrlWithPage).start();
                return;
            }
            if (Files.exists(Path.of(edgeBinary))) {
                new ProcessBuilder(edgeBinary, fileUrlWithPage).start();
                return;
            }

            NativeDesktop.openBrowser(fileUrlWithPage, externalApplicationsPreferences);
            return;
        }

        String[] cmd = {"/usr/bin/open", filePath};
        Runtime.getRuntime().exec(cmd);
    }

    @Override
    public void openFileWithApplication(String filePath, String application, int pageNumber) throws IOException {
        String executable = Path.of(application).getFileName().toString().toLowerCase(Locale.ROOT);

        if (pageNumber > 1 && (executable.contains("adobereader") || executable.contains("acrobat"))) {
            Optional<Path> appBinary = resolveAdobeExecutable(application);
            if (appBinary.isPresent()) {
                new ProcessBuilder(appBinary.get().toString(), "/A", "page=" + pageNumber, filePath).start();
                return;
            }
        }

        List<String> command = new ArrayList<>();
        command.add("/usr/bin/open");

        if ((application != null) && !application.isEmpty()) {
            command.add("-a");
            command.add(application);
        }

        command.add(filePath);

        if (pageNumber > 1) {
            if (executable.contains("skim")) {
                command.add("--args");
                command.add("-g");
                command.add(String.valueOf(pageNumber));
            } else if (executable.contains("adobereader") || executable.contains("acrobat")) {
                command.add("--args");
                command.add("/A");
                command.add("page=" + pageNumber);
            }
        }

        new ProcessBuilder(command).start();
    }

    private static boolean isPdf(String fileType) {
        return "pdf".equalsIgnoreCase(fileType);
    }

    private static Optional<Path> resolveAdobeExecutable(String application) {
        if (application.endsWith(".app")) {
            String appName = Path.of(application).getFileName().toString().toLowerCase(Locale.ROOT);
            if (appName.contains("reader") && Files.isExecutable(ADOBE_READER_EXECUTABLE)) {
                return Optional.of(ADOBE_READER_EXECUTABLE);
            }
            if (appName.contains("acrobat") && Files.isExecutable(ADOBE_ACROBAT_EXECUTABLE)) {
                return Optional.of(ADOBE_ACROBAT_EXECUTABLE);
            }
        }

        if (Files.isExecutable(ADOBE_READER_EXECUTABLE)) {
            return Optional.of(ADOBE_READER_EXECUTABLE);
        }
        if (Files.isExecutable(ADOBE_ACROBAT_EXECUTABLE)) {
            return Optional.of(ADOBE_ACROBAT_EXECUTABLE);
        }

        return Optional.empty();
    }

    @Override
    public void openFolderAndSelectFile(Path file) throws IOException {
        String[] cmd = {"/usr/bin/open", "-R", file.toString()};
        Runtime.getRuntime().exec(cmd);
    }

    @Override
    public void openConsole(String absolutePath, DialogService dialogService) throws IOException {
        new ProcessBuilder("open", "-a", "Terminal", absolutePath).start();
    }

    @Override
    public Path getApplicationDirectory() {
        return Path.of("/Applications");
    }
}
