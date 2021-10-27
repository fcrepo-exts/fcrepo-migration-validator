/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.migration.validator.impl;

/**
 * Control Groups for Fedora3
 *
 * X - Inline XML
 * M - Managed
 * E - Externally Referenced
 * R - Redirect Referenced
 *
 * @author mikejritter
 */
public enum F3ControlGroup {
    INLINE_XML, MANAGED, EXTERNALLY_REFERENCED, REDIRECT_REFERENCED;

    public static F3ControlGroup fromString(final String controlGroup) {
        switch (controlGroup.toUpperCase()) {
            case "X": return INLINE_XML;
            case "M": return MANAGED;
            case "E": return EXTERNALLY_REFERENCED;
            case "R": return REDIRECT_REFERENCED;
        }

        throw new IllegalArgumentException(controlGroup + " is not a valid control group identifier!");
    }
}
