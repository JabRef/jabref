package gnu.dtools.ritopt;

/**
 * SimpleProcess.java
 *
 * Version:
 *   $Id$
 */

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;

/**
 * A SimpleProcess is used to execute a shell process, and redirect an
 * input stream to the processes' standard input, as well as redirect
 * the processes' standard output/error to an output stream. The processes
 * is multithreaded to prevent deadlock.<p>
 *
 * The example below demonstrates the use of this class.
 * <pre>
 *  class ExecuteProcess {
 *       public static void main( String args[] ) {
 *           if ( args.length > 0 ) {
 *               String processName = args[ 0 ];
 *               try {
 *                   SimpleProcess process
 *                      = new SimpleProcess( Runtime.getRuntime.exec(
 *                                                            processName ) );
 *                                          );
 *                   int exitStatus = process.waitFor();
 *                   System.out.println( "The process ran successfully" 
 *                                       + " with an exit status of "
 *                                       + exitStatus + "." );
 *               }
 *               catch ( Exception e ) {
 *                   System.out.println( "The process was not successful. "
 *                                       + " Reason: " + e.getMessage() );
 *               }
 *           }
 *           else {
 *               System.err.println( "Please specify a command" );
 *           }
 *       }
 *  }
 * </pre>
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

public class SimpleProcess extends Process {

    /**
     * The target process.
     */

    private Process process;

    /**
     * The input stream that the processes' standard input will use.
     */

    private InputStream processInput;

    /**
     * The print stream to redirect to.
     */

    private PrintStream yourOutput;

    /**
     * The print stream to redirect to.
     */

    private PrintStream yourError;

    /**
     * The StreamPrinters.
     */

    private StreamPrinter in, out, error;

    /**
     * Constructs a SimpleProcess, redirecting System.in to the its standard
     * input, System.out to its standard output, and System.err to its standard
     * error.
     */

    public SimpleProcess( Process process ) throws IOException {
	this( process, System.in, System.out, System.err );
    }

    /**
     * Constructs a SimpleProcess, initializing it with the streams passed.
     *
     * @param process       The target process.
     * @param processInput  The stream that is redirected to the
     *                      processes' standard input.
     * @param processOutput The stream to redirect the processes's
     *                      standard output.
     * @param processError  The stream to redirect the processes's
     *                      standard input.
     */

    public SimpleProcess( Process process, InputStream processInput,
			  PrintStream yourOutput, PrintStream yourError )
                         throws IOException {
	super();
	this.process = process;
	this.processInput = processInput;
	this.yourOutput = yourOutput;
	this.yourError = yourError;
    }

    /**
     * Returns the standard input of this process.
     *
     * @return The standard input of this process.
     */

    public OutputStream getOutputStream() {
	return process.getOutputStream();
    }

    /**
     * Returns the standard output of this process.
     *
     * @return The standard output of this process.
     */

    public InputStream getInputStream() {
	return process.getInputStream();
    }

    /**
     * Returns the standard error of this process.
     *
     * @return The standard error of this process.
     */

    public InputStream getErrorStream() {
	return process.getErrorStream();
    }

    /**
     * Begin redirecting the streams passed. This method should be invoked
     * immediately after execution of a simple process to prevent thread
     * deadlock.
     *
     * @return The exit status of the target process.
     */

    public int waitFor() throws InterruptedException {
	int retval = waitForImpl();
	if ( in != null ) {
	    in.stop();
	}
	return retval;
    }

    /**
     * Contains the implementation of wait for.
     *
     * @return The exit status of the target process.
     */

    private int waitForImpl() throws InterruptedException {
		in = new StreamPrinter( processInput,
					new PrintStream( process.getOutputStream() ) );
		in.setFlush( true );
		out = new StreamPrinter( process.getInputStream(), yourOutput );
		error = new StreamPrinter( process.getErrorStream(), yourError );
		in.start();
		out.start();
		error.start();
		out.join();
		error.join();
		return process.waitFor();
    }

    /**
     * Returns the target processes' exit value.
     *
     * @return This processes' exit value.
     */

    public int exitValue() {
	return process.exitValue();
    }

    /**
     * Destroys the target process.
     */

    public void destroy() throws IllegalThreadStateException {
	process.destroy();
    }
} /** SimpleProcess **/
