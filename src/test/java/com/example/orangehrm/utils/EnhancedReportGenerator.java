package com.example.orangehrm.utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Enhanced report generator with modern UI, charts, and detailed Playwright execution info.
 * Generates a comprehensive HTML report with visual analytics and test details.
 */
public class EnhancedReportGenerator {
    
    private static final String JSON_REPORT_PATH = "target/cucumber.json";
    private static final String ENHANCED_REPORT_PATH = "target/enhanced-test-report.html";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    private static List<TestScenario> testScenarios = new ArrayList<>();
    private static LocalDateTime testStartTime = LocalDateTime.now();
    private static LocalDateTime testEndTime = LocalDateTime.now();
    private static String browserType = "chrome";
    private static boolean headlessMode = false;
    
    public static class TestScenario {
        String name;
        String status;
        List<TestStep> steps;
        LocalDateTime startTime;
        LocalDateTime endTime;
        long durationMs;
        List<String> tags;
        String errorMessage;
        List<String> screenshots;
        
        public TestScenario(String name) {
            this.name = name;
            this.steps = new ArrayList<>();
            this.tags = new ArrayList<>();
            this.screenshots = new ArrayList<>();
        }
    }
    
    public static class TestStep {
        String keyword;
        String name;
        String status;
        long durationMs;
        String errorMessage;
        String screenshotPath;
        String screenshotBase64;  // Store base64 encoded screenshot
        
        public TestStep(String keyword, String name) {
            this.keyword = keyword;
            this.name = name;
        }
    }
    
    /**
     * Set test execution metadata
     */
    public static void setMetadata(String browser, boolean headless) {
        browserType = browser;
        headlessMode = headless;
    }
    
    /**
     * Set test start time
     */
    public static void setTestStartTime(LocalDateTime startTime) {
        testStartTime = startTime;
    }
    
    /**
     * Set test end time
     */
    public static void setTestEndTime(LocalDateTime endTime) {
        testEndTime = endTime;
    }
    
    /**
     * Generate enhanced HTML report from Cucumber JSON
     */
    public static void generateEnhancedReport() {
        try {
            File jsonFile = new File(JSON_REPORT_PATH);
            if (!jsonFile.exists() || jsonFile.length() == 0) {
                TestLogger.info("No Cucumber JSON data available for enhanced report");
                return;
            }
            
            String jsonContent = new String(Files.readAllBytes(jsonFile.toPath()));
            if (jsonContent.trim().isEmpty() || jsonContent.trim().equals("[]")) {
                TestLogger.info("Empty Cucumber JSON - no scenarios completed");
                return;
            }
            
            // Parse JSON data
            parseJsonData(jsonContent);
            
            // Generate HTML report
            String htmlContent = generateEnhancedHtmlReport();
            
            try (PrintWriter writer = new PrintWriter(new FileWriter(ENHANCED_REPORT_PATH))) {
                writer.print(htmlContent);
            }
            
            TestLogger.success("[SUCCESS] Enhanced HTML report generated: " + ENHANCED_REPORT_PATH);
            
        } catch (Exception e) {
            TestLogger.error("Failed to generate enhanced report", e);
        }
    }
    
