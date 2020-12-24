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

import org.slf4j.Logger;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * A data class defining all report wide summary information
 *
 * @author dbernstein
 * @author awoods
 * @since 2020-12-17
 */
public class ValidationResultsSummary {

    private static final Logger LOGGER = getLogger(ValidationResultsSummary.class);

    // Object-id to report filename map
    private Map<String, String> objectReportFilenames = new HashMap<>();

    /**
     * Setter for collecting ObjectReport filenames
     * @param objectId of the provided report
     * @param objectReportFilename of generated HTML report
     */
    public void addObjectReport(final String objectId, final String objectReportFilename) {
        if (containsReport(objectId)) {
            throw new IllegalArgumentException("Should not be overwriting existing report: " + objectId);
        }

        LOGGER.debug("Adding report for object: {}, {}", objectId,  objectReportFilename);
        objectReportFilenames.put(objectId, objectReportFilename);
    }

    public boolean containsReport(final String objectId) {
        return objectReportFilenames.containsKey(objectId);
    }

    /**
     * Getter for collection of ObjectReport filenames
     * @return collection of ObjectReport filenames
     */
    public Collection<String> getObjectReportFilenames() {
        return objectReportFilenames.values();
    }
}
