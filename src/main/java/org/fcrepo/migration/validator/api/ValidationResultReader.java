/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.migration.validator.api;

import java.io.File;

/**
 * This class is responsible for reading validation results from disk.
 *
 * @author awoods
 * @since 2020-12-19
 */
public interface ValidationResultReader {

    /**
     * Read the result from disk
     *
     * @param validationResultFile to read
     */
    ValidationResult read(final File validationResultFile);
}