    /**
     * Parse Cucumber JSON data into structured objects
     */
    private static void parseJsonData(String jsonContent) throws IOException {
        testScenarios.clear();
        ObjectMapper mapper = new ObjectMapper();
        JsonNode features = mapper.readTree(jsonContent);
        
        if (features.isArray()) {
            for (JsonNode feature : features) {
                JsonNode elements = feature.get("elements");
                if (elements != null && elements.isArray()) {
                    for (JsonNode element : elements) {
                        String scenarioName = element.get("name").asText();
                        TestScenario scenario = new TestScenario(scenarioName);
                        
                        // Get scenario type (scenario vs background)
                        String type = element.get("type").asText();
                        if (!"scenario".equals(type)) {
                            continue; // Skip backgrounds
                        }
                        
                        // Get tags
                        JsonNode tags = element.get("tags");
                        if (tags != null && tags.isArray()) {
                            for (JsonNode tag : tags) {
                                scenario.tags.add(tag.get("name").asText());
                            }
                        }
                        
                        // Parse steps
                        JsonNode steps = element.get("steps");
                        boolean scenarioFailed = false;
                        long scenarioDuration = 0;
                        
                        if (steps != null && steps.isArray()) {
                            for (JsonNode step : steps) {
                                String keyword = step.get("keyword").asText().trim();
                                String stepName = step.get("name").asText();
                                TestStep testStep = new TestStep(keyword, stepName);
                                
                                JsonNode result = step.get("result");
                                if (result != null) {
                                    testStep.status = result.get("status").asText();
                                    if (result.has("duration")) {
                                        testStep.durationMs = result.get("duration").asLong() / 1_000_000; // Convert nanoseconds to ms
                                        scenarioDuration += testStep.durationMs;
                                    }
                                    
                                    if ("failed".equals(testStep.status)) {
                                        scenarioFailed = true;
                                        if (result.has("error_message")) {
                                            testStep.errorMessage = result.get("error_message").asText();
                                        }
                                    }
                                }
                                
                                // Check for embedded screenshots in step
                                JsonNode embeddings = step.get("embeddings");
                                if (embeddings != null && embeddings.isArray() && embeddings.size() > 0) {
                                    for (JsonNode embedding : embeddings) {
                                        String mimeType = "";
                                        if (embedding.has("mime_type")) {
                                            mimeType = embedding.get("mime_type").asText();
                                        } else if (embedding.has("mediaType")) {
                                            mimeType = embedding.get("mediaType").asText();
                                        }
                                        
                                        if (mimeType.contains("image")) {
                                            if (embedding.has("data")) {
                                                testStep.screenshotBase64 = embedding.get("data").asText();
                                                testStep.screenshotPath = "screenshot_" + scenario.steps.size();
                                                break;  // Take first image
                                            }
                                        }
                                    }
                                }
                                
                                // Check for embeddings in "after" hooks (AfterStep screenshots)
                                if (testStep.screenshotBase64 == null) {
                                    JsonNode after = step.get("after");
                                    if (after != null && after.isArray()) {
                                        for (JsonNode hook : after) {
                                            JsonNode hookEmbeddings = hook.get("embeddings");
                                            if (hookEmbeddings != null && hookEmbeddings.isArray()) {
                                                for (JsonNode embedding : hookEmbeddings) {
                                                    String mimeType = "";
                                                    if (embedding.has("mime_type")) {
                                                        mimeType = embedding.get("mime_type").asText();
                                                    } else if (embedding.has("media_type")) {
                                                        mimeType = embedding.get("media_type").asText();
                                                    }
                                                    
                                                    if (mimeType.contains("image")) {
                                                        if (embedding.has("data")) {
                                                            testStep.screenshotBase64 = embedding.get("data").asText();
                                                            testStep.screenshotPath = "screenshot_" + scenario.steps.size();
                                                            break;
                                                        }
                                                    }
                                                }
                                                if (testStep.screenshotBase64 != null) break;
                                            }
                                        }
                                    }
                                }
                                
                                scenario.steps.add(testStep);
                            }
                        }
                        
                        scenario.status = scenarioFailed ? "FAILED" : "PASSED";
                        scenario.durationMs = scenarioDuration;
                        testScenarios.add(scenario);
                    }
                }
            }
        }
    }
    
    /**
     * Generate enhanced HTML report with modern UI and charts
     */
    private static String generateEnhancedHtmlReport() {
        int total = testScenarios.size();
        int passed = 0;
        int failed = 0;
        long totalDuration = 0;
        long minDuration = Long.MAX_VALUE;
        long maxDuration = 0;
        
        for (TestScenario scenario : testScenarios) {
            if ("PASSED".equals(scenario.status)) {
                passed++;
            } else {
                failed++;
            }
            totalDuration += scenario.durationMs;
            if (scenario.durationMs < minDuration) minDuration = scenario.durationMs;
            if (scenario.durationMs > maxDuration) maxDuration = scenario.durationMs;
        }
        
        double passRate = total > 0 ? (passed * 100.0 / total) : 0;
        double avgDuration = total > 0 ? (totalDuration / (double) total) : 0;
        if (total == 0) minDuration = 0;
        
        Duration totalTestDuration = Duration.between(testStartTime, testEndTime);
        
        StringBuilder html = new StringBuilder();
        html.append(generateHtmlHeader());
        html.append(generateReportHeader(total, passed, failed, passRate, totalTestDuration));
        html.append(generateStatisticsSection(total, passed, failed, passRate, totalDuration, avgDuration, minDuration, maxDuration));
        html.append(generateChartSection(passed, failed));
        html.append(generateTimelineSection());
        html.append(generateScenarioDetailsSection());
        html.append(generateHtmlFooter());
        
        return html.toString();
    }
    
