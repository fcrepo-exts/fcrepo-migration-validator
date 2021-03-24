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

import com.google.common.hash.Funnels;
import com.google.common.hash.HashCode;
import com.google.common.io.ByteStreams;
import org.fcrepo.migration.DatastreamVersion;
import org.fcrepo.migration.FedoraObjectVersionHandler;
import org.fcrepo.migration.ObjectInfo;
import org.fcrepo.migration.ObjectProperties;
import org.fcrepo.migration.ObjectReference;
import org.fcrepo.migration.ObjectVersionReference;
import org.fcrepo.migration.validator.api.ValidationResult;
import org.fcrepo.migration.validator.api.ValidationResult.ValidationLevel;
import org.fcrepo.migration.validator.api.ValidationResult.ValidationType;
import org.fcrepo.storage.ocfl.OcflObjectSession;
import org.fcrepo.storage.ocfl.ResourceHeaders;
import org.fcrepo.storage.ocfl.exception.NotFoundException;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static java.lang.String.format;
import static org.fcrepo.migration.validator.api.ValidationResult.Status.FAIL;
import static org.fcrepo.migration.validator.api.ValidationResult.Status.OK;
import static org.fcrepo.migration.validator.api.ValidationResult.ValidationLevel.OBJECT;
import static org.fcrepo.migration.validator.api.ValidationResult.ValidationLevel.OBJECT_RESOURCE;
import static org.fcrepo.migration.validator.api.ValidationResult.ValidationType.BINARY_CHECKSUM;
import static org.fcrepo.migration.validator.api.ValidationResult.ValidationType.BINARY_HEAD_COUNT;
import static org.fcrepo.migration.validator.api.ValidationResult.ValidationType.BINARY_METADATA;
import static org.fcrepo.migration.validator.api.ValidationResult.ValidationType.BINARY_VERSION_COUNT;
import static org.fcrepo.migration.validator.api.ValidationResult.ValidationType.METADATA;
import static org.fcrepo.migration.validator.api.ValidationResult.ValidationType.SOURCE_OBJECT_EXISTS_IN_TARGET;
import static org.fcrepo.migration.validator.api.ValidationResult.ValidationType.SOURCE_OBJECT_RESOURCE_EXISTS_IN_TARGET;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * A streaming object handler implementation that performs object scoped validations on behalf
 * of the Fedora3ObjectValidator.
 *
 * @author dbernstein
 */
public class ValidatingObjectHandler implements FedoraObjectVersionHandler {
    private static final Logger LOGGER = getLogger(Fedora3ObjectValidator.class);

    public static final String F3_CREATED_DATE = "info:fedora/fedora-system:def/model#createdDate";
    public static final String F3_LAST_MODIFIED_DATE = "info:fedora/fedora-system:def/view#lastModifiedDate";

    private ObjectInfo objectInfo;
    private final boolean checksum;
    private final OcflObjectSession ocflSession;
    private final List<ValidationResult> validationResults = new ArrayList<>();
    private int indexCounter;
    private final Set<String> headDatastreamIds = new HashSet<>();
    private final F6DigestAlgorithm digestAlgorithm;

    private static final Map<String, PropertyResolver<String>> OCFL_PROPERTY_RESOLVERS = new HashMap<>();

    static {
        OCFL_PROPERTY_RESOLVERS.put(F3_CREATED_DATE, headers -> headers.getCreatedDate().toString());
        OCFL_PROPERTY_RESOLVERS.put(F3_LAST_MODIFIED_DATE, headers -> headers.getLastModifiedDate().toString());
    }

    private interface PropertyResolver<T> {
        T resolve(ResourceHeaders headers);
    }

    @Override
    public void processObjectVersions(final Iterable<ObjectVersionReference> iterable, final ObjectInfo objectInfo) {
        this.objectInfo = objectInfo;

        final Iterator<ObjectVersionReference> referenceIterator = iterable.iterator();
        if (referenceIterator.hasNext()) {
            final ObjectReference objectReference = referenceIterator.next().getObject();
            LOGGER.debug("beginning processing on object: pid={}", objectInfo);
            if (initialObjectValidation(objectReference.getObjectProperties())) {
                objectReference.listDatastreamIds().forEach(dsId -> validateDatastream(dsId, objectReference));
                completeObjectValidation();
            }
        }
    }

