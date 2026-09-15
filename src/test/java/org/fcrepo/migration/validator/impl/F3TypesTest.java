/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.migration.validator.impl;

import static org.fcrepo.migration.validator.impl.F3ControlGroup.EXTERNALLY_REFERENCED;
import static org.fcrepo.migration.validator.impl.F3ControlGroup.INLINE_XML;
import static org.fcrepo.migration.validator.impl.F3ControlGroup.MANAGED;
import static org.fcrepo.migration.validator.impl.F3ControlGroup.REDIRECT_REFERENCED;
import static org.fcrepo.migration.validator.impl.F3State.ACTIVE;
import static org.fcrepo.migration.validator.impl.F3State.DELETED;
import static org.fcrepo.migration.validator.impl.F3State.INACTIVE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.fcrepo.migration.ObjectProperty;
import org.junit.Test;

/**
 * Covers the string parsing of the Fedora 3 enum types.
 *
 * @author Dan Field
 */
public class F3TypesTest {

    @Test
    public void testControlGroupFromString() {
        assertEquals(INLINE_XML, F3ControlGroup.fromString("X"));
        assertEquals(MANAGED, F3ControlGroup.fromString("m"));
        assertEquals(EXTERNALLY_REFERENCED, F3ControlGroup.fromString("E"));
        assertEquals(REDIRECT_REFERENCED, F3ControlGroup.fromString("r"));
    }

    @Test
    public void testControlGroupRejectsUnknownValue() {
        assertThrows(IllegalArgumentException.class, () -> F3ControlGroup.fromString("Z"));
    }

    @Test
    public void testStateFromString() {
        assertEquals(ACTIVE, F3State.fromString("A"));
        assertEquals(ACTIVE, F3State.fromString("active"));
        assertEquals(DELETED, F3State.fromString("D"));
        assertEquals(DELETED, F3State.fromString("deleted"));
        assertEquals(INACTIVE, F3State.fromString("I"));
        assertEquals(INACTIVE, F3State.fromString("inactive"));
    }

    @Test
    public void testStateRejectsUnknownValue() {
        assertThrows(IllegalArgumentException.class, () -> F3State.fromString("Z"));
    }

    @Test
    public void testStateFromProperty() {
        final var property = new ObjectProperty() {
            @Override
            public String getName() {
                return "info:fedora/fedora-system:def/model#state";
            }

            @Override
            public String getValue() {
                return "D";
            }
        };

        assertEquals(DELETED, F3State.fromProperty(property));
    }

    @Test
    public void testStateIsDeleted() {
        assertTrue(DELETED.isDeleted(false));
        assertTrue(DELETED.isDeleted(true));
        assertFalse(INACTIVE.isDeleted(false));
        assertTrue(INACTIVE.isDeleted(true));
        assertFalse(ACTIVE.isDeleted(false));
        assertFalse(ACTIVE.isDeleted(true));
    }

    @Test
    public void testSourceTypeToType() {
        assertEquals(F3SourceTypes.AKUBRA, F3SourceTypes.toType("akubra"));
        assertEquals(F3SourceTypes.LEGACY, F3SourceTypes.toType("LEGACY"));
        assertEquals(F3SourceTypes.EXPORTED, F3SourceTypes.toType("Exported"));
    }

    @Test
    public void testSourceTypeRejectsUnknownValue() {
        assertThrows(IllegalArgumentException.class, () -> F3SourceTypes.toType("nope"));
    }

    @Test
    public void testDigestAlgorithm() {
        assertEquals("urn:" + F6DigestAlgorithm.sha256.getName(), F6DigestAlgorithm.sha256.getOcflUrn());
        assertNotNull(F6DigestAlgorithm.sha512.hasher());
    }
}
