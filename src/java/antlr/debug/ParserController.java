package antlr.debug;

public interface ParserController extends ParserListener {


	public void checkBreak();
	public void setParserEventSupport(ParserEventSupport p);
}
