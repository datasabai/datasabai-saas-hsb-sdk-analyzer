package com.datasabai.services.schemaanalyzer.core.parser;

import com.datasabai.services.schemaanalyzer.core.model.AnalyzerException;
import com.datasabai.services.schemaanalyzer.core.model.FileAnalysisRequest;
import com.datasabai.services.schemaanalyzer.core.model.FileType;
import com.datasabai.services.schemaanalyzer.core.model.StructureElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser for variable-length delimited files.
 * <p>
 * Supports two parsing modes:
 * <ul>
 *   <li><b>Mode A - Delimited Fields</b>: Simple delimiter-based parsing (like CSV but more flexible)</li>
 *   <li><b>Mode B - Tag-Value Pairs</b>: Parses tag=value or tag:value format</li>
 * </ul>
 * </p>
 *
 * <h3>Parser Options:</h3>
 * <ul>
 *   <li><b>delimiter</b>: Field delimiter (default: "|")</li>
 *   <li><b>encoding</b>: File encoding (default: "UTF-8")</li>
 *   <li><b>hasHeader</b>: Whether first line contains headers (default: "false")</li>
 *   <li><b>skipLines</b>: Number of lines to skip at beginning (default: "0")</li>
 *   <li><b>quoteChar</b>: Quote character for escaping delimiters (default: "\"")</li>
 *   <li><b>tagValuePairs</b>: Enable tag-value pair mode (default: "false")</li>
 *   <li><b>tagValueDelimiter</b>: Delimiter between tag and value (default: "=")</li>
 * </ul>
 *
 * <h3>Mode A Example (Delimited Fields):</h3>
 * <pre>{@code
 * 001|Product A|19.99|true
 * 002|Product B|29.99|false
 * }</pre>
 *
 * <h3>Mode B Example (Tag-Value Pairs):</h3>
 * <pre>{@code
 * ID=001|NAME=Product A|PRICE=19.99|INSTOCK=true
 * ID=002|NAME=Product B|PRICE=29.99|INSTOCK=false
 * }</pre>
 *
 * @see FileParser
 * @see TypeInferenceUtil
 */
public class VariableLengthFileParser implements FileParser {
    private static final Logger log = LoggerFactory.getLogger(VariableLengthFileParser.class);

    private static final String DEFAULT_DELIMITER = "|";
    private static final String DEFAULT_HAS_HEADER = "false";
    private static final String DEFAULT_SKIP_LINES = "0";
    private static final String DEFAULT_QUOTE_CHAR = "\"";
    private static final String DEFAULT_TAG_VALUE_PAIRS = "false";
    private static final String DEFAULT_TAG_VALUE_DELIMITER = "=";

    @Override
    public FileType getSupportedFileType() {
        return FileType.VARIABLE_LENGTH;
    }

    @Override
    public boolean canParse(FileAnalysisRequest request) {
        if (request == null || request.getFileType() != FileType.VARIABLE_LENGTH) {
            return false;
        }

        String content = request.getFileContent();
        return content != null && !content.isBlank();
    }

    @Override
    public StructureElement parse(FileAnalysisRequest request) throws AnalyzerException {
        log.debug("Starting variable-length parsing for schema: {}", request.getSchemaName());

        try {
            String content = request.getFileContent();
            if (content == null || content.isBlank()) {
                throw new AnalyzerException("INVALID_VARIABLE_LENGTH", FileType.VARIABLE_LENGTH,
                        "No file content provided");
            }

            // Get parser options
            String delimiter = request.getParserOption("delimiter", DEFAULT_DELIMITER);
            boolean hasHeader = Boolean.parseBoolean(request.getParserOption("hasHeader", DEFAULT_HAS_HEADER));
            int skipLines = Integer.parseInt(request.getParserOption("skipLines", DEFAULT_SKIP_LINES));
            String quoteChar = request.getParserOption("quoteChar", DEFAULT_QUOTE_CHAR);
            boolean tagValuePairs = Boolean.parseBoolean(request.getParserOption("tagValuePairs", DEFAULT_TAG_VALUE_PAIRS));
            String tagValueDelimiter = request.getParserOption("tagValueDelimiter", DEFAULT_TAG_VALUE_DELIMITER);

            log.debug("Variable-length options - delimiter: '{}', hasHeader: {}, tagValuePairs: {}, skipLines: {}",
                    delimiter, hasHeader, tagValuePairs, skipLines);

            StructureElement root;

            if (tagValuePairs) {
                // Mode B: Tag-Value Pairs
                root = parseTagValuePairs(content, request.getSchemaName(), delimiter, tagValueDelimiter, skipLines);
            } else {
                // Mode A: Delimited Fields
                root = parseDelimitedFields(content, request.getSchemaName(), delimiter, hasHeader, skipLines, quoteChar);
            }

            log.debug("Variable-length parsing completed successfully");
            return root;

        } catch (AnalyzerException e) {
            throw e;
        } catch (NumberFormatException e) {
            throw new AnalyzerException("INVALID_OPTION", FileType.VARIABLE_LENGTH,
                    "Invalid numeric option value: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new AnalyzerException("PARSE_ERROR", FileType.VARIABLE_LENGTH,
                    "Failed to parse variable-length file: " + e.getMessage(), e);
        }
    }

