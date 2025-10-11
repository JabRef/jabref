package org.jabref.model.metadata;

import org.jspecify.annotations.NullMarked;

/**
 * Record to represent user and host information.
 * This is used to identify the user when retrieving file directories.
 */
@NullMarked
public record UserHostInfo(
        String user,
        String host) {
    /**
     * Creates a new UserHostInfo from a user-host string.
     * The user-host string is expected to be in the format "user-host".
     * If the string does not contain a hyphen, the entire string is considered as the user and the host is empty.
     *
     * @param userHostString the user-host string
     * @return a new UserHostInfo
     */
    public static UserHostInfo parse(String userHostString) {
        if (userHostString.contains("-")) {
            int index = userHostString.lastIndexOf('-');
            String host = userHostString.substring(index + 1);
            String user = userHostString.substring(0, index);
            return new UserHostInfo(user, host);
        } else {
            return new UserHostInfo(userHostString, "");
        }
    }

    /**
     * Returns the user-host string representation.
     * If the host is empty, only the user is returned.
     * Otherwise, the format is "user-host".
     *
     * @return the user-host string
     */
    public String getUserHostString() {
        if (host.isEmpty()) {
            return user;
        } else {
            return user + "-" + host;
        }
    }

    /**
     * Checks if this UserHostInfo has the same host as the given UserHostInfo.
     *
     * @param other the other UserHostInfo
     * @return true if the hosts are the same and not empty, false otherwise
     */
    public boolean hasSameHost(UserHostInfo other) {
        return !host.isEmpty() && !other.host.isEmpty() && host.equals(other.host);
    }

    @Override
    public String toString() {
        return getUserHostString();
    }
}
