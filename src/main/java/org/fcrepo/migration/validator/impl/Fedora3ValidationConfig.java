/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
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
    private boolean failureOnly;
    private boolean deleteInactive;
    private boolean validateHeadOnly;
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

    public boolean validateHeadOnly() {
        return validateHeadOnly;
    }

    /**
     * @param validateHeadOnly
     */
    public Fedora3ValidationConfig setValidateHeadOnly(final boolean validateHeadOnly) {
        this.validateHeadOnly = validateHeadOnly;
        return this;
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

    public boolean isDeleteInactive() {
        return deleteInactive;
    }

    /**
     * @param deleteInactive
     */
    public Fedora3ValidationConfig setDeleteInactive(final boolean deleteInactive) {
        this.deleteInactive = deleteInactive;
        return this;
    }

    public boolean isFailureOnly() {
        return failureOnly;
    }

    /**
     * @param failureOnly
     */
    public Fedora3ValidationConfig setFailureOnly(final boolean failureOnly) {
        this.failureOnly = failureOnly;
        return this;
    }
}
