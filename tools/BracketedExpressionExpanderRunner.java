import org.jabref.logic.util.BracketedExpressionExpander;
import org.jabref.model.entry.BibEntry;

class BracketedExpressionExpanderRunner {
    public static void main( String[] args ) {
        BibEntry bibentry = new BibEntry();
        bibentry.setField( "author", "A. Kizune" );
        bibentry.setField( "year", "2017" );
        bibentry.setField( "pages", "213--216" );
        BracketedExpressionExpander bex = new BracketedExpressionExpander( bibentry );
        if( args.length > 0 ) {
            for( int i = 0; i < args.length; i++ ) {
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
