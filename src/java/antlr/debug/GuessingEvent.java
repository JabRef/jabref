package antlr.debug;

public abstract class GuessingEvent extends Event {
	private int guessing;


	public GuessingEvent(Object source) {
		super(source);
	}
	public GuessingEvent(Object source, int type) {
		super(source, type);
	}
	public int getGuessing() {
		return guessing;
	}
	void setGuessing(int guessing) {
		this.guessing = guessing;
	}
	/** This should NOT be called from anyone other than ParserEventSupport! */
	void setValues(int type, int guessing) {
		super.setValues(type);
		setGuessing(guessing);
	}
}
