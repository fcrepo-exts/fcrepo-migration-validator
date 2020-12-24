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

import java.util.Collections;
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
    private List<ValidationResult> results;

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
        return results.stream().findFirst().get().sourceId();
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
            if (!vr.getStatus().equals(ValidationResult.Status.OK)) {
                return true;
            }
        }

        // All good.
        return false;
    }

    /**
     * This method returns any details related to the validation errors
     * @return list of validation error details, or empty list if no errors
     */
    public List<String> getErrorDetails() {
        if (!hasErrors()) {
            return Collections.emptyList();
        }

        return results.stream().filter(
                r -> !r.getStatus().equals(ValidationResult.Status.OK)).map(
                        ValidationResult::getDetails).collect(Collectors.toList());
    }
}
