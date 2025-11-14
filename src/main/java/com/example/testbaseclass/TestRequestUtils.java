package com.example.testbaseclass;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

//import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat; // ✅ Correct Hamcrest import
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


/**
 * ✅ TestRequestUtils
 * --------------------------------------------------------------------
 * A reusable utility class for simplifying MockMvc request handling,
 * JSON parsing, and generic assertions in integration tests.
 *
 * Works with any Spring Boot app that uses this test base JAR.
 */
public class TestRequestUtils {

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;

    public TestRequestUtils(MockMvc mockMvc, ObjectMapper objectMapper) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
    }

    // -------------------------------------------------------
    // 🔹 HTTP Request Helpers
    // -------------------------------------------------------

//     /**
//      * Generic POST helper that expects HTTP 201 Created.
//      */
//     public MvcResult doPostExpectCreated(String url, Object body) throws Exception {
//         return mockMvc.perform(post(url)
//                         .contentType(MediaType.APPLICATION_JSON)
//                         .content(objectMapper.writeValueAsString(body)))
//                 .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isCreated()) // ✅ Correct static call
//                 .andReturn();
//     }

//   /** Generic POST helper that expects an error (default: 500). */
//     public MvcResult doPostExpectError(String url, Object body) throws Exception {
//         return doPostExpectError(url, body, 500);
//     }

//     /** POST helper that expects a configurable error status code. */
//     public MvcResult doPostExpectError(String url, Object body, int expectedStatus) throws Exception {
//         return mockMvc.perform(post(url)
//                         .contentType(MediaType.APPLICATION_JSON)
//                         .content(objectMapper.writeValueAsString(body)))
//                 .andExpect(status().is(expectedStatus))
//                 .andReturn();
//     }

//     /** POST helper that expects HTTP 400 Bad Request. */
//     public MvcResult doPostExpectBadRequest(String url, Object body) throws Exception {
//         return doPostExpectError(url, body, 400);
//     }

//     /** POST helper that expects HTTP 404 Not Found. */
//     public MvcResult doPostExpectNotFound(String url, Object body) throws Exception {
//         return doPostExpectError(url, body, 404);
//     }

