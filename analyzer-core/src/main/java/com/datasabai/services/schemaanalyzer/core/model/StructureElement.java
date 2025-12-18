package com.datasabai.services.schemaanalyzer.core.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Generic representation of a structure element from any file type.
 * <p>
 * This class is used to represent elements from different file formats:
 * </p>
 * <ul>
 *   <li>XML: elements and their hierarchies</li>
 *   <li>Excel: sheets, rows, columns</li>
 *   <li>CSV: columns and their types</li>
 *   <li>JSON: objects and their properties</li>
 *   <li>TXT: structured lines or fields</li>
 * </ul>
 *
 * <h3>Example for XML:</h3>
 * <pre>{@code
 * <customer>
 *   <id>123</id>
 *   <name>John</name>
 *   <orders>
 *     <order id="1">...</order>
 *     <order id="2">...</order>
 *   </orders>
 * </customer>
 * }</pre>
 * <p>
 * Would be represented as:
 * <ul>
 *   <li>name="customer", type="object", children=[id, name, orders]</li>
 *   <li>name="orders", type="object", children=[order]</li>
 *   <li>name="order", type="object", isArray=true</li>
 * </ul>
 */
public class StructureElement {
    private String name;
    private String type;
    private boolean isArray;
    private List<ElementAttribute> attributes;
    private List<StructureElement> children;
    private String namespace;
    private int minOccurs;
    private int maxOccurs;
    private Map<String, Object> parserSpecificData;
    private String description;
    private boolean required;

    public StructureElement() {
        this.attributes = new ArrayList<>();
        this.children = new ArrayList<>();
        this.parserSpecificData = new HashMap<>();
        this.minOccurs = 0;
        this.maxOccurs = 1;
        this.required = true;
    }

    public StructureElement(String name, String type) {
        this();
        this.name = name;
        this.type = type;
    }

    /**
     * Builder for fluent construction.
     */
    public static Builder builder() {
        return new Builder();
    }

    // Getters and setters

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isArray() {
        return isArray;
    }

    public void setArray(boolean array) {
        isArray = array;
    }

    public List<ElementAttribute> getAttributes() {
        return attributes;
    }

    public void setAttributes(List<ElementAttribute> attributes) {
        this.attributes = attributes != null ? attributes : new ArrayList<>();
    }

    public List<StructureElement> getChildren() {
        return children;
    }

    public void setChildren(List<StructureElement> children) {
        this.children = children != null ? children : new ArrayList<>();
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public int getMinOccurs() {
        return minOccurs;
    }

    public void setMinOccurs(int minOccurs) {
        this.minOccurs = minOccurs;
    }

    public int getMaxOccurs() {
        return maxOccurs;
    }

    public void setMaxOccurs(int maxOccurs) {
        this.maxOccurs = maxOccurs;
    }

    public Map<String, Object> getParserSpecificData() {
        return parserSpecificData;
    }

    public void setParserSpecificData(Map<String, Object> parserSpecificData) {
        this.parserSpecificData = parserSpecificData != null ? parserSpecificData : new HashMap<>();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    // Helper methods

    /**
     * Adds a child element.
     *
     * @param child child element to add
     * @return this element for chaining
     */
    public StructureElement addChild(StructureElement child) {
        if (this.children == null) {
            this.children = new ArrayList<>();
        }
        this.children.add(child);
        return this;
    }

    /**
     * Adds an attribute.
     *
     * @param attribute attribute to add
     * @return this element for chaining
     */
    public StructureElement addAttribute(ElementAttribute attribute) {
        if (this.attributes == null) {
            this.attributes = new ArrayList<>();
        }
        this.attributes.add(attribute);
        return this;
    }

    /**
     * Adds parser-specific data.
     *
     * @param key data key
     * @param value data value
     * @return this element for chaining
     */
    public StructureElement addParserData(String key, Object value) {
        if (this.parserSpecificData == null) {
            this.parserSpecificData = new HashMap<>();
        }
        this.parserSpecificData.put(key, value);
        return this;
    }

    /**
     * Finds a child by name.
     *
     * @param childName name of the child to find
     * @return the child element or null if not found
     */
    public StructureElement findChild(String childName) {
        if (children == null) {
            return null;
        }
        return children.stream()
                .filter(child -> Objects.equals(child.getName(), childName))
                .findFirst()
                .orElse(null);
    }

    /**
     * Checks if this element has children.
     *
     * @return true if has children
     */
    public boolean hasChildren() {
        return children != null && !children.isEmpty();
    }

    /**
     * Checks if this element has attributes.
     *
     * @return true if has attributes
     */
    public boolean hasAttributes() {
        return attributes != null && !attributes.isEmpty();
    }

    @Override
    public String toString() {
        return toString(0);
    }

    private String toString(int indent) {
        StringBuilder sb = new StringBuilder();
        String indentStr = "  ".repeat(indent);

        sb.append(indentStr);
        if (namespace != null && !namespace.isBlank()) {
            sb.append(namespace).append(":");
        }
        sb.append(name).append(" : ").append(type);
        if (isArray) {
            sb.append("[]");
        }
        if (!required) {
            sb.append(" (optional)");
        }

        if (hasAttributes()) {
            sb.append("\n").append(indentStr).append("  Attributes: ");
            sb.append(attributes.stream()
                    .map(ElementAttribute::toString)
                    .reduce((a, b) -> a + ", " + b)
                    .orElse(""));
        }

        if (hasChildren()) {
            for (StructureElement child : children) {
                sb.append("\n").append(child.toString(indent + 1));
            }
        }

        return sb.toString();
    }

    // Builder class
    public static class Builder {
        private final StructureElement element;

        private Builder() {
            this.element = new StructureElement();
        }

        public Builder name(String name) {
            element.name = name;
            return this;
        }

        public Builder type(String type) {
            element.type = type;
            return this;
        }

        public Builder array(boolean isArray) {
            element.isArray = isArray;
            return this;
        }

        public Builder namespace(String namespace) {
            element.namespace = namespace;
            return this;
        }

        public Builder minOccurs(int minOccurs) {
            element.minOccurs = minOccurs;
            return this;
        }

        public Builder maxOccurs(int maxOccurs) {
            element.maxOccurs = maxOccurs;
            return this;
        }

        public Builder required(boolean required) {
            element.required = required;
            return this;
        }

        public Builder description(String description) {
            element.description = description;
            return this;
        }

        public Builder addChild(StructureElement child) {
            element.addChild(child);
            return this;
        }

        public Builder addAttribute(ElementAttribute attribute) {
            element.addAttribute(attribute);
            return this;
        }

        public Builder addParserData(String key, Object value) {
            element.addParserData(key, value);
            return this;
        }

        public StructureElement build() {
            return element;
        }
    }
}