    /**
     * Generate HTML header with modern styling and Chart.js
     */
    private static String generateHtmlHeader() {
        return "<!DOCTYPE html>\n" +
               "<html lang=\"en\">\n" +
               "<head>\n" +
               "    <meta charset=\"UTF-8\">\n" +
               "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
               "    <title>Enhanced Test Report - Playwright Framework</title>\n" +
               "    <script src=\"https://cdn.jsdelivr.net/npm/chart.js@4.4.0/dist/chart.umd.min.js\"></script>\n" +
               "    <link rel=\"stylesheet\" href=\"https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css\">\n" +
               "    <style>\n" +
               "        * { margin: 0; padding: 0; box-sizing: border-box; }\n" +
               "        body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: #333; }\n" +
               "        .container { max-width: 1400px; margin: 0 auto; padding: 20px; }\n" +
               "        .header { background: white; border-radius: 16px; padding: 40px; margin-bottom: 30px; box-shadow: 0 10px 40px rgba(0,0,0,0.1); }\n" +
               "        .header h1 { font-size: 42px; color: #1e293b; margin-bottom: 10px; display: flex; align-items: center; gap: 15px; }\n" +
               "        .header .icon { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); -webkit-background-clip: text; -webkit-text-fill-color: transparent; }\n" +
               "        .header-meta { display: flex; gap: 30px; margin-top: 20px; flex-wrap: wrap; }\n" +
               "        .header-meta-item { display: flex; align-items: center; gap: 10px; color: #64748b; font-size: 15px; }\n" +
               "        .header-meta-item i { color: #667eea; font-size: 18px; }\n" +
               "        .stats-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(250px, 1fr)); gap: 20px; margin-bottom: 30px; }\n" +
               "        .stat-card { background: white; border-radius: 16px; padding: 30px; box-shadow: 0 4px 20px rgba(0,0,0,0.08); transition: transform 0.3s, box-shadow 0.3s; }\n" +
               "        .stat-card:hover { transform: translateY(-5px); box-shadow: 0 8px 30px rgba(0,0,0,0.12); }\n" +
               "        .stat-card .icon-container { width: 60px; height: 60px; border-radius: 12px; display: flex; align-items: center; justify-content: center; margin-bottom: 15px; font-size: 28px; }\n" +
               "        .stat-card h3 { font-size: 14px; color: #64748b; text-transform: uppercase; letter-spacing: 0.5px; margin-bottom: 10px; }\n" +
               "        .stat-card .value { font-size: 38px; font-weight: bold; margin-bottom: 5px; }\n" +
               "        .stat-card .subtext { font-size: 13px; color: #94a3b8; }\n" +
               "        .total .icon-container { background: linear-gradient(135deg, #3b82f6, #2563eb); color: white; }\n" +
               "        .total .value { color: #3b82f6; }\n" +
               "        .passed .icon-container { background: linear-gradient(135deg, #10b981, #059669); color: white; }\n" +
               "        .passed .value { color: #10b981; }\n" +
               "        .failed .icon-container { background: linear-gradient(135deg, #ef4444, #dc2626); color: white; }\n" +
               "        .failed .value { color: #ef4444; }\n" +
               "        .rate .icon-container { background: linear-gradient(135deg, #8b5cf6, #7c3aed); color: white; }\n" +
               "        .rate .value { color: #8b5cf6; }\n" +
               "        .duration .icon-container { background: linear-gradient(135deg, #f59e0b, #d97706); color: white; }\n" +
               "        .duration .value { color: #f59e0b; }\n" +
               "        .chart-section { background: white; border-radius: 16px; padding: 30px; margin-bottom: 30px; box-shadow: 0 4px 20px rgba(0,0,0,0.08); }\n" +
               "        .chart-section h2 { font-size: 24px; color: #1e293b; margin-bottom: 25px; display: flex; align-items: center; gap: 10px; }\n" +
               "        .charts-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 30px; }\n" +
               "        .chart-container { position: relative; height: 300px; }\n" +
               "        .timeline { background: white; border-radius: 16px; padding: 30px; margin-bottom: 30px; box-shadow: 0 4px 20px rgba(0,0,0,0.08); }\n" +
               "        .timeline h2 { font-size: 24px; color: #1e293b; margin-bottom: 25px; }\n" +
               "        .timeline-item { display: flex; gap: 20px; margin-bottom: 20px; padding-bottom: 20px; border-bottom: 1px solid #e2e8f0; }\n" +
               "        .timeline-item:last-child { border-bottom: none; }\n" +
               "        .timeline-marker { width: 12px; height: 12px; border-radius: 50%; margin-top: 6px; flex-shrink: 0; }\n" +
               "        .timeline-marker.passed { background: #10b981; box-shadow: 0 0 0 4px rgba(16, 185, 129, 0.2); }\n" +
               "        .timeline-marker.failed { background: #ef4444; box-shadow: 0 0 0 4px rgba(239, 68, 68, 0.2); }\n" +
               "        .timeline-content { flex: 1; }\n" +
               "        .timeline-content h3 { font-size: 16px; color: #1e293b; margin-bottom: 5px; }\n" +
               "        .timeline-content .meta { font-size: 13px; color: #64748b; }\n" +
               "        .timeline-content .tags { display: flex; gap: 8px; margin-top: 8px; flex-wrap: wrap; }\n" +
               "        .tag { background: #f1f5f9; color: #475569; padding: 4px 12px; border-radius: 12px; font-size: 12px; font-weight: 500; }\n" +
               "        .details-section { background: white; border-radius: 16px; padding: 30px; margin-bottom: 30px; box-shadow: 0 4px 20px rgba(0,0,0,0.08); }\n" +
               "        .details-section h2 { font-size: 24px; color: #1e293b; margin-bottom: 25px; }\n" +
               "        .scenario-card { border: 1px solid #e2e8f0; border-radius: 12px; margin-bottom: 20px; overflow: hidden; }\n" +
               "        .scenario-header { background: #f8fafc; padding: 20px; cursor: pointer; display: flex; justify-content: space-between; align-items: center; transition: background 0.2s; }\n" +
               "        .scenario-header:hover { background: #f1f5f9; }\n" +
               "        .scenario-header-left { display: flex; align-items: center; gap: 15px; flex: 1; }\n" +
               "        .scenario-status-icon { width: 32px; height: 32px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 14px; }\n" +
               "        .scenario-status-icon.passed { background: #d1fae5; color: #065f46; }\n" +
               "        .scenario-status-icon.failed { background: #fee2e2; color: #991b1b; }\n" +
               "        .scenario-title { font-size: 16px; font-weight: 600; color: #1e293b; }\n" +
               "        .scenario-meta { font-size: 13px; color: #64748b; }\n" +
               "        .scenario-body { padding: 20px; background: white; display: none; }\n" +
               "        .scenario-body.active { display: block; }\n" +
               "        .scenario-failure-reason { background: #fee2e2; border-left: 4px solid #ef4444; padding: 20px; border-radius: 8px; margin-bottom: 20px; color: #991b1b; font-weight: 600; }\n" +
               "        .scenario-failure-reason i { color: #991b1b; margin-right: 10px; font-size: 18px; }\n" +
               "        .failure-text { margin-top: 10px; font-family: 'Courier New', monospace; font-size: 12px; color: #7f1d1d; white-space: pre-wrap; word-break: break-word; background: white; padding: 10px; border-radius: 4px; }\n" +
               "        .step-list { list-style: none; }\n" +
               "        .step-item { display: flex; gap: 15px; padding: 12px; border-radius: 8px; margin-bottom: 8px; transition: background 0.2s; }\n" +
               "        .step-item:hover { background: #f8fafc; }\n" +
               "        .step-status { width: 24px; height: 24px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 12px; flex-shrink: 0; }\n" +
               "        .step-status.passed { background: #d1fae5; color: #065f46; }\n" +
               "        .step-status.failed { background: #fee2e2; color: #991b1b; }\n" +
               "        .step-status.skipped { background: #fef3c7; color: #92400e; }\n" +
               "        .step-content { flex: 1; }\n" +
               "        .step-text { color: #334155; margin-bottom: 4px; }\n" +
               "        .step-keyword { font-weight: 600; color: #667eea; }\n" +
               "        .step-duration { font-size: 12px; color: #94a3b8; }\n" +
               "        .error-message { background: #fee2e2; border-left: 4px solid #ef4444; padding: 15px; border-radius: 8px; margin-top: 10px; font-family: 'Courier New', monospace; font-size: 13px; color: #991b1b; white-space: pre-wrap; }\n" +
               "        .error-message i { margin-right: 8px; }\n" +
               "        .step-screenshot { margin-top: 12px; background: #f8fafc; padding: 12px; border-radius: 8px; border: 1px solid #e2e8f0; }\n" +
               "        .failed-step-screenshot { border: 2px solid #ef4444; background: #fee2e2; }\n" +
               "        .screenshot-img { max-width: 100%; height: auto; border-radius: 8px; display: block; box-shadow: 0 2px 8px rgba(0,0,0,0.08); }\n" +
               "        .footer { text-align: center; padding: 30px; color: white; font-size: 14px; }\n" +
               "        .badge { display: inline-block; padding: 5px 10px; border-radius: 12px; font-size: 12px; font-weight: 600; }\n" +
               "        .badge-passed { background: #d1fae5; color: #065f46; }\n" +
               "        .badge-failed { background: #fee2e2; color: #991b1b; }\n" +
               "        @media (max-width: 768px) {\n" +
               "            .charts-grid { grid-template-columns: 1fr; }\n" +
               "            .stats-grid { grid-template-columns: 1fr; }\n" +
               "        }\n" +
               "    </style>\n" +
               "</head>\n" +
               "<body>\n" +
               "    <div class=\"container\">\n";
    }
    
