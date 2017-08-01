import java.util.Properties;
import org.jabref.logic.util.BracketedExpressionExpander;
import org.jabref.model.entry.BibEntry;

class BracketedExpressionExpanderRunner {
    public static void main( String[] args ) {
        Properties props = System.getProperties();
        props.setProperty("org.apache.commons.logging.Log", "NoOpLog");
        BracketedExpressionExpander bex = new BracketedExpressionExpander(new BibEntry());
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
