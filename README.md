## TestBase Library — Reusable Integration Testing Framework for Spring Boot

    TestBase is a reusable Spring Boot integration testing library designed to eliminate repetitive setup and boilerplate code.
    It simplifies:

    - MockMvc-based API testing

    - SQL/JSON data loading

    - Standardized error handling and assertions

    - Common test database configurations (H2 / PostgreSQL Testcontainers)

  #  Once imported, it provides ready-to-use classes like:

    - IntegrationTestBase

    - TestRequestUtils

    - TestDataLoader

    - TestDatabaseConfig


# for local testing using H2 database
-mvn clean test

# for testing using docker test containers
-mvn clean test "-Dtest.profile=test-container"


# This is the complete test base common class project implemented in a complete project
1. Single configuration source with annotations such as @Active Profiles('test'), and @AutoConfigure MockMvc. @SpringBootTest,

2. Preconfigured MockMvc setup to simplify API-level testing for controllers.

3. Centralized test database configuration using H2, Testcontainers, or Embedded PostgreSQL..



# HOW TO USE THIS JAR FILE AND WRITE TEST CASES

1. Copy your exported JAR file (testbase-1.0.jar, for example) into your project’s libs/ directory:
```bash
    your-app/
    ├── libs/
    │   └── testbase-1.0.jar
    ├── src/
    │   └── main/java/...
    │   └── test/java/...
    ├── pom.xml
```

2. In your pom.xml, add this dependency entry:
```bash   
   <dependency>
    <groupId>com.example</groupId>
    <artifactId>testbase</artifactId>
    <version>1.0</version>
    <scope>test</scope>
    <systemPath>${project.basedir}/libs/testbase-1.0.jar</systemPath>
    </dependency>
```
    💡 systemPath must be an absolute or relative path pointing to where your JAR is located (libs/testbase-1.0.jar).`match the name of jar accordingly`

3. # Add Required Test Resources

    The base class expects SQL/JSON files in:

   #  src/test/resources/data/
```bash
        data/
    ├── init.sql
    └── users.json

   # Sample init.sql

     CREATE TABLE IF NOT EXISTS users (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(255),
    email VARCHAR(255)
    );

  # Sample users.json

    [
    { "name": "Alice", "email": "alice@example.com" },
    { "name": "Bob", "email": "bob@example.com" }
    ]
```

4. Create Your First Integration Test

    Now, in your project’s src/test/java, create a new test class extending IntegrationTestBase.

  #  Example: UserControllerIntegrationTest.java

```java
    package com.example.myapp;

    import com.example.testbase.test.IntegrationTestBase;
    import com.example.testbase.test.TestRequestUtils;
    import com.example.myapp.model.User;
    import org.junit.jupiter.api.BeforeEach;
    import org.junit.jupiter.api.Test;

    public class UserControllerIntegrationTest extends IntegrationTestBase {

    private TestRequestUtils utils;
```

#   keep ths default setup where you can even change the path of init.sql file 

```java
    @BeforeEach
    void setup() {
        utils = new TestRequestUtils(mockMvc, objectMapper);
        try {
            testDataLoader.clearTables("users");
            testDataLoader.loadSql("classpath:data/init.sql");
            System.out.println("🧹 Database reset successful before test.");
        } catch (Exception e) {
            System.out.println("⚠️ Database reset skipped: " + e.getMessage());
        }
    }
```
#   write test as needed with @Test annotation

#   newer way which is applicable for this jar file is below

```java
        @Test
        void createUser_shouldReturnCreatedAndList() throws Exception {
            // Create a new user
            var user = new User("Alice", "alice@example.com");

            var postResult = utils.doPost("/api/users", user);
            utils.assertFieldEquals(postResult, "name", "Alice");

            // Verify via GET
            var getResult = utils.doGet("/api/users");
            utils.assertListContains(getResult, "name", "Alice");
        }
    }
```


# If test cases were written without this jar file
```java
            @Test
        void createUser_oldWay() throws Exception {
            User u = new User("Alice", "alice@example.com");

            mockMvc.perform(post("/api/users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(u)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Alice"));

            mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", hasItem("Alice")));
        }
```
**“All test POST requests should use doPost() — it automatically detects success or error responses and performs structure validation for common codes (400, 401, 404, 500).”**

