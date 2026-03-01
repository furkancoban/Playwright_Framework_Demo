@regression
Feature: Comprehensive regression tests for OrangeHRM

  This is the full regression suite - everything that didn't make it into the
  smoke tests. We've got 40+ scenarios here covering all the different modules,
  workflows, and edge cases. Run this when you want thorough coverage before
  a release or after major changes.

  Scenario Outline: Navigate to different modules
    Given I am on the OrangeHRM login page
    When I login with valid credentials
    Then I should see the dashboard
    When I navigate to "<module>" menu
    Then the URL should contain "<urlPart>"

    Examples:
      | module      | urlPart     |
      | Admin       | admin       |
      | PIM         | pim         |
      | Leave       | leave       |
      | Time        | time        |
      | Recruitment | recruitment |
      | My Info     | myinfo      |
      | Performance | performance |
      | Dashboard   | dashboard   |

  @regression @login_workflow
  Scenario: Full login and logout cycle
    Given I am on the OrangeHRM login page
    When I login with valid credentials
    Then I should see the dashboard
    And the URL should contain "dashboard"
    When I logout
    Then I should be on the login page

  @regression @navigation
  Scenario: Check all menu items are clickable
    Given I am on the OrangeHRM login page
    When I login with valid credentials
    Then I should see the dashboard
    And I should see at least 8 menu items

  @regression @user_profile
  Scenario: Access user profile menu
    Given I am on the OrangeHRM login page
    When I login with valid credentials
    Then I should see the dashboard
    When I click on my profile
    Then I should see user menu options

  @regression @detailed
  Scenario: Navigate through multiple modules in sequence
    Given I am on the OrangeHRM login page
    When I login with valid credentials
    Then I should see the dashboard
    When I navigate to "Admin" menu
    Then the URL should contain "admin"
    When I navigate to "PIM" menu
    Then the URL should contain "pim"
    When I navigate to "Leave" menu
    Then the URL should contain "leave"
    When I navigate to "Dashboard" menu
    Then the URL should contain "dashboard"

  @regression @page_load
  Scenario Outline: Verify page load times for different modules
    Given I am on the OrangeHRM login page
    When I login with valid credentials
    Then I should see the dashboard
    When I navigate to "<module>" menu
    And I wait for 2 seconds
    Then the URL should contain "<urlPart>"

    Examples:
      | module      | urlPart     |
      | Admin       | admin       |
      | PIM         | pim         |
      | Leave       | leave       |
      | Time        | time       |
      | Recruitment | recruitment |

  @regression @stability
  Scenario: Dashboard stability check
    Given I am on the OrangeHRM login page
    When I login with valid credentials
    Then I should see the dashboard
    When I wait for 3 seconds
    Then the URL should contain "dashboard"
    And the page title should contain "OrangeHRM"

  @regression @critical
  Scenario: Verify login page elements
    Given I am on the OrangeHRM login page
    Then the page title should contain "OrangeHRM"
    And the URL should contain "auth/login"

  # Advanced workflow tests relocated from advanced.feature
  @regression @login_validation @critical
  Scenario: Login validation with invalid credentials
    Given I am on the OrangeHRM login page
    When I login with username "InvalidUser" and password "InvalidPassword"
    Then I should see error message

  @regression @multi_module_workflow @complex
  Scenario: User navigates through multiple modules sequentially
    Given I am on the OrangeHRM login page
    When I login with valid credentials
    Then I should see the dashboard
    When I navigate to "Admin" menu
    Then the URL should contain "admin"
    When I wait for 1 seconds
    When I navigate to "PIM" menu
    Then the URL should contain "pim"
    When I wait for 1 seconds
    When I navigate to "Leave" menu
    Then the URL should contain "leave"
    When I wait for 1 seconds
    When I navigate to "Time" menu
    Then the URL should contain "time"
    When I wait for 1 seconds
    When I navigate to "Dashboard" menu
    Then the URL should contain "dashboard"

  @regression @profile_menu_workflow @detailed
  Scenario: User profile menu interactions
    Given I am on the OrangeHRM login page
    When I login with valid credentials
    Then I should see the dashboard
    When I click on my profile
    Then I should see user menu options
    When I logout
    Then I should be on the login page

  @regression @menu_verification @critical
  Scenario: Verify all OrangeHRM main menu items are accessible
    Given I am on the OrangeHRM login page
    When I login with valid credentials
    Then I should see the dashboard
    And I should see at least 8 menu items
    When I verify menu item "Admin" exists
    When I verify menu item "PIM" exists
    When I verify menu item "Leave" exists
    When I verify menu item "Time" exists
    When I verify menu item "Recruitment" exists

  @regression @dashboard_content_validation @detailed
  Scenario: Dashboard content validation after login
    Given I am on the OrangeHRM login page
    When I login with valid credentials
    Then I should see the dashboard
    When I check dashboard welcome message
    And the page title should contain "OrangeHRM"
    And the URL should contain "dashboard"

  @regression @page_stability @performance
  Scenario: Page stability and load time verification
    Given I am on the OrangeHRM login page
    When I login with valid credentials
    Then I should see the dashboard
    When I wait for 2 seconds
    And the page title should contain "OrangeHRM"
    And the URL should contain "dashboard"
    When I navigate to "Admin" menu
    When I wait for 2 seconds
    Then the URL should contain "admin"

  @regression @sequential_navigation @workflow
  Scenario Outline: Navigate to multiple modules with validation
    Given I am on the OrangeHRM login page
    When I login with valid credentials
    Then I should see the dashboard
    When I navigate to "<module>" menu
    And I wait for 1 seconds
    Then the URL should contain "<urlPart>"

    Examples:
      | module      | urlPart     |
      | Admin       | admin       |
      | PIM         | pim         |
      | Leave       | leave       |
      | Time        | time       |
      | Recruitment | recruitment |
      | Maintenance | maintenance |

  @regression @error_recovery @resilience
  Scenario: Error recovery workflow
    Given I am on the OrangeHRM login page
    When I login with username "AdminWrong" and password "admin123"
    Then I should see error message
    When I login with valid credentials
    Then I should see the dashboard

  @regression @complete_user_journey @critical
  Scenario: Complete user journey - Full workflow from login to logout
    Given I am on the OrangeHRM login page
    And the page title should contain "OrangeHRM"
    When I login with valid credentials
    Then I should see the dashboard
    And I should see at least 8 menu items
    When I navigate to "Admin" menu
    Then the URL should contain "admin"
    When I navigate to "Dashboard" menu
    Then the URL should contain "dashboard"
    When I click on my profile
    Then I should see user menu options
    When I logout
    Then I should be on the login page
    And the URL should contain "auth/login"

  @regression @concurrent_menu_switches @complex
  Scenario: Rapid menu navigation without delays
    Given I am on the OrangeHRM login page
    When I login with valid credentials
    Then I should see the dashboard
    When I navigate to "Admin" menu
    When I navigate to "PIM" menu
    When I navigate to "Leave" menu
    When I navigate to "Time" menu
    When I navigate to "Recruitment" menu
    When I navigate to "Dashboard" menu
    Then the URL should contain "dashboard"

  @regression @data_persistence @workflow
  Scenario: Verify data persistence across navigation
    Given I am on the OrangeHRM login page
    When I login with valid credentials
    Then I should see the dashboard
    When I check dashboard welcome message
    When I navigate to "Admin" menu
    When I navigate to "Dashboard" menu
    Then I should see the dashboard

  @regression @page_element_validation @detailed
  Scenario: Comprehensive page element validation
    Given I am on the OrangeHRM login page
    When I login with valid credentials
    Then I should see the dashboard
    And I should see the "Dashboard" page header

      @regression @test11 @auth_resilience @senior
      Scenario: Test 11 - Validate authentication resilience with negative then positive login
        Given I am on the OrangeHRM login page
        Then I should see the username field
        And I should see the password field
        And I should see the login button
        When I login with username "InvalidUser" and password "InvalidPass123"
        Then I should see error message
        And the URL should contain "auth/login"
        And I should see the username field
        And I should see the password field
        When I login with valid credentials
        Then I should see the dashboard
        And the URL should contain "dashboard"
        And I should see the user welcome message

      @regression @test12 @cross_module @senior
      Scenario: Test 12 - Verify session continuity during cross-module traversal
        Given I am on the OrangeHRM login page
        When I login with valid credentials
        Then I should see the dashboard
        And I should see the main menu
        When I navigate to "Admin" menu
        Then the URL should contain "admin"
        And the Admin page should load successfully
        When I navigate to "PIM" menu
        Then the URL should contain "pim"
        And I should see PIM module elements
        When I navigate to "Leave" menu
        Then the URL should contain "leave"
        And I should see leave module content
        When I navigate to "Dashboard" menu
        Then the URL should contain "dashboard"
        And I should see the user welcome message

      @regression @test13 @profile_controls @senior
      Scenario: Test 13 - Validate profile controls remain stable after module navigation
        Given I am on the OrangeHRM login page
        When I login with valid credentials
        Then I should see the dashboard
        And I should see the main menu
        When I navigate to "Recruitment" menu
        Then the URL should contain "recruitment"
        And I should see recruitment page content
        When I click on my profile
        Then I should see user menu options
        And I should see the profile option
        And I should see the logout option
        When I navigate to "Time" menu
        Then the URL should contain "time"
        And I should see time module elements
        When I click on my profile
        Then I should see user menu options
        And I should see the logout option

      @regression @test14 @navigation_hardening @senior
      Scenario: Test 14 - Validate critical modules are reachable in repeated navigation sequence
        Given I am on the OrangeHRM login page
        When I login with valid credentials
        Then I should see the dashboard
        And I verify all main menu items are present
        And each menu item should be clickable
        When I navigate to "Time" menu
        Then the URL should contain "time"
        And the page should load successfully
        And I should see time module elements
        When I navigate to "Admin" menu
        Then the URL should contain "admin"
        And the Admin page should load successfully
        When I navigate to "PIM" menu
        Then the URL should contain "pim"
        And I should see PIM module elements
        And I should see at least 8 menu items

      @regression @test15 @end_to_end_logout @senior
      Scenario: Test 15 - End-to-end login, module verification, and secure logout
        Given I am on the OrangeHRM login page
        When I verify login page elements are visible
        And I login with valid credentials
        Then I should see the dashboard
        And I should see the user welcome message
        When I navigate to "Leave" menu
        Then the URL should contain "leave"
        And I should see leave module content
        When I navigate to "Dashboard" menu
        Then the URL should contain "dashboard"
        And I should see the main menu
        When I click on my profile
        Then I should see user menu options
        And I should see the logout option
        When I logout
        Then I should be on the login page
        And the login page should display properly
        And the page title should contain "OrangeHRM"


