package antlr.debug;

public interface TraceListener extends ListenerBase {


	public void enterRule(TraceEvent e);
	public void exitRule(TraceEvent e);
}
