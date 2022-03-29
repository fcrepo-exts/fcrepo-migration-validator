/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.migration.validator.api;

/**
 * This class is responsible for coordinating and managing the lifecycle of the classes involved in a validation run.
 *
 * @author dbernstein
 */
public interface ValidationExecutionManager {

    /**
     * Perform a validation run.
     * @return
     */
    boolean doValidation();
}