# While using his jar file the utilities can be simplified like below

```java
  //deprecated
        //         @Test
        // void createUser_newWay() throws Exception {
        //     var u = new User("Alice", "alice@example.com");

        //     var result = utils.doPostExpectCreated("/api/users", u);
        //     utils.assertFieldEquals(result, "name", "Alice");

        //     var getResult = utils.doGet("/api/users");
        //     utils.assertListContains(getResult, "name", "Alice");
        // }

        @Test
        void createAndGetAllUsers_withUtils_shouldReturnCreatedAndList() throws Exception {
        // Arrange
        User u = new User("Alice", "alice@example.com");

        // Act 1️⃣: Create user — doPost() auto-checks status (201 Created)
        var result = utils.doPost("/api/users", u);

        // Assert 1️⃣: Verify returned user data
        utils.assertFieldEquals(result, "name", "Alice");

        // Act 2️⃣: Fetch list of all users
        var getResult = utils.doGet("/api/users");

        // Assert 2️⃣: Ensure Alice exists in DB
        utils.assertListContains(getResult, "name", "Alice");

        utils.assertStatusCreated(result); // Optional — for human readability

    }

```

# Database Setup Options

This library supports two profiles for tests:

  # Profile	Description

```java
test-local (default)	//Uses in-memory H2 database
test-container	       // Uses PostgreSQL Testcontainers dynamically
```

    
# Switch by adding in your test’s application-test.properties:
```java
  test.profile=test-container
```




# Directory Summary for Integration Tests
```bash

    src/test/
    ├── java/
    │   └── com/example/myapp/
    │       ├── UserControllerIntegrationTest.java
    │       └── ...
    └── resources/
        └── data/
            ├── init.sql
            └── users.json
```
    more files can be loaded in data folder and can be called accordingly in Tester.java files using the setup function



## Utility Reference — What’s Inside TestRequestUtils

    Here’s a complete list of utilities and their usage patterns 👇

🔹 1. Request Helpers

    ### 🔹 Test Utility Methods

| Method                                            | Description                                                                     | Example                                                           |
| ------------------------------------------------- | ------------------------------------------------------------------------------- | ----------------------------------------------------------------- |
| `doPost(url, body)`                               | Sends a **POST** request and automatically validates status (200/201 or errors) | `utils.doPost("/api/users", user);`                               |
| `doGet(url)`                                      | Sends a **GET** request and expects **200 OK**                                  | `utils.doGet("/api/users");`                                      |
| `doPut(url, body)`                                | Sends a **PUT** request and expects **200 OK**                                  | `utils.doPut("/api/users/1", updatedUser);`                       |
| `doDelete(url)`                                   | Sends a **DELETE** request and expects **204 No Content**                       | `utils.doDelete("/api/users/1");`                                 |
| `doRequestExpectError(method, url, body, status)` | Generic method for **any HTTP verb** with custom error code                     | `utils.doRequestExpectError("GET", "/api/protected", null, 401);` |



🔹 2. Response Parsing Utilities

| Method                          | Purpose                              | Example                                                  |
| ------------------------------- | ------------------------------------ | -------------------------------------------------------- |
| `asMap(result)`                 | Convert JSON to `Map<String,Object>` | `Map<String, Object> map = utils.asMap(result);`         |
| `asList(result)`                | Convert JSON array to `List<Map>`    | `List<Map<String, Object>> list = utils.asList(result);` |
| `getField(result, "fieldName")` | Get single field from JSON           | `utils.getField(result, "name");`                        |


🔹 3. Validation Helpers

