package antlr.debug;

public class ParserReporter extends Tracer implements ParserListener {


	public void parserConsume(ParserTokenEvent e) {
		System.out.println(indent+e);
	}
	public void parserLA(ParserTokenEvent e) {
		System.out.println(indent+e);
	}
	public void parserMatch(ParserMatchEvent e) {
		System.out.println(indent+e);
	}
	public void parserMatchNot(ParserMatchEvent e) {
		System.out.println(indent+e);
	}
	public void parserMismatch(ParserMatchEvent e) {
		System.out.println(indent+e);
	}
	public void parserMismatchNot(ParserMatchEvent e) {
		System.out.println(indent+e);
	}
	public void reportError(MessageEvent e) {
		System.out.println(indent+e);
	}
	public void reportWarning(MessageEvent e) {
		System.out.println(indent+e);
	}
	public void semanticPredicateEvaluated(SemanticPredicateEvent e) {
		System.out.println(indent+e);
	}
	public void syntacticPredicateFailed(SyntacticPredicateEvent e) {
		System.out.println(indent+e);
	}
	public void syntacticPredicateStarted(SyntacticPredicateEvent e) {
		System.out.println(indent+e);
	}
	public void syntacticPredicateSucceeded(SyntacticPredicateEvent e) {
		System.out.println(indent+e);
	}
}
