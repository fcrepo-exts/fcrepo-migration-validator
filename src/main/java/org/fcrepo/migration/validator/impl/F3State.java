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

/**
 * Possible states for Fedora 3 Objects
 *
 * @author mikejritter
 */
public enum F3State {
    DELETED, INACTIVE, ACTIVE;

    public static F3State fromString(final String state) {
        switch (state.toUpperCase()) {
            case "D": return DELETED;
            case "I": return INACTIVE;
            case "A": return ACTIVE;
            default: throw new IllegalArgumentException("Invalid datastream state");
        }
    }

}
