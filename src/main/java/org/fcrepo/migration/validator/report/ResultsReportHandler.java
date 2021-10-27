/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.migration.validator.report;

import org.fcrepo.migration.validator.api.ObjectValidationResults;
import org.fcrepo.migration.validator.api.ReportHandler;
import org.fcrepo.migration.validator.api.ValidationResult;
import org.fcrepo.migration.validator.api.ValidationResultsSummary;

import java.util.ArrayList;
import java.util.List;

/**
 * This class handles/processes validation results by collecting detected errors
 * Although there may be production uses, this class is currently designed to facilitate testing.
 *
 * @author awoods
 * @since 2020-12-20
 */
public class ResultsReportHandler implements ReportHandler {

    private List<ValidationResult> errors = new ArrayList<>();
    private List<ValidationResult> passed = new ArrayList<>();

    /**
     * A hook indicating the start of a result processing run
     */
    @Override
    public void beginReport() {

    }

    /**
     * A hook hook for processing an object level validation report
     *
     * @param objectValidationResults An individual object validation report
     * @return filename of object report
     */
    @Override
    public String objectLevelReport(final ObjectValidationResults objectValidationResults) {
        errors.addAll(objectValidationResults.getErrors());
        passed.addAll(objectValidationResults.getPassed());

        // No HTML report filename
        return null;
    }

    @Override
    public String repositoryLevelReport(final ObjectValidationResults objectValidationResults) {
        errors.addAll(objectValidationResults.getErrors());
        passed.addAll(objectValidationResults.getPassed());

        // No HTML report filename
        return null;
    }

    /**
     * A hook for processing a validation run's summary info.
     *
     * @param validationSummary to be processed
     * @return filename of full report
     */
    @Override
    public String validationSummary(final ValidationResultsSummary validationSummary) {
        return null;
    }

    /**
     * A hook indicating the end of a result processing run
     */
    @Override
    public void endReport() {

    }

    public List<ValidationResult> getErrors() {
        return errors;
    }

    public List<ValidationResult> getPassed() {
        return passed;
    }
}
