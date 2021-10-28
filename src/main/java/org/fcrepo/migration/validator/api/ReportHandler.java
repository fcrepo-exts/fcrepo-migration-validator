/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.migration.validator.api;

/**
 * An interface defining hooks for validation result processors implemented by report generators
 *
 * @author dbernstein
 */
public interface ReportHandler {

    /**
     * A hook indicating the start of a result processing run
     */
    void beginReport();

    /**
     * A hook for processing an object level validation report
     * @param objectValidationResults An individual object validation report
     * @return filename of object report
     */
    String objectLevelReport(ObjectValidationResults objectValidationResults);

    /**
     * A hook for processing a repository level validation report
     *
     * @param objectValidationResults An individual validation report
     * @return filename of repository report
     */
    String repositoryLevelReport(ObjectValidationResults objectValidationResults);

    /**
     * A hook for processing a validation run's summary info.
     * @param validationSummary to be processed
     * @return filename of full report
     */
    String validationSummary(ValidationResultsSummary validationSummary);

    /**
     * A hook indicating the end of a result processing run
     */
    void endReport();
}
