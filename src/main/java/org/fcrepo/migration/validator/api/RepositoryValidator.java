/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.migration.validator.api;

import edu.wisc.library.ocfl.api.OcflRepository;

/**
 * An interface for performing validations across the repository.
 *
 * @author dbernstein
 */
public interface RepositoryValidator extends Validator<OcflRepository> {
}
