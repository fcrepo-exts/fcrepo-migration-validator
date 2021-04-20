package org.fcrepo.migration.validator.impl;

public enum F3DatastreamState {
    DELETED, INACTIVE, ACTIVE;

    public static F3DatastreamState fromString(final String state) {
        switch (state.toUpperCase()) {
            case "D": return DELETED;
            case "I": return INACTIVE;
            case "A": return ACTIVE;
            default: throw new IllegalArgumentException("Invalid datastream state");
        }
    }

}
