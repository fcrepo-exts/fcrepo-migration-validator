/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.migration.validator.impl;

/**
 * Fedora 3 source types
 *
 * @author dbernstein
 */
public enum F3SourceTypes {
    AKUBRA, LEGACY, EXPORTED;

    public static F3SourceTypes toType(final String v) {
        return valueOf(v.toUpperCase());
    }
}
