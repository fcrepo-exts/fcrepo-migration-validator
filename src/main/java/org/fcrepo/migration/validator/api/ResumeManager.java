/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.migration.validator.api;

/**
 * Interface defining methods for allowing PIDs from previous runs to be skipped and tracking the processing of PIDs
 * in current runs
 *
 * @author mikejritter
 */
public interface ResumeManager {

    /**
     * Get the total number of PIDs which have been tested through {@link ResumeManager#accept}
     *
     * @return the number of PIDs
     */
    long totalProcessed();

    /**
     * Update the resume file with all PIDs which have been processed successfully
     */
    void updateResumeFile();

    /**
     * Mark that a PID has been processed without exceptions
     *
     * @param pid the PID
     */
    void completed(final String pid);

    /**
     * Test if a PID should be processed
     *
     * @param pid the PID
     * @return true if the PID should be processed
     */
    boolean accept(final String pid);

}
