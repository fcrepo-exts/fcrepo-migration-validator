/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.migration.validator.api;

import java.util.List;

/**
 * The validation interface for all concrete validation logic.
 *
 * @author dbernstein
 */
public interface Validator<T> {
    /**
     * Performs the validation which, in turn, produces one or more results.
     *
     * @param object The object to perform the validation on.
     * @return A list of one or more validation result objects.
     */
    public List<ValidationResult> validate(T object);
}
