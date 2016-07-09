package net.sf.jabref.remote;


public class TestConnectionData {

    private String host;
    private String database;
    private String user;
    private String passord;

    public TestConnectionData() {
        // no data
    }

    public TestConnectionData(String host, String database, String user, String password) {
        this.host = host;
        this.database = database;
        this.user = user;
        this.passord = password;
    }

    public String getHost() {
        return host;
    }

    public String getDatabase() {
        return database;
    }

    public String getUser() {
        return user;
    }

    public String getPassord() {
        return passord;
    }
}
