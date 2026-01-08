package com.datasabai.services.schemaanalyzer.core.parser;

/**
 * Utility class for type inference from string values.
 * <p>
 * This class provides type inference for file parsers. By default, all values
 * are inferred as "string" type since automatic type detection from raw data
 * (CSV, JSON, etc.) cannot reliably determine the true semantic type.
 * </p>
 * <p>
 * For example, a value like "123456789" might appear numeric but could actually
 * be a string field (order number, product code) that may contain alphanumeric
 * values like "1234AZ443" in other records.
 * </p>
 * <p>
 * Type enrichment should be done in a later processing step based on business rules.
 * </p>
 *
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * String type1 = TypeInferenceUtil.inferType("123");        // "string"
 * String type2 = TypeInferenceUtil.inferType("12.34");      // "string"
 * String type3 = TypeInferenceUtil.inferType("true");       // "string"
 * String type4 = TypeInferenceUtil.inferType("hello");      // "string"
 *
 * // Merge types from multiple samples
 * String merged = TypeInferenceUtil.mergeTypes("string", "string"); // "string"
 * }</pre>
 */
public class TypeInferenceUtil {

    /**
     * Private constructor to prevent instantiation.
     */
    private TypeInferenceUtil() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Infers the JSON Schema type from a string value.
     * <p>
     * Always returns "string" type by default, as type inference from raw data
     * (CSV, JSON, etc.) cannot accurately determine the true semantic type.
     * For example, "123456789" might appear to be an integer but could actually
     * be a string field like an order number that might contain "1234AZ443".
     * </p>
     * <p>
     * The schema will be enriched with correct types in a later processing step.
     * </p>
     *
     * @param value the string value to analyze
     * @return always returns "string" (or "null" for null/blank values)
     */
    public static String inferType(String value) {
        if (value == null || value.isBlank()) {
            return "null";
        }

        // Always return string type for all non-null values
        // Type enrichment will be done in a later processing step
        return "string";
    }

    /**
     * Merges two types to find the most general compatible type.
     * <p>
     * Type merging follows these rules:
     * </p>
     * <ul>
     *   <li>Same types → same type</li>
     *   <li>integer + number → number (number is more general)</li>
     *   <li>integer + boolean → string (incompatible numeric types)</li>
     *   <li>number + boolean → string (incompatible types)</li>
     *   <li>any + string → string (string is most general)</li>
     *   <li>any + null → any (null is ignored)</li>
     * </ul>
     *
     * @param type1 first type
     * @param type2 second type
     * @return the merged (most general compatible) type
     */
    public static String mergeTypes(String type1, String type2) {
        if (type1 == null || "null".equals(type1)) {
            return type2 != null ? type2 : "string";
        }
        if (type2 == null || "null".equals(type2)) {
            return type1;
        }

        // Same types
        if (type1.equals(type2)) {
            return type1;
        }

        // String is most general - takes precedence
        if ("string".equals(type1) || "string".equals(type2)) {
            return "string";
        }

        // Number + Integer → Number (number is more general)
        if (isNumericType(type1) && isNumericType(type2)) {
            return "number";
        }

        // Incompatible types → string
        return "string";
    }

    /**
     * Checks if a type is numeric (integer or number).
     *
     * @param type the type to check
     * @return true if the type is "integer" or "number"
     */
    public static boolean isNumericType(String type) {
        return "integer".equals(type) || "number".equals(type);
    }

    /**
     * Checks if two types are compatible (can be merged without losing information).
     * <p>
     * Compatible types:
     * </p>
     * <ul>
     *   <li>Same types</li>
     *   <li>integer and number</li>
     *   <li>Any type and null</li>
     * </ul>
     *
     * @param type1 first type
     * @param type2 second type
     * @return true if types are compatible
     */
    public static boolean isCompatibleType(String type1, String type2) {
        if (type1 == null || type2 == null) {
            return true;
        }

        if (type1.equals(type2)) {
            return true;
        }

        if ("null".equals(type1) || "null".equals(type2)) {
            return true;
        }

        // integer and number are compatible
        if (isNumericType(type1) && isNumericType(type2)) {
            return true;
        }

        return false;
    }

    /**
     * Checks if a type is a primitive JSON type.
     *
     * @param type the type to check
     * @return true if the type is a primitive (not object or array)
     */
    public static boolean isPrimitiveType(String type) {
        return "string".equals(type) ||
               "integer".equals(type) ||
               "number".equals(type) ||
               "boolean".equals(type) ||
               "null".equals(type);
    }

    /**
     * Gets a more general type (widens the type).
     * <p>
     * Type hierarchy (from specific to general):
     * </p>
     * <ul>
     *   <li>null → any type</li>
     *   <li>boolean → boolean</li>
     *   <li>integer → number → string</li>
     * </ul>
     *
     * @param type the type to widen
     * @return the next more general type
     */
    public static String widenType(String type) {
        if (type == null || "null".equals(type)) {
            return "string";
        }

        return switch (type) {
            case "integer" -> "number";
            case "number", "boolean" -> "string";
            default -> "string";
        };
    }
}
