package antlr.debug;

public interface ParserMatchListener extends ListenerBase {


	public void parserMatch(ParserMatchEvent e);
	public void parserMatchNot(ParserMatchEvent e);
	public void parserMismatch(ParserMatchEvent e);
	public void parserMismatchNot(ParserMatchEvent e);
}
