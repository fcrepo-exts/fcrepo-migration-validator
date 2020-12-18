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

import org.fcrepo.migration.FedoraObjectProcessor;
import org.fcrepo.migration.validator.api.ValidationResult;
import org.fcrepo.migration.validator.api.Validator;
import org.fcrepo.storage.ocfl.OcflObjectSessionFactory;
import org.slf4j.Logger;

import java.util.List;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Validators perform the specific validation work on a Fedora 3 object and its corresponding OCFL object.
 *
 * @author dbernstein
 */
public class Fedora3ObjectValidator implements Validator<FedoraObjectProcessor> {

    private static final Logger LOGGER = getLogger(Fedora3ObjectValidator.class);


    private OcflObjectSessionFactory factory;

    public Fedora3ObjectValidator(final OcflObjectSessionFactory factory) {
        this.factory = factory;
    }

    @Override
    public List<ValidationResult> validate(final FedoraObjectProcessor object) {
        try {
            final var objectInfo = object.getObjectInfo();
            var fedoraId = objectInfo.getFedoraURI();
            if (fedoraId == null) {
                fedoraId = "info:fedora/" + objectInfo.getPid();
            }
            final var ocflSession = this.factory.newSession(fedoraId);
            final var handler = new ValidatingStreamingObjectHandler(ocflSession);
            object.processObject(handler);
            return handler.getValidationResults();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

}
