/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.migration.validator.api;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A summary of an Object Validation Report
 *
 * @author mikejritter
 */
public class ObjectReportSummary {
    private final boolean errors;
    private final String objectId;
    private final String reportFilename;

    public ObjectReportSummary(final boolean errors, final String objectId, final String reportFilename) {
        this.errors = errors;
        this.objectId = objectId;
        this.reportFilename = reportFilename;
    }

    /**
     * @return true if the report has any errors
     */
    @JsonProperty("errors")
    public boolean hasErrors() {
        return errors;
    }

    /**
     * @return the objectId
     */
    @JsonProperty
    public String getObjectId() {
        return objectId;
    }

    /**
     * @return the filename of the html report
     */
    @JsonIgnore
    public String getReportFilename() {
        return reportFilename;
    }

    /**
     * @return the encoded href for the html report
     */
    @JsonIgnore
    public String getReportHref() {
        return URLEncoder.encode(reportFilename, StandardCharsets.UTF_8);
    }
}
