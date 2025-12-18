package com.datasabai.services.schemaanalyzer.core.generator;

import com.datasabai.services.schemaanalyzer.core.model.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Generates JSON Schema (draft-07) from a StructureElement tree.
 * <p>
 * This class converts the generic {@link StructureElement} representation
 * into a valid JSON Schema that can be used for validation and code generation.
 * </p>
 *
 * <h3>Supported JSON Schema Features:</h3>
 * <ul>
 *   <li>Object types with properties</li>
 *   <li>Array types with items</li>
 *   <li>Primitive types (string, integer, number, boolean)</li>
 *   <li>Required fields</li>
 *   <li>Nested objects and arrays</li>
 *   <li>Descriptions and metadata</li>
 * </ul>
 *
 * <h3>Example Output:</h3>
 * <pre>{@code
 * {
 *   "$schema": "http://json-schema.org/draft-07/schema#",
 *   "type": "object",
 *   "title": "Customer",
 *   "properties": {
 *     "id": {
 *       "type": "integer"
 *     },
 *     "name": {
 *       "type": "string"
 *     },
 *     "orders": {
 *       "type": "array",
 *       "items": {
 *         "type": "object",
 *         "properties": {
 *           "orderId": {
 *             "type": "integer"
 *           }
 *         }
 *       }
 *     }
 *   },
 *   "required": ["id", "name"]
 * }
 * }</pre>
 */
public class JsonSchemaGenerator {
    private static final Logger log = LoggerFactory.getLogger(JsonSchemaGenerator.class);
    private static final String SCHEMA_VERSION = "http://json-schema.org/draft-07/schema#";

    private final ObjectMapper objectMapper;

    public JsonSchemaGenerator() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    /**
     * Generates a JSON Schema from a structure element.
     *
     * @param root root structure element
     * @param request original analysis request
     * @return JSON Schema as a map
     * @throws AnalyzerException if generation fails
     */
    public Map<String, Object> generateSchema(StructureElement root, FileAnalysisRequest request)
            throws AnalyzerException {
        if (root == null) {
            throw new AnalyzerException("GENERATION_ERROR", "Root element cannot be null");
        }

        log.debug("Generating JSON Schema for: {}", request.getSchemaName());

        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("$schema", SCHEMA_VERSION);
        schema.put("title", request.getSchemaName());

        // Add metadata
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("sourceType", request.getFileType().name());
        metadata.put("generatedBy", "File Schema Analyzer");
        schema.put("x-metadata", metadata);

        // Convert structure to schema
        Map<String, Object> schemaDefinition = convertStructureToSchema(root, request);
        schema.putAll(schemaDefinition);

        log.debug("JSON Schema generated successfully");
        return schema;
    }

    /**
     * Generates a JSON Schema and returns it as a formatted string.
     *
     * @param root root structure element
     * @param request original analysis request
     * @return JSON Schema as formatted JSON string
     * @throws AnalyzerException if generation fails
     */
    public String generateSchemaAsString(StructureElement root, FileAnalysisRequest request)
            throws AnalyzerException {
        Map<String, Object> schema = generateSchema(root, request);
        try {
            return objectMapper.writeValueAsString(schema);
        } catch (Exception e) {
            throw new AnalyzerException(
                    "SERIALIZATION_ERROR",
                    "Failed to serialize JSON Schema: " + e.getMessage(),
                    e
            );
        }
    }

