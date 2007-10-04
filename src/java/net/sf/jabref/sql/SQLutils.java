/*
 * SQLutils.java
 *
 * Created on October 4, 2007, 5:28 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package net.sf.jabref.sql;

import java.sql.*;

/**
 *
 * @author pattonlk
 */
public class SQLutils {
    
    public static Connection connect_mysql(String url, String username, String password)
        throws Exception {
    /**
     * This routine accepts the location of a MySQL database specified as a url as 
     * well as the username and password for the MySQL user with appropriate access
     * to this database.  The routine returns a valid Connection object if the MySQL 
     * database is successfully opened. It returns a null object otherwise.
     */

        Class.forName ("com.mysql.jdbc.Driver").newInstance ();
        Connection conn = DriverManager.getConnection (url,username,password);
              
        return conn;

    }
}
