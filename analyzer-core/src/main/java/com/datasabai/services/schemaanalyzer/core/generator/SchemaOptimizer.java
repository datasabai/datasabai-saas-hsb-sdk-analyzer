package com.datasabai.services.schemaanalyzer.core.generator;

import com.datasabai.services.schemaanalyzer.core.model.FileType;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Optimizes JSON Schemas for specific use cases, particularly BeanIO code generation.
 * <p>
 * This class adds hints and metadata to JSON Schemas to facilitate
 * automatic generation of BeanIO configuration files and POJO classes.
 * </p>
 *
 * <h3>BeanIO Optimization Features:</h3>
 * <ul>
 *   <li>Adds x-beanio-* hints for field mapping</li>
 *   <li>Converts field names to camelCase for Java POJOs</li>
 *   <li>Adds type mapping hints for BeanIO handlers</li>
 *   <li>Includes format-specific metadata (XML namespaces, CSV positions, etc.)</li>
 * </ul>
 *
 * <h3>File Type Specific Optimizations:</h3>
 * <ul>
 *   <li><b>XML</b>: namespace mapping, element vs attribute distinction</li>
 *   <li><b>CSV</b>: column positions, delimiters</li>
 *   <li><b>Excel</b>: sheet names, cell positions</li>
 * </ul>
 */
public class SchemaOptimizer {
    private static final Logger log = LoggerFactory.getLogger(SchemaOptimizer.class);

    /**
     * Optimizes a JSON Schema.
     *
     * @param schema original schema
     * @param fileType source file type
     * @param optimizeForBeanIO whether to add BeanIO-specific hints
     * @return optimized schema
     */
    public Map<String, Object> optimize(
            Map<String, Object> schema,
            FileType fileType,
            boolean optimizeForBeanIO
    ) {
        if (schema == null) {
            log.warn("Cannot optimize null schema");
            return schema;
        }

        log.debug("Optimizing schema for file type: {}, BeanIO: {}", fileType, optimizeForBeanIO);

        // Create a copy to avoid modifying the original
        Map<String, Object> optimized = new LinkedHashMap<>(schema);

        if (optimizeForBeanIO) {
            addBeanIOHints(optimized, fileType);
        }

        // Apply file type specific optimizations
        switch (fileType) {
            case CSV -> optimizeForCsv(optimized);
            case JSON -> optimizeForJson(optimized);
            case FIXED_LENGTH -> optimizeForFixedLength(optimized);
            case VARIABLE_LENGTH -> optimizeForVariableLength(optimized);
        }

        log.debug("Schema optimization completed");
        return optimized;
    }

    /**
     * Adds BeanIO-specific hints to the schema.
     */
    private void addBeanIOHints(Map<String, Object> schema, FileType fileType) {
        Map<String, Object> beanIOHints = new LinkedHashMap<>();
        beanIOHints.put("streamFormat", getBeanIOStreamFormat(fileType));
        beanIOHints.put("generatePOJO", true);
        beanIOHints.put("useBuilders", true);

        schema.put("x-beanio", beanIOHints);

        // Add hints to properties
        if (schema.containsKey("properties")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> properties = (Map<String, Object>) schema.get("properties");
            addBeanIOHintsToProperties(properties, fileType);
        }

        // Add hints to array items
        if (schema.containsKey("items")) {
            Object items = schema.get("items");
            if (items instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> itemSchema = (Map<String, Object>) items;
                addBeanIOHints(itemSchema, fileType);
            }
        }
    }

