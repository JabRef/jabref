package org.jabref.logic.sharelatex;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

import org.jabref.model.sharelatex.ShareLatexProject;

import org.junit.Test;

import static org.junit.Assert.assertFalse;

public class ShareLatexManagerTest {

    @Test
    public void test() throws URISyntaxException {

        List<ShareLatexProject> projects;
        try {
            ShareLatexManager manager = new ShareLatexManager();
            manager.login("http://192.168.1.248", "joe@example.com", "test");

            //  manager.login("https://www.sharelatex.com", "email ", "password" );

            projects = manager.getProjects();
            assertFalse(projects.isEmpty());
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }
}
