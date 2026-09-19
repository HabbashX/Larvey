package com.habbashx.larvey.example;

import com.habbashx.larvey.annotations.LarveyConfig;
import com.habbashx.larvey.annotations.LarveyProperty;

@LarveyConfig("app")
public class ApplicationConfig {
    @LarveyProperty("name")
    private String name;
    private String version;
    private boolean debug;
    private ServerConfig server;
    private DatabaseConfig database;

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public boolean isDebug() {
        return debug;
    }

    public ServerConfig getServer() {
        return server;
    }

    public DatabaseConfig getDatabase() {
        return database;
    }

    public static class ServerConfig {
        private String host;
        private int port;

        public String getHost() {
            return host;
        }

        public int getPort() {
            return port;
        }
    }

    public static class DatabaseConfig {
        private String driver;
        private String host;
        private int port;
        private String database;
        private String username;
        private String password;

        public String getDriver() {
            return driver;
        }

        public String getHost() {
            return host;
        }

        public int getPort() {
            return port;
        }

        public String getDatabase() {
            return database;
        }

        public String getUsername() {
            return username;
        }

        public String getPassword() {
            return password;
        }
    }
}