    /**
     * Constructor
     *
     * @param session
     * @param enableChecksums
     * @param digestAlgorithm
     */
    public ValidatingObjectHandler(final OcflObjectSession session,
                                   final boolean enableChecksums,
                                   final F6DigestAlgorithm digestAlgorithm) {
        this.ocflSession = session;
        this.checksum = enableChecksums;
        this.digestAlgorithm = digestAlgorithm;
    }

    /**
     * Result the validation results after processObject has been called.
     *
     * @return
     */
    public List<ValidationResult> getValidationResults() {
        return validationResults;
    }

    /**
     * @param objectProperties
     * @return true if initial validation successful and should proceed.
     */
    private boolean initialObjectValidation(final ObjectProperties objectProperties) {
        final var pid = objectInfo.getPid();
        final var ocflId = ocflSession.ocflObjectId();

        final ResourceHeaders headers;

        try {
            headers = ocflSession.readHeaders(ocflId);
            validationResults.add(new ValidationResult(indexCounter++, OK, OBJECT, SOURCE_OBJECT_EXISTS_IN_TARGET,
                                                       pid, ocflId, "Source object is present in target repository."));
        } catch (NotFoundException ex) {
            validationResults.add(new ValidationResult(indexCounter++, FAIL, OBJECT, SOURCE_OBJECT_EXISTS_IN_TARGET,
                    pid, ocflId, "Source object not present in target repository."));
            return false;
        }

        //check that last updated date:
        objectProperties.listProperties().forEach(op -> {
            final var resolver = OCFL_PROPERTY_RESOLVERS.get(op.getName());
            String details = null;
            if (resolver != null) {
                final var sourceValue = op.getValue();
                final var targetValue = resolver.resolve(headers);
                final var result = sourceValue.equals(targetValue) ? OK : FAIL;
                if (result.equals(FAIL)) {
                    details = format("pid: %s -> properties do not match: f3 prop name=%s, source=%s, target=%s",
                            op.getName(), pid, sourceValue, targetValue);
                } else {
                    details = format("pid: %s -> props match: f3 prop name=%s, source=%s, target=%s",
                            op.getName(), pid, sourceValue, targetValue);
                }
                LOGGER.info("PID = {}, object property: name = {}, value = {}", pid, op.getName(), op.getValue());
                validationResults.add(new ValidationResult(indexCounter++, result, OBJECT, METADATA,
                        pid, ocflId, details));
            }
        });

        return true;
    }

    public void validateDatastream(final String dsId, final ObjectReference objectReference) {
        final var dsVersions = objectReference.getDatastreamVersions(dsId);
        final var sourceObjectId = objectInfo.getPid();
        final var targetObjectId = ocflSession.ocflObjectId();
        final var sourceResource = sourceObjectId + "/" + dsId;
        final var targetResource = targetObjectId + "/" + dsId;
        final var targetVersions = ocflSession.listVersions(targetResource);

        var sourceVersionCount = 0;
        String sourceCreated = null;
        for (final var dsVersion : dsVersions) {
            // in f3 the created entry on the first version is what we want to check against for all ocfl versions
            if (sourceCreated == null) {
                sourceCreated = dsVersion.getCreated();
            }

            try {
                final var isHead = dsVersion.isLastVersionIn(objectReference);
                final var ocflVersionInfo = targetVersions.get(sourceVersionCount);
                final var headers = ocflSession.readHeaders(targetResource, ocflVersionInfo.getVersionNumber());

                String version = "version " + sourceVersionCount;
                if (isHead) {
                    version = "HEAD";
                    final var dsInfo = dsVersion.getDatastreamInfo();
                    if (!dsInfo.getState().equals("D")) {
                        headDatastreamIds.add(dsId);
                    }
                }

                final var builder = new ValidationResultBuilder(sourceObjectId, targetObjectId, sourceResource,
                                                                targetResource, OBJECT_RESOURCE);

                validateSize(dsVersion, headers, version, builder);
                validateCreatedDate(sourceCreated, headers, version, builder);
                validateLastModified(dsVersion, headers, version, builder);
                validateChecksum(dsVersion, headers, version, builder);
            } catch (NotFoundException | IndexOutOfBoundsException ex) {
                validationResults.add(new ValidationResult(indexCounter++, FAIL, OBJECT_RESOURCE,
                                                           SOURCE_OBJECT_RESOURCE_EXISTS_IN_TARGET, sourceObjectId,
                                                           targetObjectId, sourceResource, targetResource,
                                                           "Source object resource does not exist in target for " +
                                                           "source version=" + sourceVersionCount + "."));
            }

            sourceVersionCount++;
        }

        try {
            final var targetVersionCount = targetVersions.size();
            final var ok = sourceVersionCount == targetVersionCount;
            var details = format("binary version counts match for resource: %s", sourceVersionCount);
            if (!ok) {
                details = format("binary version counts do not match: source=%d, target=%d", sourceVersionCount,
                        targetVersionCount);
            }
            validationResults.add(new ValidationResult(indexCounter++, ok ? OK : FAIL, OBJECT_RESOURCE,
                    BINARY_VERSION_COUNT, sourceObjectId, targetObjectId, sourceResource, targetResource, details));
        } catch (NotFoundException ex) {
            // intentionally left blank: we check for existence above
        }
    }

