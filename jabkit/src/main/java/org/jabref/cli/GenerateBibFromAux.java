package org.jabref.cli;

import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.Callable;

import org.jabref.logic.auxparser.AuxParser;
import org.jabref.logic.auxparser.AuxParserResult;
import org.jabref.logic.auxparser.AuxParserStatisticsProvider;
import org.jabref.logic.auxparser.DefaultAuxParser;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabase;

import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static picocli.CommandLine.Command;
import static picocli.CommandLine.Option;
import static picocli.CommandLine.ParentCommand;

@Command(name = "generate-bib-from-aux", description = "Generate small bib from aux file.")
class GenerateBibFromAux implements Callable<Integer> {
    private static final Logger LOGGER = LoggerFactory.getLogger(GenerateBibFromAux.class);

    @ParentCommand
    private KitCommandLine kitCommandLine;

    @Option(names = "--aux", required = true)
    private Path auxFile;

    @Option(names = "--input", required = true)
    private Path inputFile;

    @Option(names = "--output")
    private Path outputFile;

    @Override
    public Integer call() {
        Optional<ParserResult> pr = kitCommandLine.importFile(inputFile, "bibtex");

        if (pr.isEmpty() || auxFile == null) {
            return 1;
        }

        BibDatabase subDatabase = createFromAux(pr.get(), auxFile);

        if (subDatabase == null || !subDatabase.hasEntries()) {
            System.out.println(Localization.lang("no library generated"));
            return 1;
        }

        if (outputFile == null) {
            System.out.println(subDatabase.getEntries().stream()); // ToDo: Make nice
        } else {
            kitCommandLine.saveDatabase(subDatabase, outputFile);
        }
        return 0;
    }

    @VisibleForTesting
    static BibDatabase createFromAux(ParserResult pr, Path auxFile) {
        BibDatabase subDatabase = null;
        BibDatabase sourceDatabase = pr.getDatabase();
        if (auxFile != null && (sourceDatabase != null)) {
            AuxParser auxParser = new DefaultAuxParser(sourceDatabase);
            AuxParserResult result = auxParser.parse(auxFile);
            LOGGER.info(new AuxParserStatisticsProvider(result).getInformation(true));
            subDatabase = result.getGeneratedBibDatabase();
        }
        return subDatabase;
    }
}
