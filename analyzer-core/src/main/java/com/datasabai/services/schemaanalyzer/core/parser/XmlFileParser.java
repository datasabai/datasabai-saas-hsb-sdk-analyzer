package com.datasabai.services.schemaanalyzer.core.parser;

import com.datasabai.services.schemaanalyzer.core.model.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Parser for XML files.
 * <p>
 * This implementation uses Jackson XML to parse XML files and build
 * a {@link StructureElement} tree representing the XML structure.
 * </p>
 *
 * <h3>Features:</h3>
 * <ul>
 *   <li>Parses XML elements and attributes</li>
 *   <li>Detects namespaces</li>
 *   <li>Infers types (string, integer, boolean, date, etc.)</li>
 *   <li>Detects arrays (repeating elements)</li>
 *   <li>Merges multiple sample files for better schema inference</li>
 * </ul>
 *
 * <h3>Parser Options:</h3>
 * <ul>
 *   <li><b>preserveNamespaces</b>: Include namespace prefixes (default: true)</li>
 *   <li><b>includeAttributes</b>: Include XML attributes (default: true)</li>
 *   <li><b>detectCDATA</b>: Detect CDATA sections (default: true)</li>
 * </ul>
 */
public class XmlFileParser implements FileParser {
    private static final Logger log = LoggerFactory.getLogger(XmlFileParser.class);

    private static final Pattern INTEGER_PATTERN = Pattern.compile("^-?\\d+$");
    private static final Pattern DECIMAL_PATTERN = Pattern.compile("^-?\\d+\\.\\d+$");
    private static final Pattern BOOLEAN_PATTERN = Pattern.compile("^(true|false)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern DATE_PATTERN = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}.*");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    private final XmlMapper xmlMapper;

    public XmlFileParser() {
        this.xmlMapper = new XmlMapper();
    }

    @Override
    public FileType getSupportedFileType() {
        return FileType.XML;
    }

    @Override
    public boolean canParse(FileAnalysisRequest request) {
        if (request == null || request.getFileType() != FileType.XML) {
            return false;
        }

        String content = request.getFileContent();
        if (content == null || content.isBlank()) {
            return false;
        }

        // Basic XML validation
        content = content.trim();
        return content.startsWith("<") && content.contains(">");
    }

    @Override
    public StructureElement parse(FileAnalysisRequest request) throws AnalyzerException {
        log.debug("Parsing XML file: {}", request.getSchemaName());

        if (!canParse(request)) {
            throw new AnalyzerException(
                    "INVALID_XML",
                    FileType.XML,
                    "Invalid XML content or request"
            );
        }

        try {
            validateOptions(request);
            String content = request.getFileContent();
            JsonNode rootNode = xmlMapper.readTree(content);

            boolean preserveNamespaces = "true".equalsIgnoreCase(
                    request.getParserOption("preserveNamespaces", "true")
            );
            boolean includeAttributes = "true".equalsIgnoreCase(
                    request.getParserOption("includeAttributes", "true")
            );

            StructureElement root = parseNode(
                    rootNode.fields().next(),
                    request.isDetectArrays(),
                    preserveNamespaces,
                    includeAttributes
            );

            log.debug("XML parsing completed: {} elements found", countElements(root));
            return root;

        } catch (Exception e) {
            throw new AnalyzerException(
                    "PARSE_ERROR",
                    FileType.XML,
                    "Failed to parse XML: " + e.getMessage(),
                    e
            );
        }
    }

    @Override
    public StructureElement mergeStructures(List<StructureElement> structures) throws AnalyzerException {
        if (structures == null || structures.isEmpty()) {
            throw new AnalyzerException("MERGE_ERROR", "No structures to merge");
        }

        if (structures.size() == 1) {
            return structures.get(0);
        }

        log.debug("Merging {} XML structures", structures.size());

        StructureElement merged = structures.get(0);
        for (int i = 1; i < structures.size(); i++) {
            merged = mergeTwoStructures(merged, structures.get(i), structures.size());
        }

        return merged;
    }

    @Override
    public Map<String, String> getAvailableOptions() {
        Map<String, String> options = new HashMap<>();
        options.put("preserveNamespaces", "Include namespace prefixes in element names (true/false, default: true)");
        options.put("includeAttributes", "Include XML attributes in the schema (true/false, default: true)");
        options.put("detectCDATA", "Detect and mark CDATA sections (true/false, default: true)");
        return options;
    }

    @Override
    public void validateOptions(FileAnalysisRequest request) {
        // Validate boolean options
        validateBooleanOption(request, "preserveNamespaces");
        validateBooleanOption(request, "includeAttributes");
        validateBooleanOption(request, "detectCDATA");
    }

