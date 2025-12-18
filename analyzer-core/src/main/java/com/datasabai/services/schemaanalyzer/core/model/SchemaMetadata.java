package com.datasabai.services.schemaanalyzer.core.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Metadata about a generated JSON Schema.
 * <p>
 * Contains information about the schema generation process,
 * the source file type, and hints for downstream processing (e.g., BeanIO).
 * </p>
 */
public class SchemaMetadata {
    private String schemaVersion;
    private String rootElement;
    private List<String> requiredFields;
    private Map<String, String> beanIOHints;
    private FileType sourceFileType;
    private LocalDateTime generatedAt;
    private int totalElements;
    private int totalAttributes;
    private int arrayElements;
    private String generatorVersion;
    private Map<String, Object> additionalMetadata;

    public SchemaMetadata() {
        this.schemaVersion = "http://json-schema.org/draft-07/schema#";
        this.requiredFields = new ArrayList<>();
        this.beanIOHints = new HashMap<>();
        this.additionalMetadata = new HashMap<>();
        this.generatedAt = LocalDateTime.now();
        this.generatorVersion = "1.0.0";
    }

    /**
     * Builder for fluent construction.
     */
    public static Builder builder() {
        return new Builder();
    }

    // Getters and setters

    public String getSchemaVersion() {
        return schemaVersion;
    }

    public void setSchemaVersion(String schemaVersion) {
        this.schemaVersion = schemaVersion;
    }

    public String getRootElement() {
        return rootElement;
    }

    public void setRootElement(String rootElement) {
        this.rootElement = rootElement;
    }

    public List<String> getRequiredFields() {
        return requiredFields;
    }

    public void setRequiredFields(List<String> requiredFields) {
        this.requiredFields = requiredFields != null ? requiredFields : new ArrayList<>();
    }

    public Map<String, String> getBeanIOHints() {
        return beanIOHints;
    }

    public void setBeanIOHints(Map<String, String> beanIOHints) {
        this.beanIOHints = beanIOHints != null ? beanIOHints : new HashMap<>();
    }

    public FileType getSourceFileType() {
        return sourceFileType;
    }

    public void setSourceFileType(FileType sourceFileType) {
        this.sourceFileType = sourceFileType;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }

    public int getTotalElements() {
        return totalElements;
    }

    public void setTotalElements(int totalElements) {
        this.totalElements = totalElements;
    }

    public int getTotalAttributes() {
        return totalAttributes;
    }

    public void setTotalAttributes(int totalAttributes) {
        this.totalAttributes = totalAttributes;
    }

    public int getArrayElements() {
        return arrayElements;
    }

    public void setArrayElements(int arrayElements) {
        this.arrayElements = arrayElements;
    }

    public String getGeneratorVersion() {
        return generatorVersion;
    }

    public void setGeneratorVersion(String generatorVersion) {
        this.generatorVersion = generatorVersion;
    }

    public Map<String, Object> getAdditionalMetadata() {
        return additionalMetadata;
    }

    public void setAdditionalMetadata(Map<String, Object> additionalMetadata) {
        this.additionalMetadata = additionalMetadata != null ? additionalMetadata : new HashMap<>();
    }

    /**
     * Adds a BeanIO hint.
     *
     * @param key hint key
     * @param value hint value
     */
    public void addBeanIOHint(String key, String value) {
        if (this.beanIOHints == null) {
            this.beanIOHints = new HashMap<>();
        }
        this.beanIOHints.put(key, value);
    }

    /**
     * Adds additional metadata.
     *
     * @param key metadata key
     * @param value metadata value
     */
    public void addMetadata(String key, Object value) {
        if (this.additionalMetadata == null) {
            this.additionalMetadata = new HashMap<>();
        }
        this.additionalMetadata.put(key, value);
    }

    // Builder class
    public static class Builder {
        private final SchemaMetadata metadata;

        private Builder() {
            this.metadata = new SchemaMetadata();
        }

        public Builder schemaVersion(String schemaVersion) {
            metadata.schemaVersion = schemaVersion;
            return this;
        }

        public Builder rootElement(String rootElement) {
            metadata.rootElement = rootElement;
            return this;
        }

        public Builder requiredFields(List<String> requiredFields) {
            metadata.requiredFields = requiredFields;
            return this;
        }

        public Builder beanIOHints(Map<String, String> beanIOHints) {
            metadata.beanIOHints = beanIOHints;
            return this;
        }

        public Builder sourceFileType(FileType sourceFileType) {
            metadata.sourceFileType = sourceFileType;
            return this;
        }

        public Builder generatedAt(LocalDateTime generatedAt) {
            metadata.generatedAt = generatedAt;
            return this;
        }

        public Builder totalElements(int totalElements) {
            metadata.totalElements = totalElements;
            return this;
        }

        public Builder totalAttributes(int totalAttributes) {
            metadata.totalAttributes = totalAttributes;
            return this;
        }

        public Builder arrayElements(int arrayElements) {
            metadata.arrayElements = arrayElements;
            return this;
        }

        public Builder generatorVersion(String generatorVersion) {
            metadata.generatorVersion = generatorVersion;
            return this;
        }

        public Builder additionalMetadata(Map<String, Object> additionalMetadata) {
            metadata.additionalMetadata = additionalMetadata;
            return this;
        }

        public SchemaMetadata build() {
            return metadata;
        }
    }
}
