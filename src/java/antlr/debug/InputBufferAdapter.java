package antlr.debug;

/** A dummy implementation of a CharBufferListener -- this class is not
  * meant to be used by itself -- it's meant to be subclassed */
public abstract class InputBufferAdapter implements InputBufferListener {


	public void doneParsing(TraceEvent e) {
	}
/**
 * charConsumed method comment.
 */
public void inputBufferConsume(InputBufferEvent e) {
}
/**
 * charLA method comment.
 */
public void inputBufferLA(InputBufferEvent e) {
}
	public void inputBufferMark(InputBufferEvent e) {}
	public void inputBufferRewind(InputBufferEvent e) {}
	public void refresh() {
	}
}