    // Private helper methods

    private StructureElement parseNode(
            Map.Entry<String, JsonNode> entry,
            boolean detectArrays,
            boolean preserveNamespaces,
            boolean includeAttributes
    ) {
        String nodeName = entry.getKey();
        JsonNode node = entry.getValue();

        StructureElement element = new StructureElement();
        element.setName(extractLocalName(nodeName, preserveNamespaces));

        if (preserveNamespaces) {
            String namespace = extractNamespace(nodeName);
            if (namespace != null) {
                element.setNamespace(namespace);
            }
        }

        if (node.isArray()) {
            // Array node
            element.setType("array");
            element.setArray(true);

            if (node.size() > 0) {
                // Analyze first element to determine array item type
                JsonNode firstItem = node.get(0);
                StructureElement itemElement = parseNodeValue(
                        nodeName,
                        firstItem,
                        detectArrays,
                        preserveNamespaces,
                        includeAttributes
                );
                element.addChild(itemElement);
            }

        } else {
            // Single node
            StructureElement parsedElement = parseNodeValue(
                    nodeName,
                    node,
                    detectArrays,
                    preserveNamespaces,
                    includeAttributes
            );
            element.setType(parsedElement.getType());
            element.setChildren(parsedElement.getChildren());
            element.setAttributes(parsedElement.getAttributes());
        }

        return element;
    }

    private StructureElement parseNodeValue(
            String nodeName,
            JsonNode node,
            boolean detectArrays,
            boolean preserveNamespaces,
            boolean includeAttributes
    ) {
        StructureElement element = new StructureElement();
        element.setName(extractLocalName(nodeName, preserveNamespaces));

        if (node.isObject()) {
            element.setType("object");

            // Parse child elements
            Map<String, List<JsonNode>> childGroups = groupChildrenByName(node);

            for (Map.Entry<String, List<JsonNode>> childGroup : childGroups.entrySet()) {
                String childName = childGroup.getKey();
                List<JsonNode> childNodes = childGroup.getValue();

                // Handle attributes (fields starting with @)
                if (includeAttributes && childName.startsWith("@")) {
                    String attrName = childName.substring(1);
                    JsonNode attrNode = childNodes.get(0);
                    ElementAttribute attr = new ElementAttribute(
                            attrName,
                            inferType(attrNode.asText()),
                            true
                    );
                    element.addAttribute(attr);
                    continue;
                }

                // Handle text content (empty string key or special keys)
                if (childName.isEmpty() || childName.equals("") || childName.equals("#text")) {
                    JsonNode textNode = childNodes.get(0);
                    if (textNode.isTextual()) {
                        element.setType(inferType(textNode.asText()));
                    }
                    continue;
                }

                // Detect arrays
                boolean isArray = detectArrays && childNodes.size() > 1;

                if (isArray) {
                    // Create array element
                    StructureElement arrayElement = new StructureElement(childName, "array");
                    arrayElement.setArray(true);

                    // Analyze first child to determine item type
                    StructureElement itemElement = parseNodeValue(
                            childName,
                            childNodes.get(0),
                            detectArrays,
                            preserveNamespaces,
                            includeAttributes
                    );
                    arrayElement.addChild(itemElement);

                    element.addChild(arrayElement);
                } else {
                    // Single child element
                    StructureElement childElement = parseNodeValue(
                            childName,
                            childNodes.get(0),
                            detectArrays,
                            preserveNamespaces,
                            includeAttributes
                    );
                    element.addChild(childElement);
                }
            }

        } else if (node.isTextual()) {
            element.setType(inferType(node.asText()));
        } else if (node.isNumber()) {
            element.setType(node.isIntegralNumber() ? "integer" : "number");
        } else if (node.isBoolean()) {
            element.setType("boolean");
        } else if (node.isNull()) {
            element.setType("null");
        } else {
            element.setType("string");
        }

        return element;
    }

    private Map<String, List<JsonNode>> groupChildrenByName(JsonNode node) {
        Map<String, List<JsonNode>> groups = new LinkedHashMap<>();

        Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            String fieldName = field.getKey();
            JsonNode fieldValue = field.getValue();

            if (fieldValue.isArray()) {
                // Array field - add all items to group
                List<JsonNode> items = new ArrayList<>();
                fieldValue.forEach(items::add);
                groups.put(fieldName, items);
            } else {
                // Single field
                groups.computeIfAbsent(fieldName, k -> new ArrayList<>()).add(fieldValue);
            }
        }

