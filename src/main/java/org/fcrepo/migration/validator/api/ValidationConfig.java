/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.migration.validator.api;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.fcrepo.migration.validator.report.ReportType;

import java.io.File;
import java.nio.file.Path;

import static org.apache.commons.lang3.builder.ToStringStyle.MULTI_LINE_STYLE;

/**
 * A data class for holding configuration information for a validation run
 *
 * @author dbernstein
 */
public abstract class ValidationConfig {

    private int threadCount = 1;
    private Path resultsDirectory;
    private File ocflRepositoryRootDirectory;

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, MULTI_LINE_STYLE);
    }

    /**
     * @return
     */
    public int getThreadCount() {
        return this.threadCount;
    }

    /**
     * @param threadCount
     */
    public void setThreadCount(final int threadCount) {
        this.threadCount = threadCount;
    }

    /**
     * Set the results directory
     * @param resultsDirectory The results directory
     */
    public void setResultsDirectory(final Path resultsDirectory) {
        this.resultsDirectory = resultsDirectory;
    }

    /**
     * The directory containing json results
     * @return
     */
    public Path getResultsDirectory() {
        return resultsDirectory;
    }

    /**
     * @param reportType the report type to get the directory of
     * @return the output directory
     */
    public Path getReportDirectory(final ReportType reportType) {
        switch (reportType) {
            case csv: return getResultsDirectory().resolve("csv");
            case tsv: return getResultsDirectory().resolve("tsv");
            case html: return getResultsDirectory().resolve("html");
            default: throw new IllegalArgumentException("Unknown report type: " + reportType);
        }
    }

    /**
     *
     * @return
     */
    public Path getJsonOutputDirectory() {
        return getResultsDirectory().resolve("json");
    }

    /**
     *
     * @return
     */
    public File getOcflRepositoryRootDirectory() {
        return ocflRepositoryRootDirectory;
    }

    /**
     *
     * @param ocflRepositoryRootDirectory
     */
    public void setOcflRepositoryRootDirectory(final File ocflRepositoryRootDirectory) {
        this.ocflRepositoryRootDirectory = ocflRepositoryRootDirectory;
    }
}