    /**
     * Converts a StructureElement to a JSON Schema definition.
     */
    private Map<String, Object> convertStructureToSchema(StructureElement element, FileAnalysisRequest request) {
        Map<String, Object> schema = new LinkedHashMap<>();

        if (element.isArray()) {
            // Array type
            schema.put("type", "array");

            if (element.hasChildren()) {
                StructureElement itemElement = element.getChildren().get(0);
                Map<String, Object> itemSchema = convertStructureToSchema(itemElement, request);
                schema.put("items", itemSchema);
            }

        } else if ("object".equals(element.getType())) {
            // Object type
            schema.put("type", "object");

            if (element.hasChildren()) {
                Map<String, Object> properties = new LinkedHashMap<>();
                List<String> required = new ArrayList<>();

                for (StructureElement child : element.getChildren()) {
                    Map<String, Object> childSchema = convertStructureToSchema(child, request);
                    properties.put(child.getName(), childSchema);

                    if (child.isRequired()) {
                        required.add(child.getName());
                    }
                }

                schema.put("properties", properties);

                if (!required.isEmpty()) {
                    schema.put("required", required);
                }
            }

            // Add attributes as properties (for XML)
            if (element.hasAttributes()) {
                if (!schema.containsKey("properties")) {
                    schema.put("properties", new LinkedHashMap<>());
                }

                @SuppressWarnings("unchecked")
                Map<String, Object> properties = (Map<String, Object>) schema.get("properties");
                List<String> required = schema.containsKey("required") ?
                        new ArrayList<>((List<String>) schema.get("required")) : new ArrayList<>();

                for (ElementAttribute attr : element.getAttributes()) {
                    Map<String, Object> attrSchema = new LinkedHashMap<>();
                    attrSchema.put("type", mapType(attr.getType()));

                    if (attr.getDefaultValue() != null) {
                        attrSchema.put("default", attr.getDefaultValue());
                    }

                    properties.put(attr.getName(), attrSchema);

                    if (attr.isRequired()) {
                        required.add(attr.getName());
                    }
                }

                if (!required.isEmpty()) {
                    schema.put("required", required);
                }
            }

        } else {
            // Primitive type
            schema.put("type", mapType(element.getType()));
        }

        // Add description if available
        if (element.getDescription() != null && !element.getDescription().isBlank()) {
            schema.put("description", element.getDescription());
        }

        // Add namespace as metadata (for XML)
        if (element.getNamespace() != null && !element.getNamespace().isBlank()) {
            schema.put("x-namespace", element.getNamespace());
        }

        // Add occurrence constraints
        if (element.getMinOccurs() > 0 || element.getMaxOccurs() != 1) {
            Map<String, Object> occurrenceHints = new LinkedHashMap<>();
            occurrenceHints.put("minOccurs", element.getMinOccurs());
            occurrenceHints.put("maxOccurs", element.getMaxOccurs());
            schema.put("x-occurrence", occurrenceHints);
        }

        return schema;
    }

    /**
     * Maps internal type names to JSON Schema types.
     */
    private String mapType(String internalType) {
        if (internalType == null) {
            return "string";
        }

        return switch (internalType.toLowerCase()) {
            case "integer", "int", "long" -> "integer";
            case "number", "decimal", "double", "float" -> "number";
            case "boolean", "bool" -> "boolean";
            case "array" -> "array";
            case "object" -> "object";
            case "null" -> "null";
            default -> "string";
        };
    }

    /**
     * Validates that a schema is well-formed.
     *
     * @param schema schema to validate
     * @return true if valid
     */
    public boolean validateSchema(Map<String, Object> schema) {
        if (schema == null || schema.isEmpty()) {
            return false;
        }

        // Basic validation
        if (!schema.containsKey("$schema")) {
            log.warn("Schema missing $schema field");
            return false;
        }

        if (!schema.containsKey("type") && !schema.containsKey("properties")) {
            log.warn("Schema missing type or properties");
            return false;
        }

        return true;
    }

    /**
     * Counts total properties in a schema (including nested).
     */
    public int countProperties(Map<String, Object> schema) {
        int count = 0;

        if (schema.containsKey("properties")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> properties = (Map<String, Object>) schema.get("properties");
            count += properties.size();

            // Count nested properties
            for (Object value : properties.values()) {
                if (value instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> propSchema = (Map<String, Object>) value;
                    count += countProperties(propSchema);
                }
            }
        }

        if (schema.containsKey("items")) {
            Object items = schema.get("items");
            if (items instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> itemSchema = (Map<String, Object>) items;
                count += countProperties(itemSchema);
            }
        }

        return count;
    }
}
