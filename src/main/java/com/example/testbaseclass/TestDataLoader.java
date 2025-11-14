// package com.example.testbaseclass;

// import org.springframework.core.io.Resource;
// import org.springframework.core.io.ResourceLoader;

// import java.io.InputStreamReader;
// import java.io.Reader;
// import java.nio.charset.StandardCharsets;
// import java.util.List;
// import java.util.Map;

// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.jdbc.core.JdbcTemplate;
// import org.springframework.stereotype.Component;
// import org.springframework.util.FileCopyUtils;

// import com.fasterxml.jackson.core.type.TypeReference;
// import com.fasterxml.jackson.databind.ObjectMapper;

// /**
//  * Generic reusable data loader that supports loading JSON and SQL test data.
//  * Works automatically in any Spring Boot test context that includes this library.
//  */

// @Component
// public class TestDataLoader {
//     @Autowired
//     private ResourceLoader resourceLoader;

//     @Autowired(required = false)
//     private JdbcTemplate jdbcTemplate;

//     @Autowired
//     private ObjectMapper objectMapper;

//      /**
//      * cleans the data from json file which calls it
     
//      */

//     public void clearTables(String... tableNames) {
//     if (jdbcTemplate == null) return;
//     for (String table : tableNames) {
//         try {
//             jdbcTemplate.execute("DELETE FROM " + table);
//             System.out.println("🧹 Cleared table: " + table);
//         } catch (Exception e) {
//             System.out.println("⚠️ Could not clear table " + table + ": " + e.getMessage());
//         }
//     }
// }

//      /**
//      * Loads and executes an SQL file from the test resources folder.
//      * Example path: "classpath:data/init.sql"
//      */

//      public void loadSql(String path){
//         try{
//             Resource resource = resourceLoader.getResource(path);
//             if(!resource.exists()){
//                 System.out.println("SQL path not found"+path);
//                 return;
//             }

//             String sql= asString(resource);
//             for(String stmt :sql.split(";")){
//                 String trimmed=stmt.trim();
//                 if(!trimmed.isEmpty()){
//                     jdbcTemplate.execute(trimmed);
//                 }
//             }
//             System.out.println("Executed SQL data filr from: "+ path);
//         }catch(Exception e){
//             throw new RuntimeException("Failed to load SQL data from " + path, e);
//         }
//      }

//       /**
//      * Loads a JSON file into a List of Map<String, Object>.
//      * Can be used for flexible validation or programmatic insertion.
//      * Example path: "classpath:data/users.json"
//      */

//      public List <Map<String,Object>> loadJson(String path){
//         try {
//             Resource resource=resourceLoader.getResource(path);
//             if(!resource.exists()){
//                 System.out.println("Json resource not found"+path);
//                 return List.of();
//             }

//             String json= asString(resource);
//             List<Map<String, Object>> list = objectMapper.readValue(json, new TypeReference<>() {});
//             System.out.println("Loaded json data from: "+ path + "("+ list.size()+ "records");
//             return list;


//         } catch (Exception e) {
//             throw new RuntimeException("Failed to load JSON data from " + path, e);
//         }
//      }
//     private String asString(Resource resource) throws Exception{
//         try  (Reader reader= new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)){
//                 return FileCopyUtils.copyToString(reader);
//             }
        

//     }

// }
package com.example.testbaseclass;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.FileCopyUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Generic reusable data loader that supports loading JSON and SQL test data.
 * Works automatically in any Spring Boot test context that includes this library.
 */

@Component
public class TestDataLoader {

    @Autowired
    private ResourceLoader resourceLoader;

    @Autowired(required = false)
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Clear specific tables.
     */
    public void clearTables(String... tableNames) {
        if (jdbcTemplate == null) return;

        for (String table : tableNames) {
            try {
                jdbcTemplate.execute("DELETE FROM " + table);
                System.out.println("🧹 Cleared table: " + table);
            } catch (Exception e) {
                System.out.println("⚠️ Could not clear table " + table + ": " + e.getMessage());
            }
        }
    }

    /**
     * Load and execute SQL file.
     */
    public void loadSql(String path) {
        try {
            Resource resource = resourceLoader.getResource(path);
            if (!resource.exists()) {
                System.out.println("SQL path not found " + path);
                return;
            }

            String sql = asString(resource);
            for (String stmt : sql.split(";")) {
                String trimmed = stmt.trim();
                if (!trimmed.isEmpty()) {
                    jdbcTemplate.execute(trimmed);
                }
            }
            System.out.println("Executed SQL data file from: " + path);

        } catch (Exception e) {
            throw new RuntimeException("Failed to load SQL data from " + path, e);
        }
    }

    /**
     * Load JSON file AND insert into database table "orders".
     */
    public List<Map<String, Object>> loadJson(String path) {
        try {
            Resource resource = resourceLoader.getResource(path);
            if (!resource.exists()) {
                System.out.println("Json resource not found " + path);
                return List.of();
            }

            String json = asString(resource);

            List<Map<String, Object>> list = objectMapper.readValue(
                    json,
                    new TypeReference<>() {}
            );

            System.out.println("Loaded json data from: " + path + " (" + list.size() + " records)");

            // 🔥 Insert into DB ONLY if JdbcTemplate is available
            if (jdbcTemplate != null) {
                for (Map<String, Object> row : list) {

                    // Only insert rows having "description" field for orders
                    if (row.containsKey("description")) {

                        jdbcTemplate.update(
                            "INSERT INTO orders (description) VALUES (?)",
                            row.get("description")
                        );

                        System.out.println("Inserted JSON row into 'orders': " + row.get("description"));
                    }
                }
            }

            return list;

        } catch (Exception e) {
            throw new RuntimeException("Failed to load JSON data from " + path, e);
        }
    }

    private String asString(Resource resource) throws Exception {
        try (Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
            return FileCopyUtils.copyToString(reader);
        }
    }
}
