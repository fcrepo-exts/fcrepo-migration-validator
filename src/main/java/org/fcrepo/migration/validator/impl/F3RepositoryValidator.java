/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.migration.validator.impl;

import static java.lang.String.format;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import io.ocfl.api.OcflRepository;
import org.fcrepo.migration.FedoraObjectProcessor;
import org.fcrepo.migration.ObjectSource;
import org.fcrepo.migration.validator.api.RepositoryValidator;
import org.fcrepo.migration.validator.api.ValidationHandler;
import org.fcrepo.migration.validator.api.ValidationResult;
import org.fcrepo.migration.validator.api.ValidationResult.Status;
import org.fcrepo.migration.validator.api.ValidationResult.ValidationLevel;
import org.fcrepo.migration.validator.api.ValidationResult.ValidationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A validator for repository scoped validations for F3 against F6
 *
 * @author mikejritter
 */
public class F3RepositoryValidator implements RepositoryValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(F3RepositoryValidator.class);

    private long deleted = 0;
    private long objects = 0;

    private final boolean headOnly;
    private final boolean enableCheckNumObjects;
    private final AtomicInteger index;
    private final boolean deleteInactive;
    private final ObjectSource source;
    private final List<ValidationResult> validationResults = new ArrayList<>();

    public F3RepositoryValidator(final ApplicationConfigurationHelper config) {
        this.index = new AtomicInteger(0);
        this.source = config.objectSource();
        this.enableCheckNumObjects = config.checkNumObjects();
        this.headOnly = config.getObjectValidationConfig().isValidateHeadOnly();
        this.deleteInactive = config.getObjectValidationConfig().deleteInactive();
    }

    @Override
    public List<ValidationResult> validate(final OcflRepository repository) {
        if (enableCheckNumObjects) {
            try {
                countF3();
                checkObjects(repository);
            } catch (IOException e) {
                LOGGER.error("Error getting Fedora3 repository count", e);
            }
        }

        return validationResults;
    }

    private void checkObjects(final OcflRepository ocflRepository) {
        final var success = "Repository object counts match: Total=%s";
        final var error = "Repository object counts do not match: sourceValue=%s, targetValue=%s";

        try (final var ocflIds = ocflRepository.listObjectIds()) {
            final long ocflCount = ocflIds.count();
            final long f3Count = headOnly ? objects - deleted : objects;

            final ValidationResult result;
            if (ocflCount == f3Count) {
                result = new ValidationResult(index.getAndIncrement(), Status.OK, ValidationLevel.REPOSITORY,
                                              ValidationType.REPOSITORY_RESOURCE_COUNT, format(success, f3Count));
            } else {
                result = new ValidationResult(index.getAndIncrement(), Status.FAIL, ValidationLevel.REPOSITORY,
                                              ValidationType.REPOSITORY_RESOURCE_COUNT,
                                              format(error, objects, f3Count));
            }

            validationResults.add(result);
        }
    }

    /**
     * Iterate the Fedora 3 repository (again) in order to get a count of objects. If the object has the state
     * object property, run an additional check to see if it has been deleted. Errors don't matter as they are handled
     * during object validation.
     *
     * @throws IOException
     */
    private void countF3() throws IOException {
        for (FedoraObjectProcessor processor : source) {
            objects++;
            final var xml = processor.getObjectInfo().getFoxmlPath();
            try (processor; Stream<String> lines = Files.lines(xml)) {
                lines.filter(line -> line.contains(ValidationHandler.F3_STATE))
                     .findFirst()
                     .ifPresent(this::testDeleted);
            }
        }
    }

    /**
     * Increment the deleted counter if the object state was deleted
     *
     * @param xml the object property xml
     */
    private void testDeleted(final String xml) {
        // Instead of parsing the xml, just extract the value attr
        final Pattern pattern = Pattern.compile("value=\"(.*)\"", Pattern.CASE_INSENSITIVE);
        final var matcher = pattern.matcher(xml);
        if (matcher.find()) {
            final var state = F3State.fromString(matcher.group(1));
            if (state.isDeleted(deleteInactive)) {
                deleted++;
            }
        }
    }
}