    /**
     * Generate report header section
     */
    private static String generateReportHeader(int total, int passed, int failed, double passRate, Duration totalDuration) {
        String statusEmoji = passRate == 100 ? "[PASS]" : passRate >= 80 ? "[WARN]" : "[FAIL]";
        String statusText = passRate == 100 ? "All Tests Passed" : passRate >= 80 ? "Mostly Passed" : "Tests Failed";
        
        return "        <div class=\"header\">\n" +
               "            <h1><span class=\"icon\">[TEST]</span> Playwright ORANGE HRM Test Execution Report</h1>\n" +
               "            <p style=\"font-size: 18px; color: #64748b; margin-top: 10px;\">" + statusEmoji + " " + statusText + " - " + passed + "/" + total + " scenarios passed</p>\n" +
               "            <div class=\"header-meta\">\n" +
               "                <div class=\"header-meta-item\"><i class=\"fas fa-calendar\"></i> <span>" + testStartTime.format(DATE_FORMATTER) + "</span></div>\n" +
               "                <div class=\"header-meta-item\"><i class=\"fas fa-clock\"></i> <span>Duration: " + formatDuration(totalDuration) + "</span></div>\n" +
               "                <div class=\"header-meta-item\"><i class=\"fas fa-browser\"></i> <span>Browser: " + capitalize(browserType) + (headlessMode ? " (Headless)" : "") + "</span></div>\n" +
               "                <div class=\"header-meta-item\"><i class=\"fas fa-code\"></i> <span>Framework: Playwright + Cucumber</span></div>\n" +
               "            </div>\n" +
               "        </div>\n\n";
    }
    
