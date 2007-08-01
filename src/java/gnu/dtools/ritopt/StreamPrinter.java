package gnu.dtools.ritopt;

/**
 * StreamPrinter.java
 *
 * Version:
 *   $Id$
 */

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;

/**
 * Reads data from an input stream and outputs to a print stream. This class
 * is used by the OptionMenu class to read from both standard output and
 * standard error simultaneously when a shell command is executed. Since the
 * StreamPrinter processes streams on a separate thread, deadlock is
 * prevented.<p>
 *
 * <hr>
 *
 * <pre>
 * Copyright (C) Damian Ryan Eads, 2001. All Rights Reserved.
 *
 * ritopt is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.

 * ritopt is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ritopt; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 * </pre>
 *
 * @author Damian Eads
 */

public class StreamPrinter implements Runnable, Stoppable {

    /**
     * The input stream to read from.
     */

    private InputStream stream;

    /**
     * The print stream to redirect to.
     */

    private PrintStream out;

    /**
     * The object to stop.
     */

    private Stoppable stop;

    /**
     * Whether this StreamPrinter has stopped processing.
     */

    private boolean stopped;

    /**
     * Whether the buffer should be flushed.
     */

    private boolean flush;

    /**
     * The thread associated with this StreamPrinter.
     */

    private Thread thread;

    /**
     * Constructs a new StreamPrinter.
     *
     * @param s The stream to read from.
     * @param p The stream to output to.
     */

    public StreamPrinter( InputStream s, PrintStream p ) {
	stream = s;
	out = p;
	thread = new Thread( this );
    }

    /**
     * Starts the thread associated with this StreamPrinter.
     */

    public void start() throws InterruptedException {
	thread.start();
    }

    /**
     * Sets the object to stop when this object is finished.
     *
     * @param tostop The object to stop.
     */

    public void setStop( Stoppable tostop ) {
	synchronized( this ) {
	    stop = tostop;
	}
    }

    /**
     * Returns whether this StreamPrinter has stopped processing.
     *
     * @returns A boolean value.
     */

    public boolean isStopped() {
	return stopped;
    }

    /**
     * Sets whether the output stream should be flushed after each output
     * step.
     *
     * @param b A boolean value.
     */

    public void setFlush( boolean flush ) {
	synchronized ( this ) {
	    this.flush = flush;
	}
    }

    /**
     * Stops this StreamPrinter's processing. 
     */

    public void stop() {
	synchronized( this ) {
	    stopped = true;
	}
	if ( stop != null ) {
	    synchronized( stop ) {
		if ( !stop.isStopped() ) {
		    stop.stop();
		}
	    }
	}
    }

    /**
     * Joins this StreamPrinter's thread with the other threads.
     */

    public void join() throws InterruptedException {
	thread.join();
    }

    /**
     * Start the StreamPrinter thread. This is done automatically during
     * construction.
     */

    // This implementation is lousy; buffering is needed.
    public void run() {
	int buf;
	try {
	    boolean me;
	    while ( !stopped && ( buf = stream.read() ) != -1 ) {
		synchronized( this ) {
		    me = flush;
		}
		synchronized( out ) {
		    out.print( (char)buf );
		    if ( me ) out.flush();
		}
	    }
	}
	catch ( IOException e ) {
	    out.println( "I/O error" );
	}
	finally {
	    synchronized( out ) {
		out.flush();
	    }
	    stop();
	}
    }
}
