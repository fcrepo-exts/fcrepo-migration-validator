/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.migration.validator.impl;

import static java.lang.String.format;
import static org.fcrepo.migration.validator.api.ValidationResult.Status.FAIL;
import static org.fcrepo.migration.validator.api.ValidationResult.Status.OK;
import static org.fcrepo.migration.validator.api.ValidationResult.ValidationLevel.OBJECT;
import static org.fcrepo.migration.validator.api.ValidationResult.ValidationLevel.OBJECT_RESOURCE;
import static org.fcrepo.migration.validator.api.ValidationResult.ValidationType.BINARY_HEAD_COUNT;
import static org.fcrepo.migration.validator.api.ValidationResult.ValidationType.BINARY_VERSION_COUNT;
import static org.fcrepo.migration.validator.api.ValidationResult.ValidationType.SOURCE_OBJECT_DELETED;
import static org.fcrepo.migration.validator.api.ValidationResult.ValidationType.SOURCE_OBJECT_EXISTS_IN_TARGET;
import static org.fcrepo.migration.validator.api.ValidationResult.ValidationType.SOURCE_OBJECT_RESOURCE_DELETED;
import static org.fcrepo.migration.validator.api.ValidationResult.ValidationType.SOURCE_OBJECT_RESOURCE_EXISTS_IN_TARGET;
import static org.slf4j.LoggerFactory.getLogger;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.collect.Sets;
import edu.wisc.library.ocfl.api.OcflRepository;
import edu.wisc.library.ocfl.api.model.ObjectVersionId;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.fcrepo.migration.ObjectInfo;
import org.fcrepo.migration.ObjectProperties;
import org.fcrepo.migration.ObjectReference;
import org.fcrepo.migration.ObjectVersionReference;
import org.fcrepo.migration.validator.api.ObjectValidationConfig;
import org.fcrepo.migration.validator.api.ValidationHandler;
import org.fcrepo.migration.validator.api.ValidationResult;
import org.fcrepo.storage.ocfl.OcflObjectSession;
import org.fcrepo.storage.ocfl.OcflVersionInfo;
import org.fcrepo.storage.ocfl.ResourceHeaders;
import org.fcrepo.storage.ocfl.exception.NotFoundException;
import org.slf4j.Logger;

/**
 * A streaming object handler implementation that performs object scoped validations on behalf
 * of the Fedora3ObjectValidator.
 *
 * @author dbernstein
 */
public class ValidatingObjectHandler implements ValidationHandler {
    private static final Logger LOGGER = getLogger(Fedora3ObjectValidator.class);

    private F3State objectState;
    private ObjectInfo objectInfo;
    private final boolean checksum;
    private final boolean deleteInactive;
    private final Path ocflRoot;
    private final OcflRepository repository;
    private final OcflObjectSession ocflSession;
    private final List<ValidationResult> validationResults = new ArrayList<>();
    private final AtomicInteger index;
    private final Set<String> headDatastreamIds = new HashSet<>();
    private final F6DigestAlgorithm digestAlgorithm;

    // track changes from RELS-INT
    private final Map<String, List<String>> relsFilenames = new HashMap<>();

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
        final var dsVersions = Optional.ofNullable(objectReference.getDatastreamVersions(RELS_INT))
                                       .orElse(List.of());

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
        this.index = new AtomicInteger();
        this.checksum = config.isChecksum();
        this.ocflRoot = config.getOcflRoot();
        this.repository = config.getOcflRepository();
        this.deleteInactive = config.deleteInactive();
        this.digestAlgorithm = config.getDigestAlgorithm();
    }

    /**
     * Result the validation results after processObject has been called.
     *
     * @return
     */
    @Override
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
        final var builder = new ValidationResultBuilder(pid, ocflId, null, null, OBJECT, index);

        try {
            headers = ocflSession.readHeaders(ocflId);

            // read the fcr-container.nt as well
            ocflSession.readContent(ocflId)
                       .getContentStream()
                       .ifPresent(is -> RDFDataMgr.read(model, is, RDFFormat.NTRIPLES.getLang()));

            validationResults.add(builder.ok(SOURCE_OBJECT_EXISTS_IN_TARGET,
                                             "Source object is present in target repository."));
        } catch (NotFoundException ex) {
            validationResults.add(builder.fail(SOURCE_OBJECT_EXISTS_IN_TARGET,
                                               "Source object is not present in target repository."));
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
            properties.forEach(op -> validateObjectProperty(ocflId, objectInfo, op, headers, model, builder)
                .ifPresent(validationResults::add));
        }

        return true;
    }

    public void validateDatastream(final String dsId, final ObjectReference objectReference) {
        final var dsVersions = objectReference.getDatastreamVersions(dsId);
        final var sourceObjectId = objectInfo.getPid();
        final var targetObjectId = ocflSession.ocflObjectId();
        final var sourceResource = sourceObjectId + "/" + dsId;
        final var targetResource = targetObjectId + "/" + dsId;
        final var targetVersions = ocflSession.listVersions(targetResource);
        final var builder = new ValidationResultBuilder(sourceObjectId, targetObjectId, sourceResource, targetResource,
                                                        OBJECT_RESOURCE, index);

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

                validateSizeMeta(dsVersion, headers, version, builder).ifPresent(validationResults::add);
                validateSizeOnDisk(dsVersion, ocflRoot, headers, ocflObject, version, builder)
                    .ifPresent(validationResults::add);
                validateCreatedDate(sourceCreated, headers, version, builder).ifPresent(validationResults::add);
                validateLastModified(dsVersion, headers, version, builder).ifPresent(validationResults::add);
                if (checksum) {
                    validateChecksum(dsVersion, headers, digestAlgorithm, version, builder)
                        .ifPresent(validationResults::add);
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

    /**
     * Search for filename updates from RELS-INT metadata changes
     *
     * @param sourceResource the name of the resource
     * @param targetVersions the OCFL versions
     * @param filenames the filename changes from RELS-INT data
     * @return the number of additional version to expect from updated filenames
     */
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

        validationResults.add(new ValidationResult(index.getAndIncrement(), result, OBJECT, BINARY_HEAD_COUNT, pid,
            ocflId, details));
    }

}
