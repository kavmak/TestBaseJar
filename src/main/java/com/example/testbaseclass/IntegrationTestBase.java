package com.example.testbaseclass;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.context.annotation.Import;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test") // central single profile to load application-test.properties
@Import(TestDatabaseConfig.class) // import the centralized test DB config
public abstract class IntegrationTestBase {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected TestDataLoader testDataLoader;

    @BeforeEach
    public void beforeEachBase() {
    if (testDataLoader != null) {
        testDataLoader.loadSql("classpath:data/init.sql");
        testDataLoader.loadJson("classpath:data/data.json");
    }
}



    // Utility helper - convert object to JSON
    protected String toJson(Object obj) throws Exception {
        return objectMapper.writeValueAsString(obj);
    }

    // Utility helper - parse JSON (tests can use objectMapper directly)
    protected <T> T fromJson(String json, Class<T> clazz) throws Exception {
        return objectMapper.readValue(json, clazz);
    }
}
