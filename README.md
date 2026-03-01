# OrangeHRM Playwright Test Framework

> A modern test automation framework for OrangeHRM using Playwright, Cucumber BDD, and Java

## 📋 Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Prerequisites](#prerequisites)
- [Quick Start](#quick-start)
- [Project Structure](#project-structure)
- [Running Tests](#running-tests)
- [CI/CD with CircleCI](#cicd-with-circleci)
- [Configuration](#configuration)
- [Writing Tests](#writing-tests)
- [Reports & Logs](#reports--logs)
- [Troubleshooting](#troubleshooting)
- [Best Practices](#best-practices)

## Overview

This is a clean, well-organized test automation framework for OrangeHRM. It uses Playwright for reliable browser automation, Cucumber for writing tests in plain English (BDD), and Java as the foundation. The framework follows industry best practices and is easy to extend.

## Features

✅ **Page Object Model** - Clean separation of test logic and page interactions  
✅ **BDD with Cucumber** - Tests written in plain English that anyone can understand  
✅ **Cross-browser Support** - Run tests on Chrome, Firefox, or Safari  
✅ **Detailed Reporting** - HTML reports with screenshots for every step  
✅ **Smart Logging** - Comprehensive logs for debugging  
✅ **Parallel Execution** - Tests run 4x faster with CircleCI parallelization  
✅ **CI/CD Active** - CircleCI integrated with automated test runs on every push  
✅ **Pre-push Hooks** - Optional local enforcement to catch failures before pushing  
✅ **Auto Cleanup** - Generated reports and logs cleaned automatically  

**Test Coverage:**
- 21 smoke tests - Critical functionality checks (runs in ~5 minutes)
- 40+ regression tests - Comprehensive coverage of all features

## Prerequisites

Before you start, make sure you have:

- **Java 11+** - [Download JDK](https://www.oracle.com/java/technologies/javase-downloads.html)
- **Maven 3.6+** - [Install Maven](https://maven.apache.org/install.html)
- **Git** - [Install Git](https://git-scm.com/downloads)

Verify your setup:
```bash
java -version   # Should show Java 11 or higher
mvn -version    # Should show Maven 3.6 or higher
```

## Quick Start

```bash
# Clone the repository
git clone <your-repo-url>
cd orangehrm-playwright-tests

# Run smoke tests (fastest - 5 minutes)
mvn "-Dcucumber.filter.tags=@smoke" test

# Run all tests
mvn test
```

That's it! Reports will be in `target/cucumber-report.html`

## Project Structure

```
orangehrm-playwright-tests/
├── src/test/
│   ├── java/com/example/orangehrm/
│   │   ├── config/
│   │   │   └── ConfigManager.java          # Configuration handler
│   │   ├── context/
│   │   │   └── TestContext.java            # Shared test state
│   │   ├── pages/
│   │   │   ├── BasePage.java               # Base page with common methods
│   │   │   ├── LoginPage.java              # Login page object
│   │   │   └── DashboardPage.java          # Dashboard page object
│   │   ├── steps/
│   │   │   ├── Hooks.java                  # Test lifecycle hooks
│   │   │   └── StepDefinitions.java        # Cucumber step implementations
│   │   └── utils/
│   │       └── TestLogger.java             # Custom logging utility
│   └── resources/
│       ├── features/
│       │   ├── smoke.feature               # 21 critical smoke tests
│       │   └── regression.feature          # 40+ regression tests
│       ├── test.properties                 # Test configuration
│       └── log4j2.xml                      # Logging configuration
├── target/                                 # Build output (auto-generated)
│   ├── cucumber-report.html               # Main test report
│   ├── screenshots/                       # Test screenshots
│   └── logs/                              # Execution logs
├── pom.xml                                # Maven configuration
└── README.md                              # This file
```

## Running Tests

**Note for PowerShell users:** You need to quote Maven properties with double quotes.

### Run Test Suites

```bash
# Smoke tests - Quick validation (21 tests, ~5 minutes)
mvn "-Dcucumber.filter.tags=@smoke" test

# Regression tests - Full suite (40+ tests, ~30 minutes)
mvn "-Dcucumber.filter.tags=@regression" test

# All tests
mvn test
```

### Debug Mode

```bash
# See the browser while tests run
mvn "-Dtest.headless=false" test

# Run slower to watch each step
mvn "-Dtest.headless=false" "-Dtest.step.delay=1000" test

# Smoke tests in debug mode
mvn "-Dcucumber.filter.tags=@smoke" "-Dtest.headless=false" test
```

### Different Browsers

```bash
# Firefox
mvn "-Dtest.browser=firefox" test

# WebKit (Safari engine)
mvn "-Dtest.browser=webkit" test

# Chrome (default)
mvn "-Dtest.browser=chrome" test
```

### Clean Build

```bash
# Clean previous reports and build fresh
mvn clean test
```

## CI/CD with CircleCI

✅ **CircleCI is connected and running!** 

This repository is integrated with CircleCI for continuous testing. Configuration: [.circleci/config.yml](.circleci/config.yml)

### What CircleCI Does

- **Automatic Triggers** - Runs tests on every push/PR to the repository
- **Parallel Execution** - Runs tests across **4 parallel nodes** for 4x faster feedback
  - Node 0: Tests tagged @test1-@test5
  - Node 1: Tests tagged @test6-@test10
  - Node 2: Tests tagged @test11-@test15
  - Node 3: Tests tagged @test16-@test21
- **Smart Caching** - Caches Maven dependencies to speed up builds
- **Headless Execution** - Runs browser tests in headless Chromium mode
- **Fast Feedback** - Full smoke suite completes in ~2 minutes with parallelization

### Pipeline Status

View live pipeline status and test results: [CircleCI Dashboard](https://app.circleci.com/pipelines/github/furkancoban/Playwright_Framework_Demo)

### Enforce Quality Gates (Branch Protection)

**Recommended:** Enable branch protection to automatically block failing code from entering `main`:

1. Go to: **GitHub** → **Settings** → **Branches** → **Branch protection rules**
2. Click **Add rule** or edit existing `main` rule
3. Branch name pattern: `main`
4. Enable these settings:
   - ✅ **Require status checks to pass before merging**
   - Search for and select the CircleCI check (appears after first pipeline run)
   - ✅ **Require branches to be up to date before merging** (optional but recommended)
   - ✅ **Require a pull request before merging** (optional - adds review requirement)
5. Click **Create** or **Save changes**

Once enabled, no code with failing tests can be merged to `main`.

### Local Pre-Push Hook (Optional)

✅ **Already configured in this workspace!**

A local pre-push hook is available at [.githooks/pre-push](.githooks/pre-push) and has been activated.

**What it does:** Runs smoke tests before every `git push` and blocks the push if tests fail.

**To enable on other machines** (when cloning the repo):

```bash
git config core.hooksPath .githooks
```

**To bypass the hook** (for urgent pushes):

```bash
git push --no-verify
```

**Note:** This is a local safety net. Branch protection on GitHub provides centralized enforcement that cannot be bypassed.

## Configuration

All test settings are in `src/test/resources/test.properties`:

```properties
# Application URL
app.url=https://opensource-demo.orangehrmlive.com/web/index.php/auth/login

# Test credentials
app.username=Admin
app.password=admin123

# Timeouts (milliseconds)
page.load.timeout=15000      # How long to wait for pages to load
element.wait.timeout=8000    # How long to wait for elements
navigation.timeout=15000     # Navigation timeout

# Wait condition (LOAD, DOMCONTENTLOADED, NETWORKIDLE)
wait.until.condition=LOAD
```

### Override Settings

You can override any setting via command line:

```bash
# Use different credentials
mvn test -Dapp.username=YourUser -Dapp.password=YourPass

# Change timeouts
mvn test -Dpage.load.timeout=30000

# Different browser
mvn test -Dtest.browser=firefox -Dtest.headless=false
```

## Writing Tests

### 1. Add a Cucumber Scenario

Create or edit feature files in `src/test/resources/features/`:

```gherkin
@smoke @login
Scenario: User can login successfully
  Given I am on the OrangeHRM login page
  When I login with valid credentials
  Then I should see the dashboard
  And the URL should contain "dashboard"
```

### 2. Implement Step Definitions

If you need new steps, add them to `StepDefinitions.java`:

```java
@When("I click on {string} button")
public void i_click_on_button(String buttonName) {
    TestLogger.testStep("Clicking on: " + buttonName);
    dashboardPage.clickButton(buttonName);
}
```

### 3. Create Page Objects

For new pages, extend `BasePage`:

```java
public class EmployeePage extends BasePage {
    private static final String ADD_BUTTON = "button:has-text('Add')";
    private static final String EMPLOYEE_NAME = "input[name='employeeName']";
    
    public EmployeePage(Page page) {
        super(page);
    }
    
    public EmployeePage clickAddButton() {
        TestLogger.testStep("Click Add Employee button");
        clickElement(ADD_BUTTON);
        return this;
    }
    
    public EmployeePage enterEmployeeName(String name) {
        fillField(EMPLOYEE_NAME, name);
        return this;
    }
}
```

### 4. Tag Your Tests

- `@smoke` - Critical tests (must pass before deployment)
- `@regression` - Thorough testing (run before releases)
- Add custom tags like `@admin`, `@employee`, `@leave` for grouping

## Reports & Logs

After each test run, you'll find:

| File/Directory | Description |
|----------------|-------------|
| `target/enhanced-test-report.html` | **⭐ NEW!** Beautiful modern report with charts, timeline, and detailed analytics |
| `target/cucumber-report.html` | Standard Cucumber HTML report |
| `target/test-execution-report.html` | Live report (auto-refreshes during test run) |
| `target/cucumber.json` | JSON report for CI/CD integration |
| `target/screenshots/` | Screenshots captured during tests |
| `target/logs/test-execution.log` | Detailed execution logs |

### 🌟 Enhanced Report Features

The new enhanced report includes:
- 📊 **Interactive Charts** - Visual analytics with Chart.js
- 🎨 **Modern UI** - Clean, professional design with gradient cards
- ⏱️ **Execution Timeline** - See your tests run in sequence
- 📈 **Detailed Metrics** - Pass rates, duration stats, scenario breakdowns
- 🔍 **Expandable Details** - Click any scenario to see step-by-step execution
- 🎯 **Playwright-Specific Info** - Browser type, headless mode, and more
- 📱 **Responsive Design** - Looks great on any device

### View Reports

```bash
# Windows - Open the enhanced report
start target/enhanced-test-report.html

# Mac/Linux
open target/enhanced-test-report.html

# Or just double-click the HTML file in your file explorer
```

## Troubleshooting

### Common Issues

<details>
<summary><b>❌ Elements not found / Selector issues</b></summary>

**Solution:**
- Run with visible browser to inspect: `mvn "-Dtest.headless=false" test`
- Check selectors in page objects - OrangeHRM may have updated their UI
- Increase element timeout in `test.properties`: `element.wait.timeout=10000`
- Add explicit waits in your page object methods
</details>

<details>
<summary><b>❌ Login failures</b></summary>

**Solution:**
- Verify credentials in `test.properties` are correct
- Check that OrangeHRM demo site is accessible in your browser
- The demo site occasionally goes down - try again later
- Check if your IP is blocked (demo sites sometimes have rate limiting)
</details>

<details>
<summary><b>❌ Timeout errors</b></summary>

**Solution:**
```bash
# Increase timeouts for slow networks
mvn test -Dpage.load.timeout=30000 -Delement.wait.timeout=10000

# The external demo site can be slow - 30 seconds is safer
```

Edit `test.properties`:
```properties
page.load.timeout=30000
navigation.timeout=30000
```
</details>

<details>
<summary><b>❌ Tests are really slow</b></summary>

**Solution:**
- Headless mode is faster (default)
- Check your internet connection - tests hit a live external site
- The OrangeHRM demo can be slow during peak hours
- Consider running smoke tests only for quick feedback
</details>

<details>
<summary><b>❌ Random/flaky failures</b></summary>

**Solution:**
- External demo sites can be unreliable - not much we can do
- Increase timeouts in `test.properties`
- Run tests again - transient network issues happen
- Check if specific scenarios always fail (genuine bugs) vs random failures (timing)
</details>

<details>
<summary><b>❌ PowerShell command not working</b></summary>

**Solution:**
```bash
# ❌ Wrong - PowerShell parses this incorrectly
mvn test -Dcucumber.filter.tags=@smoke

# ✅ Correct - Quote the property
mvn "-Dcucumber.filter.tags=@smoke" test
```
</details>

### Enable Debug Logging

Edit `src/test/resources/log4j2.xml` and change level to `DEBUG`:

```xml
<Logger name="com.example.orangehrm" level="DEBUG"/>
```

## Best Practices

### ✅ DO

- **Run smoke tests often** - They're fast (~5 min) and catch critical issues
- **Tag tests appropriately** - Use `@smoke` for critical, `@regression` for complete coverage
- **Keep selectors in page objects** - Never put selectors directly in step definitions
- **Use meaningful test names** - Clear scenario descriptions help everyone
- **Check reports after failures** - Screenshots show exactly what went wrong
- **Clean before big runs** - `mvn clean test` for a fresh start

### ❌ DON'T

- **Don't hardcode credentials** - Use `test.properties` or environment variables
- **Don't skip cleanup** - Let Hooks handle browser lifecycle
- **Don't add waits in tests** - Playwright handles waits automatically
- **Don't ignore flaky tests** - Fix them or remove them, don't let them stay
- **Don't commit generated files** - `target/` is gitignored for a reason

## CI/CD Integration

### GitHub Actions Example

```yaml
name: Tests
on: [push, pull_request]
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-java@v3
        with:
          java-version: '11'
      - name: Run smoke tests
        run: mvn "-Dcucumber.filter.tags=@smoke" test
      - name: Upload reports
        if: always()
        uses: actions/upload-artifact@v3
        with:
          name: test-reports
          path: target/cucumber-report.html
```

### Jenkins Pipeline Example

```groovy
pipeline {
    agent any
    stages {
        stage('Test') {
            steps {
                sh 'mvn clean'
                sh 'mvn "-Dcucumber.filter.tags=@smoke" test'
            }
        }
    }
    post {
        always {
            publishHTML([
                reportDir: 'target',
                reportFiles: 'cucumber-report.html',
                reportName: 'Test Report'
            ])
        }
    }
}
```

---

## Need Help?

- Check the [Playwright Java docs](https://playwright.dev/java/)
- Review [Cucumber documentation](https://cucumber.io/docs/cucumber/)
- Look at existing tests in `src/test/resources/features/` for examples

**Happy Testing! 🚀**
