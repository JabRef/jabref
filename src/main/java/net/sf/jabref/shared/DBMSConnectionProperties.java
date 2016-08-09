package net.sf.jabref.shared;

/**
 * Keeps all essential data for establishing a new connection to a DBMS using {@link DBMSConnector}.
 */
public class DBMSConnectionProperties {

    private DBMSType type;
    private String host;
    private int port;
    private String database;
    private String user;
    private String password;


    public DBMSConnectionProperties() {
        // no data
    }

    public DBMSConnectionProperties(DBMSType type, String host, int port, String database, String user,
            String password) {
        this.type = type;
        this.host = host;
        this.port = port;
        this.database = database;
        this.user = user;
        this.password = password;
    }

    public DBMSType getType() {
        return type;
    }

    public void setType(DBMSType type) {
        this.type = type;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

}
