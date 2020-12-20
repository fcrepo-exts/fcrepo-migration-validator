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

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * The result of a validation operation. A validation result corresponds to the most granular level of information
 * communicated in the validation report.
 *
 * @author dbernstein
 */
public class ValidationResult {

    public enum Status {
        OK,
        FAIL;
    }

    public enum ValidationLevel {
        REPOSITORY,
        OBJECT,
        OBJECT_RESOURCE
    }

    public enum ValidationType {
        METADATA,
        BINARY_SIZE,
        BINARY_CHECKSUM,
        BINARY_VERSION_COUNT,
        BINARY_METADATA,
        REPOSITORY_RESOURCE_COUNT;
    }

    private int index;
    @JsonProperty
    private Status status;
    @JsonProperty
    private ValidationLevel validationLevel;
    @JsonProperty
    private ValidationType validationType;
    @JsonProperty
    private String details;
    @JsonProperty
    private String sourceId;
    @JsonProperty
    private String targetId;

    /**
     * Constructor
     *
     * @param index
     * @param status
     * @param validationLevel
     * @param validationType
     * @param sourceId
     * @param targetId
     * @param details
     */
    public ValidationResult(final int index,
                            final Status status,
                            final ValidationLevel validationLevel,
                            final ValidationType validationType,
                            final String sourceId,
                            final String targetId,
                            final String details) {
        this.index = index;
        this.status = status;
        this.validationLevel = validationLevel;
        this.validationType = validationType;
        this.sourceId = sourceId;
        this.targetId = targetId;
        this.details = details;
    }

    /**
     * Default constructor
     */
    public ValidationResult() {
        // Default constructor need for Jackson to deserialize json
    }

    /**
     * An index number
     *
     * @return
     */
    public int getIndex() {
        return this.index;
    }

    /**
     * The status of the result
     *
     * @return The result status
     */
    public Status getStatus() {
        return status;
    }

    /**
     * Returns the validation level
     * @return The validation level
     */
    public ValidationLevel getValidationLevel() {
        return validationLevel;
    }

    /**
     * The validation type
     * @return The validation type
     */
    public ValidationType getValidationType() {
        return validationType;
    }

    /**
     * A detailed description of the result
     *
     * @return
     */
    public String getDetails() {
        return details;
    }

    /**
     * The source object ID
     *
     * @return The source ID
     */
    public String sourceId() {
        return this.sourceId;
    }

    /**
     * The target object ID
     *
     * @return The target ID
     */
    public String targetId() {
        return this.targetId;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

}
