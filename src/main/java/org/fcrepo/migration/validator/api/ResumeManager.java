package org.fcrepo.migration.validator.api;

/**
 * Similar to PidListManager from migration-utils
 *
 * @author mikejritter
 */
public interface ResumeManager {

    enum State {
        OK, SKIP, HALT_LIMIT
    }

    State accept(final String pid);

}
