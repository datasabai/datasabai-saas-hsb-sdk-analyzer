package com.datasabai.services.schemaanalyzer.core.model;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Descriptor for fixed-length file formats.
 * <p>
 * Defines the structure of fixed-length records including field positions,
 * lengths, and optional type information.
 * </p>
 *
 * <h3>Example JSON Descriptor:</h3>
 * <pre>{@code
 * [
 *   {"name": "id", "start": 0, "length": 5, "type": "integer", "trim": true},
 *   {"name": "name", "start": 5, "length": 20, "type": "string", "trim": true},
 *   {"name": "amount", "start": 25, "length": 10, "type": "number", "trim": true}
 * ]
 * }</pre>
 */
public class FixedLengthDescriptor {

    private List<FieldDefinition> fields;

    public FixedLengthDescriptor() {
        this.fields = new ArrayList<>();
    }

    public FixedLengthDescriptor(List<FieldDefinition> fields) {
        this.fields = fields != null ? fields : new ArrayList<>();
    }

    /**
     * Parses a FixedLengthDescriptor from JSON.
     *
     * @param json JSON string containing field definitions
     * @return parsed descriptor
     * @throws IllegalArgumentException if JSON is invalid
     */
    public static FixedLengthDescriptor fromJson(String json) {
        if (json == null || json.isBlank()) {
            throw new IllegalArgumentException("Descriptor JSON cannot be null or blank");
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            FieldDefinition[] fieldsArray = mapper.readValue(json, FieldDefinition[].class);
            return new FixedLengthDescriptor(List.of(fieldsArray));
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse descriptor JSON: " + e.getMessage(), e);
        }
    }

    /**
     * Validates the descriptor.
     *
     * @throws IllegalArgumentException if validation fails
     */
    public void validate() {
        if (fields == null || fields.isEmpty()) {
            throw new IllegalArgumentException("Descriptor must have at least one field");
        }

        Set<String> fieldNames = new HashSet<>();

        for (FieldDefinition field : fields) {
            // Validate field
            field.validate();

            // Check for duplicate names
            if (!fieldNames.add(field.getName())) {
                throw new IllegalArgumentException("Duplicate field name: " + field.getName());
            }
        }

        // Check for overlaps
        for (int i = 0; i < fields.size(); i++) {
            FieldDefinition field1 = fields.get(i);
            int end1 = field1.getStart() + field1.getLength();

            for (int j = i + 1; j < fields.size(); j++) {
                FieldDefinition field2 = fields.get(j);
                int start2 = field2.getStart();
                int end2 = start2 + field2.getLength();

                // Check if fields overlap
                if (!(end1 <= start2 || end2 <= field1.getStart())) {
                    throw new IllegalArgumentException(
                            String.format("Fields '%s' and '%s' overlap: [%d-%d] and [%d-%d]",
                                    field1.getName(), field2.getName(),
                                    field1.getStart(), end1 - 1,
                                    start2, end2 - 1));
                }
            }
        }
    }

    /**
     * Gets the expected record length (maximum end position).
     *
     * @return expected record length
     */
    public int getExpectedRecordLength() {
        int maxEnd = 0;
        for (FieldDefinition field : fields) {
            int end = field.getStart() + field.getLength();
            maxEnd = Math.max(maxEnd, end);
        }
        return maxEnd;
    }

    public List<FieldDefinition> getFields() {
        return fields;
    }

    public void setFields(List<FieldDefinition> fields) {
        this.fields = fields != null ? fields : new ArrayList<>();
    }

    /**
     * Definition of a single field in a fixed-length record.
     */
    public static class FieldDefinition {
        private String name;
        private int start;      // 0-based start position
        private int length;     // Field length
        private String type;    // Optional suggested type (integer, number, string, boolean, null)
        private boolean trim = true;  // Whether to trim whitespace

        public FieldDefinition() {
        }

        public FieldDefinition(String name, int start, int length) {
            this.name = name;
            this.start = start;
            this.length = length;
        }

        public FieldDefinition(String name, int start, int length, String type) {
            this.name = name;
            this.start = start;
            this.length = length;
            this.type = type;
        }

        public FieldDefinition(String name, int start, int length, String type, boolean trim) {
            this.name = name;
            this.start = start;
            this.length = length;
            this.type = type;
            this.trim = trim;
        }

        /**
         * Validates the field definition.
         *
         * @throws IllegalArgumentException if validation fails
         */
        public void validate() {
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("Field name cannot be null or blank");
            }
            if (start < 0) {
                throw new IllegalArgumentException(
                        String.format("Field '%s' has invalid start position: %d (must be >= 0)", name, start));
            }
            if (length <= 0) {
                throw new IllegalArgumentException(
                        String.format("Field '%s' has invalid length: %d (must be > 0)", name, length));
            }
            if (type != null && !isValidType(type)) {
                throw new IllegalArgumentException(
                        String.format("Field '%s' has invalid type: %s (must be one of: integer, number, string, boolean, null)",
                                name, type));
            }
        }

        private boolean isValidType(String type) {
            return "integer".equals(type) || "number".equals(type) || "string".equals(type) ||
                    "boolean".equals(type) || "null".equals(type);
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getStart() {
            return start;
        }

        public void setStart(int start) {
            this.start = start;
        }

        public int getLength() {
            return length;
        }

        public void setLength(int length) {
            this.length = length;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public boolean isTrim() {
            return trim;
        }

        public void setTrim(boolean trim) {
            this.trim = trim;
        }

        @Override
        public String toString() {
            return String.format("%s[%d:%d](%s)", name, start, start + length - 1, type != null ? type : "auto");
        }
    }
}
