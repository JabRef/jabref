/*
 * Package package net.sf.jabref.aspects.violations;
 * Created on Nov 6, 2004
 * Author mspiegel
 *
 */
package net.sf.jabref.aspects.violations;

import java.awt.Component;
import java.awt.EventQueue;

public aspect DetectSwingSingleThreadRuleViolations {

	pointcut guiUpdates(Component component)
		:	call(* java..Component+.*(..)) && 
		    !call(* net..jabref+.*(..)) && target(component);			
    
	pointcut safeGuiCalls()
		: call (* javax..JComponent.repaint(..))
			|| call(* javax..JComponent.revalidate(..))
		    || call(* javax..JComponent.invalidate(..))
		    || call(* javax..*.add*Listener(..))
		    || call(* javax..*.remove*Listener(..));
		        
	pointcut unsafeGuiCalls(Component component)
		: guiUpdates(component) && !safeGuiCalls();
    
    before(Component component) : unsafeGuiCalls(component) 
    	&& if (!EventQueue.isDispatchThread() && (component != null) &&
    	        	component.isDisplayable() && !Thread.currentThread().getName().equals("main")) {
            System.err.println(	
                    "Violation: Swing component method called from nonAWT thread"
                    + "\nCalled method: " + thisJoinPointStaticPart.getSignature()
                    + "\nCaller:" + thisEnclosingJoinPointStaticPart.getSignature()
                    + "\nSource Location:" + thisJoinPointStaticPart.getSourceLocation()
                    + "\nThread:" + Thread.currentThread() 
                    + "\nChange code to use EventQueue.invokeLater() or EventQueue.invokeAndWait()"
                    + "\n");
    	}
    
}
