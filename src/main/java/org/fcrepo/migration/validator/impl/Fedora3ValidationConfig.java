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

import org.fcrepo.migration.validator.api.ValidationConfig;

import java.io.File;

/**
 * A Fedora 3 Validation Configuration
 *
 * @author dbernstein
 */
public class Fedora3ValidationConfig extends ValidationConfig {

    private boolean checksum;
    private boolean checkNumObjects;
    private F6DigestAlgorithm digestAlgorithm;
    private F3SourceTypes sourceType;
    private File exportedDirectory;
    private File datastreamsDirectory;
    private File objectsDirectory;
    private File indexDirectory;
    private String fedora3Hostname;
    private File objectsToValidate;

    /**
     * @return
     */
    public File getDatastreamsDirectory() {
        return datastreamsDirectory;
    }

    /**
     * @param datastreamsDirectory
     */
    public void setDatastreamsDirectory(final File datastreamsDirectory) {
        this.datastreamsDirectory = datastreamsDirectory;
    }

    /**
     * @return
     */
    public F3SourceTypes getSourceType() {
        return sourceType;
    }

    /**
     * @param sourceType
     */
    public void setSourceType(final F3SourceTypes sourceType) {
        this.sourceType = sourceType;
    }

    /**
     * @return
     */
    public File getObjectsDirectory() {
        return objectsDirectory;
    }

    /**
     * @param objectsDirectory
     */
    public void setObjectsDirectory(final File objectsDirectory) {
        this.objectsDirectory = objectsDirectory;
    }

    /**
     * @return
     */
    public File getExportedDirectory() {
        return exportedDirectory;
    }

    /**
     * @param exportedDirectory
     */
    public void setExportedDirectory(final File exportedDirectory) {
        this.exportedDirectory = exportedDirectory;
    }

    /**
     * @param fedora3Hostname
     */
    public void setFedora3Hostname(final String fedora3Hostname) {
        this.fedora3Hostname = fedora3Hostname;
    }

    /**
     * @return
     */
    public String getFedora3Hostname() {
        return fedora3Hostname;
    }

    /**
     * @return
     */
    public File getIndexDirectory() {
        return indexDirectory;
    }

    /**
     * @param indexDirectory
     */
    public void setIndexDirectory(final File indexDirectory) {
        this.indexDirectory = indexDirectory;
    }

    public File getObjectsToValidate() {
        return objectsToValidate;
    }

    /**
     * @param objectsToValidate
     * @return
     */
    public Fedora3ValidationConfig setObjectsToValidate(final File objectsToValidate) {
        this.objectsToValidate = objectsToValidate;
        return this;
    }

    /**
     * @param checksum
     */
    public void setEnableChecksums(final boolean checksum) {
        this.checksum = checksum;
    }

    public Boolean enableChecksums() {
        return checksum;
    }

    /**
     * @param digestAlgorithm
     */
    public void setDigestAlgorithm(final F6DigestAlgorithm digestAlgorithm) {
        this.digestAlgorithm = digestAlgorithm;
    }

    public F6DigestAlgorithm getDigestAlgorithm() {
        return digestAlgorithm;
    }

    public boolean checkNumObjects() {
        return checkNumObjects;
    }

    /**
     * @param checkNumObjects
     */
    public Fedora3ValidationConfig setCheckNumObjects(final boolean checkNumObjects) {
        this.checkNumObjects = checkNumObjects;
        return this;
    }
}
