package oracle.jdbc.dcn;

/**
 * Mocking interface
 */
public interface DatabaseChangeListener {

    public void onDatabaseChangeNotification(DatabaseChangeEvent event);
}
