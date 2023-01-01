package org.jabref.build

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.*
import org.h2.mvstore.MVMap
import org.h2.mvstore.MVStore
import org.jabref.logic.journals.Abbreviation
import org.jabref.logic.journals.JournalAbbreviationLoader

import java.util.stream.Collectors

abstract class JournalAbbreviationConverter extends DefaultTask {
    @PathSensitive(PathSensitivity.NAME_ONLY)
    @InputDirectory
    abstract DirectoryProperty getInputDir()

    @OutputDirectory
    abstract DirectoryProperty getOutputDir()

    @TaskAction
    def convert() {
        def targetFile = outputDir.file("journalList.mv").get().asFile
        targetFile.delete()
        MVStore.open(targetFile.toString()).withCloseable { store ->

            MVMap<String, Abbreviation> fullToAbbreviation = store.openMap("FullToAbbreviation")
            MVMap<String, Abbreviation> abbreviationToAbbreviation = store.openMap("AbbreviationToAbbreviation")
            MVMap<String, Abbreviation> shortestUniqueToAbbreviation = store.openMap("shortestUniqueToAbbreviation")
            MVMap<String, Abbreviation> dotlessToAbbreviation = store.openMap("DotlessToAbbreviation")

            inputDir.getAsFileTree().filter({ File f -> f.name.endsWith(".csv") }).getFiles().each { file ->

                def abbreviations = JournalAbbreviationLoader.readJournalListFromFile(file.toPath())

                fullToAbbreviation.putAll(
                        abbreviations
                                .stream()
                                .collect(Collectors.toMap(
                                        { abbreviation -> abbreviation.getName() },
                                        { abbreviation -> abbreviation }))
                )

                abbreviationToAbbreviation.putAll(
                        abbreviations
                                .stream()
                                .collect(Collectors.toMap(
                                        { abbreviation -> abbreviation.getAbbreviation() },
                                        { abbreviation -> abbreviation },
                                        // dotless abbrevations might have duplicates - choose the last one in the list
                                        (abbreviation1, abbreviation2) -> {abbreviation2}
                                ))
                )

                shortestUniqueToAbbreviation.putAll(
                        abbreviations
                                .stream()
                                .collect(Collectors.toMap(
                                        { abbreviation -> abbreviation.getShortestUniqueAbbreviation() },
                                        { abbreviation -> abbreviation }))
                )

                dotlessToAbbreviation.putAll(
                        abbreviations
                                .stream()
                                .collect(Collectors.toMap(
                                        { abbreviation -> abbreviation.getDotlessAbbreviation() },
                                        { abbreviation -> abbreviation },
                                        // dotless abbrevations might have duplicates - choose the last one in the list
                                        (abbreviation1, abbreviation2) -> {abbreviation2}
                                ))
                )
            }
        }
    }
}