    /**
     * Generate statistics cards section
     */
    private static String generateStatisticsSection(int total, int passed, int failed, double passRate, 
                                                      long totalDuration, double avgDuration, long minDuration, long maxDuration) {
        return "        <div class=\"stats-grid\">\n" +
               "            <div class=\"stat-card total\">\n" +
               "                <div class=\"icon-container\"><i class=\"fas fa-list-check\"></i></div>\n" +
               "                <h3>Total Scenarios</h3>\n" +
               "                <div class=\"value\">" + total + "</div>\n" +
               "                <div class=\"subtext\">Test scenarios executed</div>\n" +
               "            </div>\n" +
               "            <div class=\"stat-card passed\">\n" +
               "                <div class=\"icon-container\"><i class=\"fas fa-check-circle\"></i></div>\n" +
               "                <h3>Passed</h3>\n" +
               "                <div class=\"value\">" + passed + "</div>\n" +
               "                <div class=\"subtext\">" + String.format("%.1f%% success rate", passRate) + "</div>\n" +
               "            </div>\n" +
               "            <div class=\"stat-card failed\">\n" +
               "                <div class=\"icon-container\"><i class=\"fas fa-times-circle\"></i></div>\n" +
               "                <h3>Failed</h3>\n" +
               "                <div class=\"value\">" + failed + "</div>\n" +
               "                <div class=\"subtext\">" + (total > 0 ? String.format("%.1f%% failure rate", (failed * 100.0 / total)) : "0%") + "</div>\n" +
               "            </div>\n" +
               "            <div class=\"stat-card rate\">\n" +
               "                <div class=\"icon-container\"><i class=\"fas fa-percentage\"></i></div>\n" +
               "                <h3>Pass Rate</h3>\n" +
               "                <div class=\"value\">" + String.format("%.1f%%", passRate) + "</div>\n" +
               "                <div class=\"subtext\">Overall success metric</div>\n" +
               "            </div>\n" +
               "            <div class=\"stat-card duration\">\n" +
               "                <div class=\"icon-container\"><i class=\"fas fa-stopwatch\"></i></div>\n" +
               "                <h3>Avg Duration</h3>\n" +
               "                <div class=\"value\">" + String.format("%.1fs", avgDuration / 1000.0) + "</div>\n" +
               "                <div class=\"subtext\">Min: " + String.format("%.1fs", minDuration / 1000.0) + " | Max: " + String.format("%.1fs", maxDuration / 1000.0) + "</div>\n" +
               "            </div>\n" +
               "        </div>\n\n";
    }
    
