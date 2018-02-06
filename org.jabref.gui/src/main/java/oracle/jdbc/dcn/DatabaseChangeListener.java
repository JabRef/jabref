package oracle.jdbc.dcn;

/**
 * A mocking class used as a placeholder for the real Oracle JDBC drivers to prevent build errors.
 */
public interface DatabaseChangeListener {

    public void onDatabaseChangeNotification(DatabaseChangeEvent event);
}
