package antlr.debug;

public class TraceEvent extends GuessingEvent {
	private int ruleNum;
	private int data;
	public static int ENTER=0;
	public static int EXIT=1;
	public static int DONE_PARSING=2;


	public TraceEvent(Object source) {
		super(source);
	}
	public TraceEvent(Object source, int type, int ruleNum, int guessing, int data) {
		super(source);
		setValues(type, ruleNum, guessing, data);
	}
	public int getData() {
		return data;
	}
	public int getRuleNum() {
		return ruleNum;
	}
	void setData(int data) {
		this.data = data;
	}
	void setRuleNum(int ruleNum) {
		this.ruleNum = ruleNum;
	}
	/** This should NOT be called from anyone other than ParserEventSupport! */
	 void setValues(int type, int ruleNum, int guessing, int data) {
		super.setValues(type, guessing);
		setRuleNum(ruleNum);
		setData(data);
	}
	public String toString() {
		return "ParserTraceEvent [" + 
		       (getType()==ENTER?"enter,":"exit,") +
		       getRuleNum() + "," + getGuessing() +"]";
	}
}
