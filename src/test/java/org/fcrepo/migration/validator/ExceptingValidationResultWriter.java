/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.migration.validator;

import java.util.List;

import org.fcrepo.migration.validator.api.ValidationResult;
import org.fcrepo.migration.validator.api.ValidationResultWriter;

/**
 * ValidationResultWriter which throws an exception
 *
 * @author mikejritter
 */
public class ExceptingValidationResultWriter implements ValidationResultWriter {
    @Override
    public void write(final List<ValidationResult> results) {
        throw new RuntimeException("failure to write");
    }
}