| Method                                                | Purpose                                                                         | Example                                                             |
| ----------------------------------------------------- | ------------------------------------------------------------------------------- | ------------------------------------------------------------------- |
| `assertFieldEquals(result, field, expected)`          | Assert a JSON field equals expected                                             | `utils.assertFieldEquals(result, "name", "Alice");`                 |
| `assertListSizeAtLeast(result, min)`                  | Assert list size is ≥ minimum value                                             | `utils.assertListSizeAtLeast(result, 2);`                           |
| `assertListSize(result, expected)`                    | Assert list size equals expected value                                          | `utils.assertListSize(result, 3);`                                  |
| `assertListContains(result, field, value)`            | Verify that a JSON array contains an object with field=value                    | `utils.assertListContains(result, "email", "alice@x.com");`         |
| `assertListContainsAll(dbList, jsonList, field)`      | Validate two object lists share all items for a given field                     | `utils.assertListContainsAll(dbUsers, apiUsers, "id");`             |
| `assertObjectListContainsAll(result, expectedList)`   | Assert a list of objects contains **all key–value pairs** from expected objects | `utils.assertObjectListContainsAll(result, expectedUsers);`         |
| `assertErrorResponse(result, error, status, msgPart)` | Validate standardized error response fields                                     | `utils.assertErrorResponse(result, "Bad Request", 400, "Invalid");` |
| `assertStandardErrorStructure(result)`                | Assert error JSON contains: `error`, `message`, `status`, `timestamp`           | `utils.assertStandardErrorStructure(result);`                       |
| `assertBadRequestError(result, messagePart)`          | Standardized **400 Bad Request** validator                                      | `utils.assertBadRequestError(result, "missing");`                   |
| `assertUnauthorizedError(result, messagePart)`        | Standardized **401 Unauthorized** validator                                     | `utils.assertUnauthorizedError(result, "Authentication");`          |
| `assertNotFoundError(result, messagePart)`            | Standardized **404 Not Found** validator                                        | `utils.assertNotFoundError(result, "not found");`                   |
| `assertInternalServerError(result, messagePart)`      | Standardized **500 Internal Server Error** validator                            | `utils.assertInternalServerError(result, "database");`              |
| `assertStatusOk(result)`                              | Assert response status is **200 OK**                                            | `utils.assertStatusOk(result);`                                     |
| `assertStatusCreated(result)`                         | Assert response status is **201 Created**                                       | `utils.assertStatusCreated(result);`                                |
| `asStringList(result)`                                | Convert JSON array → `List<String>`                                             | `List<String> names = utils.asStringList(result);`                  |
| `assertListContainsString(result, expected)`          | Assert string list contains expected value                                      | `utils.assertListContainsString(result, "Admin");`                  |
             |


🔹 4. Logging and Debugging

    
| Method / Feature                | Purpose                                             | Example / Note                                              |
| ------------------------------- | --------------------------------------------------- | ----------------------------------------------------------- |
| `logRequest(method, url, body)` | Logs outgoing requests with formatted JSON body     | Auto-called by all helper methods                           |
| `logResponse(result)`           | Logs status code and formatted JSON response        | Auto-called by all helper methods                           |
| `testbase.logging.enabled`      | System property to toggle logging (default: `true`) | Run with `-Dtestbase.logging.enabled=false` to disable logs |





##  Example: Error Handling

    Below is a sample test that demonstrates how to verify error responses using the utility methods.

```java
//deprecated
    // @Test
    // void createUser_withInvalidInput_shouldReturn400() throws Exception {
    //     var invalidUser = new User("", ""); //  Invalid input accrding to your controller

    //     var result = utils.doPostExpectBadRequest("/api/users", invalidUser);

    //     utils.assertErrorResponse(result,
    //             "Bad Request",            // expected "error" field
    //             400,                      // expected "status" field
    //             "Validation failed");     // substring expected in "message"
    // }

     @Test
    void createSimulatedError_shouldReturn500WithStructuredBody() throws Exception {
    // Arrange
    var u = new User("error", "who@err"); // triggers simulated DB error in controller

    // Act — doPost() automatically detects and validates 500 Internal Server Error
    var result = utils.doPost("/api/users", u);

    // Assert — ensure error response is structured and message is correct
    utils.assertInternalServerError(result, "Simulated database error");     //just enter the custom message to expect along with result from post request

```
##  Example Error Response (500 Internal Server Error)

    ```json
    {
    "error": "Internal Server Error",
    "message": "Simulated database error",
    "status": 500,
    "timestamp": 1731290000000
    }

```

##  Blueprint: Actual vs Expected Values

    When writing tests with these utilities, keep this blueprint in mind:

