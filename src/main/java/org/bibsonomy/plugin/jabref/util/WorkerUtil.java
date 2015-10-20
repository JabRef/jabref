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

package org.bibsonomy.plugin.jabref.util;

import net.sf.jabref.AbstractWorker;
import net.sf.jabref.CallBack;
import net.sf.jabref.Worker;

/**
 * {@link WorkerUtil} runs a worker asynchronouslys
 * @author Waldemar Biller <biller@cs.uni-kassel.de>
 *
 */
public class WorkerUtil {
	
	public static void performAsynchronously(AbstractWorker worker) throws Throwable {

		Worker wrk = worker.getWorker();
		CallBack cb = worker.getCallBack();
			
		worker.init();
			
		wrk.run();
			
		cb.update();
	}
}