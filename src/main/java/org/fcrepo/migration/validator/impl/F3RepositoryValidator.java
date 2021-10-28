/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.migration.validator.impl;

import static java.lang.String.format;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import edu.wisc.library.ocfl.api.OcflRepository;
import org.fcrepo.migration.validator.api.RepositoryValidator;
import org.fcrepo.migration.validator.api.ValidationResult;
import org.fcrepo.migration.validator.api.ValidationResult.Status;
import org.fcrepo.migration.validator.api.ValidationResult.ValidationLevel;
import org.fcrepo.migration.validator.api.ValidationResult.ValidationType;

/**
 * A validator for repository scoped validations for F3 against F6
 *
 * @author mikejritter
 */
public class F3RepositoryValidator implements RepositoryValidator {

    private final boolean enableCheckNumObjects;
    private final AtomicInteger index;
    private final long numObjectsF3;
    private final List<ValidationResult> validationResults = new ArrayList<>();

    /**
     * Constructor
     *
     * @param enableCheckNumObjects
     * @param numObjects
     */
    public F3RepositoryValidator(final boolean enableCheckNumObjects, final long numObjects) {
        this.numObjectsF3 = numObjects;
        this.index = new AtomicInteger(0);
        this.enableCheckNumObjects = enableCheckNumObjects;
    }

    @Override
    public List<ValidationResult> validate(final OcflRepository repository) {
        if (enableCheckNumObjects) {
            checkObjects(repository);
        }

        return validationResults;
    }

    private void checkObjects(final OcflRepository ocflRepository) {
        final var success = "Repository object counts match: Total=%s";
        final var error = "Repository object counts do not match: sourceValue=%s, targetValue=%s";

        try (final var ocflIds = ocflRepository.listObjectIds()) {
            final long ocflCount = ocflIds.count();

            final ValidationResult result;
            if (ocflCount == numObjectsF3) {
                result = new ValidationResult(index.getAndIncrement(), Status.OK, ValidationLevel.REPOSITORY,
                                              ValidationType.REPOSITORY_RESOURCE_COUNT, format(success, numObjectsF3));
            } else {
                result = new ValidationResult(index.getAndIncrement(), Status.FAIL, ValidationLevel.REPOSITORY,
                                              ValidationType.REPOSITORY_RESOURCE_COUNT,
                                              format(error, numObjectsF3, ocflCount));
            }

            validationResults.add(result);
        }
    }

}
