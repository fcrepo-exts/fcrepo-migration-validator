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
    public Path getJsonOuputDirectory() {
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
