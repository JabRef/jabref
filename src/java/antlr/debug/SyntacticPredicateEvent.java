package antlr.debug;

public class SyntacticPredicateEvent extends GuessingEvent {


	public SyntacticPredicateEvent(Object source) {
		super(source);
	}
	public SyntacticPredicateEvent(Object source, int type) {
		super(source, type);
	}
	/** This should NOT be called from anyone other than ParserEventSupport! */
	void setValues(int type, int guessing) {
		super.setValues(type, guessing);
	}
	public String toString() {
		return "SyntacticPredicateEvent [" + getGuessing() + "]";
	}
}
