package antlr.debug;

public class ParserAdapter implements ParserListener {


	public void doneParsing(TraceEvent e) {}
	public void enterRule(TraceEvent e) {}
	public void exitRule(TraceEvent e) {}
	public void parserConsume(ParserTokenEvent e) {}
	public void parserLA(ParserTokenEvent e) {}
	public void parserMatch(ParserMatchEvent e) {}
	public void parserMatchNot(ParserMatchEvent e) {}
	public void parserMismatch(ParserMatchEvent e) {}
	public void parserMismatchNot(ParserMatchEvent e) {}
	public void refresh() {}
	public void reportError(MessageEvent e) {}
	public void reportWarning(MessageEvent e) {}
	public void semanticPredicateEvaluated(SemanticPredicateEvent e) {}
	public void syntacticPredicateFailed(SyntacticPredicateEvent e) {}
	public void syntacticPredicateStarted(SyntacticPredicateEvent e) {}
	public void syntacticPredicateSucceeded(SyntacticPredicateEvent e) {}
}
