/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.migration.validator.api;

import org.fcrepo.storage.ocfl.OcflObjectSessionFactory;

/**
 * An abstract base builder for validation tasks  .
 *
 * @author dbernstein
 */
public abstract class AbstractValidationTaskBuilder<T extends ValidationTask> implements ValidationTaskBuilder<T> {

    protected ValidationResultWriter writer;
    protected OcflObjectSessionFactory objectSessionFactory;

    /**
     * @param writer
     * @return
     */
    public ValidationTaskBuilder<T> writer(final ValidationResultWriter writer) {
        this.writer = writer;
        return this;
    }

    /**
     * Sets the OcflObjectSessionFactory objectSessionFactory
     *
     * @param objectSessionFactory
     * @return
     */
    public ValidationTaskBuilder<T> objectSessionFactory(final OcflObjectSessionFactory objectSessionFactory) {
        this.objectSessionFactory = objectSessionFactory;
        return this;
    }
}
