package antlr.debug;

public interface ParserTokenListener extends ListenerBase {


	public void parserConsume(ParserTokenEvent e);
	public void parserLA(ParserTokenEvent e);
}
