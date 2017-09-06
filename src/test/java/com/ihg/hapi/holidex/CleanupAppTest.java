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
//        Thread.sleep(5000L);
    }

}