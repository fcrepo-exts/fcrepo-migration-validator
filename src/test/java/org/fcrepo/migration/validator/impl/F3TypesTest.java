/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.migration.validator.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.fcrepo.migration.validator.impl.F3ControlGroup.EXTERNALLY_REFERENCED;
import static org.fcrepo.migration.validator.impl.F3ControlGroup.INLINE_XML;
import static org.fcrepo.migration.validator.impl.F3ControlGroup.MANAGED;
import static org.fcrepo.migration.validator.impl.F3ControlGroup.REDIRECT_REFERENCED;
import static org.fcrepo.migration.validator.impl.F3State.ACTIVE;
import static org.fcrepo.migration.validator.impl.F3State.DELETED;
import static org.fcrepo.migration.validator.impl.F3State.INACTIVE;

import org.fcrepo.migration.ObjectProperty;
import org.junit.Test;

/**
 * Covers the string parsing of the Fedora 3 enum types.
 *
 * @author mikejritter
 */
public class F3TypesTest {

    @Test
    public void testControlGroupFromString() {
        assertThat(F3ControlGroup.fromString("X")).isEqualTo(INLINE_XML);
        assertThat(F3ControlGroup.fromString("m")).isEqualTo(MANAGED);
        assertThat(F3ControlGroup.fromString("E")).isEqualTo(EXTERNALLY_REFERENCED);
        assertThat(F3ControlGroup.fromString("r")).isEqualTo(REDIRECT_REFERENCED);
    }

    @Test
    public void testControlGroupRejectsUnknownValue() {
        assertThatThrownBy(() -> F3ControlGroup.fromString("Z"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testStateFromString() {
        assertThat(F3State.fromString("A")).isEqualTo(ACTIVE);
        assertThat(F3State.fromString("active")).isEqualTo(ACTIVE);
        assertThat(F3State.fromString("D")).isEqualTo(DELETED);
        assertThat(F3State.fromString("deleted")).isEqualTo(DELETED);
        assertThat(F3State.fromString("I")).isEqualTo(INACTIVE);
        assertThat(F3State.fromString("inactive")).isEqualTo(INACTIVE);
    }

    @Test
    public void testStateRejectsUnknownValue() {
        assertThatThrownBy(() -> F3State.fromString("Z")).isInstanceOf(IllegalArgumentException.class);
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

        assertThat(F3State.fromProperty(property)).isEqualTo(DELETED);
    }

    @Test
    public void testStateIsDeleted() {
        assertThat(DELETED.isDeleted(false)).isTrue();
        assertThat(DELETED.isDeleted(true)).isTrue();
        assertThat(INACTIVE.isDeleted(false)).isFalse();
        assertThat(INACTIVE.isDeleted(true)).isTrue();
        assertThat(ACTIVE.isDeleted(false)).isFalse();
        assertThat(ACTIVE.isDeleted(true)).isFalse();
    }

    @Test
    public void testSourceTypeToType() {
        assertThat(F3SourceTypes.toType("akubra")).isEqualTo(F3SourceTypes.AKUBRA);
        assertThat(F3SourceTypes.toType("LEGACY")).isEqualTo(F3SourceTypes.LEGACY);
        assertThat(F3SourceTypes.toType("Exported")).isEqualTo(F3SourceTypes.EXPORTED);
    }

    @Test
    public void testSourceTypeRejectsUnknownValue() {
        assertThatThrownBy(() -> F3SourceTypes.toType("nope")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testDigestAlgorithm() {
        assertThat(F6DigestAlgorithm.sha256.getOcflUrn()).isEqualTo("urn:" + F6DigestAlgorithm.sha256.getName());
        assertThat(F6DigestAlgorithm.sha512.hasher()).isNotNull();
    }
}
