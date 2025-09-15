package org.jabref.model.metadata;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserHostInfoTest {

    @ParameterizedTest
    @CsvSource({
            "user, host, user-host",
            "user, '', user",
            "user-with-hyphen, host, user-with-hyphen-host",
            "user, host-with-hyphen, user-host-with-hyphen",
            "complex-user-name, complex-host-name, complex-user-name-complex-host-name"
    })
    void getUserHostString(String user, String host, String expected) {
        UserHostInfo userHostInfo = new UserHostInfo(user, host);
        assertEquals(expected, userHostInfo.getUserHostString());
    }

    @ParameterizedTest
    @CsvSource({
            "user-host, user, host",
            "user, user, ''",
            "user-with-hyphen-host, user-with-hyphen, host",
            "user-host-with-hyphen, user-host-with, hyphen",
            "complex-user-name-complex-host-name, complex-user-name-complex-host, name"
    })
    void parse(String userHostString, String expectedUser, String expectedHost) {
        UserHostInfo userHostInfo = UserHostInfo.parse(userHostString);
        assertEquals(expectedUser, userHostInfo.user());
        assertEquals(expectedHost, userHostInfo.host());
    }

    @Test
    void hasSameHostReturnsTrueForSameNonEmptyHost() {
        UserHostInfo userHost1 = new UserHostInfo("user1", "host");
        UserHostInfo userHost2 = new UserHostInfo("user2", "host");

        assertTrue(userHost1.hasSameHost(userHost2));
        assertTrue(userHost2.hasSameHost(userHost1));
    }

    @Test
    void hasSameHostReturnsFalseForDifferentHosts() {
        UserHostInfo userHost1 = new UserHostInfo("user", "host1");
        UserHostInfo userHost2 = new UserHostInfo("user", "host2");

        assertFalse(userHost1.hasSameHost(userHost2));
        assertFalse(userHost2.hasSameHost(userHost1));
    }

    @Test
    void hasSameHostReturnsFalseForEmptyHost() {
        UserHostInfo userHostWithEmptyHost = new UserHostInfo("user", "");
        UserHostInfo userHostWithHost = new UserHostInfo("user", "host");

        assertFalse(userHostWithEmptyHost.hasSameHost(userHostWithHost));
        assertFalse(userHostWithHost.hasSameHost(userHostWithEmptyHost));
    }

    @Test
    void hasSameHostReturnsFalseForBothEmptyHosts() {
        UserHostInfo userHost1 = new UserHostInfo("user1", "");
        UserHostInfo userHost2 = new UserHostInfo("user2", "");

        assertFalse(userHost1.hasSameHost(userHost2));
        assertFalse(userHost2.hasSameHost(userHost1));
    }

    @Test
    void toStringReturnsUserHostString() {
        UserHostInfo userHostInfo = new UserHostInfo("user", "host");
        assertEquals("user-host", userHostInfo.toString());

        UserHostInfo userHostInfoWithEmptyHost = new UserHostInfo("user", "");
        assertEquals("user", userHostInfoWithEmptyHost.toString());
    }
}
