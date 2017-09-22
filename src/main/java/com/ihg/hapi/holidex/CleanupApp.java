package com.ihg.hapi.holidex;

import com.google.common.base.Stopwatch;
import com.google.common.base.Strings;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.apache.commons.cli.*;
import org.apache.commons.text.RandomStringGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.Subscriber;
import rx.schedulers.Schedulers;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * User: chaubaa Date: 9/6/2017
 */
public class CleanupApp {
    private static final Logger LOGGER = LoggerFactory.getLogger(CleanupApp.class);
    private static final String DATE_REGEX = "^(\\d{6}).*$";
    private static final String[] FILE_TYPES = {"convp", "rcinv", "jvm", "convsmw", "convtsw"};
    private static final int JOB_COMPLETION_WAIT_MIN = 2;
    private static SimpleDateFormat FMT = new SimpleDateFormat("MMddyy");
    private static RandomStringGenerator generator = new RandomStringGenerator.Builder().withinRange('a', 'z').build();
    private int maxSize = 50;
    private Pattern dateRegex;
    private Date startDate, endDate;
    private String sourceDir, targetDir;
    private int cnt;
    private String fileDate;
    private String deleteDirectory;
    private boolean isDelete = false;
    private boolean isGenerate = false;
    private boolean isDryRun = false;
    private ExecutorService executor;
    private AtomicInteger count = new AtomicInteger(0);
    private AtomicInteger desiredCount = new AtomicInteger(0);
    private AtomicInteger actualCount = new AtomicInteger(0);

    public CleanupApp() {
        createExecutor();
    }

    /**
     * accept date strings and convert them to date type
     *
     * @param start
     * @param end
     */
    public CleanupApp(String start, String end) {
        if (Strings.isNullOrEmpty(start) || Strings.isNullOrEmpty(end)) {
            throw new IllegalArgumentException("Err: start and end need to be valid dates:MMddyy format");
        }
        try {
            startDate = FMT.parse(start);
            endDate = FMT.parse(end);
        } catch (ParseException e) {
            throw new IllegalArgumentException("Err: start/end date not valid format: MMddyy");
        }
        dateRegex = Pattern.compile(DATE_REGEX);
        createExecutor();
    }

    public static void main(String[] args) {
        CleanupApp app = new CleanupApp().parseCmdOptions(args);
        if (app.isDelete) {
            LOGGER.debug("##[DELETE]: dir:{} start:{} end:{}", app.deleteDirectory, app.startDate, app.endDate);
            if (!app.isDryRun) {
                app.delete(app.deleteDirectory);
            }
        } else if (app.isGenerate) {
            LOGGER.debug("##[GENERATE]: source:{} target:{} fileDate:{} cnt:{}", app.sourceDir, app.targetDir, app
                    .fileDate, app.cnt);
            if (!app.isDryRun) {
                app.generate(app.cnt, app.fileDate, app.sourceDir, app.targetDir);
            }
        }
        if (app.isDryRun) {
            LOGGER.debug(">> Dry run completed...");
        }
    }

    private void createExecutor() {
        ThreadFactory namedThreadFactory = new ThreadFactoryBuilder().setNameFormat("Rx-Delete-%d").build();
        executor = Executors.newFixedThreadPool(maxSize, namedThreadFactory);
    }

