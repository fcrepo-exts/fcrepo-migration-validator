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

/**
 * A data class that defines all object level validation details available to report generators.
 *
 * @author dbernstein
 * @author awoods
 * @since 2020-12-17
 */
public class ObjectValidationReport {

    /*
     * Validation result which is the subject of this report
     */
    private ValidationResult result;

    /**
     * Constructor
     * @param result of the object which is the subject of this report
     */
    public ObjectValidationReport(final ValidationResult result) {
        this.result = result;
    }

    /**
     * This method returns the ID of the object about which the report applies
     * @return the object-id
     */
    public String getObjectId() {
        return result.sourceId();
    }

    /**
     * This method indicates if the result has any errors
     * @return true if there are validation errors
     */
    public boolean hasErrors() {
        return !result.getStatus().equals(ValidationResult.Status.OK);
    }

    /**
     * This method returns the type of validation related to this report
     * @return validation type
     */
    public String getValidationType() {
        return result.getValidationType().name();
    }

    /**
     * This method returns any details related to the validation
     * @return validation details
     */
    public String getDetails() {
        return result.getDetails();
    }
}
