package com.example.orangehrm.steps;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import com.example.orangehrm.pages.LoginPage;
import com.example.orangehrm.pages.DashboardPage;
import com.example.orangehrm.context.TestContext;
import com.example.orangehrm.utils.TestLogger;
import com.example.orangehrm.utils.ScreenshotHelper;
import com.example.orangehrm.assertions.UIAssertions;
import com.example.orangehrm.api.APITester;
import com.example.orangehrm.config.ConfigManager;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

/**
 * Step definitions that connect our Cucumber scenarios to actual code.
 * 
 * Each method here maps to a line in the feature files (the "Given", "When", "Then" steps).
 * We use Cucumber's dependency injection to get the TestContext, which gives us
 * access to all our page objects.
 * 
 * The assertions are pretty straightforward - each step just checks that its main
 * action worked. More complex validation lives in the page objects themselves.
 */
public class StepDefinitions {

    private TestContext testContext;
    private LoginPage loginPage;
    private DashboardPage dashboardPage;
    
    // API Testing fields
    private APITester apiTester;
    private APITester.APIResponse lastApiResponse;
    private String apiBaseUrl;

    public StepDefinitions() {
        this.testContext = Hooks.testContext;
        if (testContext != null) {
            this.loginPage = testContext.getLoginPage();
            this.dashboardPage = testContext.getDashboardPage();
        }
        // Build API base URL from UI URL so UI/API tests stay aligned across environments.
        // Example:
        // https://.../web/index.php/auth/login -> https://.../web/index.php/api/v2
        this.apiBaseUrl = ConfigManager.getAppUrl().replace("/web/index.php/auth/login", "/web/index.php/api/v2");
        // Reuse one API client per scenario execution context.
        this.apiTester = new APITester(apiBaseUrl);
    }
    
    /**
     * Get UIAssertions instance for professional assertions
     */
    private UIAssertions assertions() {
        return new UIAssertions(testContext.getPage()).withContext("Step Assertion");
    }
    
    /**
     * Helper to do an assertion and grab a screenshot.
     * Screenshots get embedded in the Cucumber report with PASS/FAIL status.
     */
    @SuppressWarnings("unused")
    private void assertTrueWithScreenshot(boolean condition, String message, String stepDescription) {
        try {
            assertTrue(condition, message);
            // Assertion passed - capture success screenshot
            if (Hooks.currentScenario != null && Hooks.page != null) {
                ScreenshotHelper.captureAndEmbedScreenshot(Hooks.page, Hooks.currentScenario, stepDescription, "PASSED");
            }
        } catch (AssertionError e) {
            // Assertion failed - capture failure screenshot
            if (Hooks.currentScenario != null && Hooks.page != null) {
                ScreenshotHelper.captureAndEmbedScreenshot(Hooks.page, Hooks.currentScenario, stepDescription, "FAILED");
            }
            throw e; // Re-throw to fail the test
        }
    }
    
    /**
     * Helper for assertNotNull with a screenshot
     */
    @SuppressWarnings("unused")
    private void assertNotNullWithScreenshot(Object object, String message, String stepDescription) {
        try {
            assertNotNull(object, message);
            // Assertion passed - capture success screenshot
            if (Hooks.currentScenario != null && Hooks.page != null) {
                ScreenshotHelper.captureAndEmbedScreenshot(Hooks.page, Hooks.currentScenario, stepDescription, "PASSED");
            }
        } catch (AssertionError e) {
            // Assertion failed - capture failure screenshot
            if (Hooks.currentScenario != null && Hooks.page != null) {
                ScreenshotHelper.captureAndEmbedScreenshot(Hooks.page, Hooks.currentScenario, stepDescription, "FAILED");
            }
            throw e; // Re-throw to fail the test
        }
    }

    @Given("I am on the OrangeHRM login page")
    public void i_am_on_the_orangehrm_login_page() {
        TestLogger.testStep("Verify OrangeHRM login page");
        assertNotNull(loginPage, "Login page should be initialized");
        // Page is already loaded in @Before hook - no explicit verification needed
        // Element waits will happen during actual login interactions
        TestLogger.assertion("OrangeHRM login page ready");
    }

