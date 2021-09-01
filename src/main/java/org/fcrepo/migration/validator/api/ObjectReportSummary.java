/*
 * Licensed to DuraSpace under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.
 *
 * DuraSpace licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
