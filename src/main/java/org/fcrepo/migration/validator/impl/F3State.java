/*
 * Licensed to DuraSpace under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.
 *
 * DuraSpace licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fcrepo.migration.validator.impl;

import org.fcrepo.migration.ObjectProperty;

/**
 * Possible states for Fedora 3 Objects
 *
 * @author mikejritter
 */
public enum F3State {
    DELETED, INACTIVE, ACTIVE;

    /**
     * Create a F3State from a String
     * @param state the state
     * @return
     */
    public static F3State fromString(final String state) {
        switch (state.toUpperCase()) {
            case "A": case "ACTIVE": return ACTIVE;
            case "D": case "DELETED": return DELETED;
            case "I": case "INACTIVE": return INACTIVE;
            default: throw new IllegalArgumentException("Invalid Fedora 3 state");
        }
    }

    /**
     * Create a F3State from an ObjectProperty
     * @param property the ObjectProperty
     * @return
     */
    public static F3State fromProperty(final ObjectProperty property) {
        return fromString(property.getValue());
    }

    /**
     * Check if a F3State should be treated as Deleted in the Fedora 6 repository
     * @param deleteInactive if Inactive state are Deleted
     * @return true if the F3State is Deleted or deleteInactive is set and the state is Inactive
     */
    public boolean isDeleted(final boolean deleteInactive) {
        return this == DELETED || (deleteInactive && this == INACTIVE);
    }

}
