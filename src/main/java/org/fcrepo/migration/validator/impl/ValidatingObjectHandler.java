package org.fcrepo.migration.validator.impl;
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

import org.fcrepo.migration.FedoraObjectHandler;
import org.fcrepo.migration.ObjectInfo;
import org.fcrepo.migration.ObjectProperties;
import org.fcrepo.migration.ObjectReference;
import org.fcrepo.migration.validator.api.ValidationResult;
import org.fcrepo.storage.ocfl.OcflObjectSession;
import org.fcrepo.storage.ocfl.ResourceHeaders;
import org.fcrepo.storage.ocfl.exception.NotFoundException;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.lang.String.format;
import static org.fcrepo.migration.validator.api.ValidationResult.Status.FAIL;
import static org.fcrepo.migration.validator.api.ValidationResult.Status.OK;
import static org.fcrepo.migration.validator.api.ValidationResult.ValidationLevel.OBJECT;
import static org.fcrepo.migration.validator.api.ValidationResult.ValidationLevel.OBJECT_RESOURCE;
import static org.fcrepo.migration.validator.api.ValidationResult.ValidationType.BINARY_METADATA;
import static org.fcrepo.migration.validator.api.ValidationResult.ValidationType.BINARY_VERSION_COUNT;
import static org.fcrepo.migration.validator.api.ValidationResult.ValidationType.METADATA;
import static org.fcrepo.migration.validator.api.ValidationResult.ValidationType.SOURCE_OBJECT_EXISTS_IN_TARGET;
import static org.fcrepo.migration.validator.api.ValidationResult.ValidationType.SOURCE_OBJECT_RESOURCE_EXISTS_IN_TARGET;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * A streaming object handler implementation that performs object scoped validations on behalf
 * of the Fedora3ObjectValidator.
 *
 * @author dbernstein
 */
public class ValidatingObjectHandler implements FedoraObjectHandler {
    private static final Logger LOGGER = getLogger(Fedora3ObjectValidator.class);

    private ObjectInfo objectInfo;
    private OcflObjectSession ocflSession;
    private final List<ValidationResult> validationResults = new ArrayList<>();
    private int indexCounter;
    private Set<String> headDatastreamIds = new HashSet<>();

    private static final Map<String, PropertyResolver> OCFL_PROPERTY_RESOLVERS = new HashMap<>();

    static {
        OCFL_PROPERTY_RESOLVERS.put("info:fedora/fedora-system:def/model#createdDate",
                new PropertyResolver<String>() {
                    @Override
                    public String resolve(final ResourceHeaders headers) {
                        return headers.getCreatedDate().toString();
                    }
                });
    }

    private interface PropertyResolver<T> {
        T resolve(ResourceHeaders headers);
    }

    @Override
    public void processObject(final ObjectReference objectReference) {
        this.objectInfo = objectReference.getObjectInfo();
        LOGGER.debug("beginning processing on object: pid={}", this.objectInfo);
        if (initialObjectValidation(objectReference.getObjectProperties())) {
            objectReference.listDatastreamIds().forEach(dsId -> validateDatastream(dsId, objectReference));
            completeObjectValidation();
        }
    }

    /**
     * Constructor
     *
     * @param session
     */
    public ValidatingObjectHandler(final OcflObjectSession session) {
        this.ocflSession = session;
    }

    /**
     * Result the validation results after processObject has been called.
     *
     * @return
     */
    public List<ValidationResult> getValidationResults() {
        return validationResults;
    }

    /**
     * @param objectProperties
     * @return true if initial validation successful and should proceed.
     */
    private boolean initialObjectValidation(final ObjectProperties objectProperties) {
        final var pid = objectInfo.getPid();
        final var ocflId = ocflSession.ocflObjectId();

        final ResourceHeaders headers;

        try {
            headers = ocflSession.readHeaders(ocflId);
        } catch (NotFoundException ex) {
            validationResults.add(new ValidationResult(indexCounter++, FAIL, OBJECT, SOURCE_OBJECT_EXISTS_IN_TARGET,
                    pid, ocflId, "Source object not present in target repository."));
            return false;
        }

        //check that last updated date:
        objectProperties.listProperties().forEach(op -> {
            final var resolver = OCFL_PROPERTY_RESOLVERS.get(op.getName());
            String details = null;
            if (resolver != null) {
                final var sourceValue = op.getValue();
                final var targetValue = resolver.resolve(headers);
                final var result = sourceValue.equals(targetValue) ? OK : FAIL;
                if (result.equals(FAIL)) {
                    details = format("pid: %s -> properties do not match: f3 prop name=%s, source=%s, target=%s",
                            op.getName(), pid, sourceValue, targetValue);
                } else {
                    details = format("pid: %s -> props match: f3 prop name=%s, source=%s, target=%s",
                            op.getName(), pid, sourceValue, targetValue);
                }
                LOGGER.info("PID = {}, object property: name = {}, value = {}", pid, op.getName(), op.getValue());
                validationResults.add(new ValidationResult(indexCounter++, result, OBJECT, METADATA,
                        pid, ocflId, details));

                validationResults.add(new ValidationResult(indexCounter++, OK, OBJECT, SOURCE_OBJECT_EXISTS_IN_TARGET,
                        pid, ocflId, "Source object not present in target repository."));

            }
        });

        return true;
    }

