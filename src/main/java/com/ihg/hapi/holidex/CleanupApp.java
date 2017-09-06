package com.ihg.hapi.holidex;

import com.google.common.base.Stopwatch;
import com.google.common.base.Strings;
import org.apache.commons.cli.*;
import org.apache.commons.text.RandomStringGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.schedulers.Schedulers;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * User: chaubaa Date: 9/6/2017
 */
public class CleanupApp {
    private static final Logger LOGGER = LoggerFactory.getLogger(CleanupApp.class);
    private static final String DATE_REGEX = "^(\\d{6}).*$";
    private static final String[] FILE_TYPES = {"convp", "rcinv", "jvm", "convsmw", "convtsw"};
    private static SimpleDateFormat FMT = new SimpleDateFormat("MMddyy");
    private static RandomStringGenerator generator;
    private Pattern dateRegex;
    private Date startDate, endDate;
    private String sourceDir, targetDir;
    private int cnt;
    private String fileDate;
    private String deleteDirectory;

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
        generator = new RandomStringGenerator.Builder().withinRange('a', 'z').build();
    }

    public static void main(String[] args) {

    }

    /**
     * int cnt, String date, String dirName, String targetDir) {
     *
     * @param args
     */
    private void parseCmdOptions(String[] args) {
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

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd = null;

        try {
            cmd = parser.parse(options, args);

            String count = cmd.getOptionValue("count");
            fileDate = cmd.getOptionValue("date");
            sourceDir = cmd.getOptionValue("sourceDir");
            targetDir = cmd.getOptionValue("targetDir");
            if (!Strings.isNullOrEmpty(count)) {
                cnt = Integer.parseInt(count);
            }

            // delete options
            deleteDirectory = cmd.getOptionValue("deleteDir");

        } catch (org.apache.commons.cli.ParseException e) {
            LOGGER.error("Err: parsing options", e);
            formatter.printHelp("utility-name", options);
            System.exit(1);
        }

        String inputFilePath = cmd.getOptionValue("input");
        String outputFilePath = cmd.getOptionValue("output");

        System.out.println(inputFilePath);
        System.out.println(outputFilePath);

    }

    public void delete(String dirName) {
        Stopwatch stopwatch = Stopwatch.createStarted();
        Path dir = Paths.get(dirName);

        LOGGER.debug("Working on dir:{}", dir.toString());
        try {
            Files.walkFileTree(dir, new FileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    LOGGER.debug(">> BEGIN :{}", dir.toString());
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Matcher m = dateRegex.matcher(file.getFileName().toString());
                    if (m.find()) {
                        LOGGER.debug(">>Found:{}", m.group(1));
                        try {
                            Date fileDate = FMT.parse(m.group(1));
                            if (!(fileDate.before(startDate) || fileDate.after(endDate))) {
                                LOGGER.debug(">>>\tDeleting file:{}", file.getFileName());
                            }
                        } catch (ParseException e) {
                            LOGGER.error("Err: can't convert date in file name:{} to date", file.getFileName(), e);
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                    LOGGER.error("Err: couldn't get file attributes:{}", file.getFileName(), exc);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    LOGGER.debug(">> END-->{}", stopwatch.stop());
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            LOGGER.error("Err: Deleting files from dir:{}", dirName, e);
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
            String sb = date + generator.generate(5) +
                    FILE_TYPES[ThreadLocalRandom.current().nextInt(0, FILE_TYPES.length)] + EXT;
            File newFile = new File(parent + sb);
            Observable.just(newFile).observeOn(Schedulers.computation()).subscribe(file -> {
                LOGGER.debug("Working on file:{}", file.toString());
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
        LOGGER.debug(">> END in:{}", stopwatch.stop());
    }
}
