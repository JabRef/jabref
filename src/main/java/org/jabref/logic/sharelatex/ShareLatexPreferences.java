package org.jabref.logic.sharelatex;

import java.util.Optional;
import java.util.prefs.BackingStoreException;

public class ShareLatexPreferences {

    private final String defaultNode;
    private final String parentNode;
    private String sharelatexUser;
    private String shareLatexUrl;
    private String shareLatexPassword;
    private boolean shareLatexRememberPassword;
    private String shareLatexProject;

    public ShareLatexPreferences(String defaultNode, String parentNode, String getSharelatexUser, String shareLatexUrl, String shareLatexPassword, boolean shareLatexRememberPassword, String shareLatexProject) {
        this.defaultNode = defaultNode;
        this.parentNode = parentNode;
        this.sharelatexUser = getSharelatexUser;
        this.shareLatexUrl = shareLatexUrl;
        this.shareLatexPassword = shareLatexPassword;
        this.shareLatexRememberPassword = shareLatexRememberPassword;
        this.shareLatexProject = shareLatexProject;
    }

    public String getSharelatexUrl() {
        return getOptionalValue(shareLatexUrl).orElse("https://www.sharelatex.com");
    }

    public Optional<String> getUser() {
        return getOptionalValue(sharelatexUser);
    }

    public Optional<String> getPassword() {
        return getOptionalValue(shareLatexPassword);
    }

    public Optional<String> getDefaultProject() {
        return getOptionalValue(shareLatexProject);
    }

    public String getDefaultNode() {
        return defaultNode;
    }

    public String getParentNode() {
        return parentNode;
    }

    public boolean getShareLatexRememberPassword() {
        return shareLatexRememberPassword;
    }

    public void setSharelatexUrl(String url) {
        this.shareLatexUrl = url;
    }

    public void setSharelatexUser(String user) {
        this.sharelatexUser = user;
    }

    public void setSharelatexPassword(String pwd) {
        this.shareLatexPassword = pwd;
    }

    public void setSharelatexProject(String project) {
        this.shareLatexProject = project;
    }

    public void setRememberPassword(boolean rememberPassword) {
        this.shareLatexRememberPassword = rememberPassword;
    }

    public void clear() throws BackingStoreException {
    }

    private Optional<String> getOptionalValue(String key) {
        return Optional.ofNullable(key);
    }

}
