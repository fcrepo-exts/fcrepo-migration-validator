package org.fcrepo.migration.validator.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.PriorityBlockingQueue;

import org.fcrepo.migration.validator.api.ResumeManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResumeManagerImpl implements ResumeManager {

    private final static Logger LOGGER = LoggerFactory.getLogger(ResumeManagerImpl.class);

    private int index;
    private String value = "foo";
    private int pidResumeIndex;
    private String pidResumeValue;
    private final int limit;
    private final boolean acceptAll;
    private final Path resumeFile;
    private final PriorityBlockingQueue<Process> processing;

    public ResumeManagerImpl(final Path pidDir, final int limit, final boolean acceptAll) {
        this.limit = limit;
        this.acceptAll = acceptAll;
        this.resumeFile = pidDir.resolve("resume.txt");
        this.processing = new PriorityBlockingQueue<>();
        try {
            loadResumeFile();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void loadResumeFile() throws IOException {
        if (!Files.exists(resumeFile) || Files.size(resumeFile) == 0) {
            updateResumeFile(0, "");
        }

        try (var bufferedReader = Files.newBufferedReader(resumeFile)) {
            this.pidResumeValue = bufferedReader.readLine();
            this.pidResumeIndex = Integer.parseInt(bufferedReader.readLine());
        }
    }

    private void updateResumeFile(final int index, final String pid) {
        final String lineSeparator = System.lineSeparator();
        try (var writer = Files.newBufferedWriter(resumeFile)) {
            writer.write(pid + lineSeparator);
            writer.write(index);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public State accept(final String pid) {
        final String logMsg = "PID: " + pid + ", accept? ";

        final int liminal = limit + pidResumeIndex;
        final String previousValue = value;
        value = pid;
        index++;

        // Do not accept, the previous run index is higher
        if (index - 1 < pidResumeIndex) {
            // Are we accepting all?
            LOGGER.debug(logMsg + acceptAll);
            return acceptAll ? State.OK : State.SKIP;
        }

        // We are at the first PID that has not previously been processed
        if (index - 1 == pidResumeIndex) {
            // index matches, but value DOES NOT match the last state of previous run!
            if (!previousValue.equalsIgnoreCase(pidResumeValue)) {
                final String msg = "Number of accept requests does not align with expected PID value! " +
                                   "index: " + index + ", " +
                                   "pid: " + pid + ", " +
                                   "expected pid: " + pidResumeValue;
                throw new IllegalStateException(msg);
            }
        }

        processing.put(new Process(pid, index));

        if (index < liminal) {
            LOGGER.debug(logMsg + true);
            return State.OK;
        } else {
            LOGGER.debug(logMsg + false);
            return State.HALT_LIMIT;
        }
    }

    public void complete(final String pid, final int index) {
        // are we the head (i.e. first in)? if so write out the resume file
        // otherwise, ignore and remove the pid from the pqueue
        final var lowest = processing.peek();
        if (lowest == null || index == lowest.index) {
            updateResumeFile(index, pid);
        }

        processing.remove(new Process(pid, index));
    }

    private static class Process implements Comparable<Process> {

        private final int index;
        private final String pid;

        private Process(final String pid, final int index) {
            this.pid = pid;
            this.index = index;
        }

        @Override
        public int compareTo(final Process o) {
            return Integer.compare(index, o.index);
        }
    }
}
