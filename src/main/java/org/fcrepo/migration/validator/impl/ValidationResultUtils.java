package org.fcrepo.migration.validator.impl;/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */

import com.google.common.base.Splitter;
import org.apache.commons.codec.digest.DigestUtils;
import org.fcrepo.migration.validator.api.ValidationResult;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.function.UnaryOperator;

/**
 * A utility class
 * @author dbernstein
 */
public class ValidationResultUtils {

    private ValidationResultUtils() {
        //intentionally blank
    }

    /**
     * Resolves the relative (i.e. to the validation output directory) path
     * of the json file associated with the validation result.
     *
     * @param result      The result
     * @param pathEncoder The encoder for the objectId
     * @return The file path
     */
    public static Path resolvePathToJsonResult(final ValidationResult result,
                                               final UnaryOperator<String> pathEncoder) {
        final var pathSegments = new ArrayList<String>();
        final var sourceId = result.getSourceObjectId();
        if (sourceId != null) {
            final var segments = Splitter.fixedLength(4).splitToList(DigestUtils.sha1Hex(sourceId)).subList(0, 4);
            pathSegments.addAll(segments);
            pathSegments.add(pathEncoder.apply(sourceId));
        }
        pathSegments.add("result-" + result.getIndex() + ".json");
        return Path.of(String.join(File.separator, pathSegments));
    }
}
