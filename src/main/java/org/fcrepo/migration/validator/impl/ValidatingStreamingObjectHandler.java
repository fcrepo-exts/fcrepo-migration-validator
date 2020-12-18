package org.fcrepo.migration.validator.impl;/*
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

import org.fcrepo.migration.DatastreamVersion;
import org.fcrepo.migration.ObjectInfo;
import org.fcrepo.migration.ObjectProperties;
import org.fcrepo.migration.StreamingFedoraObjectHandler;
import org.fcrepo.migration.validator.api.ValidationResult;
import org.fcrepo.storage.ocfl.OcflObjectSession;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.lang.String.format;
import static org.fcrepo.migration.validator.api.ValidationResult.Status.FAIL;
import static org.fcrepo.migration.validator.api.ValidationResult.Status.OK;
import static org.fcrepo.migration.validator.api.ValidationResult.ValidationLevel.OBJECT;
import static org.fcrepo.migration.validator.api.ValidationResult.ValidationLevel.OBJECT_RESOURCE;
import static org.fcrepo.migration.validator.api.ValidationResult.ValidationType.BINARY_CHECKSUM;
import static org.fcrepo.migration.validator.api.ValidationResult.ValidationType.METADATA;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * A streaming object handler implementation that performs object scoped validations on behalf
 * of the Fedora3ObjectValidator.
 *
 * @author dbernstein
 */
public class ValidatingStreamingObjectHandler implements StreamingFedoraObjectHandler {
    private static final Logger LOGGER = getLogger(Fedora3ObjectValidator.class);

    private ObjectInfo objectInfo;
    private OcflObjectSession ocflSession;
    private final List<ValidationResult> validationResults = new ArrayList<>();
    private int indexCounter;
    private Set<String> activeDsIds = new HashSet<>();
    /**
     * Constructor
     *
     * @param session
     */
    public ValidatingStreamingObjectHandler(final OcflObjectSession session) {
        this.ocflSession = session;
    }

    public List<ValidationResult> getValidationResults() {
        return validationResults;
    }

    @Override
    public void beginObject(final ObjectInfo objectInfo) {
        LOGGER.debug("beginning stream processing on object: pid={}", objectInfo.getPid());
        this.objectInfo = objectInfo;
    }

    @Override
    public void processObjectProperties(final ObjectProperties objectProperties) {
        final var pid = objectInfo.getPid();
        //check that last updated date:
        objectProperties.listProperties().stream().forEach(op -> {
            String details = null;
            final var sourceValue = "";
            final var targetValue = "";

            final var result = sourceValue.equals(targetValue) ? OK : FAIL;
            if (result.equals(FAIL)) {
                details = format("pid: %s -> last update dates do not match: source=%s, target=%s", pid, sourceValue,
                        targetValue);
            }
            LOGGER.info("PID = {}, object property: name = {}, value = {}", pid, op.getName(), op.getValue());
            validationResults.add(new ValidationResult(indexCounter++, result, OBJECT, METADATA,
                    pid, this.ocflSession.ocflObjectId(), details));
        });

    }

    @Override
    public void processDatastreamVersion(final DatastreamVersion datastreamVersion) {
        final var dsInfo = datastreamVersion.getDatastreamInfo();
        final var dsId = dsInfo.getDatastreamId();
        final var dsVersion = datastreamVersion.getVersionId().replace(dsId + ".", "");
        if (dsInfo.getState().equals("A")) {
            activeDsIds.add(dsId);
        }
        validationResults.add(new ValidationResult(indexCounter++, OK, OBJECT_RESOURCE,
                BINARY_CHECKSUM, this.objectInfo.getPid(), null, null));
    }

    @Override
    public void processDisseminator() {

    }

    @Override
    public void completeObject(final ObjectInfo objectInfo) {
        final var pid = this.objectInfo.getPid();
        final var ocflId = this.ocflSession.ocflObjectId();
        final var ocflResourceCount = ocflSession.streamResourceHeaders().filter(r -> r.getInteractionModel()
                .equals("http://www.w3.org/ns/ldp#NonRDFSource")).count();
        if (activeDsIds.size() == ocflResourceCount) {
            validationResults.add(new ValidationResult(indexCounter++, OK, OBJECT,
                    ValidationResult.ValidationType.BINARY_HEAD_COUNT, pid, ocflId,
                    "The number of binary objects in HEAD are identical."));
        } else {
            validationResults.add(new ValidationResult(indexCounter++, FAIL, OBJECT,
                    ValidationResult.ValidationType.BINARY_HEAD_COUNT, pid, ocflId,
                    format("The number of binary object in HEAD are not equal: f3-> %d vs f6-> %d",
                            activeDsIds.size(), ocflResourceCount)));

        }
        LOGGER.debug("completed stream processing on object: pid={}", objectInfo.getPid());
    }

    @Override
    public void abortObject(final ObjectInfo objectInfo) {
        LOGGER.warn(format("Not currently handling abort opertaions: pid=%s", objectInfo.getPid()));
    }

}