    public void validateDatastream(final String dsId, final ObjectReference objectReference) {
        final var dsVersions = objectReference.getDatastreamVersions(dsId);
        final var sourceOjbectId = objectInfo.getPid();
        final var targetObjectId = this.ocflSession.ocflObjectId();
        final var sourceResource = sourceOjbectId + "/" + dsId;
        final var targetResource = targetObjectId + "/" + dsId;
        var sourceVersionCount = 0;
        for (final var dsVersion : dsVersions) {
            if (dsVersion.isLastVersionIn(objectReference)) {
                final var dsInfo = dsVersion.getDatastreamInfo();
                if (!dsInfo.getState().equals("D")) {
                    headDatastreamIds.add(dsId);
                }

                try {
                    final var headers = this.ocflSession.readHeaders(targetResource);
                    final String details;
                    final var sourceValue = dsVersion.getCreated();
                    final var targetValue = headers.getCreatedDate().toString();
                    final var result = sourceValue.equals(targetValue) ? OK : FAIL;
                    if (result.equals(OK)) {
                        details = format("HEAD binary creation dates match: %s", sourceValue);
                    } else {
                        details = format("HEAD binary creation dates do no match: sourceValue=%s, target value=%s",
                                sourceValue, targetValue);
                    }
                    validationResults.add(new ValidationResult(indexCounter++, result, OBJECT_RESOURCE,
                            BINARY_METADATA, sourceOjbectId, targetObjectId, sourceResource, targetResource, details));
                    validationResults.add(new ValidationResult(indexCounter++, OK, OBJECT_RESOURCE,
                            SOURCE_OBJECT_RESOURCE_EXISTS_IN_TARGET, sourceOjbectId, targetObjectId, sourceResource,
                            targetResource, "Source object resource exists in target."));
                } catch (NotFoundException ex) {
                    validationResults.add(new ValidationResult(indexCounter++, FAIL, OBJECT_RESOURCE,
                            SOURCE_OBJECT_RESOURCE_EXISTS_IN_TARGET, sourceOjbectId, targetObjectId, sourceResource,
                            targetResource, "Source object resource does not exist in target."));

                }
            }

            sourceVersionCount++;
        }

        try {
            final var targetVersionCount = this.ocflSession.listVersions(targetResource).size();
            final var ok = sourceVersionCount == targetVersionCount;
            var details = format("binary version counts match for resource: %s", sourceVersionCount);
            if (!ok) {
                details = format("binary version counts do not match: source=%d, target=%d", sourceVersionCount,
                        targetVersionCount);
            }
            validationResults.add(new ValidationResult(indexCounter++, ok ? OK : FAIL, OBJECT_RESOURCE,
                    BINARY_VERSION_COUNT, sourceOjbectId, targetObjectId, sourceResource, targetResource, details));
        } catch (NotFoundException ex) {
            // intentionally left blank: we check for existence above
        }

    }


    private void completeObjectValidation() {
        final var pid = this.objectInfo.getPid();
        final var ocflId = this.ocflSession.ocflObjectId();
        final var ocflResourceCount = ocflSession.streamResourceHeaders().filter(r -> r.getInteractionModel()
                .equals("http://www.w3.org/ns/ldp#NonRDFSource")).count();
        final String details;
        final var result = headDatastreamIds.size() == ocflResourceCount ? OK : FAIL;
        if (headDatastreamIds.size() == ocflResourceCount) {
            details = "The number of binary objects in HEAD are identical.";
        } else {
            details = format("The number of binary object in HEAD are not equal: f3-> %d vs f6-> %d",
                    headDatastreamIds.size(), ocflResourceCount);
        }

        validationResults.add(new ValidationResult(indexCounter++, result, OBJECT,
                ValidationResult.ValidationType.BINARY_HEAD_COUNT, pid, ocflId,
                details));

    }
}
