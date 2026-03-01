package com.example.orangehrm.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**\n * Centralized test execution logger. Provides consistent, readable log output\n * across all test steps and page operations. Using SLF4J + Log4j2 for flexibility.\n * \n * The emoji prefixes help quickly identify log types when scanning test output.\n * Useful when reviewing logs after a test run or during CI/CD pipeline debugging.\n * \n * Originally used System.out.println everywhere but that became messy to manage.\n * This wrapper makes it easier to change logging strategy without updating every test.\n */
public class TestLogger {

    private static final Logger logger = LoggerFactory.getLogger("TestExecution");

    public static void info(String message) {
        logger.info("[INFO] " + message);
    }

    public static void info(String message, Object... args) {
        logger.info("[INFO] " + message, args);
    }

    public static void debug(String message) {
        logger.debug("[DEBUG] " + message);
    }

    public static void debug(String message, Object... args) {
        logger.debug("[DEBUG] " + message, args);
    }

    public static void warn(String message) {
        logger.warn("[WARN] " + message);
    }

    public static void warn(String message, Throwable throwable) {
        logger.warn("[WARN] " + message, throwable);
    }

    public static void error(String message) {
        logger.error("[ERROR] " + message);
    }

    public static void error(String message, Throwable throwable) {
        logger.error("[ERROR] " + message, throwable);
    }

    public static void success(String message) {
        logger.info("[SUCCESS] " + message);
    }

    public static void testStep(String stepName) {
        logger.info("📍 STEP: " + stepName);
    }

    public static void assertion(String assertion) {
        logger.info("🧪 ASSERT: " + assertion);
    }

    public static void startScenario(String scenarioName) {
        logger.info("\n========== SCENARIO START: {} ==========", scenarioName);
    }

    public static void endScenario(String scenarioName) {
        logger.info("========== SCENARIO END: {} ==========\n", scenarioName);
    }

    public static void flushLogs() {
        // Ensure all log messages are written to output
        try {
            org.apache.logging.log4j.LogManager.shutdown();
            org.apache.logging.log4j.LogManager.getLogger("TestExecution");
        } catch (Exception e) {
            // Logging framework already shut down or not available
        }
    }
}
