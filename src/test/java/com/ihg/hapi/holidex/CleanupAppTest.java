package com.ihg.hapi.holidex;

import org.junit.BeforeClass;
import org.junit.Test;

/**
 * User: chaubaa Date: 9/6/2017
 */
public class CleanupAppTest {
    private static final String LARGE_DIR = "C:\\Users\\chaubaa\\Documents\\project\\poller-etl\\sample_large";
    private static final String TARGET_LARGE_DIR =
            "C:\\Users\\chaubaa\\Documents\\project\\poller-etl\\test_sample_large";

    private static final String startDate = "010100", endDate = "123150";
    private static CleanupApp app;

    @BeforeClass
    public static void init() {
        app = new CleanupApp(startDate, endDate);
    }

    @Test
    public void delete() throws Exception {
        app.delete(LARGE_DIR);
    }

    @Test
    public void testGenerate() throws InterruptedException {
        app.generate(20, "051217", LARGE_DIR, TARGET_LARGE_DIR);
    }

    @Test
    public void testCmdOptions() {
        String[] opts = {"-x", "-dd", "/home/eztld/testdir", "-sd", "031211", "-ed", "042318"};
        app.parseCmdOptions(opts);
    }

    @Test
    public void validDelOptions() {
        String[] opts1 = {"-d", "-dd", "C:\\Users\\chaubaa\\Documents\\project\\poller-etl\\test_sample_large",
                "-sd", "031211", "-ed", "042318"};
        app.parseCmdOptions(opts1);
    }

    @Test
    public void validGenOptions() {
        String[] opts1 = {"-g", "-tgt", "C:\\Users\\chaubaa\\Documents\\project\\poller-etl\\test_sample_large",
                "-src", "C:\\Users\\chaubaa\\Documents\\project\\poller-etl\\sample_large", "-dt", "051214", "-c", "1"};
        app.parseCmdOptions(opts1);
    }

    @Test
    public void validGenDryRunOptions() {
        String[] opts1 = {"-g", "-tgt", "C:\\Users\\chaubaa\\Documents\\project\\poller-etl\\test_sample_large",
                "-src", "C:\\Users\\chaubaa\\Documents\\project\\poller-etl\\sample_large", "-dt", "051214", "-c",
                "1", "-dry"};
        CleanupApp.main(opts1);
    }
    // Following 2 are the actual tests.

    @Test
    public void genWithOptions() {
        String[] opts1 = {"-g", "-tgt", "C:\\Users\\chaubaa\\Documents\\project\\poller-etl\\test_sample_large",
                "-src", "C:\\Users\\chaubaa\\Documents\\project\\poller-etl\\sample_large", "-dt", "051214", "-c",
                "100"};
        CleanupApp.main(opts1);
    }

    @Test
    public void delWithOptions() {
        String[] opts1 = {"-d", "-dd", "C:\\Users\\chaubaa\\Documents\\project\\poller-etl\\test_sample_large",
                "-sd", "031211", "-ed", "042319"};
        CleanupApp.main(opts1);
    }
}