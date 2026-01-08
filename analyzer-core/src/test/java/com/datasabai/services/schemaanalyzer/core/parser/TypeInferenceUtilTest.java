package com.datasabai.services.schemaanalyzer.core.parser;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for {@link TypeInferenceUtil}.
 */
public class TypeInferenceUtilTest {

    @Test
    public void testInferType_Integer() {
        // All numeric values now return "string" as we cannot reliably infer types from raw data
        // For example, "123456789" might be an order number, not a true integer
        assertThat(TypeInferenceUtil.inferType("123")).isEqualTo("string");
        assertThat(TypeInferenceUtil.inferType("0")).isEqualTo("string");
        assertThat(TypeInferenceUtil.inferType("-456")).isEqualTo("string");
        assertThat(TypeInferenceUtil.inferType("  789  ")).isEqualTo("string");
    }

    @Test
    public void testInferType_Number() {
        // All numeric values now return "string" as we cannot reliably infer types from raw data
        assertThat(TypeInferenceUtil.inferType("12.34")).isEqualTo("string");
        assertThat(TypeInferenceUtil.inferType("0.5")).isEqualTo("string");
        assertThat(TypeInferenceUtil.inferType("-45.67")).isEqualTo("string");
        assertThat(TypeInferenceUtil.inferType("  89.01  ")).isEqualTo("string");
    }

    @Test
    public void testInferType_Boolean() {
        // All values now return "string" as we cannot reliably infer types from raw data
        assertThat(TypeInferenceUtil.inferType("true")).isEqualTo("string");
        assertThat(TypeInferenceUtil.inferType("True")).isEqualTo("string");
        assertThat(TypeInferenceUtil.inferType("TRUE")).isEqualTo("string");
        assertThat(TypeInferenceUtil.inferType("false")).isEqualTo("string");
        assertThat(TypeInferenceUtil.inferType("False")).isEqualTo("string");
        assertThat(TypeInferenceUtil.inferType("FALSE")).isEqualTo("string");
        assertThat(TypeInferenceUtil.inferType("  true  ")).isEqualTo("string");
    }

    @Test
    public void testInferType_String() {
        // Regular strings
        assertThat(TypeInferenceUtil.inferType("hello")).isEqualTo("string");
        assertThat(TypeInferenceUtil.inferType("Hello World")).isEqualTo("string");

        // Alphanumeric
        assertThat(TypeInferenceUtil.inferType("abc123")).isEqualTo("string");

        // Special characters
        assertThat(TypeInferenceUtil.inferType("test@example")).isEqualTo("string");
    }

    @Test
    public void testInferType_Null() {
        assertThat(TypeInferenceUtil.inferType(null)).isEqualTo("null");
        assertThat(TypeInferenceUtil.inferType("")).isEqualTo("null");
        assertThat(TypeInferenceUtil.inferType("   ")).isEqualTo("null");
    }

    @Test
    public void testInferType_DateLike() {
        // Date patterns return string (could be enhanced for date format in future)
        assertThat(TypeInferenceUtil.inferType("2024-12-26")).isEqualTo("string");
        assertThat(TypeInferenceUtil.inferType("2024-12-26T10:30:00")).isEqualTo("string");
    }

    @Test
    public void testInferType_EmailLike() {
        // Email patterns return string (could be enhanced for email format in future)
        assertThat(TypeInferenceUtil.inferType("user@example.com")).isEqualTo("string");
        assertThat(TypeInferenceUtil.inferType("test.user+tag@domain.co.uk")).isEqualTo("string");
    }

    @Test
    public void testMergeTypes_SameTypes() {
        assertThat(TypeInferenceUtil.mergeTypes("string", "string")).isEqualTo("string");
        assertThat(TypeInferenceUtil.mergeTypes("integer", "integer")).isEqualTo("integer");
        assertThat(TypeInferenceUtil.mergeTypes("number", "number")).isEqualTo("number");
        assertThat(TypeInferenceUtil.mergeTypes("boolean", "boolean")).isEqualTo("boolean");
    }

    @Test
    public void testMergeTypes_NumericTypes() {
        // Integer + Number → Number (number is more general)
        assertThat(TypeInferenceUtil.mergeTypes("integer", "number")).isEqualTo("number");
        assertThat(TypeInferenceUtil.mergeTypes("number", "integer")).isEqualTo("number");
    }

