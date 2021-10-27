/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.migration.validator.impl;

import org.fcrepo.migration.FedoraObjectProcessor;
import org.fcrepo.migration.validator.api.AbstractValidationTaskBuilder;
import org.fcrepo.migration.validator.api.ObjectValidationConfig;

/**
 * A builder for F3ObjectValidationTask instances.
 *
 * @author dbernstein
 */
public class F3ObjectValidationTaskBuilder extends AbstractValidationTaskBuilder<F3ObjectValidationTask> {

    private FedoraObjectProcessor processor;
    private ObjectValidationConfig objectValidationConfig;

    @Override
    public F3ObjectValidationTask build() {
        return new F3ObjectValidationTask(processor, objectSessionFactory, writer, objectValidationConfig);
    }

    /**
     * @param processor
     */
    public F3ObjectValidationTaskBuilder processor(final FedoraObjectProcessor processor) {
        this.processor = processor;
        return this;
    }

    public F3ObjectValidationTaskBuilder withValidationConfig(final ObjectValidationConfig objectValidationConfig) {
        this.objectValidationConfig = objectValidationConfig;
        return this;
    }
}
