package antlr.debug;

public class SyntacticPredicateAdapter implements SyntacticPredicateListener {


	public void doneParsing(TraceEvent e) {}
	public void refresh() {}
	public void syntacticPredicateFailed(SyntacticPredicateEvent e) {}
	public void syntacticPredicateStarted(SyntacticPredicateEvent e) {}
	public void syntacticPredicateSucceeded(SyntacticPredicateEvent e) {}
}