| Type | Meaning | Example |
|------|----------|----------|
| **Actual Input** | Data you send to the API or database | `new User("Alice", "alice@example.com")` |
| **Expected Output** | What you assert on in the response | `"Alice"`, `"Internal Server Error"`, etc. |
| **Field Path** | JSON key or property to check | `"name"`, `"error"`, `"status"` |

    ### 💡 Rule of Thumb

    -  **Actual values** → passed to methods like `doPost(...)`, `doPut(...)`, etc.  
    - ✅ **Expected values** → used with assertion helpers such as `assertFieldEquals(...)` or `assertListContains(...)`.
    

**Example:*
```java
    MvcResult result = utils.doPost("/api/users", new User("Alice", "alice@example.com"));
    utils.assertFieldEquals(result, "name", "Alice");
```


## 🧰 Summary of Available Utils

| **Category**         | **Methods**                                                                                                                                                                                                                                                                                    |
| -------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Request Helpers**  | `doPost`, `doGet`, `doPut`, `doDelete`, `doRequestExpectError`                                                                                                                                                                                                                                     |
| **Response Parsers** | `asMap`, `asList`, `getField`                                                                                                                                                                                                                                                                      |
| **Assertions**       | `assertFieldEquals`, `assertListContains`, `assertListSizeAtLeast`, `assertListContainsAll`, `assertErrorResponse`, `asserStandardErrorStructure`, `assertBadRequestError`, `assertUnauthorizedError`, `assertNotFoundError`, `assertInternalServerError`, `assertStatusOk`, `assertStatusCreated` |
| **Logging Helpers**  | `logRequest`, `logResponse` (auto-enabled), `testbase.logging.enabled` property                                                                                                                                                                                                                    |

---

### 💡 Usage Example

```java
    // 1️⃣ Load data from SQL and JSON files
    testDataLoader.loadSql("classpath:data/init.sql");
    testDataLoader.loadJson("classpath:data/users.json");

    // 2️⃣ Send POST request and verify response
    var result = utils.doPost("/api/users", newUser);
    utils.assertFieldEquals(result, "name", "Alice");


```


## 🧩 Summary — Developer Blueprint
    
    When writing a new **integration test** using this library, follow this blueprint:

| **Step** | **Action**                                 | **Example**                                                |
| -------- | ------------------------------------------ | ---------------------------------------------------------- |
| 1️⃣      | Extend `IntegrationTestBase`               | `public class MyApiTest extends IntegrationTestBase {}`    |
| 2️⃣      | Initialize utils in `@BeforeEach`          | `utils = new TestRequestUtils(mockMvc, objectMapper);`     |
| 3️⃣      | Make requests using helpers                | `utils.doPost("/api/items", new Item("Pen"));`             |
| 4️⃣      | Validate results using assertions          | `utils.assertFieldEquals(result, "name", "Pen");`          |
| 5️⃣      | (Optional) Disable logging when not needed | `System.setProperty("testbase.logging.enabled", "false");` |
|           | Use `testDataLoader` to load test data | `testDataLoader.loadSql("classpath:data/init.sql");` |

    ---

### 🧠 Quick Notes

    -  **IntegrationTestBase** provides setup for `MockMvc`, `ObjectMapper`, and test utilities.
    -  **TestRequestUtils** offers helper methods like `doPost`, `doGet`, `doPut`, and error assertions.
    -  **TestDataLoader** handles loading of SQL or JSON data before tests.

---

###  Example Test

```java
    @Test
    void createItem_shouldReturn201() throws Exception {
        var newItem = Map.of("name", "Pen", "price", 10);

        var result = utils.doPost("/api/items", newItem);

        utils.assertFieldEquals(result, "name", "Pen");
    }
```
## 🧪 Example Usage — Error Assertions

This library provides two styles of error validation — both are valid and can be used **interchangeably** depending on developer preference.

---

### ✅ Old Style for this jar (Still Works)

The classic approach using the generic `assertErrorResponse()` method `where you can write custom status codes too`:

```java
    utils.assertErrorResponse(result,
            "Internal Server Error",
            500,
            "Simulated database error");
```
### Shorthand for standard errors

