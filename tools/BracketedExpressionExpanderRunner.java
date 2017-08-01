import org.jabref.logic.util.BracketedExpressionExpander;

class BracketedExpressionExpanderRunner {
    public static void main( String[] args ) {
        if( args.length > 0 ) {
            for( int i = 0; i < args.length; i++ ) {
                System.out.println( args[i] );
            }
        } else {
            System.err.println( System.getProperty("sun.java.command") +
                                ": WARNING, " +
                                "no arguments provided on the command line" );
        }
    }
}
