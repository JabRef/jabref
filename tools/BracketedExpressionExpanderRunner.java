
class BracketedExpressionExpanderRunner {
    public static void main( String[] args ) {
        long sum = 1;
        for( long i = 1; i <= 10000000; i++ ) {
            sum += i;
        }
        System.out.println( sum );
    }
}