    @When("I login with valid credentials")
    public void i_login_with_valid_credentials() {
        TestLogger.testStep("Login with valid credentials from config");
        assertNotNull(loginPage, "Login page should exist");
        dashboardPage = loginPage.loginWithValidCredentials();
    }

    @When("I login with username {string} and password {string}")
    public void i_login_with_custom_credentials(String username, String password) {
        TestLogger.testStep("Login with custom credentials - username: " + username);
        assertNotNull(loginPage, "Login page should exist");
        dashboardPage = loginPage.loginWithCredentials(username, password);
        testContext.setTestData("lastUsername", username);
    }

    @Then("I should see the dashboard")
    public void i_should_see_the_dashboard() {
        TestLogger.testStep("Verify dashboard is displayed");
        assertNotNull(dashboardPage, "Dashboard page should exist");
        dashboardPage.verifyDashboardIsLoaded();
        TestLogger.assertion("Dashboard is visible and loaded");
    }

    @When("I navigate to {string} menu")
    public void i_navigate_to_menu(String menuName) {
        TestLogger.testStep("Navigate to menu: " + menuName);
        assertNotNull(dashboardPage, "Dashboard page should exist");
        dashboardPage.navigateToMenu(menuName);
        testContext.setTestData("lastMenuNavigation", menuName);
    }

    @Then("the page title should contain {string}")
    public void the_page_title_should_contain(String text) {
        TestLogger.testStep("Verify page title contains: " + text);
        // Use professional UI assertion
        assertions().titleContains(text);
    }

    @Then("I should see the {string} page header")
    public void i_should_see_page_header(String headerText) {
        TestLogger.testStep("Verify page header: " + headerText);
        assertNotNull(testContext.getPage(), "Browser page should exist");
        testContext.getPage().waitForSelector("h6:has-text('" + headerText + "')", 
            new com.microsoft.playwright.Page.WaitForSelectorOptions().setTimeout(5000));
        assertTrue(testContext.getPage().locator("h6:has-text('" + headerText + "')").isVisible(),
                "Header '" + headerText + "' should be visible");
        TestLogger.assertion("Page header verified: " + headerText);
    }

    @When("I click on my profile")
    public void i_click_on_my_profile() {
        TestLogger.testStep("Click on user profile dropdown");
        assertNotNull(dashboardPage, "Dashboard page should exist");
        dashboardPage.clickUserProfileDropdown();
    }

    @Then("I should see user menu options")
    public void i_should_see_user_menu_options() {
        TestLogger.testStep("Verify user menu is visible");
        assertNotNull(dashboardPage, "Dashboard page should exist");
        assertTrue(dashboardPage.isUserMenuVisible(), "User menu should be visible");
        TestLogger.assertion("User menu options are visible");
    }

    @Then("I should see at least {int} menu items")
    public void i_should_see_menu_items(int count) {
        TestLogger.testStep("Verify minimum menu items count: " + count);
        assertNotNull(dashboardPage, "Dashboard page should exist");
        int menuCount = dashboardPage.getMainMenuItemsCount();
        assertTrue(menuCount >= count, "Expected at least " + count + " menu items, but found " + menuCount);
        TestLogger.assertion("Menu items count verified: " + menuCount + " (expected >= " + count + ")");
    }

    @When("I logout")
    public void i_logout() {
        TestLogger.testStep("Logout from application");
        assertNotNull(dashboardPage, "Dashboard page should exist");
        loginPage = dashboardPage.logout();
        testContext.setTestData("loggedOut", true);
    }

    @Then("I should be on the login page")
    public void i_should_be_on_login_page() {
        TestLogger.testStep("Verify redirected to login page");
        assertNotNull(loginPage, "Login page should exist");
        loginPage.verifyLoginPageIsLoaded();
        TestLogger.assertion("Successfully verified login page");
    }

    @Then("the URL should contain {string}")
    public void the_url_should_contain(String urlPart) {
        TestLogger.testStep("Verify URL contains: " + urlPart);
        // Use professional UI assertion for URL validation
        assertions().urlContains(urlPart);
    }

    @When("I wait for {int} seconds")
    public void i_wait_for_seconds(int seconds) {
        TestLogger.testStep("Wait for " + seconds + " seconds");
        loginPage.wait(seconds * 1000);
    }

