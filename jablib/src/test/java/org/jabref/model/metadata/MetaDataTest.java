package org.jabref.model.metadata;

import java.nio.file.Path;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MetaDataTest {

    private MetaData metaData;

    @BeforeEach
    void setUp() {
        metaData = new MetaData();
    }

    @Test
    void emptyGroupsIfNotSet() {
        assertEquals(Optional.empty(), metaData.getGroups());
    }
    
    @Test
    void getLatexFileDirectoryReturnsEmptyWhenNotSet() {
        assertEquals(Optional.empty(), metaData.getLatexFileDirectory("user-host"));
    }
    
    @Test
    void getLatexFileDirectoryReturnsPathForExactUserHostMatch() {
        String userHost = "user-host";
        Path expectedPath = Path.of("/path/to/latex");
        metaData.setLatexFileDirectory(userHost, expectedPath);
        
        Optional<Path> result = metaData.getLatexFileDirectory(userHost);
        
        assertTrue(result.isPresent());
        assertEquals(expectedPath, result.get());
    }
    
    @Test
    void getLatexFileDirectoryReturnsPathForSameHostButDifferentUser() {
        String originalUserHost = "user1-host";
        String newUserHost = "user2-host";
        Path expectedPath = Path.of("/path/to/latex");
        metaData.setLatexFileDirectory(originalUserHost, expectedPath);
        
        Optional<Path> result = metaData.getLatexFileDirectory(newUserHost);
        
        assertTrue(result.isPresent());
        assertEquals(expectedPath, result.get());
    }
    
    @Test
    void getLatexFileDirectoryReturnsEmptyForDifferentHost() {
        String userHost1 = "user-host1";
        Path path1 = Path.of("/path/for/host1");
        metaData.setLatexFileDirectory(userHost1, path1);
        
        Optional<Path> result = metaData.getLatexFileDirectory("user-host2");
        
        assertEquals(Optional.empty(), result);
    }
    
    @Test
    void getLatexFileDirectoryHandlesMultipleEntriesCorrectly() {
        String userHost1 = "user1-host1";
        String userHost2 = "user2-host2";
        String userHost3 = "user3-host1";
        
        Path path1 = Path.of("/path/for/host1/user1");
        Path path2 = Path.of("/path/for/host2");
        Path path3 = Path.of("/path/for/host1/user3");
        
        metaData.setLatexFileDirectory(userHost1, path1);
        metaData.setLatexFileDirectory(userHost2, path2);
        metaData.setLatexFileDirectory(userHost3, path3);
        
        assertEquals(path1, metaData.getLatexFileDirectory(userHost1).get());
        assertEquals(path2, metaData.getLatexFileDirectory(userHost2).get());
        assertEquals(path3, metaData.getLatexFileDirectory(userHost3).get());
        
        String newUserOnHost1 = "newuser-host1";
        Optional<Path> resultForNewUser = metaData.getLatexFileDirectory(newUserOnHost1);
        assertTrue(resultForNewUser.isPresent());
        
        assertEquals(Optional.empty(), metaData.getLatexFileDirectory("user-differenthost"));
    }
}
