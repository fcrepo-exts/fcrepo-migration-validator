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

import edu.wisc.library.ocfl.api.OcflRepository;
import org.fcrepo.migration.validator.api.ValidationResultWriter;
import org.fcrepo.migration.validator.api.ValidationTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class just starts a {@link F3RepositoryValidator} and writes the output to a file
 *
 * @author mikejritter
 */
public class F3RepositoryValidationTask extends ValidationTask {

    private static final Logger LOGGER = LoggerFactory.getLogger(F3RepositoryValidationTask.class);

    private final long numObjects;
    private final boolean checkNumObjects;
    private final OcflRepository ocflRepository;
    private final ValidationResultWriter writer;

    /**
     * Constructor
     *
     * @param checkNumObjects
     * @param numObjects
     * @param ocflRepository
     * @param writer
     */
    public F3RepositoryValidationTask(final boolean checkNumObjects,
                                      final long numObjects,
                                      final OcflRepository ocflRepository,
                                      final ValidationResultWriter writer) {
        this.writer = writer;
        this.numObjects = numObjects;
        this.ocflRepository = ocflRepository;
        this.checkNumObjects = checkNumObjects;
    }

    @Override
    public void run() {
        LOGGER.info("starting repository processor");
        final var repositoryValidator = new F3RepositoryValidator(checkNumObjects, numObjects);
        final var results = repositoryValidator.validate(ocflRepository);
        writer.write(results);
    }
}
