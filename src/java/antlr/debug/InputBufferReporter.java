package antlr.debug;

public class InputBufferReporter implements InputBufferListener {


/**
 * doneParsing method comment.
 */
public void doneParsing(TraceEvent e) {
}
	public void inputBufferChanged(InputBufferEvent e) {
		System.out.println(e);
	}
/**
 * charBufferConsume method comment.
 */
public void inputBufferConsume(InputBufferEvent e) {
	System.out.println(e);
}
/**
 * charBufferLA method comment.
 */
public void inputBufferLA(InputBufferEvent e) {
	System.out.println(e);
}
	public void inputBufferMark(InputBufferEvent e) {
		System.out.println(e);
	}
	public void inputBufferRewind(InputBufferEvent e) {
		System.out.println(e);
	}
/**
 * refresh method comment.
 */
public void refresh() {
}
}
