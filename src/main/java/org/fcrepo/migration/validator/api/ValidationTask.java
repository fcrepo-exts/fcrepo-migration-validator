/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.migration.validator.api;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * A runnable responsible for executing validations and processing the results.
 *
 * @author dbernstein
 */
public abstract class ValidationTask implements Supplier<ValidationTask> {

    public abstract Optional<String> getPid();
}
