/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.migration.validator.api;

import java.util.List;

/**
 * This class is responsible for writing validation results to disk.
 *
 * @author dbernstein
 */
public interface ValidationResultWriter {
    /**
     * Write the result to disk
     *
     * @param results The results to write
     */
    void write(final List<ValidationResult> results);
}