```java
    utils.assertInternalServerError(result, "Simulated database error");
    utils.assertBadRequestError(result, "Validation failed");
    utils.assertUnauthorizedError(result, "Authentication required");
    utils.assertNotFoundError(result, "Resource not found");
```

`Each of these automatically verifies that the response follows the standard JSON error structure:`

```json
    {
    "error": "Internal Server Error",
    "message": "Simulated database error",
    "status": 500,
    "timestamp": 1731305000000
    }
```




**JaCoCo Plugin for Logging**

`Add the below plugin in the pom.xml of your project in your <build> <plugins>`

```xml
<plugin>
  <groupId>org.jacoco</groupId>
  <artifactId>jacoco-maven-plugin</artifactId>
  <version>0.8.12</version>

  <executions>
    <!-- Step 1️⃣: Attach the JaCoCo agent before tests -->
    <execution>
      <id>prepare-agent</id>
      <goals>
        <goal>prepare-agent</goal>
      </goals>
      <configuration>
        <!-- Optional: save .exec file for analysis tools -->
        <destFile>${project.build.directory}/jacoco.exec</destFile>
        <append>true</append>
      </configuration>
    </execution>

    <!-- Step 2️⃣: Generate the JaCoCo report after tests -->
    <execution>
      <id>report</id>
      <phase>test</phase>
      <goals>
        <goal>report</goal>
      </goals>
      <configuration>
        <dataFile>${project.build.directory}/jacoco.exec</dataFile>
        <outputDirectory>${project.reporting.outputDirectory}/jacoco</outputDirectory>
        <reports>
          <report>
            <outputEncoding>UTF-8</outputEncoding>
            <name>JaCoCo Coverage Report</name>
          </report>
        </reports>
      </configuration>
    </execution>

    <!-- Step 3️⃣ (Optional): Fail build if coverage below 80% -->
    <execution>
      <id>check</id>
      <goals>
        <goal>check</goal>
      </goals>
      <configuration>
        <rules>
          <rule>
            <element>BUNDLE</element>
            <limits>
              <limit>
                <counter>LINE</counter>
                <value>COVEREDRATIO</value>
                <minimum>0.80</minimum>
              </limit>
              <limit>
                <counter>BRANCH</counter>
                <value>COVEREDRATIO</value>
                <minimum>0.70</minimum>
              </limit>
            </limits>
          </rule>
        </rules>
      </configuration>
    </execution>
  </executions>
</plugin>

```


# 📊 JaCoCo Code Coverage Integration

## 🌟 What is JaCoCo?

**JaCoCo (Java Code Coverage)** is a free and open-source toolkit that measures how much of your Java code is executed by your test cases.

It helps developers:
- Identify **untested code** 🧩  
- Maintain **high code quality** ✅  
- Track **test effectiveness** over time 📈  
- Generate **shareable reports** for teams 💼  

This project integrates JaCoCo with Maven and Spring Boot to produce coverage reports automatically after each test run.

---

## ⚙️ 1. Add JaCoCo Plugin to `pom.xml`

Insert this inside your `<build><plugins>` section:

```xml
<plugin>
  <groupId>org.jacoco</groupId>
  <artifactId>jacoco-maven-plugin</artifactId>
  <version>0.8.12</version>

  <executions>
    <!-- 1️⃣ Attach JaCoCo Agent Before Tests -->
    <execution>
      <id>prepare-agent</id>
      <goals>
        <goal>prepare-agent</goal>
      </goals>
      <configuration>
        <destFile>${project.build.directory}/jacoco.exec</destFile>
        <append>true</append>
      </configuration>
    </execution>

    <!-- 2️⃣ Generate HTML Report After Tests -->
    <execution>
      <id>report</id>
      <phase>test</phase>
      <goals>
        <goal>report</goal>
      </goals>
      <configuration>
        <dataFile>${project.build.directory}/jacoco.exec</dataFile>
        <outputDirectory>${project.reporting.outputDirectory}/jacoco</outputDirectory>
      </configuration>
    </execution>

    <!-- 3️⃣ Enforce Coverage Thresholds (Optional) -->
    <execution>
      <id>check</id>
      <goals>
        <goal>check</goal>
      </goals>
      <configuration>
        <rules>
          <rule>
            <element>BUNDLE</element>
            <limits>
              <limit>
                <counter>LINE</counter>
                <value>COVEREDRATIO</value>
                <minimum>0.80</minimum>
              </limit>
              <limit>
                <counter>BRANCH</counter>
                <value>COVEREDRATIO</value>
                <minimum>0.70</minimum>
              </limit>
            </limits>
          </rule>
        </rules>
      </configuration>
    </execution>
  </executions>
</plugin>
```

