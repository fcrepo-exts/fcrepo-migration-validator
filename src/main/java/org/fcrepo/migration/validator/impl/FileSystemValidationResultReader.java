/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.migration.validator.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.fcrepo.migration.validator.api.ValidationResult;
import org.fcrepo.migration.validator.api.ValidationResultReader;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileReader;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * This class deserializes ValidationResult JSON files into ValidationResult objects
 *
 * @author awoods
 * @since 2020-12-20
 */
public class FileSystemValidationResultReader implements ValidationResultReader {

    private static final Logger LOGGER = getLogger(FileSystemValidationResultReader.class);

    /**
     * This method reads the validation result from disk
     *
     * @param validationResultFile to read
     */
    @Override
    public ValidationResult read(final File validationResultFile) {
        final var objectMapper = new ObjectMapper();

        LOGGER.debug("Reading result from here: {}", validationResultFile);
        try (final var reader = new FileReader(validationResultFile)) {
            return objectMapper.readValue(reader, ValidationResult.class);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
