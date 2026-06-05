package com.datasabai.services.schemaanalyzer.core.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({
        "id",
        "organisationCode",
        "version",
        "createdAt",
        "representation",
        "specification",
        "specVersion",
        "code",
        "documentType",
        "modelName",
        "category",
        "baseStandardId",
        "projectionId",
        "customerName",
        "aliases",
        "tags",
        "summary"
})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class XSchemaMetadata {

    // --- 14 base fields (all from API) ---

    private String id;
    private String organisationCode;
    private String version;
    private String createdAt;
    private String representation;
    private String specification;
    private String specVersion;
    private String code;
    private String documentType;
    private String modelName;
    private String category;
    private String baseStandardId;
    private String projectionId;
    private String customerName;

    // --- 3 RAG fields (auto-generated) ---

    private Map<String, String> aliases;
    private List<String> tags;
    private String summary;

    public XSchemaMetadata() {
    }

    private XSchemaMetadata(Builder builder) {
        this.id = builder.id;
        this.organisationCode = builder.organisationCode;
        this.version = builder.version;
        this.createdAt = builder.createdAt;
        this.representation = builder.representation;
        this.specification = builder.specification;
        this.specVersion = builder.specVersion;
        this.code = builder.code;
        this.documentType = builder.documentType;
        this.modelName = builder.modelName;
        this.category = builder.category;
        this.baseStandardId = builder.baseStandardId;
        this.projectionId = builder.projectionId;
        this.customerName = builder.customerName;
    }

    /**
     * Populates the RAG fields (aliases, tags, summary) from the 13 base fields.
     * Called after base fields are set. Does not overwrite if already populated.
     */
    public void populateRagFields() {
        if (aliases == null) {
            Map<String, String> a = new LinkedHashMap<>();
            if (code != null && !code.isBlank()) {
                a.put("code", code);
            }
            if (modelName != null && !modelName.isBlank()) {
                a.put("modelName", modelName);
            }
            if (specVersion != null && !specVersion.isBlank()) {
                a.put("specVersion", specVersion);
            }
            if (!a.isEmpty()) {
                aliases = a;
            }
        }

        if (tags == null) {
            List<String> t = new ArrayList<>();
            if (specification != null && !specification.isBlank()) {
                t.add(specification.toLowerCase());
            }
            if (representation != null && !representation.isBlank()) {
                t.add(representation.toLowerCase());
            }
            if (code != null && !code.isBlank()) {
                t.add(code.toLowerCase());
            }
            if (documentType != null && !documentType.isBlank()) {
                t.add(documentType.toLowerCase());
            }
            if (category != null && !category.isBlank()) {
                t.add(category.toLowerCase());
            }
            if (!t.isEmpty()) {
                tags = t;
            }
        }

        if (summary == null) {
            StringBuilder sb = new StringBuilder("Schema");
            if (modelName != null && !modelName.isBlank()) {
                sb.append(" ").append(modelName);
            }
            if (code != null && !code.isBlank()) {
                sb.append(" - ").append(code);
            }
            if (documentType != null && !documentType.isBlank()) {
                sb.append(" (").append(documentType).append(")");
            }
            if (representation != null && !representation.isBlank()) {
                sb.append(", format: ").append(representation);
            }
            summary = sb.toString();
        }
    }

    /**
     * Converts this metadata to a LinkedHashMap for injection into the schema map.
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        if (id != null) map.put("id", id);
        // projectionId sits right after id: value for projection, empty for standard/customer
        map.put("projectionId",
                "projection".equalsIgnoreCase(category) && projectionId != null ? projectionId : "");
        if (organisationCode != null) map.put("organisationCode", organisationCode);
        if (version != null) map.put("version", version);
        if (createdAt != null) map.put("createdAt", createdAt);
        if (representation != null) map.put("representation", representation);
        if (specification != null) map.put("specification", specification);
        if (specVersion != null) map.put("specVersion", specVersion);
        if (code != null) map.put("code", code);
        map.put("documentType", documentType != null ? documentType : "");
        if (modelName != null) map.put("modelName", modelName);
        if (category != null) map.put("category", category);
        if (baseStandardId != null) map.put("baseStandardId", baseStandardId);
        if (projectionId != null) map.put("projectionId", projectionId);
        if (customerName != null) map.put("customerName", customerName);
        if (aliases != null) map.put("aliases", aliases);
        if (tags != null) map.put("tags", tags);
        if (summary != null) map.put("summary", summary);
        return map;
    }

    // --- Getters & Setters ---

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getOrganisationCode() { return organisationCode; }
    public void setOrganisationCode(String organisationCode) { this.organisationCode = organisationCode; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getRepresentation() { return representation; }
    public void setRepresentation(String representation) { this.representation = representation; }

    public String getSpecification() { return specification; }
    public void setSpecification(String specification) { this.specification = specification; }

    public String getSpecVersion() { return specVersion; }
    public void setSpecVersion(String specVersion) { this.specVersion = specVersion; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getDocumentType() { return documentType; }
    public void setDocumentType(String documentType) { this.documentType = documentType; }

    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getBaseStandardId() { return baseStandardId; }
    public void setBaseStandardId(String baseStandardId) { this.baseStandardId = baseStandardId; }

    public String getProjectionId() { return projectionId; }
    public void setProjectionId(String projectionId) { this.projectionId = projectionId; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public Map<String, String> getAliases() { return aliases; }
    public void setAliases(Map<String, String> aliases) { this.aliases = aliases; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    // --- Builder ---

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String id;
        private String organisationCode;
        private String version;
        private String createdAt;
        private String representation;
        private String specification;
        private String specVersion;
        private String code;
        private String documentType;
        private String modelName;
        private String category;
        private String baseStandardId;
        private String projectionId;
        private String customerName;

        private Builder() {
        }

        public Builder id(String id) { this.id = id; return this; }
        public Builder organisationCode(String organisationCode) { this.organisationCode = organisationCode; return this; }
        public Builder version(String version) { this.version = version; return this; }
        public Builder createdAt(String createdAt) { this.createdAt = createdAt; return this; }
        public Builder representation(String representation) { this.representation = representation; return this; }
        public Builder specification(String specification) { this.specification = specification; return this; }
        public Builder specVersion(String specVersion) { this.specVersion = specVersion; return this; }
        public Builder code(String code) { this.code = code; return this; }
        public Builder documentType(String documentType) { this.documentType = documentType; return this; }
        public Builder modelName(String modelName) { this.modelName = modelName; return this; }
        public Builder category(String category) { this.category = category; return this; }
        public Builder baseStandardId(String baseStandardId) { this.baseStandardId = baseStandardId; return this; }
        public Builder projectionId(String projectionId) { this.projectionId = projectionId; return this; }
        public Builder customerName(String customerName) { this.customerName = customerName; return this; }

        public XSchemaMetadata build() {
            return new XSchemaMetadata(this);
        }
    }

    @Override
    public String toString() {
        return "XSchemaMetadata{id='" + id + "', category='" + category + "', modelName='" + modelName + "'}";
    }
}
