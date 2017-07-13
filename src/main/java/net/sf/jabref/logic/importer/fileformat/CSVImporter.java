package net.sf.jabref.logic.importer.fileformat;

import net.sf.jabref.logic.importer.Importer;
import net.sf.jabref.logic.importer.ParserResult;
import net.sf.jabref.logic.util.FileExtensions;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.FieldName;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Importer para o formato CSV.
 * Neste caso, os campos do arquivo devem ser separados por virgula!
 */
public class CSVImporter extends Importer {
    private static final Pattern COPAC_PATTERN = Pattern.compile("^\\s*TI- ");

    @Override
    public String getName() {
        return "CSV";
    }

    @Override
    public FileExtensions getExtensions() {
        return FileExtensions.CSV;
    }

    @Override
    public String getId() {
        return "csv";
    }

    @Override
    public String getDescription() {
        return "Importer para o formato CSV.";
    }

    @Override
    public boolean isRecognizedFormat(BufferedReader reader) throws IOException {
        return true;
    }

    @Override
    public ParserResult importDatabase(BufferedReader reader) throws IOException {

        // Lista de entradas bib que sera importada
        List<BibEntry> results = new LinkedList<>();

        // StringBuilder junta as linhas lidas do CSV
        StringBuilder sb = new StringBuilder();
        String str;
        while ((str = reader.readLine()) != null) {
            sb.append(str);
            sb.append('\n');
        }

        // Verifica se o arquivo nao estava vazio
        if (sb.length() > 0) {

            // Separa a StringBuilder (ja convertida) linha por linha
            String[] fileLines = sb.toString().split("\n");

            // Como a primeira linha contem o cabecalho, separa os seus campos
            String[] headerFields =  fileLines[0].split(",");

            // As demais linhas sao as entradas, de fato
            String[] entries = Arrays.copyOfRange(fileLines, 1, fileLines.length);

            // Assim, deve-se adicionar cada entrada uma a uma
            for (String entry : entries) {

                // Primeiro, os valores de cada campo sao separados
                String[] fieldValues = entry.split(",");

                // Em seguida, verifica-se se o campo de tipo nao esta vazio
                if (!fieldValues[0].equals("")) {

                    // Se nao estiver vazio, cria uma entrada bib
                    BibEntry newBibEntry = new BibEntry(fieldValues[0]);

                    // E, em seguida, completa com os demais campos
                    for (int i = 1; i < headerFields.length; i++) {
                        newBibEntry.setField(headerFields[i], fieldValues[i]);
                    }
                    results.add(newBibEntry);
                }

            }
        }

        return new ParserResult(results);
    }
}
