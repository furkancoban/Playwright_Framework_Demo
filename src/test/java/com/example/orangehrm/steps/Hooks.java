package com.example.orangehrm.steps;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.WaitUntilState;
import com.example.orangehrm.config.ConfigManager;
import com.example.orangehrm.context.TestContext;
import com.example.orangehrm.utils.TestLogger;
import com.example.orangehrm.utils.ScreenshotHelper;
import com.example.orangehrm.utils.VisualCheckpointHelper;
import com.example.orangehrm.utils.ElementLocatorHelper;
import com.example.orangehrm.utils.EnhancedReportGenerator;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import java.util.Properties;

/**
 * Cucumber hooks for managing the test lifecycle.
 * 
 * @Before - Runs before each test scenario, sets up the browser and goes to the app
 * @After - Runs after each scenario, cleans up resources and grabs screenshots if things failed
 * @AfterStep - Lets you add delays between steps if you want to watch what's happening
 * 
 * Note: We're using static fields here for simplicity. Yeah, it's not great for parallel
 * execution, but for running tests one at a time it works fine and keeps the code simple.
 * If we ever need parallel runs, we can switch to ThreadLocal or Cucumber's Scenario context.
 */
public class Hooks {

    public static Playwright playwright;
    public static Browser browser;
    public static Page page;
    public static TestContext testContext;
    public static Properties testProperties;
    public static Scenario currentScenario;

    @SuppressWarnings("unused")
	private static final int MIN_NAVIGATION_TIMEOUT_MS = 60000;  // 60 seconds for reliable navigation on slow demo sites
    @SuppressWarnings("unused")
	private static final int NAVIGATION_RETRY_COUNT = 2;  // Reduce retries for faster failure
    private static volatile int scenariosExecuted = 0;
    private static volatile int scenariosPassed = 0;
    private static volatile int scenariosFailed = 0;
    private static ThreadLocal<java.time.LocalDateTime> scenarioStartTime = new ThreadLocal<>();
    private static java.time.LocalDateTime testSuiteStartTime = java.time.LocalDateTime.now();

