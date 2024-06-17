/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.migration.validator.api;

import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A data class that defines all object level validation details available to report generators.
 *
 * @author dbernstein
 * @author awoods
 * @since 2020-12-17
 */
public class ObjectValidationResults {

    /*
     * Validation result which is the subject of this report
     */
    private final List<ValidationResult> results;

    /**
     * Constructor
     * @param results for the object which is the subject of this report
     */
    public ObjectValidationResults(final List<ValidationResult> results) {
        this.results = results;
    }

    /**
     * This method returns the ID of the object about which the report applies
     * @return the object-id
     */
    public String getObjectId() {
        if (results == null || results.isEmpty()) {
            return "unknown";
        }

        return results.stream().findFirst().map(ValidationResult::getSourceObjectId).orElse("unknown");
    }

    /**
     * This method returns an encoded version of the object id for use as a filesystem path
     * @return the encoded object-id
     */
    public String getEncodedObjectId() {
        if (results == null || results.isEmpty()) {
            return "unknown";
        }

        return results.stream().findFirst()
                      .map(ValidationResult::getSourceObjectId)
                      .map(objectId -> URLEncoder.encode(objectId, Charset.defaultCharset()))
                      .orElse("unknown");
    }

    /**
     * This method indicates if the result set has any errors
     * @return true if there are validation errors
     */
    public boolean hasErrors() {
        // No error is no results
        if (results == null || results.isEmpty()) {
            return false;
        }

        // Return 'true' if any result has an error.
        for (final ValidationResult vr : results) {
            if (vr.getStatus().equals(ValidationResult.Status.FAIL)) {
                return true;
            }
        }

        // All good.
        return false;
    }

    /**
     * @return all ValidationResults
     */
    public List<ValidationResult> getResults() {
        return results;
    }

    /**
     * Return all results with an OK validation status
     * @return list of all passed validations
     */
    public List<ValidationResult> getPassed() {
        return results.stream()
                      .filter(result -> result.getStatus().equals(ValidationResult.Status.OK))
                      .collect(Collectors.toList());
    }

    /**
     * This method returns all results with a FAIL validation status.
     * @return list of validation errors or empty list if no errors
     */
    public List<ValidationResult> getErrors() {
        return results.stream()
                      .filter(result -> result.getStatus().equals(ValidationResult.Status.FAIL))
                      .collect(Collectors.toList());
    }
}
