/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.migration.validator.api;

import org.fcrepo.storage.ocfl.OcflObjectSessionFactory;

/**
 * An interface for building validation tasks
 *
 * @author dbernstein
 */
public interface ValidationTaskBuilder<T extends ValidationTask> {
     /**
      * Build a new task instance
      *
      * @return
      */
     T build();

     /**
      * @param writer
      * @return
      */
     ValidationTaskBuilder<T> writer(final ValidationResultWriter writer);

     /**
      * Sets the OcflObjectSessionFactory objectSessionFactory
      *
      * @param objectSessionFactory
      * @return
      */
     ValidationTaskBuilder<T> objectSessionFactory(final OcflObjectSessionFactory objectSessionFactory);
}
