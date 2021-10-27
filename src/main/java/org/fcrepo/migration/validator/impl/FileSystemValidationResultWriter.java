/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.migration.validator.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.fcrepo.migration.validator.api.ValidationResult;
import org.fcrepo.migration.validator.api.ValidationResultWriter;
import org.slf4j.Logger;

import java.io.FileWriter;
import java.nio.file.Path;
import java.util.List;

import static org.fcrepo.migration.validator.impl.ValidationResultUtils.resolvePathToJsonResult;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * A file-system based result writer
 *
 * @author dbernstein
 */
public class FileSystemValidationResultWriter implements ValidationResultWriter {

    private static final Logger LOGGER = getLogger(FileSystemValidationResultWriter.class);

    private Path validationRoot;


    /**
     * Constructor
     *
     * @param validationRoot The root of validation report associated with the run
     */
    public FileSystemValidationResultWriter(final Path validationRoot) {
        this.validationRoot = validationRoot;
        this.validationRoot.toFile().mkdirs();
    }

    @Override
    public void write(final List<ValidationResult> results) {
        final var objectMapper = new ObjectMapper();
        for (final var result : results) {
            LOGGER.info("Writing of results here: {}", result);
            final var jsonFilePath = this.validationRoot.resolve(resolvePathToJsonResult(result));
            final var file = jsonFilePath.toFile();
            file.getParentFile().mkdirs();
            try (final var writer = new FileWriter(file)) {
                final var resultStr = objectMapper.writeValueAsString(result);
                writer.write(resultStr);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
