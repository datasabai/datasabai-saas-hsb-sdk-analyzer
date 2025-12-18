package com.datasabai.services.schemaanalyzer.core.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Result of file schema analysis.
 * <p>
 * Contains the generated JSON Schema and metadata about the analysis process.
 * </p>
 */
public class SchemaGenerationResult {
    private String schemaName;
    private FileType sourceFileType;
    private Map<String, Object> jsonSchema;
    private String jsonSchemaAsString;
    private SchemaMetadata metadata;
    private List<String> warnings;
    private List<String> detectedArrayFields;
    private Map<String, String> parserMetadata;
    private long analysisTimeMs;
    private int elementsAnalyzed;
    private boolean success;

    public SchemaGenerationResult() {
        this.warnings = new ArrayList<>();
        this.detectedArrayFields = new ArrayList<>();
        this.parserMetadata = new HashMap<>();
        this.success = false;
    }

    /**
     * Builder for fluent construction.
     */
    public static Builder builder() {
        return new Builder();
    }

    // Getters and setters

    /**
     * Gets the name of the generated schema.
     *
     * @return schema name
     */
    public String getSchemaName() {
        return schemaName;
    }

    public void setSchemaName(String schemaName) {
        this.schemaName = schemaName;
    }

    /**
     * Gets the source file type.
     *
     * @return file type
     */
    public FileType getSourceFileType() {
        return sourceFileType;
    }

    public void setSourceFileType(FileType sourceFileType) {
        this.sourceFileType = sourceFileType;
    }

    /**
     * Gets the JSON Schema as a map.
     *
     * @return JSON Schema map
     */
    public Map<String, Object> getJsonSchema() {
        return jsonSchema;
    }

    public void setJsonSchema(Map<String, Object> jsonSchema) {
        this.jsonSchema = jsonSchema;
    }

    /**
     * Gets the JSON Schema as a formatted string.
     *
     * @return JSON Schema string
     */
    public String getJsonSchemaAsString() {
        return jsonSchemaAsString;
    }

    public void setJsonSchemaAsString(String jsonSchemaAsString) {
        this.jsonSchemaAsString = jsonSchemaAsString;
    }

    /**
     * Gets the schema metadata.
     *
     * @return metadata
     */
    public SchemaMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(SchemaMetadata metadata) {
        this.metadata = metadata;
    }

    /**
     * Gets warnings generated during analysis.
     *
     * @return list of warnings
     */
    public List<String> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings != null ? warnings : new ArrayList<>();
    }

    /**
     * Gets fields detected as arrays.
     *
     * @return list of array field names
     */
    public List<String> getDetectedArrayFields() {
        return detectedArrayFields;
    }

    public void setDetectedArrayFields(List<String> detectedArrayFields) {
        this.detectedArrayFields = detectedArrayFields != null ? detectedArrayFields : new ArrayList<>();
    }

    /**
     * Gets parser-specific metadata.
     *
     * @return parser metadata map
     */
    public Map<String, String> getParserMetadata() {
        return parserMetadata;
    }

    public void setParserMetadata(Map<String, String> parserMetadata) {
        this.parserMetadata = parserMetadata != null ? parserMetadata : new HashMap<>();
    }

    /**
     * Gets the analysis time in milliseconds.
     *
     * @return analysis time
     */
    public long getAnalysisTimeMs() {
        return analysisTimeMs;
    }

    public void setAnalysisTimeMs(long analysisTimeMs) {
        this.analysisTimeMs = analysisTimeMs;
    }

    /**
     * Gets the number of elements analyzed.
     *
     * @return element count
     */
    public int getElementsAnalyzed() {
        return elementsAnalyzed;
    }

    public void setElementsAnalyzed(int elementsAnalyzed) {
        this.elementsAnalyzed = elementsAnalyzed;
    }

    /**
     * Checks if the analysis was successful.
     *
     * @return true if successful
     */
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    /**
     * Checks if there are warnings.
     *
     * @return true if has warnings
     */
    public boolean hasWarnings() {
        return warnings != null && !warnings.isEmpty();
    }

    /**
     * Adds a warning message.
     *
     * @param warning warning message
     */
    public void addWarning(String warning) {
        if (this.warnings == null) {
            this.warnings = new ArrayList<>();
        }
        this.warnings.add(warning);
    }

    /**
     * Adds a detected array field.
     *
     * @param fieldName field name
     */
    public void addDetectedArrayField(String fieldName) {
        if (this.detectedArrayFields == null) {
            this.detectedArrayFields = new ArrayList<>();
        }
        this.detectedArrayFields.add(fieldName);
    }

    /**
     * Adds parser metadata.
     *
     * @param key metadata key
     * @param value metadata value
     */
    public void addParserMetadata(String key, String value) {
        if (this.parserMetadata == null) {
            this.parserMetadata = new HashMap<>();
        }
        this.parserMetadata.put(key, value);
    }

    // Builder class
    public static class Builder {
        private final SchemaGenerationResult result;

        private Builder() {
            this.result = new SchemaGenerationResult();
        }

        public Builder schemaName(String schemaName) {
            result.schemaName = schemaName;
            return this;
        }

        public Builder sourceFileType(FileType sourceFileType) {
            result.sourceFileType = sourceFileType;
            return this;
        }

        public Builder jsonSchema(Map<String, Object> jsonSchema) {
            result.jsonSchema = jsonSchema;
            return this;
        }

        public Builder jsonSchemaAsString(String jsonSchemaAsString) {
            result.jsonSchemaAsString = jsonSchemaAsString;
            return this;
        }

        public Builder metadata(SchemaMetadata metadata) {
            result.metadata = metadata;
            return this;
        }

        public Builder warnings(List<String> warnings) {
            result.warnings = warnings;
            return this;
        }

        public Builder addWarning(String warning) {
            result.addWarning(warning);
            return this;
        }

        public Builder detectedArrayFields(List<String> detectedArrayFields) {
            result.detectedArrayFields = detectedArrayFields;
            return this;
        }

        public Builder addDetectedArrayField(String fieldName) {
            result.addDetectedArrayField(fieldName);
            return this;
        }

        public Builder parserMetadata(Map<String, String> parserMetadata) {
            result.parserMetadata = parserMetadata;
            return this;
        }

        public Builder addParserMetadata(String key, String value) {
            result.addParserMetadata(key, value);
            return this;
        }

        public Builder analysisTimeMs(long analysisTimeMs) {
            result.analysisTimeMs = analysisTimeMs;
            return this;
        }

        public Builder elementsAnalyzed(int elementsAnalyzed) {
            result.elementsAnalyzed = elementsAnalyzed;
            return this;
        }

        public Builder success(boolean success) {
            result.success = success;
            return this;
        }

        public SchemaGenerationResult build() {
            return result;
        }
    }
}
