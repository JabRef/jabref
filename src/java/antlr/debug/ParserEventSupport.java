package antlr.debug;

import java.util.Vector;
import java.util.Hashtable;
import java.util.Enumeration;
import antlr.collections.impl.BitSet;
import antlr.RecognitionException;


/** A class to assist in firing parser events
 *  NOTE: I intentionally _did_not_ synchronize the event firing and
 *        add/remove listener methods.  This is because the add/remove should
 *        _only_ be called by the parser at its start/end, and the _same_thread_
 *        should be performing the parsing.  This should help performance a tad...
 */
public class ParserEventSupport {
	private Object source;
	private Hashtable doneListeners;
	private Vector matchListeners;
	private Vector messageListeners;
	private Vector tokenListeners;
	private Vector traceListeners;
	private Vector semPredListeners;
	private Vector synPredListeners;
	private Vector newLineListeners;
	private ParserMatchEvent        matchEvent;
	private MessageEvent            messageEvent;
	private ParserTokenEvent        tokenEvent;
	private SemanticPredicateEvent  semPredEvent;
	private SyntacticPredicateEvent synPredEvent;
	private TraceEvent              traceEvent;
	private NewLineEvent            newLineEvent;
	private ParserController        controller;
	protected static final int CONSUME=0;
	protected static final int ENTER_RULE=1;
	protected static final int EXIT_RULE=2;
	protected static final int LA=3;
	protected static final int MATCH=4;
	protected static final int MATCH_NOT=5;
	protected static final int MISMATCH=6;
	protected static final int MISMATCH_NOT=7;
	protected static final int REPORT_ERROR=8;
	protected static final int REPORT_WARNING=9;
	protected static final int SEMPRED=10;
	protected static final int SYNPRED_FAILED=11;
	protected static final int SYNPRED_STARTED=12;
	protected static final int SYNPRED_SUCCEEDED=13;
	protected static final int NEW_LINE=14;
	protected static final int DONE_PARSING=15;
	private int ruleDepth = 0;