//     /** POST helper that expects HTTP 500 Internal Server Error. */
//     public MvcResult doPostExpectInternalError(String url, Object body) throws Exception {
//         return doPostExpectError(url, body, 500);
//     }

    //new post
    public MvcResult doPost(String url,Object body) throws Exception{

          if (url == null || !url.startsWith("/")) {
        throw new IllegalArgumentException("❌ Invalid URL: '" + url + "'. Must start with '/' (e.g. '/api/users').");
    }

    logRequest("POST", url, body);
         var request = post(url)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(body));

        var result = mockMvc.perform(request).andReturn();
    logResponse(result);

        int status=result.getResponse().getStatus();

    //Handle success 2xx
    if(status >= 200 && status <300){
        if(status==201){
            assertStatusCreated(result);
        }
        else if(status==200){
            assertStatusOk(result);
        }
        else{
            System.out.println("Uncommon status code:"+ status);
        }
        return result;
    }

     // ⚠️ Handle Errors (4xx or 5xx)
    switch (status) {
        case 400 -> {
            System.out.println("⚠️ Detected Bad Request (400)");
            asserStandardErrorStructure(result);
            assertBadRequestError(result, "Validation failed");
        }
        case 401 -> {
            System.out.println("⚠️ Detected Unauthorized (401)");
            asserStandardErrorStructure(result);
            assertUnauthorizedError(result, "Authentication required");
        }
        case 404 -> {
            System.out.println("⚠️ Detected Not Found (404)");
            asserStandardErrorStructure(result);
            assertNotFoundError(result, "Resource not found");
        }
        case 500 -> {
            System.out.println("⚠️ Detected Internal Server Error (500)");
            asserStandardErrorStructure(result);
            assertInternalServerError(result, "Simulated database error");
        }
        default -> System.out.println("⚠️ Unhandled HTTP status: " + status);
    }
        return result;
    }


    /**
     * Generic GET helper returning MvcResult.
     */
    public MvcResult doGet(String url) throws Exception {
       logRequest("GET", url, null);
            var result= mockMvc.perform(get(url))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk()) // ✅ Optional safety
                    .andReturn();
        logResponse(result);    
            return result;
    }

    /**
     * Generic DELETE helper returning MvcResult.
     */
    public MvcResult doDelete(String url) throws Exception {
        return mockMvc.perform(delete(url))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isNoContent()) // ✅ Optional safety
                .andReturn();
    }

    /**
     * Generic PUT helper returning MvcResult.
     */
    public MvcResult doPut(String url, Object body) throws Exception {
        return mockMvc.perform(put(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andReturn();
    }

    // -------------------------------------------------------
    // 🔹 Response Parsing Utilities
    // -------------------------------------------------------

    /** Parse JSON response to Map */
    public Map<String, Object> asMap(MvcResult result) throws Exception {
        return objectMapper.readValue(
                result.getResponse().getContentAsString(),
                new TypeReference<>() {});
    }

    /** Parse JSON response to List */
    public List<Map<String, Object>> asList(MvcResult result) throws Exception {
        return objectMapper.readValue(
                result.getResponse().getContentAsString(),
                new TypeReference<>() {});
    }

    /** Extract a single field from JSON */
    public Object getField(MvcResult result, String field) throws Exception {
        Map<String, Object> map = asMap(result);
        return map.get(field);
    }

    // -------------------------------------------------------
    // 🔹 Validation Helpers
    // -------------------------------------------------------

    /** Assert field value equals expected */
    public void assertFieldEquals(MvcResult result, String field, Object expected) throws Exception {
        Object actual = getField(result, field);
        assertThat("Field '" + field + "' mismatch", actual, equalTo(expected));
    }

    /** Assert list size is at least a given number */
    public void assertListSizeAtLeast(MvcResult result, int min) throws Exception {
        List<?> list = asList(result);
        assertThat("List size check", list.size(), greaterThanOrEqualTo(min));
    }

    /** Assert list contains specific value by field */
    public void assertListContains(MvcResult result, String field, String value) throws Exception {
        List<Map<String, Object>> list = asList(result);
        boolean found = list.stream().anyMatch(item -> value.equals(item.get(field)));
        assertThat("Expected value '" + value + "' not found in field '" + field + "'",
                found, is(true));
    }

    public MvcResult doRequestExpectError(String method, String url, Object body, int expectedStatus) throws Exception {
    var request = switch (method.toUpperCase()) {
        case "POST" -> post(url);
        case "PUT" -> put(url);
        case "GET" -> get(url);
        case "DELETE" -> delete(url);
        default -> throw new IllegalArgumentException("Unsupported HTTP method: " + method);
    };

    return mockMvc.perform(request
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body != null ? objectMapper.writeValueAsString(body) : ""))
            .andExpect(status().is(expectedStatus))
            .andReturn();
}


// -------------------------------------------------------
// 🔹 Error Response Assertion Helper
// -------------------------------------------------------

/**
 * Validates a structured error response payload.
 * Expected structure example:
 * {
 *   "error": "Internal Server Error",
 *   "message": "Simulated database error",
 *   "status": 500
 * }
 *
 * @param result               MvcResult returned by the request
 * @param expectedError        expected value of "error" field
 * @param expectedStatus       expected numeric "status" field (e.g. 500)
 * @param expectedMessagePart  substring that must appear inside the "message" field
 */
public void assertErrorResponse(MvcResult result,
                                String expectedError,
                                int expectedStatus,
                                String expectedMessagePart) throws Exception {
    // check error label
    assertFieldEquals(result, "error", expectedError);

    // check numeric status in body (some APIs include status in JSON)
    Object statusField = getField(result, "status");
    if (statusField != null) {
        // numeric may come as Integer or String
        if (statusField instanceof Number) {
            assertThat("Status field mismatch", ((Number) statusField).intValue(), equalTo(expectedStatus));
        } else {
            assertThat("Status field mismatch", Integer.parseInt(statusField.toString()), equalTo(expectedStatus));
        }
    }

    // check message contains expected substring
    Object message = getField(result, "message");
    if (message == null) {
        throw new AssertionError("Expected 'message' field in error response but it was missing.");
    }
    String messageText = message.toString();
    if (!messageText.contains(expectedMessagePart)) {
        throw new AssertionError("Expected message to contain '" + expectedMessagePart + "' but was: " + messageText);
    }
}


/**
 * ✅ Assert that all expected records exist in a list of DB results by a given field.
 *
 * @param dbList       List of actual records (e.g., from DB or API response)
 * @param expectedList List of expected records (e.g., from JSON)
 * @param field        The field name to match (e.g., "name", "email")
 *
 * Example:
 * utils.assertListContainsAll(dbUsers, jsonUsers, "name");
 */
    public void assertListContainsAll(List<Map<String, Object>> dbList,
                                    List<Map<String, Object>> expectedList,
                                    String field) {
        for (Map<String, Object> expected : expectedList) {
            String expectedValue = expected.get(field).toString();
            boolean found = dbList.stream()
                    .anyMatch(item -> expectedValue.equals(item.get(field)));

            if (!found) {
                throw new AssertionError("❌ Expected value '" + expectedValue +
                        "' for field '" + field + "' not found in DB list.");
            }
        }
        System.out.println(" Verified all expected values exist in DB by field '" + field + "'.");
    }


// -------------------------------------------------------
// 🔹 Enhanced Error Handling & Validation Helpers
// -------------------------------------------------------

//Error handling validation methods to verify standardized error responses.

/**
 * ✅ Checks that all standard error fields exist and timestamp is valid.
 * Expected fields: error, message, status, timestamp
 */

    public void asserStandardErrorStructure(MvcResult result) throws Exception{
        Map<String, Object> body = asMap(result);

        String[] requiredFields={"error","message","status","timestamp"};
        for(String field: requiredFields){
            if(!body.containsKey(field)){
                throw new AssertionError("Missing required field in error structure"+ field);
            }
        }

    //validating that timestamp is numeric
    Object ts= body.get("timestamp");
    if(!(ts instanceof Number)){
        throw new AssertionError("'timestamp' field is not numeric: " + ts);
    }

     System.out.println("Standard error response structure validated successfully.");
    }

/**
 *  Shortcut helpers for common HTTP error types.
 * Uses your existing assertErrorResponse() internally.
 */

    public void assertBadRequestError(MvcResult result, String messagePart)throws Exception{
        asserStandardErrorStructure(result);
        assertErrorResponse(result, "Bad Request", 400, messagePart);
    }

    public void assertUnauthorizedError(MvcResult result, String messagePart) throws Exception{
        asserStandardErrorStructure(result);
        assertErrorResponse(result, "Unauthorized", 401, messagePart);
    }

    public void assertNotFoundError(MvcResult result, String messagePart) throws Exception{
        asserStandardErrorStructure(result);
        assertErrorResponse(result, "Not Found", 404, messagePart);
    }

    public void assertInternalServerError(MvcResult result, String messagePart) throws Exception{
        asserStandardErrorStructure(result);
        assertErrorResponse(result, "Internal Server Error", 500, messagePart);
    }

    public void assertStatusOk(MvcResult result) throws Exception {
    int status = result.getResponse().getStatus();
    assertThat("Expected 200 OK", status, equalTo(200));
}

    public void assertStatusCreated(MvcResult result) throws Exception {
        int status = result.getResponse().getStatus();
        assertThat("Expected 201 Created", status, equalTo(201));
    }


 /*
 * -------------------------------------------------------
 * 🧾 Unified Logging for Test Requests & Responses
 * -------------------------------------------------------
 * Automatically logs request/response details for every test action.
 * Controlled via system property: -Dtestbase.logging.enabled=true/false
 */
private final boolean loggingEnabled =
        Boolean.parseBoolean(System.getProperty("testbase.logging.enabled", "true"));

/** Central logging method */
private void log(String message) {
    if (loggingEnabled) {
        System.out.println(message);
    }
}

/** Logs outgoing request details */
private void logRequest(String method, String url, Object body) {
    log("\n[TESTBASE] ▶ " + method.toUpperCase() + " " + url);

    if (body != null) {
        try {
            String json = objectMapper
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsString(body);
            log("[TESTBASE] Request Body:\n" + json);
        } catch (Exception e) {
            log("[TESTBASE] ⚠ Could not serialize request body: " + e.getMessage());
        }
    }
}

/** Logs incoming response details */
private void logResponse(MvcResult result) {
    try {
        int status = result.getResponse().getStatus();
        String content = result.getResponse().getContentAsString();

        log("\n[TESTBASE] ◀ Response (" + status + ")");

        if (content != null && !content.isBlank()) {
            try {
                Object json = objectMapper.readValue(content, Object.class);
                String prettyJson = objectMapper
                        .writerWithDefaultPrettyPrinter()
                        .writeValueAsString(json);
                log(prettyJson);
            } catch (Exception parseEx) {
                // Handles plain text responses (not JSON)
                log("[TESTBASE] Raw Response Body:\n" + content);
            }
        } else {
            log("[TESTBASE] (empty body)");
        }

    } catch (Exception e) {
        log("[TESTBASE] ⚠ Could not log response: " + e.getMessage());
    }
}

public List<String> asStringList(MvcResult result) throws Exception {
    return objectMapper.readValue(
            result.getResponse().getContentAsString(),
            new TypeReference<List<String>>() {}
    );
}

public void assertListContainsString(MvcResult result, String expected) throws Exception {
    List<String> list = asStringList(result);

    if (!list.contains(expected)) {
        throw new AssertionError("Expected list to contain: " + expected + " but got: " + list);
    }
}

public void assertListSize(MvcResult result, int expected) throws Exception {
    List<?> list = asList(result);
    assertThat("Expected list size " + expected + " but got " + list.size(),
            list.size(), equalTo(expected));
}

//checks for list of objects having all expected key-value pairs
public void assertObjectListContainsAll(MvcResult result,
                                        List<Map<String, Object>> expectedList) throws Exception {
    List<Map<String, Object>> actualList = asList(result);

    for (Map<String, Object> expected : expectedList) {

        boolean matchFound = actualList.stream().anyMatch(actual ->
                expected.entrySet().stream().allMatch(entry ->
                        entry.getValue().equals(actual.get(entry.getKey()))
                )
        );

        if (!matchFound) {
            throw new AssertionError(
                "❌ Expected object " + expected +
                " not found in actual list: " + actualList
            );
        }
    }

    System.out.println("✔ All expected objects found in object list.");
}

}
