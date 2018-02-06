package oracle.jdbc;

import oracle.jdbc.dcn.DatabaseChangeRegistration;

/**
 * A mocking class used as a placeholder for the real Oracle JDBC drivers to prevent build errors.
 */
public class OracleStatement {

    public void setDatabaseChangeRegistration(@SuppressWarnings("unused") DatabaseChangeRegistration registration) {
        // do nothing
    }
}