    /**
     * Parses file using Mode A: Delimited Fields.
     */
    private StructureElement parseDelimitedFields(String content, String schemaName, String delimiter,
                                                   boolean hasHeader, int skipLines, String quoteChar)
            throws AnalyzerException {
        String[] lines = content.split("\\r?\\n");

        if (skipLines >= lines.length) {
            throw new AnalyzerException("INVALID_SKIP_LINES", FileType.VARIABLE_LENGTH,
                    String.format("skipLines (%d) exceeds total lines (%d)", skipLines, lines.length));
        }

        int startIndex = skipLines;
        List<String> headers;
        int dataStartIndex;

        if (hasHeader) {
            // Parse header line
            String headerLine = lines[startIndex];
            headers = splitLine(headerLine, delimiter, quoteChar);
            dataStartIndex = startIndex + 1;
        } else {
            // Determine column count from first data line
            String firstLine = lines[startIndex];
            List<String> fields = splitLine(firstLine, delimiter, quoteChar);
            headers = new ArrayList<>();
            for (int i = 0; i < fields.size(); i++) {
                headers.add("field" + (i + 1));
            }
            dataStartIndex = startIndex;
        }

        log.debug("Delimited mode - headers: {}", headers);

        // Collect values for type inference
        Map<String, List<String>> fieldValues = new LinkedHashMap<>();
        for (String header : headers) {
            fieldValues.put(header, new ArrayList<>());
        }

        // Parse data lines
        for (int i = dataStartIndex; i < lines.length; i++) {
            String line = lines[i];
            if (line.isBlank()) {
                continue;
            }

            List<String> fields = splitLine(line, delimiter, quoteChar);

            for (int j = 0; j < headers.size() && j < fields.size(); j++) {
                String value = fields.get(j);
                if (value != null && !value.isBlank()) {
                    fieldValues.get(headers.get(j)).add(value);
                }
            }
        }

        // Infer types
        Map<String, String> fieldTypes = new LinkedHashMap<>();
        for (String header : headers) {
            String type = inferFieldType(fieldValues.get(header));
            fieldTypes.put(header, type);
            log.debug("Field '{}' inferred type: {}", header, type);
        }

        // Build structure
        return buildStructure(schemaName, headers, fieldTypes);
    }

    /**
     * Parses file using Mode B: Tag-Value Pairs.
     */
    private StructureElement parseTagValuePairs(String content, String schemaName, String pairDelimiter,
                                                 String tagValueDelimiter, int skipLines)
            throws AnalyzerException {
        String[] lines = content.split("\\r?\\n");

        if (skipLines >= lines.length) {
            throw new AnalyzerException("INVALID_SKIP_LINES", FileType.VARIABLE_LENGTH,
                    String.format("skipLines (%d) exceeds total lines (%d)", skipLines, lines.length));
        }

        int startIndex = skipLines;

        // Collect all tags and values
        Map<String, List<String>> tagValues = new LinkedHashMap<>();

        // Pattern to match tag-value pairs
        String patternStr = "([^" + Pattern.quote(pairDelimiter) + Pattern.quote(tagValueDelimiter) + "]+)" +
                Pattern.quote(tagValueDelimiter) +
                "([^" + Pattern.quote(pairDelimiter) + "]*)";
        Pattern pattern = Pattern.compile(patternStr);

        for (int i = startIndex; i < lines.length; i++) {
            String line = lines[i];
            if (line.isBlank()) {
                continue;
            }

            // Split by pair delimiter and parse each tag-value
            String[] pairs = line.split(Pattern.quote(pairDelimiter));
            for (String pair : pairs) {
                pair = pair.trim();
                if (pair.isEmpty()) {
                    continue;
                }

                Matcher matcher = pattern.matcher(pair);
                if (matcher.matches()) {
                    String tag = matcher.group(1).trim();
                    String value = matcher.group(2).trim();

                    tagValues.computeIfAbsent(tag, k -> new ArrayList<>()).add(value);
                } else {
                    log.warn("Invalid tag-value pair: {}", pair);
                }
            }
        }

        log.debug("Tag-value mode - found {} tags", tagValues.size());

        // Infer types for each tag
        Map<String, String> tagTypes = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : tagValues.entrySet()) {
            String tag = entry.getKey();
            String type = inferFieldType(entry.getValue());
            tagTypes.put(tag, type);
            log.debug("Tag '{}' inferred type: {}", tag, type);
        }

