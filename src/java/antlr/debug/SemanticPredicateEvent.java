package antlr.debug;

public class SemanticPredicateEvent extends GuessingEvent {
	public static final int VALIDATING=0;
	public static final int PREDICTING=1;
	private int condition;
	private boolean result;


	public SemanticPredicateEvent(Object source) {
		super(source);
	}
	public SemanticPredicateEvent(Object source, int type) {
		super(source, type);
	}
	public int getCondition() {
		return condition;
	}
	public boolean getResult() {
		return result;
	}
	void setCondition(int condition) {
		this.condition = condition;
	}
	void setResult(boolean result) {
		this.result = result;
	}
	/** This should NOT be called from anyone other than ParserEventSupport! */
	void setValues(int type, int condition, boolean result, int guessing) {
		super.setValues(type, guessing);
		setCondition(condition);
		setResult(result);
	}
	public String toString() {
		return "SemanticPredicateEvent [" + 
		       getCondition() + "," + getResult() + "," + getGuessing() + "]";
	}
}