    /**
     * Generate charts section with Chart.js
     */
    private static String generateChartSection(int passed, int failed) {
        return "        <div class=\"chart-section\">\n" +
               "            <h2><i class=\"fas fa-chart-pie\"></i> Test Analytics</h2>\n" +
               "            <div class=\"charts-grid\">\n" +
               "                <div class=\"chart-container\">\n" +
               "                    <canvas id=\"pieChart\"></canvas>\n" +
               "                </div>\n" +
               "                <div class=\"chart-container\">\n" +
               "                    <canvas id=\"barChart\"></canvas>\n" +
               "                </div>\n" +
               "            </div>\n" +
               "        </div>\n\n" +
               "        <script>\n" +
               "            // Pie Chart - Pass/Fail Distribution\n" +
               "            const pieCtx = document.getElementById('pieChart').getContext('2d');\n" +
               "            new Chart(pieCtx, {\n" +
               "                type: 'doughnut',\n" +
               "                data: {\n" +
               "                    labels: ['Passed', 'Failed'],\n" +
               "                    datasets: [{\n" +
               "                        data: [" + passed + ", " + failed + "],\n" +
               "                        backgroundColor: ['#10b981', '#ef4444'],\n" +
               "                        borderWidth: 0\n" +
               "                    }]\n" +
               "                },\n" +
               "                options: {\n" +
               "                    responsive: true,\n" +
               "                    maintainAspectRatio: false,\n" +
               "                    plugins: {\n" +
               "                        legend: { position: 'bottom', labels: { padding: 20, font: { size: 14 } } },\n" +
               "                        title: { display: true, text: 'Test Results Distribution', font: { size: 16, weight: 'bold' } }\n" +
               "                    }\n" +
               "                }\n" +
               "            });\n\n" +
               "            // Bar Chart - Comparison\n" +
               "            const barCtx = document.getElementById('barChart').getContext('2d');\n" +
               "            new Chart(barCtx, {\n" +
               "                type: 'bar',\n" +
               "                data: {\n" +
               "                    labels: ['Test Results'],\n" +
               "                    datasets: [\n" +
               "                        { label: 'Passed', data: [" + passed + "], backgroundColor: '#10b981', borderRadius: 8 },\n" +
               "                        { label: 'Failed', data: [" + failed + "], backgroundColor: '#ef4444', borderRadius: 8 }\n" +
               "                    ]\n" +
               "                },\n" +
               "                options: {\n" +
               "                    responsive: true,\n" +
               "                    maintainAspectRatio: false,\n" +
               "                    plugins: {\n" +
               "                        legend: { position: 'bottom', labels: { padding: 20, font: { size: 14 } } },\n" +
               "                        title: { display: true, text: 'Pass vs Fail Comparison', font: { size: 16, weight: 'bold' } }\n" +
               "                    },\n" +
               "                    scales: {\n" +
               "                        y: { beginAtZero: true, ticks: { stepSize: 1 } }\n" +
               "                    }\n" +
               "                }\n" +
               "            });\n" +
               "        </script>\n\n";
    }
    
