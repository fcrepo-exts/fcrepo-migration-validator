package org.fcrepo.migration.validator.impl;/*
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

import com.google.common.base.Splitter;
import org.apache.commons.codec.digest.DigestUtils;
import org.fcrepo.migration.validator.api.ValidationResult;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;

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
     * @param result The result
     * @return The file path
     */
    public static Path resolvePathToJsonResult(final ValidationResult result) {
        final var pathSegments = new ArrayList<String>();
        final var sourceId = result.getSourceObjectId();
        if (sourceId != null) {
            final var segments = Splitter.fixedLength(4).splitToList(DigestUtils.sha1Hex(sourceId)).subList(0, 4);
            pathSegments.addAll(segments);
            pathSegments.add(sourceId);
        }
        pathSegments.add("result-" + result.getIndex() + ".json");
        return Path.of(String.join(File.separator, pathSegments));


    }
}
