package gnu.dtools.ritopt;

/**
 * Implementors are capable of being stopped. This interface is used
 * by the StreamPrinters in the SimpleProcess class so that it if an
 * error has occurred.
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

public interface Stoppable {

    /**
     * Stop the implemetor from performing some sort of processing.
     */

    public void stop();

    /**
     * Returns whether the implementor has stopped.
     *
     * @return A boolean value.
     */

    public boolean isStopped();
}
