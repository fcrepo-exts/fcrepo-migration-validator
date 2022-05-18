/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.migration.validator.impl;

import java.util.Optional;

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
    public ValidationTask get() {
        LOGGER.info("starting repository processor");
        final var repositoryValidator = new F3RepositoryValidator(checkNumObjects, numObjects);
        final var results = repositoryValidator.validate(ocflRepository);
        writer.write(results);
        return this;
    }

    @Override
    public Optional<String> getPid() {
        return Optional.empty();
    }
}
