package org.fcrepo.migration.validator.api;

/**
 * Similar to PidListManager from migration-utils
 *
 * @author mikejritter
 */
public interface ResumeManager {

    void fail(final String pid);
    void completed(final String pid);
    void updateResumeFile();

    boolean accept(final String pid);

}
