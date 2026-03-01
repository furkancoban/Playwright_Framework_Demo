package com.example.orangehrm.api;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.orangehrm.utils.TestLogger;

/**
 * API Testing Helper - Supports REST API testing alongside UI tests.
 * Provides clean interface for API requests with assertions.
 * Can be integrated with same reporting system as UI tests.
 */
public class APITester {
    
    private HttpClient httpClient;
    private String baseUrl;
    private Map<String, String> defaultHeaders;
    @SuppressWarnings("unused")
	private ObjectMapper objectMapper;
    
    public APITester(String baseUrl) {
        this.baseUrl = baseUrl;
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
        this.defaultHeaders = new HashMap<>();
        // Sensible JSON defaults for most REST endpoints.
        this.defaultHeaders.put("Content-Type", "application/json");
        this.defaultHeaders.put("Accept", "application/json");
    }
    
    public APITester addHeader(String key, String value) {
        this.defaultHeaders.put(key, value);
        return this;
    }
    
    public APITester setBearerToken(String token) {
        this.defaultHeaders.put("Authorization", "Bearer " + token);
        return this;
    }
    
    /**
     * GET request
     */
    public APIResponse get(String endpoint) {
        return get(endpoint, null);
    }
    
    public APIResponse get(String endpoint, Map<String, String> queryParams) {
        try {
            String url = baseUrl + endpoint;
            if (queryParams != null && !queryParams.isEmpty()) {
                url += "?" + buildQueryString(queryParams);
            }
            
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .GET()
                .uri(new URI(url));
            
            defaultHeaders.forEach(requestBuilder::header);
            
            HttpRequest request = requestBuilder.build();
            // Measure end-to-end request latency for performance assertions.
            long startTime = System.currentTimeMillis();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            long endTime = System.currentTimeMillis();
            
            // Wrap raw HTTP response in helper object used by step definitions.
            APIResponse apiResponse = new APIResponse(response);
            apiResponse.setResponseTime(endTime - startTime);
            
            TestLogger.info("✓ GET " + endpoint + " : " + response.statusCode() + " (" + (endTime - startTime) + "ms)");
            return apiResponse;
        } catch (Exception e) {
            TestLogger.error("✗ GET request failed: " + endpoint, e);
            throw new RuntimeException("API GET request failed: " + endpoint, e);
        }
    }
    
    /**
     * POST request
     */
    public APIResponse post(String endpoint, String body) {
        try {
            String url = baseUrl + endpoint;
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .uri(new URI(url));
            
            defaultHeaders.forEach(requestBuilder::header);
            
            HttpRequest request = requestBuilder.build();
            long startTime = System.currentTimeMillis();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            long endTime = System.currentTimeMillis();
            
            APIResponse apiResponse = new APIResponse(response);
            apiResponse.setResponseTime(endTime - startTime);
            
            TestLogger.info("✓ POST " + endpoint + " : " + response.statusCode() + " (" + (endTime - startTime) + "ms)");
            return apiResponse;
        } catch (Exception e) {
            TestLogger.error("✗ POST request failed: " + endpoint, e);
            throw new RuntimeException("API POST request failed: " + endpoint, e);
        }
    }
    