    static {
        // Register shutdown hook to ensure report is generated even if tests are interrupted
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            TestLogger.info("\n=== TEST EXECUTION STOPPED ===");
            TestLogger.info("Scenarios Executed: " + scenariosExecuted);
            TestLogger.info("Scenarios Passed: " + scenariosPassed);
            TestLogger.info("Scenarios Failed: " + scenariosFailed);
            TestLogger.info("\nGenerating enhanced report...");
            
            // Generate enhanced report with beautiful UI
            EnhancedReportGenerator.setTestEndTime(java.time.LocalDateTime.now());
            EnhancedReportGenerator.generateEnhancedReport();
            
            TestLogger.info("\n[REPORTS] REPORTS GENERATED:");
            TestLogger.info("  [DONE] Enhanced Report: target/enhanced-test-report.html");
            TestLogger.info("==============================\n");
        }));
    }

    @Before
    public void beforeScenario(Scenario scenario) {
        // Capture scenario reference early so utilities can attach screenshots/metadata.
        currentScenario = scenario;
        scenarioStartTime.set(java.time.LocalDateTime.now()); // Track start time
        String browserName = ConfigManager.getBrowser();
        boolean headless = ConfigManager.isHeadless();
        
        // Set metadata for enhanced report (only once)
        if (scenariosExecuted == 0) {
            EnhancedReportGenerator.setTestStartTime(testSuiteStartTime);
            EnhancedReportGenerator.setMetadata(browserName, headless);
        }
        
        TestLogger.startScenario(scenario.getName());
        TestLogger.info("Starting test scenario... browser=" + browserName + " headless=" + headless);
        
        try {
            // Initialize Playwright browser. We're using channel="chrome" because it's more stable
            // than letting Playwright download its own version. Just make sure Chrome is installed.
            // For CI/CD, you'll want to ensure Chrome is available in your container or runner.
            
            playwright = Playwright.create();
            if ("chrome".equals(browserName)) {
                // Use system Chrome if available, otherwise Playwright's bundled chromium
                try {
                    browser = playwright.chromium()
                        .launch(new BrowserType.LaunchOptions()
                            .setChannel("chrome")
                            .setArgs(java.util.Arrays.asList("--start-maximized"))
                            .setHeadless(headless));
                } catch (Exception e) {
                    TestLogger.warn("Chrome not found, using bundled Chromium: " + e.getMessage());
                    browser = playwright.chromium()
                        .launch(new BrowserType.LaunchOptions()
                            .setArgs(java.util.Arrays.asList("--start-maximized"))
                            .setHeadless(headless));
                }
            } else if ("chromium".equals(browserName)) {
                browser = playwright.chromium()
                    .launch(new BrowserType.LaunchOptions()
                        .setHeadless(headless));
            } else if ("firefox".equals(browserName)) {
                browser = playwright.firefox()
                    .launch(new BrowserType.LaunchOptions()
                        .setHeadless(headless));
            } else if ("webkit".equals(browserName)) {
                browser = playwright.webkit()
                    .launch(new BrowserType.LaunchOptions().setHeadless(headless));
            } else {
                browser = playwright.chromium()
                    .launch(new BrowserType.LaunchOptions()
                        .setHeadless(headless));
            }
            
            // Create browser context with real browser headers to avoid anti-bot detection.
            // OrangeHRM's demo site can be picky about automated requests.
            java.util.Map<String, String> headers = new java.util.HashMap<>();
            headers.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36");
            headers.put("Accept-Language", "en-US,en;q=0.9");
            headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
            headers.put("Accept-Encoding", "gzip, deflate, br");
            headers.put("DNT", "1");
            headers.put("Sec-Fetch-Dest", "document");
            headers.put("Sec-Fetch-Mode", "navigate");
            headers.put("Sec-Fetch-Site", "none");
            headers.put("Cache-Control", "max-age=0");
            
            var contextOptions = new Browser.NewContextOptions()
                .setExtraHTTPHeaders(headers)
                .setLocale("en-US")
                .setTimezoneId("America/Chicago")
                .setViewportSize(1920, 1080);  // Set large viewport for maximized effect
            
            // Context-level options are shared for all tabs/pages in this scenario.
            var context = browser.newContext(contextOptions);
            page = context.newPage();
            TestLogger.info("Browser window set to maximized resolution (1920x1080)");
            testContext = new TestContext(page);
            
            // Initialize helper utilities
            VisualCheckpointHelper.initializeVisualTesting(page, scenario.getName());
            ElementLocatorHelper.initializeSelfHealing(page);
            
            String appUrl = ConfigManager.getAppUrl();
            // Use 60 second timeout for reliable navigation
            int navigationTimeout = 60000;

            page.setDefaultTimeout(navigationTimeout);
            page.setDefaultNavigationTimeout(navigationTimeout);

            navigateWithRetry(appUrl);
            TestLogger.success("Browser launched and navigated to: " + appUrl);
        } catch (Exception e) {
            TestLogger.error("Playwright initialization failed", e);
            playwright = null;
            browser = null;
            page = null;
            testContext = null;
            throw new RuntimeException("Failed to initialize browser", e);
        }
    }

    private void navigateWithRetry(String appUrl) {
        // Navigate to the app. We wait for domcontentloaded which means the HTML is parsed
        // and the page is interactive. "load" event waits for all resources (images, etc)
        // and can timeout on slow demo sites. Try 2 times with exponential backoff.
        int maxRetries = 2;
        long initialDelay = 2000;  // 2 seconds
        
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                TestLogger.info("Navigation attempt " + attempt + " to: " + appUrl);

                long startTime = System.currentTimeMillis();
                page.navigate(
                    appUrl,
                    new Page.NavigateOptions()
                        .setTimeout(60000.0)  // 60 second timeout for slow demo sites
                        .setWaitUntil(WaitUntilState.DOMCONTENTLOADED)  // Wait for HTML parsing, not all images/css
                );
                
                long navigationTime = System.currentTimeMillis() - startTime;
                TestLogger.info("[OK] Page navigation completed in " + navigationTime + "ms");

                TestLogger.info("Navigation successful, login page ready for interaction");
                return;  // Success!
            } catch (Exception e) {
                // Retry once with backoff to handle intermittent network/demo-site slowness.
                if (attempt < maxRetries) {
                    long backoffDelay = initialDelay * attempt;  // Exponential backoff: 2s, 4s
                    TestLogger.warn("Navigation attempt " + attempt + " failed: " + e.getMessage());
                    TestLogger.info("Waiting " + backoffDelay + "ms before retry...");
                    try {
                        Thread.sleep(backoffDelay);
                    } catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                    }
                } else {
                    TestLogger.error("All navigation retries exhausted");
                    throw new RuntimeException("Cannot load login page after " + maxRetries + " attempts: " + appUrl, e);
                }
            }
        }
    }

    @io.cucumber.java.AfterStep
    public void afterStep(Scenario scenario) {
        // Capture screenshot after EVERY step and embed in report
        if (page != null && scenario != null) {
            try {
                // Get current step info
                String stepName = scenario.getSourceTagNames().isEmpty() ? 
                    "Step Screenshot" : 
                    "Screenshot for step";
                
                byte[] screenshot = page.screenshot();
                if (screenshot != null && screenshot.length > 0) {
                    scenario.attach(screenshot, "image/png", stepName);
                    TestLogger.debug("[OK] Screenshot captured and embedded (" + screenshot.length + " bytes)");
                } else {
                    TestLogger.warn("Screenshot captured but empty");
                }
            } catch (Exception e) {
                TestLogger.error("Failed to capture step screenshot: " + e.getMessage());
            }
        } else {
            TestLogger.debug("Cannot capture screenshot - page or scenario is null");
        }
        
        // Add step delay if configured (for observation during manual test runs)
        long stepDelay = ConfigManager.getStepDelay();
        if (stepDelay > 0) {
            try {
                Thread.sleep(stepDelay);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @After
    public void afterScenario(Scenario scenario) {
        TestLogger.endScenario(scenario.getName());
        
        // Calculate scenario duration
        java.time.LocalDateTime startTime = scenarioStartTime.get();
        java.time.LocalDateTime endTime = java.time.LocalDateTime.now();
        long durationMs = startTime != null ? 
            java.time.Duration.between(startTime, endTime).toMillis() : 0;
        
        // Track counters used by shutdown hook summary and enhanced report metadata.
        scenariosExecuted++;
        String status;
        if (scenario.isFailed()) {
            scenariosFailed++;
            status = "FAILED";
        } else {
            scenariosPassed++;
            status = "PASSED";
        }
        
        TestLogger.info("Scenario " + status + " - Duration: " + durationMs + "ms");
        
        try {
            // Clear browser cache, cookies, and storage after each scenario
            if (page != null) {
                try {
                    // Clear cookies
                    page.context().clearCookies();
                    TestLogger.info("Cookies cleared after scenario");

                    // Clear local storage and session storage
                    page.evaluate("() => { localStorage.clear(); sessionStorage.clear(); }");
                    TestLogger.info("Local storage and session storage cleared");

                    // Clear IndexedDB
                    page.evaluate("() => { indexedDB.databases().then(dbs => { dbs.forEach(db => { indexedDB.deleteDatabase(db.name); }); }); }");
                    TestLogger.debug("IndexedDB cleared");
                } catch (Exception e) {
                    TestLogger.warn("Could not clear all browser storage: " + e.getMessage());
                }
            }

            // Close visual checkpoint tracking
            if (VisualCheckpointHelper.isVisualTestingActive()) {
                VisualCheckpointHelper.closeVisualTesting();
            }
            
            // Print element locator statistics
            ElementLocatorHelper.printHealingStats();
            
            // Capture screenshot on test failure for troubleshooting
            if (scenario.isFailed() && page != null) {
                String screenshotPath = ScreenshotHelper.captureScreenshotOnFailure(
                    page, 
                    scenario.getName(),
                    null
                );
                TestLogger.error("Test failed. Screenshot saved: " + screenshotPath);
            }
            
            // Flush logs and ensure report data is written
            TestLogger.flushLogs();
            
            // Close browser and cleanup
            if (page != null) page.close();
            if (browser != null) browser.close();
            if (playwright != null) playwright.close();
            TestLogger.success("Browser closed and cleanup completed");
        } catch (Exception e) {
            TestLogger.error("Error during cleanup", e);
        }
    }
}
