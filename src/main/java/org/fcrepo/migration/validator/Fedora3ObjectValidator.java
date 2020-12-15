package org.fcrepo.migration.validator;
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

import org.fcrepo.migration.DatastreamVersion;
import org.fcrepo.migration.FedoraObjectProcessor;
import org.fcrepo.migration.ObjectInfo;
import org.fcrepo.migration.ObjectProperties;
import org.fcrepo.migration.StreamingFedoraObjectHandler;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Validators perform the specific validation work on a Fedora 3 object and its corresponding OCFL object.
 *
 * @author dbernstein
 */
public class Fedora3ObjectValidator implements Validator<FedoraObjectProcessor>, StreamingFedoraObjectHandler {

    private static final Logger LOGGER = getLogger(Fedora3ObjectValidator.class);

    private final List<ValidationResult> validationResults = new ArrayList<>();

    @Override
    public List<ValidationResult> validate(final FedoraObjectProcessor object) {
        try {
            object.processObject(this);
        } catch (Exception ex) {

        }

        return validationResults;
    }

    @Override
    public void beginObject(final ObjectInfo objectInfo) {

    }

    @Override
    public void processObjectProperties(final ObjectProperties objectProperties) {
        validationResults.add(new ValidationResultImpl());
    }

    @Override
    public void processDatastreamVersion(final DatastreamVersion datastreamVersion) {
        validationResults.add(new ValidationResultImpl());
    }

    @Override
    public void processDisseminator() {

    }

    @Override
    public void completeObject(final ObjectInfo objectInfo) {
        validationResults.add(new ValidationResultImpl());
    }

    @Override
    public void abortObject(final ObjectInfo objectInfo) {

    }
}
