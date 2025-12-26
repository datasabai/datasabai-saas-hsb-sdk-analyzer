package com.datasabai.services.schemaanalyzer.core.parser;

import java.util.regex.Pattern;

/**
 * Utility class for type inference from string values.
 * <p>
 * This class provides common type inference patterns used by all file parsers
 * to detect data types from string representations.
 * </p>
 * <p>
 * Type inference uses pattern matching in the following order of precedence:
 * </p>
 * <ol>
 *   <li>Null or blank → "null"</li>
 *   <li>Boolean (true/false) → "boolean"</li>
 *   <li>Integer (-?\d+) → "integer"</li>
 *   <li>Decimal (-?\d+\.\d+) → "number"</li>
 *   <li>Date (YYYY-MM-DD*) → "string" (with date format hint)</li>
 *   <li>Email → "string" (with email format hint)</li>
 *   <li>Default → "string"</li>
 * </ol>
 *
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * String type1 = TypeInferenceUtil.inferType("123");        // "integer"
 * String type2 = TypeInferenceUtil.inferType("12.34");      // "number"
 * String type3 = TypeInferenceUtil.inferType("true");       // "boolean"
 * String type4 = TypeInferenceUtil.inferType("hello");      // "string"
 *
 * // Merge types from multiple samples
 * String merged = TypeInferenceUtil.mergeTypes("integer", "number"); // "number"
 * }</pre>
 */
public class TypeInferenceUtil {

    // Pattern constants for type detection
    private static final Pattern INTEGER_PATTERN = Pattern.compile("^-?\\d+$");
    private static final Pattern DECIMAL_PATTERN = Pattern.compile("^-?\\d+\\.\\d+$");
    private static final Pattern BOOLEAN_PATTERN = Pattern.compile("^(true|false)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern DATE_PATTERN = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}.*");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    /**
     * Private constructor to prevent instantiation.
     */
    private TypeInferenceUtil() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Infers the JSON Schema type from a string value.
     * <p>
     * Uses pattern matching to detect the most specific type.
     * Returns JSON Schema type names: "string", "integer", "number", "boolean", "null".
     * </p>
     *
     * @param value the string value to analyze
     * @return the inferred JSON Schema type
     */
    public static String inferType(String value) {
        if (value == null || value.isBlank()) {
            return "null";
        }

        String trimmed = value.trim();

        // Check boolean first (most specific)
        if (BOOLEAN_PATTERN.matcher(trimmed).matches()) {
            return "boolean";
        }

        // Check integer
        if (INTEGER_PATTERN.matcher(trimmed).matches()) {
            return "integer";
        }

        // Check decimal/number
        if (DECIMAL_PATTERN.matcher(trimmed).matches()) {
            return "number";
        }

        // Check date (returns string, but could add format hint in the future)
        if (DATE_PATTERN.matcher(trimmed).matches()) {
            return "string"; // Could be enhanced to return date format
        }

        // Check email (returns string, but could add format hint in the future)
        if (EMAIL_PATTERN.matcher(trimmed).matches()) {
            return "string"; // Could be enhanced to return email format
        }

        // Default to string
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