	public ParserEventSupport(Object source) {
		matchEvent   = new ParserMatchEvent(source);
		messageEvent = new MessageEvent(source);
		tokenEvent   = new ParserTokenEvent(source);
		traceEvent   = new TraceEvent(source);
		semPredEvent = new SemanticPredicateEvent(source);
		synPredEvent = new SyntacticPredicateEvent(source);
		newLineEvent = new NewLineEvent(source);
		this.source = source;
	}
	public void addDoneListener(ListenerBase l) {
		if (doneListeners == null) doneListeners = new Hashtable();
		Integer i = (Integer)doneListeners.get(l);
		int val;
		if (i != null)
			val = i.intValue() + 1;
		else
			val = 1;
		doneListeners.put(l, new Integer(val));
	}
	public void addMessageListener(MessageListener l) {
		if (messageListeners == null) messageListeners = new Vector();
		messageListeners.addElement(l);
		addDoneListener(l);
	}
	public void addNewLineListener(NewLineListener l) {
		if (newLineListeners == null) newLineListeners = new Vector();
		newLineListeners.addElement(l);
		addDoneListener(l);
	}
	public void addParserListener(ParserListener l) {
		if (l instanceof ParserController) {
			((ParserController)l).setParserEventSupport(this);
			controller = (ParserController)l;
		}	
		addParserMatchListener(l);
		addParserTokenListener(l);

		addMessageListener(l);
		addTraceListener(l);
		addSemanticPredicateListener(l);
		addSyntacticPredicateListener(l);
	}
	public void addParserMatchListener(ParserMatchListener l) {
		if (matchListeners == null) matchListeners = new Vector();
		matchListeners.addElement(l);
		addDoneListener(l);
	}
	public void addParserTokenListener(ParserTokenListener l) {
		if (tokenListeners == null) tokenListeners = new Vector();
		tokenListeners.addElement(l);
		addDoneListener(l);
	}
	public void addSemanticPredicateListener(SemanticPredicateListener l) {
		if (semPredListeners == null) semPredListeners = new Vector();
		semPredListeners.addElement(l);
		addDoneListener(l);
	}
	public void addSyntacticPredicateListener(SyntacticPredicateListener l) {
		if (synPredListeners == null) synPredListeners = new Vector();
		synPredListeners.addElement(l);
		addDoneListener(l);
	}
	public void addTraceListener(TraceListener l) {
		if (traceListeners == null) traceListeners = new Vector();
		traceListeners.addElement(l);
		addDoneListener(l);
	}
	public void fireConsume(int value) {
		tokenEvent.setValues(ParserTokenEvent.CONSUME, 1, value);
		fireEvents(CONSUME, tokenListeners);		
	}
	public void fireDoneParsing() {
		traceEvent.setValues(TraceEvent.DONE_PARSING, 0,0,0);

		Hashtable targets=null;
//		Hashtable targets=doneListeners;
		ListenerBase l=null;
		
		synchronized (this) {
			if (doneListeners == null) return;
			targets = (Hashtable)doneListeners.clone();
		}
		
		if (targets != null) {
			Enumeration e = targets.keys();
			while(e.hasMoreElements()) {
				l = (ListenerBase)e.nextElement();
				fireEvent(DONE_PARSING, l);
			}
		}	
		if (controller != null)
			controller.checkBreak();
	}
	public void fireEnterRule(int ruleNum, int guessing, int data) {
		ruleDepth++;
		traceEvent.setValues(TraceEvent.ENTER, ruleNum, guessing, data);
		fireEvents(ENTER_RULE, traceListeners);
	}
	public void fireEvent(int type, ListenerBase l) {
		switch(type) {
			case CONSUME:    ((ParserTokenListener)l).parserConsume(tokenEvent); break;
			case LA:         ((ParserTokenListener)l).parserLA(tokenEvent);      break;

			case ENTER_RULE: ((TraceListener)l).enterRule(traceEvent);           break;
			case EXIT_RULE:  ((TraceListener)l).exitRule(traceEvent);            break;

			case MATCH:        ((ParserMatchListener)l).parserMatch(matchEvent);       break;
			case MATCH_NOT:    ((ParserMatchListener)l).parserMatchNot(matchEvent);    break;
			case MISMATCH:     ((ParserMatchListener)l).parserMismatch(matchEvent);    break;
			case MISMATCH_NOT: ((ParserMatchListener)l).parserMismatchNot(matchEvent); break;

			case SEMPRED:      ((SemanticPredicateListener)l).semanticPredicateEvaluated(semPredEvent); break;

			case SYNPRED_STARTED:   ((SyntacticPredicateListener)l).syntacticPredicateStarted(synPredEvent);   break;
			case SYNPRED_FAILED:    ((SyntacticPredicateListener)l).syntacticPredicateFailed(synPredEvent);    break;
			case SYNPRED_SUCCEEDED: ((SyntacticPredicateListener)l).syntacticPredicateSucceeded(synPredEvent); break;

			case REPORT_ERROR:   ((MessageListener)l).reportError(messageEvent);   break;
			case REPORT_WARNING: ((MessageListener)l).reportWarning(messageEvent); break;

			case DONE_PARSING: l.doneParsing(traceEvent); break;
			case NEW_LINE:     ((NewLineListener)l).hitNewLine(newLineEvent); break;
			
			default:
				throw new IllegalArgumentException("bad type "+type+" for fireEvent()");
		}	
	}
	public void fireEvents(int type, Vector listeners) {
		ListenerBase l=null;
		
		if (listeners != null)
			for (int i = 0; i < listeners.size(); i++) {
				l = (ListenerBase)listeners.elementAt(i);
				fireEvent(type, l);
			}
		if (controller != null)
			controller.checkBreak();
	}
	public void fireExitRule(int ruleNum, int guessing, int data) {
		traceEvent.setValues(TraceEvent.EXIT, ruleNum, guessing, data);
		fireEvents(EXIT_RULE, traceListeners);
		ruleDepth--;
		if (ruleDepth == 0)
			fireDoneParsing();
	}
	public void fireLA(int k, int la) {
		tokenEvent.setValues(ParserTokenEvent.LA, k, la);
		fireEvents(LA, tokenListeners);
	}
	public void fireMatch(char c, int guessing) {
		matchEvent.setValues(ParserMatchEvent.CHAR, c, new Character(c), null, guessing, false, true);
		fireEvents(MATCH, matchListeners);
	}
	public void fireMatch(char value, BitSet b, int guessing) {
		matchEvent.setValues(ParserMatchEvent.CHAR_BITSET, value, b, null, guessing, false, true);
		fireEvents(MATCH, matchListeners);
	}
	public void fireMatch(char value, String target, int guessing) {
		matchEvent.setValues(ParserMatchEvent.CHAR_RANGE, value, target, null, guessing, false, true);
		fireEvents(MATCH, matchListeners);
	}
	public void fireMatch(int value, BitSet b, String text, int guessing) {
		matchEvent.setValues(ParserMatchEvent.BITSET, value, b, text, guessing, false, true);
		fireEvents(MATCH, matchListeners);
	}
	public void fireMatch(int n, String text, int guessing) {
		matchEvent.setValues(ParserMatchEvent.TOKEN, n, new Integer(n), text, guessing, false, true);
		fireEvents(MATCH, matchListeners);
	}
	public void fireMatch(String s, int guessing) {
		matchEvent.setValues(ParserMatchEvent.STRING, 0, s, null, guessing, false, true);
		fireEvents(MATCH, matchListeners);
	}
	public void fireMatchNot(char value, char n, int guessing) {
		matchEvent.setValues(ParserMatchEvent.CHAR, value, new Character(n), null, guessing, true, true);
		fireEvents(MATCH_NOT, matchListeners);
	}
	public void fireMatchNot(int value, int n, String text, int guessing) {
		matchEvent.setValues(ParserMatchEvent.TOKEN, value, new Integer(n), text, guessing, true, true);
		fireEvents(MATCH_NOT, matchListeners);
	}
	public void fireMismatch(char value, char n, int guessing) {
		matchEvent.setValues(ParserMatchEvent.CHAR, value, new Character(n), null, guessing, false, false);
		fireEvents(MISMATCH, matchListeners);
	}
	public void fireMismatch(char value, BitSet b, int guessing) {
		matchEvent.setValues(ParserMatchEvent.CHAR_BITSET, value, b, null, guessing, false, true);
		fireEvents(MISMATCH, matchListeners);
	}
	public void fireMismatch(char value, String target, int guessing) {
		matchEvent.setValues(ParserMatchEvent.CHAR_RANGE, value, target, null, guessing, false, true);
		fireEvents(MISMATCH, matchListeners);
	}
	public void fireMismatch(int value, int n, String text, int guessing) {
		matchEvent.setValues(ParserMatchEvent.TOKEN, value, new Integer(n), text, guessing, false, false);
		fireEvents(MISMATCH, matchListeners);
	}
	public void fireMismatch(int value, BitSet b, String text, int guessing) {
		matchEvent.setValues(ParserMatchEvent.BITSET, value, b, text, guessing, false, true);
		fireEvents(MISMATCH, matchListeners);
	}
	public void fireMismatch(String value, String text, int guessing) {
		matchEvent.setValues(ParserMatchEvent.STRING, 0, text, value, guessing, false, true);
		fireEvents(MISMATCH, matchListeners);
	}
	public void fireMismatchNot(char value, char c, int guessing) {
		matchEvent.setValues(ParserMatchEvent.CHAR, value, new Character(c), null, guessing, true, true);
		fireEvents(MISMATCH_NOT, matchListeners);
	}
	public void fireMismatchNot(int value, int n, String text, int guessing) {
		matchEvent.setValues(ParserMatchEvent.TOKEN, value, new Integer(n), text, guessing, true, true);
		fireEvents(MISMATCH_NOT, matchListeners);
	}
	public void fireNewLine(int line) {
		newLineEvent.setValues(line);
		fireEvents(NEW_LINE, newLineListeners);
	}
	public void fireReportError(Exception e) {
		messageEvent.setValues(MessageEvent.ERROR, e.toString());
		fireEvents(REPORT_ERROR, messageListeners);
	}
	public void fireReportError(String s) {
		messageEvent.setValues(MessageEvent.ERROR, s);
		fireEvents(REPORT_ERROR, messageListeners);
	}
	public void fireReportWarning(String s) {
		messageEvent.setValues(MessageEvent.WARNING, s);
		fireEvents(REPORT_WARNING, messageListeners);
	}
	public boolean fireSemanticPredicateEvaluated(int type, int condition, boolean result, int guessing) {
		semPredEvent.setValues(type, condition, result, guessing);
		fireEvents(SEMPRED, semPredListeners);
		return result;
	}
	public void fireSyntacticPredicateFailed(int guessing) {
		synPredEvent.setValues(0, guessing);
		fireEvents(SYNPRED_FAILED, synPredListeners);
	}
	public void fireSyntacticPredicateStarted(int guessing) {
		synPredEvent.setValues(0, guessing);
		fireEvents(SYNPRED_STARTED, synPredListeners);
	}
	public void fireSyntacticPredicateSucceeded(int guessing) {
		synPredEvent.setValues(0, guessing);
		fireEvents(SYNPRED_SUCCEEDED, synPredListeners);
	}
	protected void refresh(Vector listeners) {
		Vector v;
		synchronized (listeners) {
			v = (Vector)listeners.clone();
		}
		if (v != null)
			for (int i = 0; i < v.size(); i++)
				((ListenerBase)v.elementAt(i)).refresh();
	}
	public void refreshListeners() {
		refresh(matchListeners);
		refresh(messageListeners);
		refresh(tokenListeners);
		refresh(traceListeners);
		refresh(semPredListeners);
		refresh(synPredListeners);
	}
	public void removeDoneListener(ListenerBase l) {
		if (doneListeners == null) return;
		Integer i = (Integer)doneListeners.get(l);
		int val=0;
		if (i != null)
			val = i.intValue() - 1;

		if (val == 0) 
			doneListeners.remove(l);
		else
			doneListeners.put(l, new Integer(val));
	}
	public void removeMessageListener(MessageListener l) {
		if (messageListeners != null)
			messageListeners.removeElement(l);
		removeDoneListener(l);
	}
	public void removeNewLineListener(NewLineListener l) {
		if (newLineListeners != null)
			newLineListeners.removeElement(l);
		removeDoneListener(l);
	}
	public void removeParserListener(ParserListener l) {
		removeParserMatchListener(l);
		removeMessageListener(l);
		removeParserTokenListener(l);
		removeTraceListener(l);
		removeSemanticPredicateListener(l);
		removeSyntacticPredicateListener(l);
	}
	public void removeParserMatchListener(ParserMatchListener l) {
		if (matchListeners != null)
			matchListeners.removeElement(l);
		removeDoneListener(l);
	}
	public void removeParserTokenListener(ParserTokenListener l) {
		if (tokenListeners != null)
			tokenListeners.removeElement(l);
		removeDoneListener(l);
	}
	public void removeSemanticPredicateListener(SemanticPredicateListener l) {
		if (semPredListeners != null)
			semPredListeners.removeElement(l);
		removeDoneListener(l);
	}
	public void removeSyntacticPredicateListener(SyntacticPredicateListener l) {
		if (synPredListeners != null)
			synPredListeners.removeElement(l);
		removeDoneListener(l);
	}
	public void removeTraceListener(TraceListener l) {
		if (traceListeners != null)
			traceListeners.removeElement(l);
		removeDoneListener(l);
	}
}