    @Test
    public void testMergeTypes_WithNull() {
        // Any type + null → any type
        assertThat(TypeInferenceUtil.mergeTypes("string", "null")).isEqualTo("string");
        assertThat(TypeInferenceUtil.mergeTypes("null", "integer")).isEqualTo("integer");
        assertThat(TypeInferenceUtil.mergeTypes(null, "boolean")).isEqualTo("boolean");
        assertThat(TypeInferenceUtil.mergeTypes("number", null)).isEqualTo("number");
    }

    @Test
    public void testMergeTypes_WithString() {
        // String is most general - always wins
        assertThat(TypeInferenceUtil.mergeTypes("string", "integer")).isEqualTo("string");
        assertThat(TypeInferenceUtil.mergeTypes("boolean", "string")).isEqualTo("string");
        assertThat(TypeInferenceUtil.mergeTypes("number", "string")).isEqualTo("string");
    }

    @Test
    public void testMergeTypes_IncompatibleTypes() {
        // Incompatible types → string
        assertThat(TypeInferenceUtil.mergeTypes("integer", "boolean")).isEqualTo("string");
        assertThat(TypeInferenceUtil.mergeTypes("boolean", "number")).isEqualTo("string");
    }

    @Test
    public void testIsNumericType() {
        // Numeric types
        assertThat(TypeInferenceUtil.isNumericType("integer")).isTrue();
        assertThat(TypeInferenceUtil.isNumericType("number")).isTrue();

        // Non-numeric types
        assertThat(TypeInferenceUtil.isNumericType("string")).isFalse();
        assertThat(TypeInferenceUtil.isNumericType("boolean")).isFalse();
        assertThat(TypeInferenceUtil.isNumericType("null")).isFalse();
        assertThat(TypeInferenceUtil.isNumericType("object")).isFalse();
    }

    @Test
    public void testIsCompatibleType() {
        // Same types are compatible
        assertThat(TypeInferenceUtil.isCompatibleType("string", "string")).isTrue();
        assertThat(TypeInferenceUtil.isCompatibleType("integer", "integer")).isTrue();

        // Numeric types are compatible
        assertThat(TypeInferenceUtil.isCompatibleType("integer", "number")).isTrue();
        assertThat(TypeInferenceUtil.isCompatibleType("number", "integer")).isTrue();

        // Null is compatible with everything
        assertThat(TypeInferenceUtil.isCompatibleType("null", "string")).isTrue();
        assertThat(TypeInferenceUtil.isCompatibleType("integer", "null")).isTrue();
        assertThat(TypeInferenceUtil.isCompatibleType(null, "boolean")).isTrue();

        // Incompatible types
        assertThat(TypeInferenceUtil.isCompatibleType("string", "integer")).isFalse();
        assertThat(TypeInferenceUtil.isCompatibleType("boolean", "number")).isFalse();
    }

    @Test
    public void testIsPrimitiveType() {
        // Primitive types
        assertThat(TypeInferenceUtil.isPrimitiveType("string")).isTrue();
        assertThat(TypeInferenceUtil.isPrimitiveType("integer")).isTrue();
        assertThat(TypeInferenceUtil.isPrimitiveType("number")).isTrue();
        assertThat(TypeInferenceUtil.isPrimitiveType("boolean")).isTrue();
        assertThat(TypeInferenceUtil.isPrimitiveType("null")).isTrue();

        // Non-primitive types
        assertThat(TypeInferenceUtil.isPrimitiveType("object")).isFalse();
        assertThat(TypeInferenceUtil.isPrimitiveType("array")).isFalse();
    }

    @Test
    public void testWidenType() {
        // Integer widens to number
        assertThat(TypeInferenceUtil.widenType("integer")).isEqualTo("number");

        // Number widens to string
        assertThat(TypeInferenceUtil.widenType("number")).isEqualTo("string");

        // Boolean widens to string
        assertThat(TypeInferenceUtil.widenType("boolean")).isEqualTo("string");

        // String stays string (most general)
        assertThat(TypeInferenceUtil.widenType("string")).isEqualTo("string");

        // Null widens to string
        assertThat(TypeInferenceUtil.widenType("null")).isEqualTo("string");
        assertThat(TypeInferenceUtil.widenType(null)).isEqualTo("string");
    }

    @Test
    public void testConstructorThrowsException() {
        // Utility class should not be instantiable
        assertThatThrownBy(() -> {
            var constructor = TypeInferenceUtil.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            constructor.newInstance();
        })
        .hasCauseInstanceOf(UnsupportedOperationException.class)
        .cause()
        .hasMessageContaining("Utility class cannot be instantiated");
    }
}
