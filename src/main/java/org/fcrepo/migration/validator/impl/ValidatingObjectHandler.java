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

import com.google.common.collect.Sets;
import com.google.common.hash.Funnels;
import com.google.common.hash.HashCode;
import com.google.common.io.ByteStreams;
import edu.wisc.library.ocfl.api.OcflRepository;
import edu.wisc.library.ocfl.api.model.ObjectVersionId;
import edu.wisc.library.ocfl.api.model.OcflObjectVersion;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.fcrepo.migration.DatastreamVersion;
import org.fcrepo.migration.FedoraObjectVersionHandler;
import org.fcrepo.migration.ObjectInfo;
import org.fcrepo.migration.ObjectProperties;
import org.fcrepo.migration.ObjectProperty;
import org.fcrepo.migration.ObjectReference;
import org.fcrepo.migration.ObjectVersionReference;
import org.fcrepo.migration.validator.api.ObjectValidationConfig;
import org.fcrepo.migration.validator.api.ValidationResult;
import org.fcrepo.migration.validator.api.ValidationResult.ValidationLevel;
import org.fcrepo.migration.validator.api.ValidationResult.ValidationType;
import org.fcrepo.storage.ocfl.OcflObjectSession;
import org.fcrepo.storage.ocfl.OcflVersionInfo;
import org.fcrepo.storage.ocfl.ResourceHeaders;
import org.fcrepo.storage.ocfl.exception.NotFoundException;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static java.lang.String.format;
import static org.fcrepo.migration.validator.api.ValidationResult.Status.FAIL;
import static org.fcrepo.migration.validator.api.ValidationResult.Status.OK;
import static org.fcrepo.migration.validator.api.ValidationResult.ValidationLevel.OBJECT;
import static org.fcrepo.migration.validator.api.ValidationResult.ValidationLevel.OBJECT_RESOURCE;
import static org.fcrepo.migration.validator.api.ValidationResult.ValidationType.BINARY_CHECKSUM;
import static org.fcrepo.migration.validator.api.ValidationResult.ValidationType.BINARY_HEAD_COUNT;
import static org.fcrepo.migration.validator.api.ValidationResult.ValidationType.BINARY_METADATA;
import static org.fcrepo.migration.validator.api.ValidationResult.ValidationType.BINARY_SIZE;
import static org.fcrepo.migration.validator.api.ValidationResult.ValidationType.BINARY_VERSION_COUNT;
import static org.fcrepo.migration.validator.api.ValidationResult.ValidationType.METADATA;
import static org.fcrepo.migration.validator.api.ValidationResult.ValidationType.SOURCE_OBJECT_DELETED;
import static org.fcrepo.migration.validator.api.ValidationResult.ValidationType.SOURCE_OBJECT_EXISTS_IN_TARGET;
import static org.fcrepo.migration.validator.api.ValidationResult.ValidationType.SOURCE_OBJECT_RESOURCE_DELETED;
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
    private static final DateTimeFormatter ISO_8601 = DateTimeFormatter.ISO_INSTANT;

    public static final String F3_LABEL = "info:fedora/fedora-system:def/model#label";
    public static final String F3_STATE = "info:fedora/fedora-system:def/model#state";
    public static final String F3_CREATED_DATE = "info:fedora/fedora-system:def/model#createdDate";
    public static final String F3_LAST_MODIFIED_DATE = "info:fedora/fedora-system:def/view#lastModifiedDate";
    public static final String F3_OWNER_ID = "info:fedora/fedora-system:def/model#ownerId";

    private static final String RELS_INT = "RELS-INT";
    private static final String DOWNLOAD_NAME_PROP = "info:fedora/fedora-system:def/model#downloadFilename";
    private static final String RELS_DELETED_ENTRY = "FCREPO_MIGRATION_VALIDATOR_DELETED_ENTRY";

    private F3State objectState;
    private ObjectInfo objectInfo;
    private final boolean checksum;
    private final boolean deleteInactive;
    private final boolean validateHeadOnly;
    private final Path ocflRoot;
    private final OcflRepository repository;
    private final OcflObjectSession ocflSession;
    private final List<ValidationResult> validationResults = new ArrayList<>();
    private int indexCounter;
    private final Set<String> headDatastreamIds = new HashSet<>();
    private final F6DigestAlgorithm digestAlgorithm;

    // track changes from RELS-INT
    private final Map<String, List<String>> relsFilenames = new HashMap<>();

    /**
     * Properties which migrated to OCFL headers
     */
    private static final Map<String, PropertyResolver> OCFL_PROPERTY_RESOLVERS = Map.of(
        F3_LABEL, headers -> Optional.empty(),
        F3_STATE, headers -> Optional.empty(),
        F3_OWNER_ID, headers -> Optional.empty(),
        F3_CREATED_DATE, (DateTimeResolver) headers -> Optional.of(headers.getCreatedDate().toString()),
        F3_LAST_MODIFIED_DATE, (DateTimeResolver) headers -> Optional.of(headers.getLastModifiedDate().toString())
    );

    private interface PropertyResolver {
        Optional<String> resolve(ResourceHeaders headers);

        default boolean equals(final String source, final String target) {
            return source.equals(target);
        }

        default Optional<String> fromModel(final Model model, final String ocflId, final String property) {
            return Optional.ofNullable(model.getProperty(model.createResource(ocflId), model.createProperty(property)))
                           .map(Statement::getObject)
                           .map(RDFNode::toString);
        }
    }

    private interface DateTimeResolver extends PropertyResolver {
        @Override
        default boolean equals(final String source, final String target) {
            // For DateTime comparisons we convert back to Instants as Strings might have differences in formatting
            final var sourceDT = Instant.from(ISO_8601.parse(source));
            final var targetDT = Instant.from(ISO_8601.parse(target));
            return sourceDT.equals(targetDT);
        }
    }

    @Override
    public void processObjectVersions(final Iterable<ObjectVersionReference> iterable, final ObjectInfo objectInfo) {
        this.objectInfo = objectInfo;

        final Iterator<ObjectVersionReference> referenceIterator = iterable.iterator();
        if (referenceIterator.hasNext()) {
            final ObjectReference objectReference = referenceIterator.next().getObject();
            LOGGER.debug("beginning processing on object: pid={}", objectInfo);
            if (initialObjectValidation(objectReference.getObjectProperties())) {
                preprocessRelsInt(objectReference);
                objectReference.listDatastreamIds().forEach(dsId -> validateDatastream(dsId, objectReference));
                completeObjectValidation();
            }
        }
    }

    /**
     * Process the RELS-INT for any updates to datastreams which we need to track later
     */
    private void preprocessRelsInt(final ObjectReference objectReference) {
        final var pid = objectInfo.getPid();

        final var filenameMap = new HashMap<String, String>();
        final var dsVersions = objectReference.getDatastreamVersions(RELS_INT);

        for (final var dsVersion : dsVersions) {
            final var rdf = parseRdf(dsVersion);
            final var models = splitRelsInt(rdf);

            final var oldIds = new HashSet<>(filenameMap.keySet());
            filenameMap.clear();

            // Pretty much the same as ArchiveGroupHandler - check for the downloadFilename triple in RELS-INT
            // and keep a running list of changes/deletes
            models.forEach((id, model) -> {
                model.listStatements().forEach(statement -> {
                    if (DOWNLOAD_NAME_PROP.equals(statement.getPredicate().getURI())) {
                        LOGGER.trace("{} has download prop for {}", pid, id);
                        final var filename = statement.getObject().toString();
                        final var prevFilenames =
                            relsFilenames.computeIfAbsent(id, ignored -> new ArrayList<>(List.of(filename)));

                        // track filenames changes in RELS-INT
                        if (!prevFilenames.get(prevFilenames.size() - 1).equals(filename)) {
                            prevFilenames.add(filename);
                        }
                        filenameMap.put(id, statement.getObject().toString());
                    }
                });
            });

            // when a deleted filename occurs, track that with a distinct name
            final var deleted = Sets.difference(oldIds, filenameMap.keySet());
            deleted.forEach(id -> {
                LOGGER.trace("{} has a deleted download prop for {}", pid, id);
                relsFilenames.get(id).add(RELS_DELETED_ENTRY);
            });
        }
    }

    /**
     * Constructor
     *
     * @param session
     * @param config
     */
    public ValidatingObjectHandler(final OcflObjectSession session, final ObjectValidationConfig config) {
        this.ocflSession = session;
        this.checksum = config.isChecksum();
        this.ocflRoot = config.getOcflRoot();
        this.repository = config.getOcflRepository();
        this.deleteInactive = config.deleteInactive();
        this.digestAlgorithm = config.getDigestAlgorithm();
        this.validateHeadOnly = config.isValidateHeadOnly();
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
        final ResourceHeaders headers;
        final var pid = objectInfo.getPid();
        final var ocflId = ocflSession.ocflObjectId();
        final var model = ModelFactory.createDefaultModel();

        try {
            headers = ocflSession.readHeaders(ocflId);

            // read the fcr-container.nt as well
            ocflSession.readContent(ocflId)
                       .getContentStream()
                       .ifPresent(is -> RDFDataMgr.read(model, is, RDFFormat.NTRIPLES.getLang()));

            validationResults.add(new ValidationResult(indexCounter++, OK, OBJECT, SOURCE_OBJECT_EXISTS_IN_TARGET,
                                                       pid, ocflId, "Source object is present in target repository."));
        } catch (NotFoundException ex) {
            validationResults.add(new ValidationResult(indexCounter++, FAIL, OBJECT, SOURCE_OBJECT_EXISTS_IN_TARGET,
                    pid, ocflId, "Source object not present in target repository."));
            return false;
        }

        // set the object state
        final var properties = objectProperties.listProperties();
        final var stateProperty = properties.stream()
                                            .filter(p -> p.getName().equals(F3_STATE))
                                            .findFirst()
                                            .orElseThrow(() -> new IllegalStateException("Could not find " + F3_STATE +
                                                                                         "on object" + ocflId));
        objectState = F3State.fromProperty(stateProperty);

        final var builder = new ValidationResultBuilder(pid, ocflId, null, null, OBJECT);
        if (objectState.isDeleted(deleteInactive)) {
            final var success = "pid: %s -> object deleted states match: source=%s, target=%s";
            final var error = "pid: %s -> object deleted states do not match: source=%s, target=%s";

            // if an object is deleted, only validate that the deleted flag is set
            final ValidationResult deletedResult;
            if (headers.isDeleted()) {
                deletedResult = builder.ok(SOURCE_OBJECT_DELETED, format(success, pid, objectState, true));
            } else {
                deletedResult = builder.fail(SOURCE_OBJECT_DELETED, format(error, pid, objectState, false));
            }

            validationResults.add(deletedResult);
        } else {
            properties.forEach(op -> validateObjectProperty(op, headers, model, builder));
        }

        return true;
    }

    private void validateObjectProperty(final ObjectProperty op, final ResourceHeaders headers,
                                        final Model model, final ValidationResultBuilder builder) {
        final var pid = objectInfo.getPid();
        final var ocflId = ocflSession.ocflObjectId();
        final var property = op.getName();
        final var sourceValue = op.getValue();

        LOGGER.info("PID = {}, object property: name = {}, value = {}", pid, property, sourceValue);
        final var resolver = OCFL_PROPERTY_RESOLVERS.get(property);
        if (resolver != null) {
            final var success = "pid: %s -> properties match: f3 prop name=%s, source=%s, target=%s";
            final var error = "pid: %s -> properties do not match: f3 prop name=%s, source=%s, target=%s";
            final var notFound = "pid: %s -> property not found in OCFL: f3 prop name=%s, source=%s";

            // try to get the ocfl property from the headers first, otherwise fallback to reading the n-triples
            final var result =
                resolver.resolve(headers)
                        .or(() -> resolver.fromModel(model, ocflId, property))
                        .map(targetVal -> resolver.equals(sourceValue, targetVal) ?
                                 builder.ok(METADATA, format(success, pid, property, sourceValue, targetVal)) :
                                 builder.fail(METADATA, format(error, pid, property, sourceValue, targetVal)))
                        .orElse(builder.fail(METADATA, format(notFound, pid, property, sourceValue)));
            validationResults.add(result);
        }
    }

    public void validateDatastream(final String dsId, final ObjectReference objectReference) {
        final var dsVersions = objectReference.getDatastreamVersions(dsId);
        final var sourceObjectId = objectInfo.getPid();
        final var targetObjectId = ocflSession.ocflObjectId();
        final var sourceResource = sourceObjectId + "/" + dsId;
        final var targetResource = targetObjectId + "/" + dsId;
        final var targetVersions = ocflSession.listVersions(targetResource);
        final var builder = new ValidationResultBuilder(sourceObjectId, targetObjectId, sourceResource, targetResource,
                                                        OBJECT_RESOURCE);

        var sourceVersionCount = 0;
        var sourceDeletedCount = 0;
        String sourceCreated = null;

        final var downloadFilenames = Optional.ofNullable(relsFilenames.get(sourceResource));
        final int softVersionCount =
            downloadFilenames.map(filenames -> searchSoftVersions(sourceResource, targetVersions, filenames))
                             .orElse(0);

        for (final var dsVersion : dsVersions) {
            final var dsInfo = dsVersion.getDatastreamInfo();

            // in f3 the created entry on the first version is what we want to check against for all ocfl versions
            if (sourceCreated == null) {
                sourceCreated = dsVersion.getCreated();
            }

            // setup the version info and check for deleted/head datastreams
            // if head store the dataStreamId for future validations and skip all versions from rels-int changes
            final String version;
            final int currentVersion;
            final var isHead = dsVersion.isLastVersionIn(objectReference);
            if (isHead) {
                version = "HEAD";
                headDatastreamIds.add(dsId);
                currentVersion = sourceVersionCount + softVersionCount;
            } else {
                version = "version " + sourceVersionCount;
                currentVersion = sourceVersionCount;
            }

            try {
                final var ocflVersionInfo = targetVersions.get(currentVersion + sourceDeletedCount);
                final var objectVersionId = ObjectVersionId.version(ocflVersionInfo.getOcflObjectId(),
                                                                    ocflVersionInfo.getVersionNumber());
                final var headers = ocflSession.readHeaders(targetResource, ocflVersionInfo.getVersionNumber());
                final var ocflObject = repository.getObject(objectVersionId);

                if (isHead || !validateHeadOnly) {
                    validateSize(dsVersion, headers, ocflObject, version, builder);
                    validateCreatedDate(sourceCreated, headers, version, builder);
                    validateLastModified(dsVersion, headers, version, builder);
                    validateChecksum(dsVersion, headers, version, builder);
                }
            } catch (NotFoundException | IndexOutOfBoundsException ex) {
                final var error = "Source object resource does not exist in target for source version=%d";
                validationResults.add(builder.fail(SOURCE_OBJECT_RESOURCE_EXISTS_IN_TARGET,
                                                   format(error, currentVersion)));
            }

            // check if we need to handle a delete as well
            final var state = F3State.fromString(dsInfo.getState());
            if (state.isDeleted(deleteInactive) || (isHead && objectState.isDeleted(deleteInactive))) {
                sourceDeletedCount++;
                headDatastreamIds.remove(dsId);
                validateDeleted(targetResource, currentVersion, targetVersions, builder);
            }

            sourceVersionCount++;
        }

        final var versionSuccess = "binary version counts match for resource: source=%d, RELS-INT=%d, target=%d";
        final var versionFailure = "binary version counts do not match for resource: source=%d, RELS-INT=%d, target=%d";
        final var f3VersionCount = sourceVersionCount + softVersionCount;
        final var targetVersionCount = targetVersions.size() - sourceDeletedCount;
        if (f3VersionCount == targetVersionCount) {
            final var details = format(versionSuccess, sourceVersionCount, softVersionCount, targetVersionCount);
            validationResults.add(builder.ok(BINARY_VERSION_COUNT, details));
        } else {
            final var details = format(versionFailure, sourceVersionCount, softVersionCount, targetVersionCount);
            validationResults.add(builder.fail(BINARY_VERSION_COUNT, details));
        }
    }

    private int searchSoftVersions(final String sourceResource,
                                   final List<OcflVersionInfo> targetVersions,
                                   final List<String> filenames) {
        int transitions = 0;
        for (final var targetVersion : targetVersions) {
            if (filenames.isEmpty()) {
                break;
            }

            final var f3Filename = filenames.remove(0);
            final var headers = ocflSession.readHeaders(targetVersion.getResourceId(),
                                                        targetVersion.getVersionNumber());
            if (!f3Filename.equals(headers.getFilename())) {
                LOGGER.debug("{} has filename update {} -> {}", sourceResource, headers.getFilename(), f3Filename);
                transitions++;
            }
        }

        return transitions;
    }

    private Model parseRdf(final DatastreamVersion dv) {
        final var model = ModelFactory.createDefaultModel();
        try (var is = dv.getContent()) {
            RDFDataMgr.read(model, is, Lang.RDFXML);
            return model;
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse RDF XML", e);
        }
    }

    private Map<String, Model> splitRelsInt(final Model relsIntModel) {
        final var infoFedora = "info:fedora/";
        final Map<String, Model> splitModels = new HashMap<>();
        for (final var it = relsIntModel.listStatements(); it.hasNext();) {
            final var statement = it.next();
            final var uri = statement.getSubject().getURI();
            final var id = uri.startsWith(infoFedora) ? uri.substring(infoFedora.length()) : uri;
            final var model = splitModels.computeIfAbsent(id, k -> ModelFactory.createDefaultModel());
            model.add(statement);
        }
        return splitModels;
    }

    private void validateDeleted(final String resource,
                                 final int sourceVersionCount,
                                 final List<OcflVersionInfo> versions,
                                 final ValidationResultBuilder builder) {
        final var version = "version " + sourceVersionCount;
        final var success = "%s is marked as deleted";
        final var failure = "%s is not marked as deleted in Fedora 6 OCFL";
        final var error = "Deleted object for %s does not exist in Fedora 6 OCFL";
        try {
            // ocfl creates a new version for deletes, so we need to get the next highest version
            final var versionInfo = versions.get(sourceVersionCount + 1);
            final var headers = ocflSession.readHeaders(resource, versionInfo.getVersionNumber());
            if (headers.isDeleted()) {
                validationResults.add(builder.ok(SOURCE_OBJECT_RESOURCE_DELETED, format(success, version)));
            } else  {
                validationResults.add(builder.fail(SOURCE_OBJECT_RESOURCE_DELETED, format(failure, version)));
            }
        } catch (NotFoundException | IndexOutOfBoundsException ex) {
            validationResults.add(builder.fail(SOURCE_OBJECT_RESOURCE_DELETED, format(error, version)));
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
    private void validateChecksum(final DatastreamVersion dsVersion,
                                  final ResourceHeaders headers,
                                  final String version,
                                  final ValidationResultBuilder builder) {
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
                validationResults.add(builder.fail(BINARY_CHECKSUM, format(exception, version, e)));
                return; // halt further validation
            }

            // retrieve the digest from the ocfl headers
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

    private void validateLastModified(final DatastreamVersion dsVersion,
                                      final ResourceHeaders headers,
                                      final String version,
                                      final ValidationResultBuilder builder) {
        final var error = "%s binary last modified dates do no match: sourceValue=%s, targetValue=%s";
        final var success = "%s binary last modified dates match: %s";

        // the last modified date in ocfl is derived from when a datastream was created
        // so we check equality of the two values
        final var sourceValue = Instant.from(ISO_8601.parse(dsVersion.getCreated()));
        final var targetValue = headers.getLastModifiedDate();

        if (sourceValue.equals(targetValue)) {
            validationResults.add(builder.ok(BINARY_METADATA, format(success, version, sourceValue)));
        } else {
            validationResults.add(builder.fail(BINARY_METADATA, format(error, version, sourceValue, targetValue)));
        }
    }

    private void validateCreatedDate(final String sourceCreated,
                                     final ResourceHeaders headers,
                                     final String version,
                                     final ValidationResultBuilder builder) {
        final var error = "%s binary creation dates do no match: sourceValue=%s, targetValue=%s";
        final var success = "%s binary creation dates match: %s";

        final var sourceInstant = Instant.from(ISO_8601.parse(sourceCreated));
        final var targetCreated = headers.getCreatedDate();
        if (sourceInstant.equals(targetCreated)) {
            validationResults.add(builder.ok(BINARY_METADATA, format(success, version, sourceCreated)));
        } else {
            validationResults.add(builder.fail(BINARY_METADATA, format(error, version, sourceCreated, targetCreated)));
        }
    }

    private void validateSize(final DatastreamVersion dsVersion,
                              final ResourceHeaders headers,
                              final OcflObjectVersion ocflObjectVersion,
                              final String version,
                              final ValidationResultBuilder builder) {
        final var error = "%s binary size does not match: sourceValue=%s, targetValue=%s";
        final var success = "%s binary size matches: %s";
        final var notFound = "%s %s file could not be found to check size!";

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

            // compare file size by looking at the filesystem
            final var sourceFile = dsVersion.getFile();
            final var result = sourceFile.map(file -> {
                final var ocflRelativePath = ocflObjectVersion.getFile(headers.getContentPath())
                                                              .getStorageRelativePath();
                final var targetPath = ocflRoot.resolve(ocflRelativePath);
                if (Files.notExists(targetPath)) {
                    return builder.fail(BINARY_SIZE, format(notFound, version, "target"));
                }

                final var sourceBytes = file.length();
                final var targetBytes = targetPath.toFile().length();
                if (sourceBytes == targetBytes) {
                    return builder.ok(BINARY_SIZE, format(success, version, sourceBytes));
                }
                return builder.fail(BINARY_SIZE, format(error, version, sourceBytes, targetBytes));
            }).orElse(builder.fail(BINARY_SIZE, format(notFound, version, "source")));
            validationResults.add(result);
        }
    }

    private void completeObjectValidation() {
        final var pid = objectInfo.getPid();
        final var ocflId = ocflSession.ocflObjectId();
        final var nonRdfSource = "http://www.w3.org/ns/ldp#NonRDFSource";
        final var ocflResourceCount = ocflSession.streamResourceHeaders()
                       .filter(r -> !r.isDeleted() && r.getInteractionModel().equals(nonRdfSource))
                       .count();
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