        return groups;
    }

    private String inferType(String value) {
        if (value == null || value.isBlank()) {
            return "string";
        }

        value = value.trim();

        if (BOOLEAN_PATTERN.matcher(value).matches()) {
            return "boolean";
        }
        if (INTEGER_PATTERN.matcher(value).matches()) {
            return "integer";
        }
        if (DECIMAL_PATTERN.matcher(value).matches()) {
            return "number";
        }
        if (DATE_PATTERN.matcher(value).matches()) {
            return "string"; // Could be enhanced with format: date-time
        }
        if (EMAIL_PATTERN.matcher(value).matches()) {
            return "string"; // Could be enhanced with format: email
        }

        return "string";
    }

    private String extractLocalName(String name, boolean preserveNamespace) {
        if (!preserveNamespace && name.contains(":")) {
            return name.substring(name.indexOf(":") + 1);
        }
        return name;
    }

    private String extractNamespace(String name) {
        if (name.contains(":")) {
            return name.substring(0, name.indexOf(":"));
        }
        return null;
    }

    private StructureElement mergeTwoStructures(
            StructureElement struct1,
            StructureElement struct2,
            int totalSamples
    ) {
        StructureElement merged = new StructureElement();
        merged.setName(struct1.getName());
        merged.setNamespace(struct1.getNamespace());

        // Merge types
        String mergedType = mergeTypes(struct1.getType(), struct2.getType());
        merged.setType(mergedType);

        // Merge array flags
        merged.setArray(struct1.isArray() || struct2.isArray());

        // Merge attributes
        merged.setAttributes(mergeAttributes(struct1.getAttributes(), struct2.getAttributes()));

        // Merge children
        merged.setChildren(mergeChildren(struct1.getChildren(), struct2.getChildren(), totalSamples));

        return merged;
    }

    private String mergeTypes(String type1, String type2) {
        if (type1.equals(type2)) {
            return type1;
        }

        // If types differ, use more general type
        if ("object".equals(type1) || "object".equals(type2)) {
            return "object";
        }
        if ("array".equals(type1) || "array".equals(type2)) {
            return "array";
        }
        if ("number".equals(type1) && "integer".equals(type2)) {
            return "number";
        }
        if ("integer".equals(type1) && "number".equals(type2)) {
            return "number";
        }

        // Default to string for incompatible types
        return "string";
    }

    private List<ElementAttribute> mergeAttributes(
            List<ElementAttribute> attrs1,
            List<ElementAttribute> attrs2
    ) {
        Map<String, ElementAttribute> mergedMap = new HashMap<>();

        // Add all from first
        if (attrs1 != null) {
            attrs1.forEach(attr -> mergedMap.put(attr.getName(), attr));
        }

        // Merge with second
        if (attrs2 != null) {
            for (ElementAttribute attr2 : attrs2) {
                if (mergedMap.containsKey(attr2.getName())) {
                    ElementAttribute attr1 = mergedMap.get(attr2.getName());
                    // Merge types if different
                    String mergedType = mergeTypes(attr1.getType(), attr2.getType());
                    attr1.setType(mergedType);
                } else {
                    // New attribute - mark as optional
                    attr2.setRequired(false);
                    mergedMap.put(attr2.getName(), attr2);
                }
            }
        }

        return new ArrayList<>(mergedMap.values());
    }

    private List<StructureElement> mergeChildren(
            List<StructureElement> children1,
            List<StructureElement> children2,
            int totalSamples
    ) {
        Map<String, StructureElement> mergedMap = new LinkedHashMap<>();

        // Add all from first
        if (children1 != null) {
            children1.forEach(child -> mergedMap.put(child.getName(), child));
        }

        // Merge with second
        if (children2 != null) {
            for (StructureElement child2 : children2) {
                if (mergedMap.containsKey(child2.getName())) {
                    StructureElement child1 = mergedMap.get(child2.getName());
                    // Recursively merge
                    StructureElement merged = mergeTwoStructures(child1, child2, totalSamples);
                    mergedMap.put(merged.getName(), merged);
                } else {
                    // New child - mark as optional
                    child2.setRequired(false);
                    mergedMap.put(child2.getName(), child2);
                }
            }
        }

        return new ArrayList<>(mergedMap.values());
    }

    private int countElements(StructureElement element) {
        if (element == null) {
            return 0;
        }

        int count = 1;
        if (element.hasChildren()) {
            for (StructureElement child : element.getChildren()) {
                count += countElements(child);
            }
        }
        return count;
    }

    private void validateBooleanOption(FileAnalysisRequest request, String optionName) {
        String value = request.getParserOption(optionName);
        if (value != null && !value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("false")) {
            throw new IllegalArgumentException(
                    "Invalid value for option '" + optionName + "': " + value + ". Must be 'true' or 'false'."
            );
        }
    }
}
