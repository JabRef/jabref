import java.io.FileReader;
import java.io.FileNotFoundException;
import java.util.List;

import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.fileformat.BibtexParser;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.util.BracketedExpressionExpander;
import org.jabref.model.entry.BibEntry;

import org.mockito.Answers;
import static org.mockito.Mockito.mock;

class BracketedExpressionExpanderRunner {
    public static void main( String[] args ) throws FileNotFoundException, ParseException {
        BibEntry bibentry;
        int start_arg = 0;
        if( args.length > 1 ) {
            start_arg ++;
            ImportFormatPreferences importFormatPreferences;
            BibtexParser parser;

            importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
            parser = new BibtexParser(importFormatPreferences);
            List<BibEntry> parsedEntries = parser.parseEntries(new FileReader(args[0]));
            bibentry = parsedEntries.get(0);
        } else {
            
            bibentry = new BibEntry();
            bibentry.setField( "author", "O. Kitsune" );
            bibentry.setField( "year", "2017" );
            bibentry.setField( "pages", "213--216" );
        }
        BracketedExpressionExpander bex = new BracketedExpressionExpander( bibentry );
        if( args.length > 0 ) {
            for( int i = start_arg; i < args.length; i++ ) {
                System.out.println( args[i] + "\t" +
                                    bex.expandBrackets( args[i] ));
            }
        } else {
            System.err.println( System.getProperty("sun.java.command") +
                                ": WARNING, " +
                                "no arguments provided on the command line" );
        }
    }
}