    /**
     * Validate the checksum of a datastream.
     *
     * This can fail in multiple ways:
     * 1 - The F3 datastream can not be read
     * 2 - The F6 headers do not contain a checksum (only checking sha512 atm)
     * 3 - The two calculated checksums do not match
     *
     * @param dsVersion the datastream
     * @param headers the ocfl headers
     * @param version the version number
     * @param builder helper for building ValidationResults
     */
    private void validateChecksum(final DatastreamVersion dsVersion, final ResourceHeaders headers,
                                  final String version, final ValidationResultBuilder builder) {
        final var success = "%s binary checksums match: %s";
        final var error = "%s binary checksums do no match: sourceValue=%s, targetValue=%s";
        final var notFound = "%s binary checksum not found in Fedora 6 headers";
        final var exception = "%s binary checksum was unable to be calculated: exception=%s";

        final HashCode sourceHash;
        final var controlGroup = F3ControlGroup.fromString(dsVersion.getDatastreamInfo().getControlGroup());
        if (checksum && controlGroup == F3ControlGroup.MANAGED) {
            try {
                // compute the checksum of the datastream
                final var hasher = digestAlgorithm.hasher();
                ByteStreams.copy(dsVersion.getContent(), Funnels.asOutputStream(hasher));
                sourceHash = hasher.hash();
            } catch (IOException e) {
                validationResults.add(builder.fail(BINARY_CHECKSUM, format(exception, version, e.toString())));
                return; // halt further validation
            }

            // retrieve the SHA-512 digest from the ocfl headers
            // note that digests are stored as urn:algorithm:hash
            final var ocflDigest = headers.getDigests().stream()
                                          .map(URI::toString)
                                          .filter(uri -> uri.startsWith(digestAlgorithm.getOcflUrn()))
                                          .map(uri -> uri.substring(uri.lastIndexOf(":") + 1))
                                          .findFirst();

            final var sourceValue = sourceHash.toString();
            ocflDigest.ifPresentOrElse(targetValue -> {
                if (Objects.equals(sourceValue, targetValue)) {
                    validationResults.add(builder.ok(BINARY_CHECKSUM, format(success, version, sourceValue)));
                } else {
                    validationResults.add(builder.fail(BINARY_CHECKSUM,
                                                       format(error, version, sourceValue, targetValue)));
                }
            }, () -> validationResults.add(builder.fail(BINARY_CHECKSUM, format(notFound, version))));
        }
    }

