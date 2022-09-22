/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.migration.validator.api;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * A data class defining all report wide summary information
 *
 * @author dbernstein
 * @author awoods
 * @since 2020-12-17
 */
public class ValidationResultsSummary {

    // Object-id to report filename map
    private final Map<String, ObjectReportSummary> objectReports = new HashMap<>();
    private ObjectReportSummary repositoryReport;

    /**
     * Setter for collecting ObjectReport filenames
     * @param objectId of the provided report
     * @param objectReportSummary of generated HTML report
     */
    public void addObjectReport(final String objectId, final ObjectReportSummary objectReportSummary) {
        if (containsReport(objectId)) {
            throw new IllegalArgumentException("Should not be overwriting existing report: " + objectId);
        }

        objectReports.put(objectId, objectReportSummary);
    }

    public boolean containsReport(final String objectId) {
        return objectReports.containsKey(objectId);
    }

    /**
     * Getter for collection of ObjectReportSummary
     * @return collection of ObjectReportSummary
     */
    public Collection<ObjectReportSummary> getObjectReports() {
        return objectReports.values();
    }

    /**
     * Setter for the repository level ObjectReportSummary
     * @param repositoryReport the report
     */
    public void addRepositoryReport(final ObjectReportSummary repositoryReport) {
        this.repositoryReport = repositoryReport;
    }

    public ObjectReportSummary getRepositoryReport() {
        return repositoryReport;
    }
}