    /**
     * int cnt, String date, String dirName, String targetDir) {
     *
     * @param args
     */
    CleanupApp parseCmdOptions(String[] args) {
        Options options = new Options();

        Option generate = new Option("g", "generate", false, "generate test data");
        generate.setRequired(false);
        options.addOption(generate);
        Option generateCnt = new Option("c", "count", true, "number of files to generate");
        generateCnt.setRequired(false);
        options.addOption(generateCnt);
        Option generateDate = new Option("dt", "date", true, "date for files");
        generateDate.setRequired(false);
        options.addOption(generateDate);
        Option generateSourceDir = new Option("src", "sourceDir", true, "source directory");
        generateSourceDir.setRequired(false);
        options.addOption(generateSourceDir);
        Option generateTargetDir = new Option("tgt", "targetDir", true, "target directory");
        generateTargetDir.setRequired(false);
        options.addOption(generateTargetDir);

        Option delete = new Option("d", "delete", false, "delete files from dir");
        delete.setRequired(false);
        options.addOption(delete);
        Option deleteDir = new Option("dd", "deleteDir", true, "directory to delete");
        deleteDir.setRequired(false);
        options.addOption(deleteDir);
        Option startDateOption = new Option("sd", "startDate", true, "start date for files:MMddyy [011516]");
        startDateOption.setRequired(false);
        options.addOption(startDateOption);
        Option endDateOption = new Option("ed", "endDate", true, "end date for files:MMddyy [031418]");
        endDateOption.setRequired(false);
        options.addOption(endDateOption);

        // thread
        Option maxThreadOpt = new Option("threads", "maxThreads", true, "max threads for processing");
        maxThreadOpt.setRequired(false);
        options.addOption(maxThreadOpt);

        // DRY Run
        Option dryOpt = new Option("dry", "dryRun", false, "dry run, no action taken");
        delete.setRequired(false);
        options.addOption(dryOpt);

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd = null;
        String sD = null, eD = null;
        String count = null;
        boolean isError = false;
        try {
            cmd = parser.parse(options, args);
            if (cmd.hasOption("g")) {
                count = cmd.getOptionValue("count");
                fileDate = cmd.getOptionValue("date");
                sourceDir = cmd.getOptionValue("sourceDir");
                targetDir = cmd.getOptionValue("targetDir");
                if (!Strings.isNullOrEmpty(count)) {
                    cnt = Integer.parseInt(count);
                }
                File src = new File(sourceDir);
                File tgt = new File(targetDir);
                if (!src.exists() || !src.isDirectory() || !tgt.exists() || !tgt.isDirectory()) {
                    throw new org.apache.commons.cli.ParseException("Err: src or target dir not valid");
                }
                if (cnt < 1) {
                    throw new org.apache.commons.cli.ParseException("Err: generate record count needs to be > 0");
                }
                FMT.parse(fileDate);
                isGenerate = true;
            } else if (cmd.hasOption("d")) {
                deleteDirectory = cmd.getOptionValue("deleteDir");
                sD = cmd.getOptionValue("startDate");
                eD = cmd.getOptionValue("endDate");
                startDate = FMT.parse(sD);
                endDate = FMT.parse(eD);
                if (!new File(deleteDirectory).exists()) {
                    throw new org.apache.commons.cli.ParseException("Err: delete dir:" + deleteDirectory + " does not" +
                            " exist or can't be accessed");
                }
                LOGGER.debug(">> DELETE action selected.{} -To- {} on dir:{}", startDate, endDate, deleteDirectory);
                dateRegex = Pattern.compile(DATE_REGEX);
                isDelete = true;
            }
            String maxString = cmd.getOptionValue("threads");
            try {
                int maxTSize = Integer.parseInt(maxString);
                LOGGER.info("New thread size:{}", maxTSize);
                maxSize = maxTSize;
            } catch (NumberFormatException e) {
                LOGGER.info("max threads:{}", maxSize);
            }

            // validation
            if (!cmd.hasOption("d") && !cmd.hasOption("g")) {
                LOGGER.error("Err: either -d or -g needs to be provided");
                isError = true;
            }

            // dry
            if (cmd.hasOption("dry")) {
                isDryRun = true;
            }

        } catch (org.apache.commons.cli.ParseException e) {
            LOGGER.error("Err: parsing options:{}", e.getMessage());
            isError = true;
        } catch (ParseException e) {
            LOGGER.error("Err: date parsing in delete option. start:{} end:{} {}", sD, eD, e.getMessage());
            isError = true;
        } catch (NumberFormatException ne) {
            LOGGER.error("Err: parsing count:{} {}", count, ne.getMessage());
            isError = true;
        }

        if (isError) {
            formatter.printHelp("ftp-cleanup", options);
            System.exit(1);
        }
        return this;
    }

    private void shutdownAndAwaitTermination(ExecutorService pool) {
        pool.shutdown(); // Disable new tasks from being submitted
        try {
            if (!pool.awaitTermination(JOB_COMPLETION_WAIT_MIN, TimeUnit.MINUTES)) {
                LOGGER.error("Forceful shutdown, may lose work!!!");
                pool.shutdownNow(); // Cancel currently executing tasks
                // Wait a while for tasks to respond to being cancelled
                if (!pool.awaitTermination(JOB_COMPLETION_WAIT_MIN, TimeUnit.MINUTES))
                    LOGGER.debug("Pool did not terminate in {} min.", JOB_COMPLETION_WAIT_MIN);
            }
        } catch (InterruptedException ie) {
            // (Re-)Cancel if current thread also interrupted
            pool.shutdownNow();
            // Preserve interrupt status
            Thread.currentThread().interrupt();
        }
        LOGGER.debug(">>Completed executor pool shutdown");
    }