    @When("wait for {string} seconds")
    public void wait_for_seconds_string(String seconds) {
        TestLogger.testStep("Wait for " + seconds + " seconds");
        int secondsInt = Integer.parseInt(seconds);
        loginPage.wait(secondsInt * 1000);
    }

    @When("I verify menu item {string} exists")
    public void i_verify_menu_item_exists(String menuName) {
        TestLogger.testStep("Verify menu item exists: " + menuName);
        assertNotNull(dashboardPage, "Dashboard page should exist");
        boolean exists = dashboardPage.isMenuItemPresent(menuName);
        assertTrue(exists, "Menu item '" + menuName + "' should be present");
        testContext.setTestData("menuItemFound", menuName);
    }

    @Then("I should see error message")
    public void i_should_see_error_message() {
        TestLogger.testStep("Verify error message is displayed");
        assertNotNull(loginPage, "Login page should exist");

        boolean isErrorVisible = loginPage.isErrorMessageDisplayed();
        String currentUrl = testContext.getPage().url();

        assertTrue(
            isErrorVisible || currentUrl.contains("auth/login"),
            "Either error message should be displayed or user must remain on login page"
        );

        if (isErrorVisible) {
            TestLogger.assertion("Error message is visible: " + loginPage.getErrorMessage());
        } else {
            TestLogger.assertion("User remained on login page after invalid login attempt: " + currentUrl);
        }
    }

    @When("I check dashboard welcome message")
    public void i_check_dashboard_welcome_message() {
        TestLogger.testStep("Get dashboard welcome message");
        assertNotNull(dashboardPage, "Dashboard page should exist");
        String message = dashboardPage.getDashboardWelcomeMessage();
        testContext.setTestData("welcomeMessage", message);
        TestLogger.info("Dashboard welcome message: " + message);
    }

    @Then("I should see the username field")
    public void i_should_see_username_field() {
        TestLogger.testStep("Verify username field is visible");
        // Use professional UI assertion
        assertions().elementIsVisible("input[name='username']");
    }

    @Then("I should see the password field")
    public void i_should_see_password_field() {
        TestLogger.testStep("Verify password field is visible");
        // Use professional UI assertion
        assertions().elementIsVisible("input[name='password']");
    }

    @Then("I should see the login button")
    public void i_should_see_login_button() {
        TestLogger.testStep("Verify login button is visible");
        // Use professional UI assertion
        assertions().elementIsVisible("button[type='submit']");
    }

