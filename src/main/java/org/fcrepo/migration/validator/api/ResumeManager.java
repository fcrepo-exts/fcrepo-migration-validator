package org.fcrepo.migration.validator.api;

/**
 * Similar to PidListManager from migration-utils
 *
 * @author mikejritter
 */
public interface ResumeManager {

    long totalProcessed();
    void completed(final String pid);
    void updateResumeFile();

    boolean accept(final String pid);

}
