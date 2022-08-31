/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.migration.validator.impl;

import java.util.Optional;

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

    private final ValidationResultWriter writer;
    private final ApplicationConfigurationHelper config;

    /**
     * Constructor
     *
     * @param config
     * @param writer
     */
    public F3RepositoryValidationTask(final ApplicationConfigurationHelper config,
                                      final ValidationResultWriter writer) {
        this.config = config;
        this.writer = writer;
    }


    @Override
    public ValidationTask get() {
        LOGGER.info("Starting repository processor");
        final var repository = config.ocflRepository();
        final var repositoryValidator = new F3RepositoryValidator(config);
        final var results = repositoryValidator.validate(repository);
        writer.write(results);
        return this;
    }

    @Override
    public Optional<String> getPid() {
        return Optional.empty();
    }
}
