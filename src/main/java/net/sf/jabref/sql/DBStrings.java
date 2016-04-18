/*  Copyright (C) 2003-2016 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/
package net.sf.jabref.sql;

import net.sf.jabref.Globals;

/**
 * @author pattonlk
 */
public class DBStrings {

    private DBStringsPreferences dbPreferences;
    private String password;
    private String dbParameters = "";
    private boolean isInitialized;
    private boolean configValid;

    /**
     * Creates a new instance of DBStrings
     */
    public DBStrings() {
        this.dbPreferences = new DBStringsPreferences(null, null, null, null);
        this.setPassword(null);
        this.isInitialized(false);
        this.isConfigValid(false);
    }

    /**
     * Initializes the variables needed with defaults
     */
    public void initialize() {
        this.dbPreferences = DBStringsPreferences.loadFromPreferences(Globals.prefs);
        setPassword("");
        isInitialized(true);
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPassword() {
        return password;
    }

    public boolean isInitialized() {
        return isInitialized;
    }

    private void isInitialized(boolean isInit) {
        this.isInitialized = isInit;
    }

    public boolean isConfigValid() {
        return configValid;
    }

    public void isConfigValid(boolean confValid) {
        this.configValid = confValid;
    }

    /**
     * Returns the database parameters set
     *
     * @return dbParameters: The concatenated parameters
     */
    public String getDbParameters() {
        return dbParameters;
    }

    /**
     * Add server specific database parameter(s) <br>
     * Multiple parameters must be concatenated in the format <br>
     * {@code ?Parameter1=value&parameter2=value2}
     *
     * @param dbParameter The concatendated parameter
     */
    public void setDbParameters(String dbParameters) {
        this.dbParameters = dbParameters;
    }

    public DBStringsPreferences getDbPreferences() {
        return dbPreferences;
    }

    public void setDbPreferences(DBStringsPreferences dbPreferences) {
        this.dbPreferences = dbPreferences;
    }
}
