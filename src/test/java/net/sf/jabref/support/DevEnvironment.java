package net.sf.jabref.support;

/**
 * Checks whether we are on a local development environment and not on a CI server.
 * This is needed as some remote fetcher tests are blocked by Google when executed by CI servers.
 */
public class DevEnvironment {

    public static boolean isCIServer() {
        // See http://docs.travis-ci.com/user/environment-variables/#Default-Environment-Variables
        // See https://circleci.com/docs/environment-variables
        // See https://docs.snap-ci.com/environment-variables/
        return Boolean.valueOf(System.getenv("CI"));
    }

    public static boolean isCircleCI() {
        // See https://circleci.com/docs/environment-variables
        return Boolean.valueOf(System.getenv("CIRCLECI"));
    }

    public static boolean isSnapCI() {
        // See https://docs.snap-ci.com/environment-variables/
        return Boolean.valueOf(System.getenv("SNAP_CI"));
    }

}