        // Build structure
        List<String> tags = new ArrayList<>(tagTypes.keySet());
        return buildStructure(schemaName, tags, tagTypes);
    }

    /**
     * Splits a line by delimiter, respecting quoted values.
     */
    private List<String> splitLine(String line, String delimiter, String quoteChar) {
        List<String> result = new ArrayList<>();

        if (quoteChar == null || quoteChar.isEmpty()) {
            // Simple split if no quote character
            String[] parts = line.split(Pattern.quote(delimiter), -1);
            for (String part : parts) {
                result.add(part.trim());
            }
            return result;
        }

        // Handle quoted values
        char quote = quoteChar.charAt(0);
        char delim = delimiter.charAt(0);
        StringBuilder currentField = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == quote) {
                inQuotes = !inQuotes;
            } else if (c == delim && !inQuotes) {
                result.add(currentField.toString().trim());
                currentField.setLength(0);
            } else {
                currentField.append(c);
            }
        }

        // Add last field
        result.add(currentField.toString().trim());

        return result;
    }

    /**
     * Infers the type of a field from its sample values.
     */
    private String inferFieldType(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "null";
        }

        String mergedType = TypeInferenceUtil.inferType(values.get(0));

        for (int i = 1; i < values.size(); i++) {
            String valueType = TypeInferenceUtil.inferType(values.get(i));
            mergedType = TypeInferenceUtil.mergeTypes(mergedType, valueType);

            if ("string".equals(mergedType)) {
                break;
            }
        }

        return mergedType;
    }

    /**
     * Builds the StructureElement tree.
     * Structure: Root (array) → item (object) → fields
     */
    private StructureElement buildStructure(String schemaName, List<String> fieldNames,
                                            Map<String, String> fieldTypes) {
        // Root element (array of objects)
        StructureElement root = StructureElement.builder()
                .name(schemaName)
                .type("array")
                .array(true)
                .build();

        // Item element (object representing a single record)
        StructureElement item = StructureElement.builder()
                .name("item")
                .type("object")
                .build();

        // Add fields to item
        for (String fieldName : fieldNames) {
            String type = fieldTypes.get(fieldName);
            StructureElement field = StructureElement.builder()
                    .name(fieldName)
                    .type(type)
                    .build();

            item.addChild(field);
        }

        root.addChild(item);

        return root;
    }

    @Override
    public StructureElement mergeStructures(List<StructureElement> structures) throws AnalyzerException {
        if (structures == null || structures.isEmpty()) {
            throw new AnalyzerException("MERGE_ERROR", FileType.VARIABLE_LENGTH, "No structures to merge");
        }

        if (structures.size() == 1) {
            return structures.get(0);
        }

        log.debug("Merging {} variable-length structures", structures.size());

        try {
            // Take first structure as base
            StructureElement base = structures.get(0);
            StructureElement mergedRoot = StructureElement.builder()
                    .name(base.getName())
                    .type("array")
                    .array(true)
                    .build();

            // Collect all fields from all structures
            Map<String, String> allFields = new LinkedHashMap<>();

            for (StructureElement structure : structures) {
                // Navigate to item element
                if (!structure.getChildren().isEmpty()) {
                    StructureElement item = structure.getChildren().get(0);

                    // Merge fields
                    for (StructureElement field : item.getChildren()) {
                        String fieldName = field.getName();
                        String fieldType = field.getType();

                        if (allFields.containsKey(fieldName)) {
                            // Merge types
                            String existingType = allFields.get(fieldName);
                            String mergedType = TypeInferenceUtil.mergeTypes(existingType, fieldType);
                            allFields.put(fieldName, mergedType);
                        } else {
                            // Add new field
                            allFields.put(fieldName, fieldType);
                        }
                    }
                }
            }

            // Build merged item structure
            StructureElement mergedItem = StructureElement.builder()
                    .name("item")
                    .type("object")
                    .build();

            for (Map.Entry<String, String> entry : allFields.entrySet()) {
                StructureElement field = StructureElement.builder()
                        .name(entry.getKey())
                        .type(entry.getValue())
                        .build();
                mergedItem.addChild(field);
            }

            mergedRoot.addChild(mergedItem);

            log.debug("Structure merging completed - total fields: {}", allFields.size());
            return mergedRoot;

        } catch (Exception e) {
            throw new AnalyzerException("MERGE_ERROR", FileType.VARIABLE_LENGTH,
                    "Failed to merge structures: " + e.getMessage(), e);
        }
    }

    @Override
    public Map<String, String> getAvailableOptions() {
        Map<String, String> options = new HashMap<>();
        options.put("delimiter", "Field delimiter (default: |)");
        options.put("encoding", "File encoding (default: UTF-8)");
        options.put("hasHeader", "Whether the first line contains headers (true/false, default: false)");
        options.put("skipLines", "Number of lines to skip at the beginning (default: 0)");
        options.put("quoteChar", "Quote character for escaping delimiters (default: \")");
        options.put("tagValuePairs", "Enable tag-value pair mode (true/false, default: false)");
        options.put("tagValueDelimiter", "Delimiter between tag and value (default: =)");
        return options;
    }
}
