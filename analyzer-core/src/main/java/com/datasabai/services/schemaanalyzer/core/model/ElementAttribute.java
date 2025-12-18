package com.datasabai.services.schemaanalyzer.core.model;

import java.util.Objects;

/**
 * Represents an attribute of a structure element.
 * <p>
 * Used for XML attributes, CSV column metadata, Excel cell properties, etc.
 * This class is generic and can be adapted to different file types.
 * </p>
 *
 * <h3>Examples:</h3>
 * <ul>
 *   <li>XML: {@code <person age="30">} - "age" is an attribute</li>
 *   <li>CSV: Column metadata like "nullable", "format"</li>
 *   <li>Excel: Cell format, validation rules</li>
 * </ul>
 */
public class ElementAttribute {
    private String name;
    private String type;
    private boolean required;
    private String defaultValue;
    private String namespace;

    public ElementAttribute() {
    }

    public ElementAttribute(String name, String type) {
        this.name = name;
        this.type = type;
        this.required = false;
    }

    public ElementAttribute(String name, String type, boolean required) {
        this.name = name;
        this.type = type;
        this.required = required;
    }

    public ElementAttribute(String name, String type, boolean required, String defaultValue) {
        this.name = name;
        this.type = type;
        this.required = required;
        this.defaultValue = defaultValue;
    }

    /**
     * Gets the attribute name.
     *
     * @return attribute name
     */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the inferred or declared type of the attribute.
     *
     * @return type (e.g., "string", "integer", "boolean")
     */
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    /**
     * Checks if this attribute is required.
     *
     * @return true if required
     */
    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    /**
     * Gets the default value if specified.
     *
     * @return default value or null
     */
    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    /**
     * Gets the namespace (for XML attributes).
     *
     * @return namespace or null
     */
    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ElementAttribute that = (ElementAttribute) o;
        return Objects.equals(name, that.name) &&
               Objects.equals(namespace, that.namespace);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, namespace);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (namespace != null && !namespace.isBlank()) {
            sb.append(namespace).append(":");
        }
        sb.append(name);
        sb.append(":").append(type);
        if (required) {
            sb.append(" (required)");
        }
        if (defaultValue != null) {
            sb.append(" = ").append(defaultValue);
        }
        return sb.toString();
    }
}