    /**
     * Generate timeline section
     */
    private static String generateTimelineSection() {
        StringBuilder timeline = new StringBuilder();
        timeline.append("        <div class=\"timeline\">\n");
        timeline.append("            <h2><i class=\"fas fa-timeline\"></i> Execution Timeline</h2>\n");
        
        for (TestScenario scenario : testScenarios) {
            String markerClass = "PASSED".equals(scenario.status) ? "passed" : "failed";
            String durationStr = String.format("%.2fs", scenario.durationMs / 1000.0);
            
            timeline.append("            <div class=\"timeline-item\">\n");
            timeline.append("                <div class=\"timeline-marker ").append(markerClass).append("\"></div>\n");
            timeline.append("                <div class=\"timeline-content\">\n");
            timeline.append("                    <h3>").append(escapeHtml(scenario.name)).append("</h3>\n");
            timeline.append("                    <div class=\"meta\">Duration: ").append(durationStr);
            timeline.append(" | Steps: ").append(scenario.steps.size());
            timeline.append(" | Status: <span class=\"badge badge-").append(scenario.status.toLowerCase()).append("\">").append(scenario.status).append("</span></div>\n");
            
            if (!scenario.tags.isEmpty()) {
                timeline.append("                    <div class=\"tags\">\n");
                for (String tag : scenario.tags) {
                    timeline.append("                        <span class=\"tag\">").append(escapeHtml(tag)).append("</span>\n");
                }
                timeline.append("                    </div>\n");
            }
            
            timeline.append("                </div>\n");
            timeline.append("            </div>\n");
        }
        
        timeline.append("        </div>\n\n");
        return timeline.toString();
    }
    
