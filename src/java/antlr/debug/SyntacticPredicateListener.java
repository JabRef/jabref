package antlr.debug;

public interface SyntacticPredicateListener extends ListenerBase {


	public void syntacticPredicateFailed(SyntacticPredicateEvent e);
	public void syntacticPredicateStarted(SyntacticPredicateEvent e);
	public void syntacticPredicateSucceeded(SyntacticPredicateEvent e);
}
