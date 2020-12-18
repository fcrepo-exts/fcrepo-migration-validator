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
package org.fcrepo.migration.validator.impl;

import org.fcrepo.migration.DatastreamVersion;
import org.fcrepo.migration.FedoraObjectProcessor;
import org.fcrepo.migration.ObjectInfo;
import org.fcrepo.migration.ObjectProperties;
import org.fcrepo.migration.StreamingFedoraObjectHandler;
import org.fcrepo.migration.validator.api.ValidationResult;
import org.fcrepo.migration.validator.api.Validator;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

import static org.fcrepo.migration.validator.api.ValidationResult.Status.OK;
import static org.fcrepo.migration.validator.api.ValidationResult.ValidationLevel.OBJECT;
import static org.fcrepo.migration.validator.api.ValidationResult.ValidationLevel.OBJECT_RESOURCE;
import static org.fcrepo.migration.validator.api.ValidationResult.ValidationType.BINARY_CHECKSUM;
import static org.fcrepo.migration.validator.api.ValidationResult.ValidationType.METADATA;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Validators perform the specific validation work on a Fedora 3 object and its corresponding OCFL object.
 *
 * @author dbernstein
 */
public class Fedora3ObjectValidator implements Validator<FedoraObjectProcessor> {

    private static final Logger LOGGER = getLogger(Fedora3ObjectValidator.class);

    private final List<ValidationResult> validationResults = new ArrayList<>();

    private int indexCounter;

    @Override
    public List<ValidationResult> validate(final FedoraObjectProcessor object) {
        try {
            object.processObject(new InternalStreamingObjectHandler());
        } catch (Exception ex) {

        }

        return validationResults;
    }

    private class InternalStreamingObjectHandler implements StreamingFedoraObjectHandler {
        private ObjectInfo objectInfo;

        @Override
        public void beginObject(final ObjectInfo objectInfo) {
            this.objectInfo = objectInfo;
        }

        @Override
        public void processObjectProperties(final ObjectProperties objectProperties) {
            validationResults.add(new ValidationResult(indexCounter++, OK, OBJECT, METADATA,
                    this.objectInfo.getPid(), null, null));
        }

        @Override
        public void processDatastreamVersion(final DatastreamVersion datastreamVersion) {
            validationResults.add(new ValidationResult(indexCounter++, OK, OBJECT_RESOURCE,
                    BINARY_CHECKSUM, this.objectInfo.getPid(), null, null));
        }

        @Override
        public void processDisseminator() {

        }

        @Override
        public void completeObject(final ObjectInfo objectInfo) {
        }

        @Override
        public void abortObject(final ObjectInfo objectInfo) {

        }

    }
}
