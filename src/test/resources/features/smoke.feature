@smoke
Feature: Smoke Test Suite - OrangeHRM Critical Functionality

  This is our smoke test suite - just 10 critical tests to make sure nothing's
  completely broken after a deployment. We're checking login, basic navigation,
  and that the main modules load. If these pass, we're good to go deeper.
  Each test grabs screenshots so you can see what happened in the reports.

  @smoke @test1 @login_verification
  Scenario: Test 1 - Make sure the login page loads properly
    Given I am on the OrangeHRM login page
    Then the page title should contain "OrangeHRM"
    And I should see the username field
    And I should see the password field
    And I should see the login button

  @smoke @test2 @login_workflow
  Scenario: Test 2 - Full login flow with valid credentials
    Given I am on the OrangeHRM login page
    When I verify login page elements are visible
    And I login with valid credentials
    Then I should see the dashboard
    And the URL should contain "dashboard"
    And I should see the user welcome message

  @smoke @test3 @navigation @admin
  Scenario: Test 3 - Check that Admin module loads
    Given I am on the OrangeHRM login page
    When I login with valid credentials
    Then I should see the dashboard
    And I should see the main menu
    When I navigate to "Admin" menu
    Then the URL should contain "admin"
    And the Admin page should load successfully

  @smoke @test4 @navigation @pim
  Scenario: Test 4 - Check PIM module works
    Given I am on the OrangeHRM login page
    When I login with valid credentials
    Then I should see the dashboard
    When I navigate to "PIM" menu
    Then the URL should contain "pim"
    And I should see PIM module elements

  @smoke @test5 @navigation @leave
  Scenario: Test 5 - Navigate to Leave module
    Given I am on the OrangeHRM login page
    When I login with valid credentials
    Then I should see the dashboard
    And I verify dashboard menu is visible
    When I navigate to "Leave" menu
    Then the URL should contain "leave"
    And I should see leave module content

  @smoke @test6 @navigation @time
  Scenario: Test 6 - Make sure Time module loads
    Given I am on the OrangeHRM login page
    When I login with valid credentials
    Then I should see the dashboard
    When I navigate to "Time" menu
    Then the URL should contain "time"
    And the page should load successfully
    And I should see time module elements

  @smoke @test7 @navigation @recruitment
  Scenario: Test 7 - Navigate to Recruitment module
    Given I am on the OrangeHRM login page
    When I login with valid credentials
    Then I should see the dashboard
    When I navigate to "Recruitment" menu
    Then the URL should contain "recruitment"
    And I should see recruitment page content

  @smoke @test8 @profile_menu
  Scenario: Test 8 - Check user profile dropdown
    Given I am on the OrangeHRM login page
    When I login with valid credentials
    Then I should see the dashboard
    When I click on my profile
    Then I should see user menu options
    And I should see the profile option
    And I should see the logout option

  @smoke @test9 @menu_verification
  Scenario: Test 9 - Make sure all menu items are there
    Given I am on the OrangeHRM login page
    When I login with valid credentials
    Then I should see the dashboard
    And I verify all main menu items are present
    And I should see at least 8 menu items
    And each menu item should be clickable

  @smoke @test10 @logout_workflow
  Scenario: Test 10 - Login and logout flow
    Given I am on the OrangeHRM login page
    When I login with valid credentials
    Then I should see the dashboard
    And wait for "2" seconds
    When I click on my profile
    Then I should see user menu options
    When I logout
    Then I should be on the login page
    And the login page should display properly

  # ==================== API SMOKE TESTS ====================
  # These 10 API tests verify the REST API endpoints are working
  # They complement the UI tests above for complete coverage

  @smoke @api @test11 @api_health
  Scenario: Test 11 - API Health Check
    Given the API base URL is configured
    Then the API should be accessible
    When I send a GET request to "/"
    Then the API response status code should be 200
    And the API response time should be less than 5000 milliseconds

  @smoke @api @test12 @api_auth
  Scenario: Test 12 - API Authentication Endpoint
    Given the API base URL is configured
    When I send a POST request to "/auth/login" with body:
      """
      {
        "username": "Admin",
        "password": "admin123"
      }
      """
    Then the API response should be successful
    And the API response should be valid JSON
    And the API response time should be less than 3000 milliseconds

  @smoke @api @test13 @api_employees_list
  Scenario: Test 13 - Get Employee List
    Given the API base URL is configured
    When I add API header "Authorization" with value "Bearer test-token"
    And I send a GET request to "/pim/employees"
    Then the API response should be valid JSON
    And the API response time should be less than 5000 milliseconds

  @smoke @api @test14 @api_employee_details
  Scenario: Test 14 - Get Single Employee Details
    Given the API base URL is configured
    When I add API header "Authorization" with value "Bearer test-token"
    And I send a GET request to "/pim/employees/1"
    Then the API response should be valid JSON
    And the API response time should be less than 3000 milliseconds

  @smoke @api @test15 @api_create_employee
  Scenario: Test 15 - Create New Employee
    Given the API base URL is configured
    When I add API header "Authorization" with value "Bearer test-token"
    And I send a POST request to "/pim/employees" with body:
      """
      {
        "firstName": "John",
        "middleName": "Test",
        "lastName": "Doe",
        "employeeId": "EMP-API-001"
      }
      """
    Then the API response should be valid JSON
    And the API response time should be less than 5000 milliseconds

  @smoke @api @test16 @api_update_employee
  Scenario: Test 16 - Update Employee Information
    Given the API base URL is configured
    When I add API header "Authorization" with value "Bearer test-token"
    And I send a PUT request to "/pim/employees/1" with body:
      """
      {
        "firstName": "Jane",
        "lastName": "Smith",
        "employeeId": "EMP-API-002"
      }
      """
    Then the API response should be valid JSON
    And the API response time should be less than 3000 milliseconds

  @smoke @api @test17 @api_leave_requests
  Scenario: Test 17 - Get Leave Requests List
    Given the API base URL is configured
    When I add API header "Authorization" with value "Bearer test-token"
    And I send a GET request to "/leave/leave-requests"
    Then the API response should be valid JSON
    And the API response time should be less than 5000 milliseconds

  @smoke @api @test18 @api_create_leave
  Scenario: Test 18 - Create Leave Request
    Given the API base URL is configured
    When I add API header "Authorization" with value "Bearer test-token"
    And I send a POST request to "/leave/leave-requests" with body:
      """
      {
        "leaveType": "Annual",
        "fromDate": "2026-03-15",
        "toDate": "2026-03-17",
        "comment": "Family vacation"
      }
      """
    Then the API response should be valid JSON
    And the API response time should be less than 3000 milliseconds

  @smoke @api @test19 @api_user_profile
  Scenario: Test 19 - Get Current User Profile
    Given the API base URL is configured
    When I add API header "Authorization" with value "Bearer test-token"
    And I send a GET request to "/admin/users/current"
    Then the API response should be valid JSON
    And the API response time should be less than 3000 milliseconds

  @smoke @api @test20 @api_time_records
  Scenario: Test 20 - Get Time Records
    Given the API base URL is configured
    When I add API header "Authorization" with value "Bearer test-token"
    And I send a GET request to "/time/timesheets"
    Then the API response should be valid JSON
    And the API response time should be less than 5000 milliseconds

  @smoke @test21 @navigation @myinfo
  Scenario: Test 21 - Navigate to My Info module
    Given I am on the OrangeHRM login page
    When I login with valid credentials
    Then I should see the dashboard
    When I navigate to "My Info" menu
    Then the URL should contain "pim/viewPersonalDetails"
    And the page should load successfully
    And I should see personal details section
