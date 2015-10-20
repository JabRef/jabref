/**
 *  
 *  JabRef Bibsonomy Plug-in - Plugin for the reference management 
 * 		software JabRef (http://jabref.sourceforge.net/) 
 * 		to fetch, store and delete entries from BibSonomy.
 *   
 *  Copyright (C) 2008 - 2011 Knowledge & Data Engineering Group, 
 *                            University of Kassel, Germany
 *                            http://www.kde.cs.uni-kassel.de/
 *  
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License
 *  as published by the Free Software Foundation; either version 2
 *  of the License, or (at your option) any later version.
 * 
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *  
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.bibsonomy.plugin.jabref.action;

import javax.swing.AbstractAction;
import javax.swing.Icon;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bibsonomy.plugin.jabref.util.WorkerUtil;

import net.sf.jabref.AbstractWorker;
import net.sf.jabref.JabRefFrame;

/**
 * {@link AbstractPluginAction} is the base class for all actions.
 * Provides a method to run workers asynchronously.
 * @author Waldemar Biller <biller@cs.uni-kassel.de>
 *
 */
public abstract class AbstractPluginAction extends AbstractAction {

	private static final long serialVersionUID = -5607100284690271238L;

	private static final Log LOG = LogFactory.getLog(SearchAction.class);

	/**
	 * the jabRefFrame
	 */
	private JabRefFrame jabRefFrame;
	
	/**
	 * creates a new Action with the parameters.
	 * @param jabRefFrame
	 * @param text
	 * @param icon
	 */
	public AbstractPluginAction(JabRefFrame jabRefFrame, String text, Icon icon) {
		
		super(text, icon);
		this.jabRefFrame = jabRefFrame;
	}
	
	/**
	 * Creates a action without text and icon
	 * @param jabRefFrame
	 */
	public AbstractPluginAction(JabRefFrame jabRefFrame) {
		
		super();
		this.jabRefFrame = jabRefFrame;
	}
	
	/**
	 * Runs a worker asynchronously. Includes exception handling.
	 * @param worker the worker to be run asynchronously
	 */
	protected void performAsynchronously(AbstractWorker worker) {
		
		try {
			
			WorkerUtil.performAsynchronously(worker);
		} catch (Throwable t) {
			
			jabRefFrame.unblock();
			LOG.error("Failed to initialize Worker", t);
			t.printStackTrace();
		}
	}

	/**
	 * get the jabRefFrame
	 * @return the {@link JabRefFrame}
	 */
	protected JabRefFrame getJabRefFrame() {
	
		return jabRefFrame;
	}
}
