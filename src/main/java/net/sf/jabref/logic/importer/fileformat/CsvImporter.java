package net.sf.jabref.logic.importer.fileformat;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.jabref.logic.importer.Importer;
import net.sf.jabref.logic.importer.ParserResult;
import net.sf.jabref.logic.util.FileExtensions;
import net.sf.jabref.model.entry.BibEntry;

/**
 * Importer para o formato CSV.
 * Neste caso, os campos do arquivo devem ser separados por virgula!
 */
public class CsvImporter extends Importer {
    private static final Pattern CSV_PATTERN = Pattern.compile("((\"([^\"]*),+([^\"]*)\")||([^\"]*),([^\"]*))\n?");

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

        // Auxiliar para ler linha a linha o arquivo
        String readLine;
        // Boolean que indica se a linha lida eh o cabecalho
        boolean inHeader = true;
        // Contador do numero de campos do cabecalho
        int headerSize = 0;
        // Contador de campos da linha lida
        int lineSize;

        // Enquanto uma nova linha eh lida:
        while((readLine = reader.readLine()) != null) {

            // Se for o cabecalho (primeira linha), verifica a RegEx e computa o nro de campos
            if(inHeader) {
                if(!CsvImporter.CSV_PATTERN.matcher(readLine).find())
                    return false;
                else {
                    headerSize = substituteInsideCommaByTripleHash(readLine).split(",").length;
                    inHeader = false;
                }
            }

            // Se for uma linha de entrada, verifica se o tamanho eh o mesmo que o cabecalho e
            // se a RegEx eh satisfeita
            else {
                lineSize = substituteInsideCommaByTripleHash(readLine).split(",").length;
                if(((lineSize != headerSize) && (lineSize > 0)) || (!CsvImporter.CSV_PATTERN.matcher(readLine).find()))
                    return false;
            }
        }

        // Se todas as linhas foram lidas sem erros, entao o arquivo CSV eh valido!
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
                String[] fieldValues = substituteInsideCommaByTripleHash(entry).split(",");

                // Em seguida, verifica-se se o campo de tipo nao esta vazio
                if (!fieldValues[0].equals("")) {

                    // Se nao estiver vazio, cria uma entrada bib
                    BibEntry newBibEntry = new BibEntry(matchType(fieldValues[0]));

                    // E, em seguida, completa com os demais campos
                    for (int i = 1; i < headerFields.length; i++) {
                        // Insere campo apenas se nao for vazio
                        if(!fieldValues[i].equals("\"" + "\"") && !fieldValues[i].equals("")) {
                            // Remove as "s de alguns campos cujos valores sao envoltos
                            // por " (o export do jabref gera alguns com estas "s)
                            if (String.valueOf(fieldValues[i].charAt(0)).equals("\"") &&
                                    String.valueOf(fieldValues[i].charAt(fieldValues[i].length() - 1)).equals("\""))
                                newBibEntry.setField(headerFields[i], fieldValues[i].substring(1, fieldValues[i].length() - 1).replace("###", ","));
                            else
                                newBibEntry.setField(headerFields[i], fieldValues[i]);
                        }
                    }
                    results.add(newBibEntry);
                }

            }
        }

        return new ParserResult(results);
    }

    // substituteInsideCommaByTripleHash substitui as virgulas dentro de campos por ###. Assim, quando
    // os campos sao separados essas virgulas "internas" nao influenciam. Por exemplo, para:
    // "Ito Nagura, V.","2017",5, o resultado sera "Ito Nagura### V.","2017",5.
    private static String substituteInsideCommaByTripleHash(String inputString) {
        // Substitui inicialmente \" por %%. Por exemplo, em "Ahlberg, J{\"o}rgen"
        String stringAfterReplacingInsideQuotes = inputString.replace("\\" + "\"", "%%");
        // Utiliza RegEx para localizar os campos entre aspas, pois considera que as virgulas
        // internas estao presente apenas em campos entre aspas (aspas internas foram removidas)
        Pattern p = Pattern.compile("\\" + "\"" + "(.*?)\\" + "\"");
        Matcher matcherForCounter = p.matcher(stringAfterReplacingInsideQuotes);
        Matcher matcherForCorrection = p.matcher(stringAfterReplacingInsideQuotes);

        // O primeiro matcher conta quantos campos entre aspas existem
        int counter = 0;
        while(matcherForCounter.find()) {
            counter++;
        }

        // Se existir algum campo entre aspas para ser verificado:
        if (counter > 0) {
            String[] uncorrectedFields = new String[counter];
            String[] correctedFields = new String[counter];
            String[] correction = new String[counter];

            // Cria dois arrays para correcao das strings
            int i = 0;
            while(matcherForCorrection.find()) {
                uncorrectedFields[i] = matcherForCorrection.group();
                correctedFields[i] = matcherForCorrection.group().replace(",", "###");
                i++;
            }

            // Efetua a correcao de maneira "recursiva" sobre a entrada
            correction[0] = stringAfterReplacingInsideQuotes.replace(uncorrectedFields[0], correctedFields[0]);
            for (int j = 1; j < counter; j++)
                correction[j] = correction[j - 1].replace(uncorrectedFields[j], correctedFields[j]);

            return correction[counter - 1].replace("%%", "\\" + "\"");
        }
        else
            return inputString;
    }

    // Realiza a conversao inversa da presente em GetOpenOfficeType, inserindo os tipos como
    // "strings", e nao "numeros"
    private static String matchType(String fieldType) {
        if ("7".equalsIgnoreCase(fieldType)) {
            return "Article";
        }
        if ("1".equalsIgnoreCase(fieldType)) {
            return "Book";
        }
        if ("2".equalsIgnoreCase(fieldType)) {
            return "Booklet";
        }
        if ("5".equalsIgnoreCase(fieldType)) {
            return "Inbook";
        }
        if ("6".equalsIgnoreCase(fieldType)) {
            return "Inproceedings";
        }
        if ("8".equalsIgnoreCase(fieldType)) {
            return "Manual";
        }
        if ("9".equalsIgnoreCase(fieldType)) {
            return "Mastersthesis";
        }
        if ("10".equalsIgnoreCase(fieldType)) {
            return "Misc";
        }
        if ("3".equalsIgnoreCase(fieldType)) {
            return "Proceedings";
        }
        if ("13".equalsIgnoreCase(fieldType)) {
            return "Techreport";
        }
        if ("14".equalsIgnoreCase(fieldType)) {
            return "Unpublished";
        }
        // Default
        return fieldType;
    }
}
