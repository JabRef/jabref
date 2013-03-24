package gnu.dtools.ritopt;

/**
 * OptionModuleRegistrar.java
 *
 * Version:
 *   $Id$
 */

/**
 * Implementors are capable of registering option modules and storing them in a
 * repository. A parent object may pass its child a reference to an
 * OptionModuleRegistrar to preserve abstraction and constrain access to
 * registration. This may be preferred so that children may only
 * register their OptionModules without performing any administrating the
 * repository.<p>
 *
 * The Options class implements this interface. It is not necessary to refer
 * to instances as an OptionModuleRegistrar.<p>
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

public interface OptionModuleRegistrar {

    /**
     * Register an option module based on its name.
     *
     * @param module The option module to register.
     */

    public void register( OptionModule module );

    /**
     * Register an option module and associate it with the name passed.
     *
     * @param name   The name associated with the option module.
     * @param module The option module to register.
     */

    public void register( String name, OptionModule module );
}
