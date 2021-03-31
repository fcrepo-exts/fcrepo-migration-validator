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
import org.fcrepo.migration.validator.api.ObjectValidationConfig;
import org.fcrepo.migration.validator.api.ValidationResultWriter;
import org.fcrepo.migration.validator.api.ValidationTask;
import org.fcrepo.storage.ocfl.OcflObjectSessionFactory;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * This class is responsible for performing all validations on a single F3 object.
 *
 * @author dbernstein
 */
public class F3ObjectValidationTask extends ValidationTask {

    private static final Logger LOGGER = getLogger(F3ObjectValidationTask.class);

    private final FedoraObjectProcessor processor;
    private final OcflObjectSessionFactory ocflObjectSessionFactory;
    private final ValidationResultWriter writer;
    private final ObjectValidationConfig objectValidationConfig;

    /**
     * Constructor
     *
     * @param processor                The processor
     * @param ocflObjectSessionFactory The object session factory
     * @param writer                   The shared validation state
     * @param objectValidationConfig   The config to use when validating objects
     */
    public F3ObjectValidationTask(final FedoraObjectProcessor processor,
                                  final OcflObjectSessionFactory ocflObjectSessionFactory,
                                  final ValidationResultWriter writer,
                                  final ObjectValidationConfig objectValidationConfig) {
        super();
        this.processor = processor;
        this.ocflObjectSessionFactory = ocflObjectSessionFactory;
        this.writer = writer;
        this.objectValidationConfig = objectValidationConfig;
    }

    @Override
    public void run() {
        LOGGER.info("starting to process {} ", processor.getObjectInfo().getPid());
        final var validator = new Fedora3ObjectValidator(ocflObjectSessionFactory, objectValidationConfig);
        final var results = validator.validate(processor);
        writer.write(results);
    }
}