2. Copy Coverage Reports to Static Folder (for Web Access)
    Add this plugin below the JaCoCo plugin:

```xml
    <plugin>
  <artifactId>maven-antrun-plugin</artifactId>
  <version>3.1.0</version>
  <executions>
    <execution>
      <id>copy-coverage-report</id>
      <phase>test</phase>
      <configuration>
        <target>
          <mkdir dir="${project.basedir}/src/main/resources/static/coverage" />
          <copy todir="${project.basedir}/src/main/resources/static/coverage" overwrite="true">
            <fileset dir="${project.reporting.outputDirectory}/jacoco" />
          </copy>
        </target>
      </configuration>
      <goals>
        <goal>run</goal>
      </goals>
    </execution>
  </executions>
</plugin>

```
    This automatically copies JaCoCo’s HTML output from target/site/jacoco/ → src/main/resources/static/coverage/
    so you can view it directly in your browser after starting your app.

3. Run Tests and Generate Reports

    Run the following command to generate coverage data:

```bash
    mvn clean verify
```
    This will:

    -Run all unit and integration tests 🧠

    -Generate JaCoCo reports under target/site/jacoco/

    -Copy reports to src/main/resources/static/coverage/

    You should see something like:
```bash
    [INFO] Copying 42 files to src\main\resources\static\coverage
    [INFO] BUILD SUCCESS
```

4. View the Coverage Report
    Start your Spring Boot application:
```bash
    mvn spring-boot:run
```
    Then open your browser:
```bash
    http://localhost:8080/coverage/index.html
```
    You’ll see a fully interactive HTML dashboard with:
    🟩 Green lines → covered by tests
    🟥 Red lines → missed by tests
    📂 Clickable packages and classes
    📈 Summary percentages for lines, branches, and methods

5. Share or Archive Reports
    You can share or store the report folder directly:
```bash
    src/main/resources/static/coverage/
```

    Or create a zipped copy:
```bash
    zip -r coverage-report.zip src/main/resources/static/coverage
```
    Anyone can open index.html locally to view the full report.

6. Common Paths

| Type                  | Location                                        |
| --------------------- | ----------------------------------------------- |
| Raw coverage data     | `target/jacoco.exec`                            |
| Generated HTML report | `target/site/jacoco/index.html`                 |
| Copied static report  | `src/main/resources/static/coverage/index.html` |
| Local web view        | `http://localhost:8080/coverage/index.html`     |

7. Troubleshooting

| Problem                                   | Solution                                                         |
| ----------------------------------------- | ---------------------------------------------------------------- |
| `No static resource coverage/index.html`  | Run `mvn clean verify` again to regenerate and copy              |
| Folder missing under `target/site/jacoco` | Ensure `report` execution phase is set to `test`                 |
| Outdated coverage report                  | Delete `/static/coverage` and rerun tests                        |
| Not served at `/coverage` URL             | Ensure the folder is under `src/main/resources/static/coverage/` |

8. Summary

| Step | Description                             | Result                                 |
| ---- | --------------------------------------- | -------------------------------------- |
| 1️⃣  | Add JaCoCo plugin                       | Collects coverage data during tests    |
| 2️⃣  | Add Antrun plugin                       | Copies generated report for web access |
| 3️⃣  | Run `mvn clean verify`                  | Executes tests + builds report         |
| 4️⃣  | Start app & open `/coverage/index.html` | View live interactive report           |

Once complete, open after running the application:

👉 src/main/resources/static/coverage/index.html
or
👉 http://localhost:8080/coverage/index.html

That’s It!

Once testbase.jar is added, you can focus only on writing meaningful tests, not repetitive setup.
Every test gets automatic data initialization, validation helpers, and error handling—ready to go 🚀.