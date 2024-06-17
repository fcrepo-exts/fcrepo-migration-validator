/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.migration.validator.impl;

import static java.lang.String.format;
import static org.fcrepo.migration.validator.api.ValidationResult.ValidationLevel.OBJECT;
import static org.fcrepo.migration.validator.api.ValidationResult.ValidationLevel.OBJECT_RESOURCE;
import static org.fcrepo.migration.validator.api.ValidationResult.ValidationType.BINARY_VERSION_COUNT;
import static org.fcrepo.migration.validator.api.ValidationResult.ValidationType.SOURCE_OBJECT_EXISTS_IN_TARGET;
import static org.fcrepo.migration.validator.api.ValidationResult.ValidationType.SOURCE_OBJECT_RESOURCE_EXISTS_IN_TARGET;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import io.ocfl.api.OcflRepository;
import io.ocfl.api.model.ObjectVersionId;
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
import org.fcrepo.storage.ocfl.exception.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author mikejritter
 */
public class HeadOnlyValidationHandler implements ValidationHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(HeadOnlyValidationHandler.class);

    private ObjectInfo objectInfo;

    private final AtomicInteger index;
    private final Path ocflRoot;
    private final boolean checksum;
    private final boolean deleteInactive;
    private final OcflRepository repository;
    private final OcflObjectSession ocflSession;
    private final F6DigestAlgorithm digestAlgorithm;
    private final List<ValidationResult> validationResults;

    /**
     * Constructor
     *
     * @param session the ocfl session
     * @param config the validation config
     */
    public HeadOnlyValidationHandler(final OcflObjectSession session, final ObjectValidationConfig config) {
        this.index = new AtomicInteger();
        this.ocflSession = session;
        this.checksum = config.isChecksum();
        this.ocflRoot = config.getOcflRoot();
        this.deleteInactive = config.deleteInactive();
        this.digestAlgorithm = config.getDigestAlgorithm();
        this.repository = config.getOcflRepository();
        this.validationResults = new ArrayList<>();
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
            }
        }
    }

    /**
     * Perform initial object level validations. When an object is marked as deleted in F3, make sure it does not exist
     * in OCFL, otherwise perform the normal validations against the object properties.
     *
     * @param objectProperties the object properties
     * @return whether to continue further validations on the object resources
     */
    private boolean initialObjectValidation(final ObjectProperties objectProperties) {
        final var errorActive = "Source object not found in target repository";
        final var errorDeleted = "Source object should be deleted in target repository";
        final var successDeleted = "Source object deleted in target repository";

        final var pid = objectInfo.getPid();
        final var ocflId = ocflSession.ocflObjectId();
        final var model = ModelFactory.createDefaultModel();
        final var builder = new ValidationResultBuilder(pid, ocflId, null, null, OBJECT, index);

        // set the object state
        final var properties = objectProperties.listProperties();
        final var stateProperty = properties.stream()
                                            .filter(p -> p.getName().equals(F3_STATE))
                                            .findFirst()
                                            .orElseThrow(() -> new IllegalStateException("Could not find " + F3_STATE +
                                                                                         "on object" + ocflId));
        boolean continueValidations = false;
        final var objectState = F3State.fromProperty(stateProperty);
        final var isDeleted = objectState.isDeleted(deleteInactive);

        try {
            final var headers = ocflSession.readHeaders(ocflId);
            if (!isDeleted) {
                // read the fcr-container.nt
                ocflSession.readContent(ocflId)
                           .getContentStream()
                           .ifPresent(is -> RDFDataMgr.read(model, is, RDFFormat.NTRIPLES.getLang()));

                properties.forEach(op -> validateObjectProperty(ocflId, objectInfo, op, headers, model, builder)
                    .ifPresent(validationResults::add));
                continueValidations = true;
            } else {
                validationResults.add(builder.fail(SOURCE_OBJECT_EXISTS_IN_TARGET, errorDeleted));
            }
        } catch (final NotFoundException ignored) {
            // object not in ocfl, check if it is deleted in F3 as well
            if (isDeleted) {
                validationResults.add(builder.ok(SOURCE_OBJECT_EXISTS_IN_TARGET, successDeleted));
            } else {
                validationResults.add(builder.fail(SOURCE_OBJECT_EXISTS_IN_TARGET, errorActive));
            }
        }

        return continueValidations;
    }

    private void validateDatastream(final String dsId, final ObjectReference objectReference) {
        final var dsVersions = objectReference.getDatastreamVersions(dsId);
        final var sourceObjectId = objectInfo.getPid();
        final var targetObjectId = ocflSession.ocflObjectId();
        final var sourceResource = sourceObjectId + "/" + dsId;
        final var targetResource = targetObjectId + "/" + dsId;
        final var builder = new ValidationResultBuilder(sourceObjectId, targetObjectId, sourceResource, targetResource,
                                                        OBJECT_RESOURCE, index);

        final var head = dsVersions.get(dsVersions.size() - 1);
        final var dsInfo = head.getDatastreamInfo();
        final var state = F3State.fromString(dsInfo.getState());

        // check that the datastream was deleted + return
        if (state.isDeleted(deleteInactive)) {
            final var success = "Source object resource deleted from ocfl object";
            final var error = "Source object resource does not exist in target for source version";
            try {
                ocflSession.readHeaders(targetResource);
                validationResults.add(builder.fail(SOURCE_OBJECT_RESOURCE_EXISTS_IN_TARGET, error));
            } catch (NotFoundException ex) {
                validationResults.add(builder.ok(SOURCE_OBJECT_RESOURCE_EXISTS_IN_TARGET, success));
            }

            return;
        }

        // get the original created date
        final var created = dsVersions.get(0).getCreated();

        try {
            final var headers = ocflSession.readHeaders(targetResource);
            final var versions = ocflSession.listVersions(targetResource);
            final var ocflVersionInfo = versions.get(0);
            final var objectVersionId = ObjectVersionId.version(ocflVersionInfo.getOcflObjectId(),
                                                                ocflVersionInfo.getVersionNumber());
            final var ocflObject = repository.getObject(objectVersionId);

            // datastream validations
            validateSizeMeta(head, headers, "HEAD", builder).ifPresent(validationResults::add);
            validateSizeOnDisk(head, ocflRoot, headers, ocflObject, "HEAD", builder).ifPresent(validationResults::add);
            validateCreatedDate(created, headers, "HEAD", builder).ifPresent(validationResults::add);
            validateLastModified(head, headers, "HEAD", builder).ifPresent(validationResults::add);
            if (checksum) {
                validateChecksum(head, headers, digestAlgorithm, "HEAD", builder).ifPresent(validationResults::add);
            }

            // validate we have only one version in ocfl
            if (versions.size() == 1) {
                final var versionSuccess = "Resource has single binary version";
                validationResults.add(builder.ok(BINARY_VERSION_COUNT, versionSuccess));
            } else {
                final var versionFailure = "Resource has more than just a HEAD version: count=%s";
                validationResults.add(builder.fail(BINARY_VERSION_COUNT, format(versionFailure, versions.size())));
            }
        } catch (NotFoundException ignored) {
            final var readError = "Source object resource does not exist in target";
            validationResults.add(builder.fail(SOURCE_OBJECT_RESOURCE_EXISTS_IN_TARGET, readError));
        }
    }

    @Override
    public List<ValidationResult> getValidationResults() {
        return validationResults;
    }
}
