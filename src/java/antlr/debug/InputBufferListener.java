package antlr.debug;

public interface InputBufferListener extends ListenerBase {


	public void inputBufferConsume(InputBufferEvent e);
	public void inputBufferLA(InputBufferEvent e);
	public void inputBufferMark(InputBufferEvent e);
	public void inputBufferRewind(InputBufferEvent e);
}
