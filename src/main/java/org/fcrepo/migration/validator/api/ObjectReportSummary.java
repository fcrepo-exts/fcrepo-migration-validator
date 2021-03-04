package org.fcrepo.migration.validator.api;

/**
 * A summary of an Object Validation Report
 *
 * @author mikejritter
 */
public class ObjectReportSummary {
    private final boolean errors;
    private final String reportFilename;

    public ObjectReportSummary(final boolean errors, final String reportFilename) {
        this.errors = errors;
        this.reportFilename = reportFilename;
    }

    /**
     * @return true if the report had any errors
     */
    public boolean hasErrors() {
        return errors;
    }

    /**
     * @return the filename of the html report
     */
    public String getReportFilename() {
        return reportFilename;
    }
}
