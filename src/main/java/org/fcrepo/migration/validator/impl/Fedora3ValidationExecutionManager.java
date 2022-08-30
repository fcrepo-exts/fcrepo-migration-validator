/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree.
 */
package org.fcrepo.migration.validator.impl;

import org.fcrepo.migration.ObjectSource;
import org.fcrepo.migration.validator.api.ObjectValidationConfig;
import org.fcrepo.migration.validator.api.ResumeManager;
import org.fcrepo.migration.validator.api.ValidationExecutionManager;
import org.fcrepo.migration.validator.api.ValidationResultWriter;
import org.fcrepo.migration.validator.api.ValidationTask;
import org.fcrepo.storage.ocfl.OcflObjectSessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This class is responsible for coordinating and managing the lifecycle of the classes involved in a validation run.
 *
 * @author dbernstein
 */
public class Fedora3ValidationExecutionManager implements ValidationExecutionManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(Fedora3ValidationExecutionManager.class);

    private long numProcessed;
    private final int limit;
    private final AtomicBoolean abort;
    private final Semaphore semaphore;
    private final ResumeManager resumeManager;
    private final ExecutorService executorService;
    private final OcflObjectSessionFactory ocflObjectSessionFactory;
    private final ValidationResultWriter writer;
    private final ObjectSource source;
    private final Set<String> objectsToValidate;
    private final ObjectValidationConfig objectValidationConfig;
    private final ApplicationConfigurationHelper config;

    /**
     * Constructor
     * @param config The config
     */
    public Fedora3ValidationExecutionManager(final ApplicationConfigurationHelper config) {
        this.config = config;
        this.limit = config.getLimit();
        this.source = config.objectSource();
        this.writer = config.validationResultWriter();
        this.objectsToValidate = config.readObjectsToValidate();
        this.ocflObjectSessionFactory = config.ocflObjectSessionFactory();
        this.executorService = Executors.newFixedThreadPool(config.getThreadCount());
        this.semaphore = new Semaphore(config.getThreadCount());
        this.objectValidationConfig = config.getObjectValidationConfig();
        this.abort = new AtomicBoolean();
        this.resumeManager = config.resumeManager();
        this.numProcessed = 0;
    }

    @Override
    public boolean doValidation() {
        try {
            // track count of objects processed for final f3 to ocfl validation
            var totalCount = 0L;
            var halted = false;

            // When iterating, we block on the semaphore as creating a new ObjectProcessor will open a file handle
            for (final var objectProcessor : source) {
                totalCount++;
                if (resumeManager.accept(objectProcessor.getObjectInfo().getPid())) {
                    if (abort.get() || halted) {
                        break;
                    }

                    numProcessed++;
                    final var sourceObjectId = objectProcessor.getObjectInfo().getPid();
                    try {
                        if (objectsToValidate.isEmpty() || objectsToValidate.contains(sourceObjectId)) {
                            final var task = new F3ObjectValidationTaskBuilder()
                                .processor(objectProcessor)
                                .withValidationConfig(objectValidationConfig)
                                .writer(writer)
                                .objectSessionFactory(ocflObjectSessionFactory)
                                .build();
                            submit(task);
                        }
                    } catch (InterruptedException ex) {
                        LOGGER.error("Error submitting task", ex);
                        abort.set(true);
                    }

                    halted = limit != 0 && limit <= numProcessed;
                }
            }

            // only run repository validator if doing a full run
            final var repository = config.ocflRepository();
            //final var checkNumObjects = config.checkNumObjects() && objectsToValidate.isEmpty() && !halted;
            final var repositoryTask = new F3RepositoryValidationTask(config, totalCount, repository, writer);
            submit(repositoryTask);

            awaitCompletion();

            resumeManager.updateResumeFile();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        } finally {
            try {
                shutdown();
            } catch (InterruptedException ex) {
            }
        }

        return !abort.get();
    }

    private void submit(final ValidationTask task) throws InterruptedException {
        semaphore.acquire();
        CompletableFuture.supplyAsync(task, executorService)
                         .whenComplete(this::finishTask);
    }

    /**
     * Check if a ValidationTask completed successfully and release a permit on the semaphore for other tasks
     *
     * @param task the task which was run
     * @param throwable the exception thrown by the ValidationTask
     */
    private void finishTask(final ValidationTask task, final Throwable throwable) {
        semaphore.release();

        //TODO Handle this in such a away that it is captured in the final report
        //https://jira.lyrasis.org/browse/FCREPO-3633
        if (throwable != null) {
            LOGGER.error("Validation task failed", throwable);
            abort.set(true);
        } else {
            task.getPid().ifPresent(resumeManager::completed);
        }
    }


    /**
     * Blocks until all migration tasks are complete.
     *
     * @throws InterruptedException on interrupt
     */
    private void awaitCompletion() throws InterruptedException {
        semaphore.acquire(config.getThreadCount());
    }

    /**
     * Shutsdown the executor and closes all resources.
     *
     * @throws InterruptedException on interrupt
     */
    private void shutdown() throws InterruptedException {
        try {
            executorService.shutdown();
            if (!executorService.awaitTermination(1, TimeUnit.MINUTES)) {
                LOGGER.error("Failed to shutdown executor service cleanly after 1 minute of waiting");
                executorService.shutdownNow();
            }
        } finally {
            //close any open resources.
        }
    }

    /**
     * Returns the number of PIDs which were validated
     * @return numProcessed
     */
    public long getNumProcessed() {
        return numProcessed;
    }
}