    private void validateLastModified(final DatastreamVersion dsVersion, final ResourceHeaders headers,
                                      final String version, final ValidationResultBuilder builder) {
        final var error = "%s binary last modified dates do no match: sourceValue=%s, targetValue=%s";
        final var success = "%s binary last modified dates match: %s";

        // the last modified date in ocfl is derived from when a datastream was created
        // so we check equality of the two values
        final var sourceValue = dsVersion.getCreated();
        final var targetValue = headers.getLastModifiedDate().toString();

        if (sourceValue.equals(targetValue)) {
            validationResults.add(builder.ok(BINARY_METADATA, format(success, version, sourceValue)));
        } else {
            validationResults.add(builder.fail(BINARY_METADATA, format(error, version, sourceValue, targetValue)));
        }
    }

    private void validateCreatedDate(final String sourceCreated, final ResourceHeaders headers,
                                     final String version, final ValidationResultBuilder builder) {
        final var error = "%s binary creation dates do no match: sourceValue=%s, targetValue=%s";
        final var success = "%s binary creation dates match: %s";

        final var targetCreated = headers.getCreatedDate().toString();
        if (sourceCreated.equals(targetCreated)) {
            validationResults.add(builder.ok(BINARY_METADATA, format(success, version, sourceCreated)));
        } else {
            validationResults.add(builder.fail(BINARY_METADATA, format(error, version, sourceCreated, targetCreated)));
        }
    }

    private void validateSize(final DatastreamVersion dsVersion, final ResourceHeaders headers,
                              final String version, final ValidationResultBuilder builder) {
        final var error = "%s binary size does not match: sourceValue=%s, targetValue=%s";
        final var success = "%s binary size matches: %s";

        final var dsInfo = dsVersion.getDatastreamInfo();
        final var controlGroup = F3ControlGroup.fromString(dsInfo.getControlGroup());
        if (controlGroup == F3ControlGroup.MANAGED) {
            final var sourceSize = dsVersion.getSize();
            final var targetSize = headers.getContentSize();
            if (sourceSize == targetSize) {
                validationResults.add(builder.ok(BINARY_METADATA, format(success, version, sourceSize)));
            } else {
                validationResults.add(builder.fail(BINARY_METADATA, format(error, version, sourceSize, targetSize)));
            }
        }
    }

    private void completeObjectValidation() {
        final var pid = this.objectInfo.getPid();
        final var ocflId = this.ocflSession.ocflObjectId();
        final var ocflResourceCount = ocflSession.streamResourceHeaders().filter(r -> r.getInteractionModel()
                .equals("http://www.w3.org/ns/ldp#NonRDFSource")).count();
        final String details;
        final var result = headDatastreamIds.size() == ocflResourceCount ? OK : FAIL;
        if (headDatastreamIds.size() == ocflResourceCount) {
            details = "The number of binary objects in HEAD are identical.";
        } else {
            details = format("The number of binary object in HEAD are not equal: f3-> %d vs f6-> %d",
                    headDatastreamIds.size(), ocflResourceCount);
        }

        validationResults.add(new ValidationResult(indexCounter++, result, OBJECT, BINARY_HEAD_COUNT, pid, ocflId,
                details));

    }

    /**
     * Hold a few values so we can have an easier time creating ValidationResults in various places
     */
    private class ValidationResultBuilder {
        private final String sourceObjectId;
        private final String targetObjectId;
        private final String sourceResource;
        private final String targetResource;
        private final ValidationLevel validationLevel;

        private ValidationResultBuilder(final String sourceObjectId, final String targetObjectId,
                                        final String sourceResource, final String targetResource,
                                        final ValidationLevel validationLevel) {
            this.sourceObjectId = sourceObjectId;
            this.targetObjectId = targetObjectId;
            this.sourceResource = sourceResource;
            this.targetResource = targetResource;
            this.validationLevel = validationLevel;
        }

        public ValidationResult ok(final ValidationType type, final String details) {
            return new ValidationResult(indexCounter++, OK, validationLevel, type, sourceObjectId, targetObjectId,
                                        sourceResource, targetResource, details);
        }

        public ValidationResult fail(final ValidationType type, final String details) {
            return new ValidationResult(indexCounter++, FAIL, validationLevel, type, sourceObjectId, targetObjectId,
                                        sourceResource, targetResource, details);
        }
    }
}
