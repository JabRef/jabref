package antlr.debug;

public class ParserMatchAdapter implements ParserMatchListener {


	public void doneParsing(TraceEvent e) {}
	public void parserMatch(ParserMatchEvent e) {}
	public void parserMatchNot(ParserMatchEvent e) {}
	public void parserMismatch(ParserMatchEvent e) {}
	public void parserMismatchNot(ParserMatchEvent e) {}
	public void refresh() {}
}