    /**
     * initially tried using Schedulers.io, the problem is we don't know and can't determine when the threads
     * complete reliably.
     * I don't want to count the # of tasks, since that's resource intensive. CachedThreadPool is not useful,
     * since it can quickly exhaust the resources, there is no upper limit.
     * so had to choose fixed thread pool. using an executor service so that we can shutdown
     *
     * @param dirName
     */
    public void delete(String dirName) {
        Stopwatch stopwatch = Stopwatch.createStarted();
        Path dir = Paths.get(dirName);

        LOGGER.info("Working on dir:{}", dir.toString());
        try {
            Files.walkFileTree(dir, new FileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    LOGGER.debug(">> BEGIN :{}", dir.toString());
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    desiredCount.incrementAndGet();
                    Observable.just(file).observeOn(Schedulers.from(executor)).subscribe(new Subscriber<Path>() {
                        @Override
                        public void onCompleted() {
                            if (LOGGER.isTraceEnabled()) {
                                LOGGER.trace(">>Completed:{}", count.get());
                            }
                            actualCount.incrementAndGet();
                        }

                        @Override
                        public void onError(Throwable e) {

                        }

                        @Override
                        public void onNext(Path path) {
                            deleteFile(path);
                        }
                    });

                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                    LOGGER.error("Err: couldn't get file attributes:{}", file.getFileName(), exc);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    LOGGER.debug(">> END-->{}", stopwatch);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            LOGGER.error("Err: Deleting files from dir:{}", dirName, e);
        }
        LOGGER.debug("Desired count:{} actual count:{}", desiredCount.get(), actualCount.get());

        int sleepCnt = 0;
        while (true) {
            try {
                Thread.sleep(5000L);
            } catch (InterruptedException e) {
                LOGGER.error("Err: sleep interrupted");
            }
            LOGGER.debug("[{}]:POST SLeep: Desired count:{} actual count:{}", sleepCnt, desiredCount.get(),
                    actualCount.get());
            if (desiredCount.get() == actualCount.get() || sleepCnt > 4) {
                break;
            }
            sleepCnt++;
        }
        LOGGER.debug("POST SLeep: Desired count:{} actual count:{}", desiredCount.get(), actualCount.get());
        shutdownAndAwaitTermination(executor);
        LOGGER.info("#{} completed: in:{}", count.get(), stopwatch.stop());
    }

    /**
     * a single file
     *
     * @param file
     */
    private void deleteFile(Path file) {
        Matcher m = dateRegex.matcher(file.getFileName().toString());
        if (m.find()) {
            try {
                Date fileDate = FMT.parse(m.group(1));
                if (!(fileDate.before(startDate) || fileDate.after(endDate))) {
                    boolean stat = Files.deleteIfExists(file);
                    int cnt = count.incrementAndGet();
                    if (LOGGER.isTraceEnabled()) {
                        LOGGER.trace(">>>\tDeleting file:{}->{} count:{}", file.getFileName(), stat, cnt);
                    }
                }
            } catch (ParseException e) {
                LOGGER.error("Err: can't convert date in file name:{} to date", file.getFileName());
            } catch (IOException e) {
                LOGGER.error("Err: IO exception deleting:{}", file.getFileName());
            }
        }
    }

    public void generate(int cnt, String date, String dirName, String targetDir) {
        Stopwatch stopwatch = Stopwatch.createStarted();
        Path dir = Paths.get(dirName);
        File[] files = dir.toFile().listFiles();
        if (files == null || files.length < 1) {
            LOGGER.error("Err: no files in dir:{}", dirName);
            return;
        }
        CountDownLatch latch = new CountDownLatch(cnt);
        File sample = files[0];
        String SEP = System.getProperty("file.separator");
        String parent = targetDir + SEP;
        LOGGER.debug(">> parent:{} sample:{}", parent, sample.toString());
        String EXT = ".txt";
        for (int i = 0; i < cnt; i++) {
            String sb = date + generator.generate(5) + generator.generate(8) + EXT;
            File newFile = new File(parent + sb);
            Observable.just(newFile).observeOn(Schedulers.from(executor)).subscribe(file -> {
                LOGGER.trace("Working on file:{}", file.toString());
                try {
                    Files.copy(sample.toPath(), newFile.toPath());
                } catch (IOException e) {
                    LOGGER.error("Err: copying file:{}->{}", sample.toString(), newFile.toString());
                }
                latch.countDown();
            });
        }
        try {
            latch.await();
        } catch (InterruptedException e) {
            LOGGER.error("Err: waiting for all threads of audit processing in RX via latch", e);
        }
        LOGGER.info(">> END in:{}", stopwatch.stop());
        shutdownAndAwaitTermination(executor);
    }
}
