package antlr.debug;

public interface ParserListener extends SemanticPredicateListener, 
										ParserMatchListener, 
										MessageListener, 
										ParserTokenListener, 
										TraceListener, 
										SyntacticPredicateListener {
}