    /**
     * Adds BeanIO hints to individual properties.
     */
    private void addBeanIOHintsToProperties(Map<String, Object> properties, FileType fileType) {
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            String propName = entry.getKey();
            Object propValue = entry.getValue();

            if (propValue instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> propSchema = (Map<String, Object>) propValue;

                // Add Java field name (camelCase)
                String javaFieldName = toJavaFieldName(propName);
                propSchema.put("x-java-field", javaFieldName);

                // Add BeanIO field hints
                Map<String, Object> fieldHints = new LinkedHashMap<>();
                fieldHints.put("name", propName);
                fieldHints.put("javaName", javaFieldName);

                // Add type handler hints
                String type = (String) propSchema.get("type");
                if (type != null) {
                    fieldHints.put("typeHandler", getTypeHandler(type));
                }

                propSchema.put("x-beanio-field", fieldHints);

                // Recursively process nested properties
                if (propSchema.containsKey("properties")) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> nestedProps = (Map<String, Object>) propSchema.get("properties");
                    addBeanIOHintsToProperties(nestedProps, fileType);
                }

                // Process array items
                if (propSchema.containsKey("items")) {
                    Object items = propSchema.get("items");
                    if (items instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> itemSchema = (Map<String, Object>) items;
                        addBeanIOHints(itemSchema, fileType);
                    }
                }
            }
        }
    }

    /**
     * Gets BeanIO stream format for a file type.
     */
    private String getBeanIOStreamFormat(FileType fileType) {
        return switch (fileType) {
            case CSV -> "csv";
            case JSON -> "json";
            case FIXED_LENGTH -> "fixedlength";
            case VARIABLE_LENGTH -> "delimited";
        };
    }

    /**
     * Gets BeanIO type handler for a JSON Schema type.
     */
    private String getTypeHandler(String jsonSchemaType) {
        return switch (jsonSchemaType) {
            case "integer" -> "java.lang.Integer";
            case "number" -> "java.math.BigDecimal";
            case "boolean" -> "java.lang.Boolean";
            case "string" -> "java.lang.String";
            case "array" -> "java.util.List";
            case "object" -> "java.lang.Object";
            default -> "java.lang.String";
        };
    }

    /**
     * Converts a field name to Java camelCase convention.
     */
    private String toJavaFieldName(String name) {
        if (name == null || name.isBlank()) {
            return name;
        }

        // Remove special characters
        name = name.replaceAll("[^a-zA-Z0-9_-]", "");

        // Convert to camelCase
        String[] parts = name.split("[_-]");
        if (parts.length == 0) {
            return name;
        }

        StringBuilder camelCase = new StringBuilder(parts[0].toLowerCase());
        for (int i = 1; i < parts.length; i++) {
            if (!parts[i].isEmpty()) {
                camelCase.append(StringUtils.capitalize(parts[i].toLowerCase()));
            }
        }

        return camelCase.toString();
    }

    /**
     * Applies CSV-specific optimizations.
     */
    private void optimizeForCsv(Map<String, Object> schema) {
        log.debug("Applying CSV optimizations");

        Map<String, Object> csvHints = new LinkedHashMap<>();
        csvHints.put("delimiter", ",");
        csvHints.put("hasHeader", true);

        schema.put("x-csv", csvHints);

        // Add column positions
        if (schema.containsKey("properties")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> properties = (Map<String, Object>) schema.get("properties");
            addColumnPositions(properties);
        }
    }

    /**
     * Adds column positions to CSV properties.
     */
    private void addColumnPositions(Map<String, Object> properties) {
        int position = 0;
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            Object propValue = entry.getValue();
            if (propValue instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> propSchema = (Map<String, Object>) propValue;
                propSchema.put("x-csv-position", position);
                position++;
            }
        }
    }

    /**
     * Applies JSON-specific optimizations.
     */
    private void optimizeForJson(Map<String, Object> schema) {
        log.debug("Applying JSON optimizations");

        Map<String, Object> jsonHints = new LinkedHashMap<>();
        jsonHints.put("prettyPrint", true);

        schema.put("x-json", jsonHints);
    }

    /**
     * Applies Fixed-Length specific optimizations.
     */
    private void optimizeForFixedLength(Map<String, Object> schema) {
        log.debug("Applying Fixed-Length optimizations");

        Map<String, Object> fixedLengthHints = new LinkedHashMap<>();
        fixedLengthHints.put("format", "fixedlength");

        schema.put("x-fixed-length", fixedLengthHints);

        // Add field positions if properties exist
        if (schema.containsKey("properties")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> properties = (Map<String, Object>) schema.get("properties");
            addFieldPositions(properties);
        }
    }

    /**
     * Applies Variable-Length specific optimizations.
     */
    private void optimizeForVariableLength(Map<String, Object> schema) {
        log.debug("Applying Variable-Length optimizations");

        Map<String, Object> variableLengthHints = new LinkedHashMap<>();
        variableLengthHints.put("delimiter", "|");
        variableLengthHints.put("format", "delimited");

        schema.put("x-variable-length", variableLengthHints);

        // Add field positions
        if (schema.containsKey("properties")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> properties = (Map<String, Object>) schema.get("properties");
            addColumnPositions(properties);
        }
    }

    /**
     * Adds field positions to Fixed-Length properties.
     */
    private void addFieldPositions(Map<String, Object> properties) {
        int position = 0;
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            Object propValue = entry.getValue();
            if (propValue instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> propSchema = (Map<String, Object>) propValue;
                propSchema.put("x-field-position", position);
                position++;
            }
        }
    }
}