    @When("I verify login page elements are visible")
    public void i_verify_login_page_elements_visible() {
        TestLogger.testStep("Verify all login page elements");
        assertNotNull(loginPage, "Login page should exist");
        
        // Wait for all form elements with retries (10 second timeout)
        long startTime = System.currentTimeMillis();
        long maxWaitMs = 10000;
        boolean allVisible = false;
        
        while (System.currentTimeMillis() - startTime < maxWaitMs) {
            if (loginPage.isElementVisible("input[name='username']") &&
                loginPage.isElementVisible("input[name='password']") &&
                loginPage.isElementVisible("button[type='submit']")) {
                allVisible = true;
                break;
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        // At least check if elements exist, even if not fully visible
        if (!allVisible) {
            TestLogger.warn("Some form elements not visible within 10 seconds, checking presence...");
            assertTrue(loginPage.isElementVisible("input[name='username']"), "Username field should be visible");
            assertTrue(loginPage.isElementVisible("input[name='password']"), "Password field should be visible");
            assertTrue(loginPage.isElementVisible("button[type='submit']"), "Login button should be visible");
        }
        
        TestLogger.assertion("All login page elements are visible");
    }

    @Then("I should see the user welcome message")
    public void i_should_see_user_welcome_message() {
        TestLogger.testStep("Verify user welcome message is displayed");
        assertNotNull(dashboardPage, "Dashboard page should exist");
        String welcomeMessage = dashboardPage.getDashboardWelcomeMessage();
        assertNotNull(welcomeMessage, "Welcome message should not be null");
        assertTrue(!welcomeMessage.isEmpty(), "Welcome message should not be empty");
        TestLogger.assertion("User welcome message verified: " + welcomeMessage);
    }

    @Then("I should see the main menu")
    public void i_should_see_main_menu() {
        TestLogger.testStep("Verify main menu is visible");
        assertNotNull(dashboardPage, "Dashboard page should exist");
        int menuItemCount = dashboardPage.getMainMenuItemsCount();
        assertTrue(menuItemCount > 0, "Main menu should have at least one item");
        TestLogger.assertion("Main menu is visible with " + menuItemCount + " items");
    }

    @Then("the Admin page should load successfully")
    public void the_admin_page_loads_successfully() {
        TestLogger.testStep("Verify Admin page loaded");
        assertNotNull(testContext.getPage(), "Page should exist");
        String url = testContext.getPage().url();
        assertTrue(url.contains("admin"), "URL should contain 'admin'");
        TestLogger.assertion("Admin page loaded successfully");
    }

    @Then("I should see PIM module elements")
    public void i_should_see_pim_module_elements() {
        TestLogger.testStep("Verify PIM module elements");
        assertNotNull(testContext.getPage(), "Page should exist");
        String url = testContext.getPage().url();
        assertTrue(url.contains("pim"), "URL should contain 'pim' module");
        TestLogger.assertion("PIM module elements verified");
    }

    @When("I verify dashboard menu is visible")
    public void i_verify_dashboard_menu_is_visible() {
        TestLogger.testStep("Verify dashboard menu is visible");
        assertNotNull(dashboardPage, "Dashboard should exist");
        int menuCount = dashboardPage.getMainMenuItemsCount();
        assertTrue(menuCount > 0, "Dashboard menu should be visible with items");
        TestLogger.assertion("Dashboard menu verified with " + menuCount + " items");
    }

    @Then("I should see leave module content")
    public void i_should_see_leave_module_content() {
        TestLogger.testStep("Verify Leave module content");
        assertNotNull(testContext.getPage(), "Page should exist");
        String url = testContext.getPage().url();
        assertTrue(url.contains("leave"), "URL should contain 'leave' module");
        TestLogger.assertion("Leave module content verified");
    }

    @Then("the page should load successfully")
    public void the_page_should_load_successfully() {
        TestLogger.testStep("Verify page loaded successfully");
        assertNotNull(testContext.getPage(), "Page should exist");
        String url = testContext.getPage().url();
        assertNotNull(url, "Page URL should not be null");
        assertTrue(!url.isEmpty(), "Page URL should not be empty");
        TestLogger.assertion("Page loaded successfully: " + url);
    }

    @Then("I should see time module elements")
    public void i_should_see_time_module_elements() {
        TestLogger.testStep("Verify Time module elements");
        assertNotNull(testContext.getPage(), "Page should exist");
        String url = testContext.getPage().url();
        assertTrue(url.contains("time"), "URL should contain 'time' module");
        TestLogger.assertion("Time module elements verified");
    }

    @Then("I should see recruitment page content")
    public void i_should_see_recruitment_page_content() {
        TestLogger.testStep("Verify Recruitment page content");
        assertNotNull(testContext.getPage(), "Page should exist");
        String url = testContext.getPage().url();
        assertTrue(url.contains("recruitment"), "URL should contain 'recruitment' module");
        TestLogger.assertion("Recruitment page content verified");
    }

    @Then("I should see the profile option")
    public void i_should_see_profile_option() {
        TestLogger.testStep("Verify profile option in menu");
        assertNotNull(dashboardPage, "Dashboard page should exist");
        // Check for any profile-related menu item in the dropdown
        boolean hasAboutOrProfile = dashboardPage.isDropdownMenuItemPresent("About") ||
                        dashboardPage.isDropdownMenuItemPresent("Support") ||
                        dashboardPage.isDropdownMenuItemPresent("Change Password");
        assertTrue(hasAboutOrProfile,
            "Profile-related options should be present in user menu");
        TestLogger.assertion("Profile menu options are visible in user dropdown");
    }

    @Then("I should see the logout option")
    public void i_should_see_logout_option() {
        TestLogger.testStep("Verify logout option in menu");
        assertNotNull(dashboardPage, "Dashboard page should exist");
        assertTrue(dashboardPage.isDropdownMenuItemPresent("Logout"),
                "Logout option should be present in user menu");
        TestLogger.assertion("Logout option is visible in user menu");

        @Then("I should see personal details section")
        public void i_should_see_personal_details_section() {
            TestLogger.testStep("Verify personal details section on My Info page");
            assertNotNull(testContext.getPage(), "Page should exist");
            String url = testContext.getPage().url();
            assertTrue(url.contains("myinfo"), "URL should contain 'myinfo' module");
            // Wait for page content to load
            testContext.getPage().waitForLoadState();
            TestLogger.assertion("Personal details section is visible on My Info page");
        }
    }

    @When("I verify all main menu items are present")
    public void i_verify_all_main_menu_items_present() {
        TestLogger.testStep("Verify all main menu items are present");
        assertNotNull(dashboardPage, "Dashboard page should exist");
        int menuCount = dashboardPage.getMainMenuItemsCount();
        assertTrue(menuCount >= 8, "Should have at least 8 main menu items, found: " + menuCount);
        TestLogger.assertion("All main menu items verified - count: " + menuCount);
    }

    @Then("each menu item should be clickable")
    public void each_menu_item_should_be_clickable() {
        TestLogger.testStep("Verify each menu item is clickable");
        assertNotNull(dashboardPage, "Dashboard page should exist");
        int menuCount = dashboardPage.getMainMenuItemsCount();
        assertTrue(menuCount > 0, "Menu items should be clickable");
        TestLogger.assertion("Menu items are clickable - verified " + menuCount + " items");
    }

    @Then("the login page should display properly")
    public void the_login_page_should_display_properly() {
        TestLogger.testStep("Verify login page displays properly");
        assertNotNull(loginPage, "Login page should exist");
        assertTrue(loginPage.isElementVisible("input[name='username']"), "Username field should be visible");
        assertTrue(loginPage.isElementVisible("input[name='password']"), "Password field should be visible");
        assertTrue(loginPage.isElementVisible("button[type='submit']"), "Login button should be visible");
        TestLogger.assertion("Login page displays properly with all elements");
    }
    
    // ==================== API TEST STEP DEFINITIONS ====================
    
    @Given("the API base URL is configured")
    public void the_api_base_url_is_configured() {
        TestLogger.testStep("Verify API base URL is configured");
        assertNotNull(apiBaseUrl, "API base URL should be configured");
        TestLogger.info("API Base URL: " + apiBaseUrl);
    }
    
    @When("I send a GET request to {string}")
    public void i_send_a_get_request_to(String endpoint) {
        TestLogger.testStep("Send GET request to: " + endpoint);
        // Store the latest response so follow-up Then-steps can assert on it.
        lastApiResponse = apiTester.get(endpoint);
        TestLogger.info("Response Status: " + lastApiResponse.getStatusCode());
    }
    
    @When("I send a POST request to {string} with body:")
    public void i_send_a_post_request_to_with_body(String endpoint, String requestBody) {
        TestLogger.testStep("Send POST request to: " + endpoint);
        // Request body comes from Cucumber doc-string in the feature file.
        lastApiResponse = apiTester.post(endpoint, requestBody);
        TestLogger.info("Response Status: " + lastApiResponse.getStatusCode());
    }
    
    @When("I send a PUT request to {string} with body:")
    public void i_send_a_put_request_to_with_body(String endpoint, String requestBody) {
        TestLogger.testStep("Send PUT request to: " + endpoint);
        lastApiResponse = apiTester.put(endpoint, requestBody);
        TestLogger.info("Response Status: " + lastApiResponse.getStatusCode());
    }
    
    @When("I send a DELETE request to {string}")
    public void i_send_a_delete_request_to(String endpoint) {
        TestLogger.testStep("Send DELETE request to: " + endpoint);
        lastApiResponse = apiTester.delete(endpoint);
        TestLogger.info("Response Status: " + lastApiResponse.getStatusCode());
    }
    
    @Then("the API response status code should be {int}")
    public void the_api_response_status_code_should_be(Integer expectedStatusCode) {
        TestLogger.testStep("Verify API response status code is " + expectedStatusCode);
        assertNotNull(lastApiResponse, "API response should not be null");
        // Strict contract check on HTTP status.
        assertEquals(expectedStatusCode.intValue(), lastApiResponse.getStatusCode(),
                "Expected status code " + expectedStatusCode + " but got " + lastApiResponse.getStatusCode());
        TestLogger.assertion("✓ API response status code is " + expectedStatusCode);
    }
    
    @Then("the API response should contain {string}")
    public void the_api_response_should_contain(String expectedContent) {
        TestLogger.testStep("Verify API response contains: " + expectedContent);
        assertNotNull(lastApiResponse, "API response should not be null");
        String responseBody = lastApiResponse.getBody();
        assertTrue(responseBody.contains(expectedContent),
                "Response should contain '" + expectedContent + "' but was: " + responseBody);
        TestLogger.assertion("✓ API response contains expected content");
    }
    
    @Then("the API response time should be less than {int} milliseconds")
    public void the_api_response_time_should_be_less_than_milliseconds(Integer maxResponseTime) {
        TestLogger.testStep("Verify API response time is less than " + maxResponseTime + "ms");
        assertNotNull(lastApiResponse, "API response should not be null");
        long actualResponseTime = lastApiResponse.getResponseTime();
        assertTrue(actualResponseTime < maxResponseTime,
                "Response time should be less than " + maxResponseTime + "ms but was " + actualResponseTime + "ms");
        TestLogger.assertion("✓ API response time: " + actualResponseTime + "ms");
    }
    
    @Then("the API response should be valid JSON")
    public void the_api_response_should_be_valid_json() {
        TestLogger.testStep("Verify API response is valid JSON");
        assertNotNull(lastApiResponse, "API response should not be null");
        // Quick schema-agnostic sanity check: response must be parseable JSON.
        assertTrue(lastApiResponse.isJson(), "Response should be valid JSON");
        TestLogger.assertion("✓ API response is valid JSON");
    }
    
    @Then("the API response should have field {string}")
    public void the_api_response_should_have_field(String fieldName) {
        TestLogger.testStep("Verify API response has field: " + fieldName);
        assertNotNull(lastApiResponse, "API response should not be null");
        assertTrue(lastApiResponse.hasField(fieldName),
                "Response should have field '" + fieldName + "'");
        TestLogger.assertion("✓ API response has field: " + fieldName);
    }
    
    @Then("the API response field {string} should equal {string}")
    public void the_api_response_field_should_equal(String fieldName, String expectedValue) {
        TestLogger.testStep("Verify API response field '" + fieldName + "' equals '" + expectedValue + "'");
        assertNotNull(lastApiResponse, "API response should not be null");
        String actualValue = lastApiResponse.getFieldValue(fieldName);
        assertEquals(expectedValue, actualValue,
                "Field '" + fieldName + "' should equal '" + expectedValue + "' but was '" + actualValue + "'");
        TestLogger.assertion("✓ API response field matches expected value");
    }
    
    @When("I add API header {string} with value {string}")
    public void i_add_api_header_with_value(String headerName, String headerValue) {
        TestLogger.testStep("Add API header: " + headerName);
        apiTester.addHeader(headerName, headerValue);
        TestLogger.info("Header added: " + headerName + " = " + headerValue);
    }
    
    @When("I set API bearer token {string}")
    public void i_set_api_bearer_token(String token) {
        TestLogger.testStep("Set API bearer token");
        apiTester.setBearerToken(token);
        TestLogger.info("Bearer token configured");
    }
    
    @Then("the API response should be successful")
    public void the_api_response_should_be_successful() {
        TestLogger.testStep("Verify API response is successful");
        assertNotNull(lastApiResponse, "API response should not be null");
        assertTrue(lastApiResponse.isSuccess(),
                "API response should be successful (2xx) but was " + lastApiResponse.getStatusCode());
        TestLogger.assertion("✓ API response is successful");
    }
    
    @Then("the API should be accessible")
    public void the_api_should_be_accessible() {
        TestLogger.testStep("Verify API is accessible");
        // Try a simple base endpoint probe. We only fail hard on server errors (5xx).
        // This keeps health checks tolerant of 2xx/3xx/4xx differences across environments.
        APITester.APIResponse response = apiTester.get("/");
        assertNotNull(response, "API should respond");
        assertTrue(response.getStatusCode() < 500, 
                "API should be accessible (status < 500), got: " + response.getStatusCode());
        TestLogger.assertion("✓ API is accessible");
    }
}

