/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
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
        OBJECT_READABLE,
        SOURCE_OBJECT_DELETED,
        SOURCE_OBJECT_EXISTS_IN_TARGET,
        TARGET_OBJECT_EXISTS_IN_SOURCE,
        SOURCE_OBJECT_RESOURCE_DELETED,
        SOURCE_OBJECT_RESOURCE_EXISTS_IN_TARGET,
        TARGET_OBJECT_RESOURCE_EXISTS_IN_SOURCE,
        METADATA,
        BINARY_SIZE,
        BINARY_CHECKSUM,
        BINARY_VERSION_COUNT,
        BINARY_HEAD_COUNT,
        BINARY_METADATA,
        REPOSITORY_RESOURCE_COUNT
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
    private String sourceObjectId;
    @JsonProperty
    private String targetObjectId;
    @JsonProperty
    private String sourceResourceId;
    @JsonProperty
    private String targetResourceId;

    /**
     * Constructor Repository level constructor
     *
     * @param index
     * @param status
     * @param validationLevel
     * @param validationType
     * @param details
     */
    public ValidationResult(final int index,
                            final Status status,
                            final ValidationLevel validationLevel,
                            final ValidationType validationType,
                            final String details) {
        this(index, status, validationLevel, validationType, null, null,details);
    }


    /**
     * Constructor Object level constructor
     *
     * @param index
     * @param status
     * @param validationLevel
     * @param validationType
     * @param sourceObjectId
     * @param targetObjectId
     * @param details
     */
    public ValidationResult(final int index,
                            final Status status,
                            final ValidationLevel validationLevel,
                            final ValidationType validationType,
                            final String sourceObjectId,
                            final String targetObjectId,
                            final String details) {
        this(index, status, validationLevel, validationType, sourceObjectId, targetObjectId,
                null, null, details);
    }

    /**
     * Constructor
     *
     * @param index
     * @param status
     * @param validationLevel
     * @param validationType
     * @param sourceObjectId
     * @param targetObjectId
     * @param sourceResourceId
     * @param targetResourceId
     * @param details
     */
    public ValidationResult(final int index,
                            final Status status,
                            final ValidationLevel validationLevel,
                            final ValidationType validationType,
                            final String sourceObjectId,
                            final String targetObjectId,
                            final String sourceResourceId,
                            final String targetResourceId,
                            final String details) {
        this.index = index;
        this.status = status;
        this.validationLevel = validationLevel;
        this.validationType = validationType;
        this.sourceObjectId = sourceObjectId;
        this.targetObjectId = targetObjectId;
        this.sourceResourceId = sourceResourceId;
        this.targetResourceId = targetResourceId;
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
    public String getSourceObjectId() {
        return this.sourceObjectId;
    }

    /**
     * The target object ID (ie the OCFL root object)
     *
     * @return The target ID
     */
    public String getTargetObjectId() {
        return this.targetObjectId;
    }

    /**
     * The full resource ID of a source resource. Returns non-null for OBJECT_RESOURCE level results, otherwise null.
     *
     * @return The resource ID
     */
    public String getSourceResourceId() {
        return sourceResourceId;
    }

    /**
     * The full resource ID of a target resource. Returns non-null for OBJECT_RESOURCE level results, otherwise null.
     *
     * @return The resource ID
     */
    public String getTargetResourceId() {
        return targetResourceId;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

}
