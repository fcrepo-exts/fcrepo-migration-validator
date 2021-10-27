/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.migration.validator.report;

/**
 * Types of reports we can generate
 *
 * @author mikejritter
 */
public enum ReportType {
    html(".html", null), csv(".csv", ','), tsv(".tsv", '\t');

    private final String extension;
    private final Character separator;

    ReportType(final String extension, final Character separator) {
        this.extension = extension;
        this.separator = separator;
    }

    public String getExtension() {
        return extension;
    }

    public Character getSeparator() {
        return separator;
    }
}
