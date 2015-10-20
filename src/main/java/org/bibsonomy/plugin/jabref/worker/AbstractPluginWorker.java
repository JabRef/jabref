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

package org.bibsonomy.plugin.jabref.worker;

import net.sf.jabref.AbstractWorker;
import net.sf.jabref.JabRefFrame;

import org.bibsonomy.model.logic.LogicInterface;
import org.bibsonomy.plugin.jabref.PluginProperties;
import org.bibsonomy.plugin.jabref.util.JabRefFileFactory;
import org.bibsonomy.rest.client.RestLogicFactory;
import org.bibsonomy.rest.client.util.FileFactory;

/**
 * {@link AbstractPluginWorker} is the base for all Workers which need to support stopping execution.
 * @author Waldemar Biller <biller@cs.uni-kassel.de>
 *
 */
public abstract class AbstractPluginWorker extends AbstractWorker {

	private boolean fetchNext = true;
	private final FileFactory fileFactory;
	protected final JabRefFrame jabRefFrame;
	
	public AbstractPluginWorker(JabRefFrame jabRefFrame) {
		this.jabRefFrame = jabRefFrame;
		this.fileFactory = new JabRefFileFactory(jabRefFrame);
	}
	
	public synchronized void stopFetching() {
		
		fetchNext = false;
	}
	
	protected synchronized boolean fetchNext() {
		
		return fetchNext;
	}
	
	protected LogicInterface getLogic() {
		return new RestLogicFactory(PluginProperties.getApiUrl(), RestLogicFactory.DEFAULT_RENDERING_FORMAT, RestLogicFactory.DEFAULT_CALLBACK_FACTORY, fileFactory).getLogicAccess(PluginProperties.getUsername(), PluginProperties.getApiKey());
	}
}