    /**
     * Generate detailed scenario section
     */
    private static String generateScenarioDetailsSection() {
        StringBuilder details = new StringBuilder();
        details.append("        <div class=\"details-section\">\n");
        details.append("            <h2><i class=\"fas fa-file-lines\"></i> Detailed Test Results</h2>\n");
        
        int scenarioIndex = 0;
        for (TestScenario scenario : testScenarios) {
            String statusClass = "PASSED".equals(scenario.status) ? "passed" : "failed";
            String statusIcon = "PASSED".equals(scenario.status) ? "[OK]" : "[NO]";
            String durationStr = String.format("%.2fs", scenario.durationMs / 1000.0);
            
            details.append("            <div class=\"scenario-card\">\n");
            details.append("                <div class=\"scenario-header\" onclick=\"toggleScenario(").append(scenarioIndex).append(")\">\n");
            details.append("                    <div class=\"scenario-header-left\">\n");
            details.append("                        <div class=\"scenario-status-icon ").append(statusClass).append("\">").append(statusIcon).append("</div>\n");
            details.append("                        <div>\n");
            details.append("                            <div class=\"scenario-title\">").append(escapeHtml(scenario.name)).append("</div>\n");
            details.append("                            <div class=\"scenario-meta\">").append(scenario.steps.size()).append(" steps | ").append(durationStr).append("</div>\n");
            details.append("                        </div>\n");
            details.append("                    </div>\n");
            details.append("                    <i class=\"fas fa-chevron-down\" id=\"icon-").append(scenarioIndex).append("\"></i>\n");
            details.append("                </div>\n");
            details.append("                <div class=\"scenario-body\" id=\"scenario-").append(scenarioIndex).append("\">\n");
            
            // Show scenario-level failure reason if failed
            if ("FAILED".equals(scenario.status) && scenario.errorMessage != null && !scenario.errorMessage.isEmpty()) {
                details.append("                    <div class=\"scenario-failure-reason\">\n");
                details.append("                        <i class=\"fas fa-exclamation-circle\"></i> Failure Reason\n");
                details.append("                        <div class=\"failure-text\">").append(escapeHtml(scenario.errorMessage)).append("</div>\n");
                details.append("                    </div>\n");
            }
            
            details.append("                    <ul class=\"step-list\">\n");
            
            for (TestStep step : scenario.steps) {
                String stepStatusClass = step.status != null ? step.status.toLowerCase() : "unknown";
                String stepIcon = "passed".equals(stepStatusClass) ? "[OK]" : "failed".equals(stepStatusClass) ? "[NO]" : "[~]";
                String stepDuration = String.format("%.0fms", (double) step.durationMs);
                
                details.append("                        <li class=\"step-item\">\n");
                details.append("                            <div class=\"step-status ").append(stepStatusClass).append("\">").append(stepIcon).append("</div>\n");
                details.append("                            <div class=\"step-content\">\n");
                details.append("                                <div class=\"step-text\"><span class=\"step-keyword\">").append(step.keyword).append("</span> ").append(escapeHtml(step.name)).append("</div>\n");
                details.append("                                <div class=\"step-duration\">").append(stepDuration).append("</div>\n");
                
                if (step.errorMessage != null && !step.errorMessage.isEmpty()) {
                    details.append("                                <div class=\"error-message\"><i class=\"fas fa-exclamation-triangle\"></i> ").append(escapeHtml(step.errorMessage)).append("</div>\n");
                }
                
                // Display screenshot if available (especially important for failed steps)
                if (step.screenshotBase64 != null && !step.screenshotBase64.isEmpty()) {
                    details.append("                                <div class=\"step-screenshot").append("failed".equals(stepStatusClass) ? " failed-step-screenshot" : "").append("\">\n");
                    details.append("                                    <img src=\"data:image/png;base64,").append(step.screenshotBase64).append("\" alt=\"Step Screenshot\" class=\"screenshot-img\">\n");
                    details.append("                                </div>\n");
                }
                
                details.append("                            </div>\n");
                details.append("                        </li>\n");
            }
            
            details.append("                    </ul>\n");
            details.append("                </div>\n");
            details.append("            </div>\n");
            
            scenarioIndex++;
        }
        
        details.append("        </div>\n\n");
        details.append("        <script>\n");
        details.append("            function toggleScenario(index) {\n");
        details.append("                const body = document.getElementById('scenario-' + index);\n");
        details.append("                const icon = document.getElementById('icon-' + index);\n");
        details.append("                body.classList.toggle('active');\n");
        details.append("                icon.style.transform = body.classList.contains('active') ? 'rotate(180deg)' : 'rotate(0deg)';\n");
        details.append("            }\n");
        details.append("        </script>\n\n");
        
        return details.toString();
    }
    
    /**
     * Generate HTML footer
     */
    private static String generateHtmlFooter() {
        return "        <div class=\"footer\">\n" +
               "            <p>Generated by Enhanced Playwright Report Generator</p>\n" +
               "            <p style=\"margin-top: 5px; font-size: 12px; opacity: 0.8;\">Powered by Playwright + Cucumber + Chart.js</p>\n" +
               "        </div>\n" +
               "    </div>\n" +
               "</body>\n" +
               "</html>";
    }
    
    /**
     * Escape HTML special characters
     */
    private static String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }
    
    /**
     * Format duration nicely
     */
    private static String formatDuration(Duration duration) {
        long seconds = duration.getSeconds();
        long minutes = seconds / 60;
        long secs = seconds % 60;
        
        if (minutes > 0) {
            return String.format("%dm %ds", minutes, secs);
        } else {
            return String.format("%ds", secs);
        }
    }
    
    /**
     * Capitalize first letter
     */
    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
    
    /**
     * Main method to manually regenerate the report from existing cucumber.json
     */
    public static void main(String[] args) {
        System.out.println("Manually regenerating enhanced test report...");
        try {
            generateEnhancedReport();
            System.out.println("[OK] Report regenerated successfully!");
            System.out.println("Open target/enhanced-test-report.html to view");
        } catch (Exception e) {
            System.err.println("[FAIL] Failed to regenerate report: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