    /**
     * PUT request
     */
    public APIResponse put(String endpoint, String body) {
        try {
            String url = baseUrl + endpoint;
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .PUT(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .uri(new URI(url));
            
            defaultHeaders.forEach(requestBuilder::header);
            
            HttpRequest request = requestBuilder.build();
            long startTime = System.currentTimeMillis();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            long endTime = System.currentTimeMillis();
            
            APIResponse apiResponse = new APIResponse(response);
            apiResponse.setResponseTime(endTime - startTime);
            
            TestLogger.info("✓ PUT " + endpoint + " : " + response.statusCode() + " (" + (endTime - startTime) + "ms)");
            return apiResponse;
        } catch (Exception e) {
            TestLogger.error("✗ PUT request failed: " + endpoint, e);
            throw new RuntimeException("API PUT request failed: " + endpoint, e);
        }
    }
    
    /**
     * DELETE request
     */
    public APIResponse delete(String endpoint) {
        try {
            String url = baseUrl + endpoint;
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .DELETE()
                .uri(new URI(url));
            
            defaultHeaders.forEach(requestBuilder::header);
            
            HttpRequest request = requestBuilder.build();
            long startTime = System.currentTimeMillis();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            long endTime = System.currentTimeMillis();
            
            APIResponse apiResponse = new APIResponse(response);
            apiResponse.setResponseTime(endTime - startTime);
            
            TestLogger.info("✓ DELETE " + endpoint + " : " + response.statusCode() + " (" + (endTime - startTime) + "ms)");
            return apiResponse;
        } catch (Exception e) {
            TestLogger.error("✗ DELETE request failed: " + endpoint, e);
            throw new RuntimeException("API DELETE request failed: " + endpoint, e);
        }
    }
    
    private String buildQueryString(Map<String, String> params) {
        StringBuilder sb = new StringBuilder();
        // Simple key=value&key=value builder (no URL encoding by design for current test data).
        params.forEach((k, v) -> {
            if (sb.length() > 0) sb.append("&");
            sb.append(k).append("=").append(v);
        });
        return sb.toString();
    }
    
    /**
     * API Response wrapper with assertions
     */
    public static class APIResponse {
        private int statusCode;
        private String body;
        private long responseTime;
        @SuppressWarnings("unused")
		private HttpResponse<String> rawResponse;
        
        public APIResponse(HttpResponse<String> response) {
            this.rawResponse = response;
            this.statusCode = response.statusCode();
            this.body = response.body();
            this.responseTime = 0; // Will be set externally if needed
        }
        
        public int getStatusCode() {
            return statusCode;
        }
        
        public String getBody() {
            return body;
        }
        
        public long getResponseTime() {
            return responseTime;
        }
        
        public void setResponseTime(long responseTime) {
            this.responseTime = responseTime;
        }
        
        public boolean isSuccess() {
            return statusCode >= 200 && statusCode < 300;
        }
        
        public boolean isJson() {
            try {
                if (body == null || body.trim().isEmpty()) {
                    return false;
                }
                // Valid if Jackson can parse it into any JSON node type (object/array/value).
                new ObjectMapper().readTree(body);
                return true;
            } catch (Exception e) {
                return false;
            }
        }
        
        @SuppressWarnings("unchecked")
        public boolean hasField(String fieldName) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                Map<String, Object> json = mapper.readValue(body, Map.class);
                return json.containsKey(fieldName);
            } catch (Exception e) {
                return false;
            }
        }
        
        @SuppressWarnings("unchecked")
        public String getFieldValue(String fieldName) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                Map<String, Object> json = mapper.readValue(body, Map.class);
                Object value = json.get(fieldName);
                // Normalize to String for easy assertion in Cucumber steps.
                return value != null ? String.valueOf(value) : null;
            } catch (Exception e) {
                return null;
            }
        }
        
        public APIResponse assertStatusCode(int expectedCode) {
            if (statusCode != expectedCode) {
                throw new AssertionError("Status code mismatch. Expected: " + expectedCode + ", Got: " + statusCode);
            }
            TestLogger.success("✓ Status code verified: " + statusCode);
            return this;
        }
        
        public APIResponse assertStatusSuccess() {
            if (statusCode < 200 || statusCode >= 300) {
                throw new AssertionError("Expected success status code (2xx), got: " + statusCode);
            }
            TestLogger.success("✓ Response successful: " + statusCode);
            return this;
        }
        
        public APIResponse assertBodyContains(String expectedText) {
            if (!body.contains(expectedText)) {
                throw new AssertionError("Response body does not contain: " + expectedText);
            }
            TestLogger.success("✓ Response body contains: " + expectedText);
            return this;
        }
        
        @SuppressWarnings("unchecked")
		public APIResponse assertJsonField(String fieldName, String expectedValue) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                Map<String, Object> json = mapper.readValue(body, Map.class);
                Object actualValue = json.get(fieldName);
                
                if (!String.valueOf(actualValue).equals(expectedValue)) {
                    throw new AssertionError("JSON field '" + fieldName + "' mismatch. Expected: " + expectedValue + ", Got: " + actualValue);
                }
                TestLogger.success("✓ JSON field '" + fieldName + "' verified: " + expectedValue);
            } catch (Exception e) {
                throw new RuntimeException("Failed to parse or verify JSON", e);
            }
            return this;
        }
    }
}
