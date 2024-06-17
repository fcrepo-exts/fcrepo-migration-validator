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
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.List;
import java.util.function.UnaryOperator;

import static org.fcrepo.migration.validator.api.ValidationResult.Status.OK;
import static org.fcrepo.migration.validator.impl.ValidationResultUtils.resolvePathToJsonResult;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * A file-system based result writer
 *
 * @author dbernstein
 */
public class FileSystemValidationResultWriter implements ValidationResultWriter {

    private static final Logger LOGGER = getLogger(FileSystemValidationResultWriter.class);

    private final Path validationRoot;
    private final boolean writeFailureOnly;
    private final UnaryOperator<String> pathEncoder;

    /**
     * Constructor
     *
     * @param validationRoot The root of validation report associated with the run
     * @param writeFailureOnly Flag to indicate if we should write only failed validations or all
     */
    public FileSystemValidationResultWriter(final Path validationRoot, final boolean writeFailureOnly) {
        this.validationRoot = validationRoot;
        this.writeFailureOnly = writeFailureOnly;
        validationRoot.toFile().mkdirs();

        pathEncoder = original -> URLEncoder.encode(original, Charset.defaultCharset());
    }

    @Override
    public void write(final List<ValidationResult> results) {
        final var objectMapper = new ObjectMapper();
        for (final var result : results) {
            if (result.getStatus() == OK && writeFailureOnly) {
                continue;
            }

            final var jsonFilePath = this.validationRoot.resolve(resolvePathToJsonResult(result, pathEncoder));
            LOGGER.debug("Writing of results here: {}", jsonFilePath);
            final var file = jsonFilePath.toFile();
            file.getParentFile().mkdirs();
            try (final var writer = new FileWriter(file)) {
                final var resultStr = objectMapper.writeValueAsString(result);
                writer.write(resultStr);
            } catch (final IOException ex) {
                throw new RuntimeException(ex);
            }
        }
    }
}
