package com.datasabai.services.schemaanalyzer.core.parser;

import com.datasabai.services.schemaanalyzer.core.model.AnalyzerException;
import com.datasabai.services.schemaanalyzer.core.model.FileAnalysisRequest;
import com.datasabai.services.schemaanalyzer.core.model.FileType;
import com.datasabai.services.schemaanalyzer.core.model.FixedLengthDescriptor;
import com.datasabai.services.schemaanalyzer.core.model.StructureElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Parser for fixed-length files.
 * <p>
 * Parses fixed-length files using a descriptor that defines field positions and lengths.
 * Supports both external descriptor files and inline field definitions.
 * </p>
 *
 * <h3>Parser Options:</h3>
 * <ul>
 *   <li><b>descriptorFile</b>: Content of descriptor JSON file (default: null)</li>
 *   <li><b>fieldDefinitions</b>: Inline field definitions in JSON array format (default: null)</li>
 *   <li><b>encoding</b>: File encoding (default: "UTF-8")</li>
 *   <li><b>skipLines</b>: Number of lines to skip at beginning (default: "0")</li>
 *   <li><b>trimFields</b>: Whether to trim whitespace from fields (default: "true")</li>
 *   <li><b>recordLength</b>: Expected record length for validation (default: null)</li>
 * </ul>
 *
 * <h3>Descriptor Format:</h3>
 * <pre>{@code
 * [
 *   {"name": "id", "start": 0, "length": 5, "type": "integer"},
 *   {"name": "name", "start": 5, "length": 20, "type": "string"},
 *   {"name": "amount", "start": 25, "length": 10, "type": "number"}
 * ]
 * }</pre>
 *
 * <h3>Structure Generated:</h3>
 * <pre>{@code
 * - Root: type="array", name=schemaName
 *   - item: type="object"
 *     - id: type="integer"
 *     - name: type="string"
 *     - amount: type="number"
 * }</pre>
 *
 * @see FileParser
 * @see FixedLengthDescriptor
 */
public class FixedLengthFileParser implements FileParser {
    private static final Logger log = LoggerFactory.getLogger(FixedLengthFileParser.class);

    private static final String DEFAULT_SKIP_LINES = "0";
    private static final String DEFAULT_TRIM_FIELDS = "true";

    @Override
    public FileType getSupportedFileType() {
        return FileType.FIXED_LENGTH;
    }

    @Override
    public boolean canParse(FileAnalysisRequest request) {
        if (request == null || request.getFileType() != FileType.FIXED_LENGTH) {
            return false;
        }

        String content = request.getFileContent();
        if (content == null || content.isBlank()) {
            return false;
        }

        // Must have at least one of: descriptorFile or fieldDefinitions
        String descriptorFile = request.getParserOption("descriptorFile", null);
        String fieldDefinitions = request.getParserOption("fieldDefinitions", null);

        return descriptorFile != null || fieldDefinitions != null;
    }

    @Override
    public StructureElement parse(FileAnalysisRequest request) throws AnalyzerException {
        log.debug("Starting fixed-length parsing for schema: {}", request.getSchemaName());

        try {
            String content = request.getFileContent();
            if (content == null || content.isBlank()) {
                throw new AnalyzerException("INVALID_FIXED_LENGTH", FileType.FIXED_LENGTH, "No file content provided");
            }

            // Get parser options
            String descriptorFile = request.getParserOption("descriptorFile", null);
            String fieldDefinitions = request.getParserOption("fieldDefinitions", null);
            int skipLines = Integer.parseInt(request.getParserOption("skipLines", DEFAULT_SKIP_LINES));
            boolean trimFields = Boolean.parseBoolean(request.getParserOption("trimFields", DEFAULT_TRIM_FIELDS));
            String recordLengthStr = request.getParserOption("recordLength", null);
            Integer recordLength = recordLengthStr != null ? Integer.parseInt(recordLengthStr) : null;

            log.debug("Fixed-length options - skipLines: {}, trimFields: {}, recordLength: {}",
                    skipLines, trimFields, recordLength);

            // Load descriptor
            FixedLengthDescriptor descriptor = loadDescriptor(descriptorFile, fieldDefinitions);

            // Split content into lines
            String[] lines = content.split("\\r?\\n");

            // Skip lines
            int startIndex = skipLines;
            if (startIndex >= lines.length) {
                throw new AnalyzerException("INVALID_SKIP_LINES", FileType.FIXED_LENGTH,
                        String.format("skipLines (%d) exceeds total lines (%d)", skipLines, lines.length));
            }

            // Collect values for type inference
            Map<String, List<String>> fieldValues = new LinkedHashMap<>();
            for (FixedLengthDescriptor.FieldDefinition field : descriptor.getFields()) {
                fieldValues.put(field.getName(), new ArrayList<>());
            }

            // Parse lines
            for (int i = startIndex; i < lines.length; i++) {
                String line = lines[i];

                // Validate record length if specified
                if (recordLength != null && line.length() != recordLength) {
                    log.warn("Line {} has length {} (expected {})", i + 1, line.length(), recordLength);
                }

                // Extract fields
                for (FixedLengthDescriptor.FieldDefinition field : descriptor.getFields()) {
                    String value = extractField(line, field, trimFields);
                    if (value != null && !value.isBlank()) {
                        fieldValues.get(field.getName()).add(value);
                    }
                }
            }

            // Infer types for fields without explicit types
            Map<String, String> fieldTypes = new LinkedHashMap<>();
            for (FixedLengthDescriptor.FieldDefinition field : descriptor.getFields()) {
                String type;
                if (field.getType() != null && !field.getType().isBlank()) {
                    // Use explicit type
                    type = field.getType();
                } else {
                    // Infer type from values
                    type = inferFieldType(fieldValues.get(field.getName()));
                }
                fieldTypes.put(field.getName(), type);
                log.debug("Field '{}' type: {}", field.getName(), type);
            }

            // Build structure
            StructureElement root = buildStructure(request.getSchemaName(), descriptor.getFields(), fieldTypes);

            log.debug("Fixed-length parsing completed successfully");
            return root;

        } catch (AnalyzerException e) {
            throw e;
        } catch (NumberFormatException e) {
            throw new AnalyzerException("INVALID_OPTION", FileType.FIXED_LENGTH,
                    "Invalid numeric option value: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new AnalyzerException("PARSE_ERROR", FileType.FIXED_LENGTH,
                    "Failed to parse fixed-length file: " + e.getMessage(), e);
        }
    }

    /**
     * Loads descriptor from descriptorFile or fieldDefinitions option.
     */
    private FixedLengthDescriptor loadDescriptor(String descriptorFile, String fieldDefinitions)
            throws AnalyzerException {
        try {
            FixedLengthDescriptor descriptor;

            if (fieldDefinitions != null && !fieldDefinitions.isBlank()) {
                // Use inline field definitions
                log.debug("Loading descriptor from fieldDefinitions");
                descriptor = FixedLengthDescriptor.fromJson(fieldDefinitions);
            } else if (descriptorFile != null && !descriptorFile.isBlank()) {
                // Use external descriptor file content
                log.debug("Loading descriptor from descriptorFile");
                descriptor = FixedLengthDescriptor.fromJson(descriptorFile);
            } else {
                throw new AnalyzerException("MISSING_DESCRIPTOR", FileType.FIXED_LENGTH,
                        "Either descriptorFile or fieldDefinitions must be provided");
            }

            // Validate descriptor
            descriptor.validate();

            log.debug("Loaded descriptor with {} fields", descriptor.getFields().size());
            return descriptor;

        } catch (AnalyzerException e) {
            throw e;
        } catch (IllegalArgumentException e) {
            throw new AnalyzerException("INVALID_DESCRIPTOR", FileType.FIXED_LENGTH,
                    "Invalid descriptor: " + e.getMessage(), e);
        }
    }

    /**
     * Extracts a field value from a line.
     */
    private String extractField(String line, FixedLengthDescriptor.FieldDefinition field, boolean trimFields) {
        int start = field.getStart();
        int length = field.getLength();

        // Handle line too short
        if (start >= line.length()) {
            return "";
        }

        int end = Math.min(start + length, line.length());
        String value = line.substring(start, end);

        if (trimFields || field.isTrim()) {
            value = value.trim();
        }

        return value;
    }

    /**
     * Infers the type of a field from its sample values.
     */
    private String inferFieldType(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "null";
        }

        // Infer type from first value, then merge with others
        String mergedType = TypeInferenceUtil.inferType(values.get(0));

        for (int i = 1; i < values.size(); i++) {
            String valueType = TypeInferenceUtil.inferType(values.get(i));
            mergedType = TypeInferenceUtil.mergeTypes(mergedType, valueType);

            // Early exit if type widened to string
            if ("string".equals(mergedType)) {
                break;
            }
        }

        return mergedType;
    }

    /**
     * Builds the StructureElement tree for fixed-length.
     * Structure: Root (array) → item (object) → fields
     */
    private StructureElement buildStructure(String schemaName,
                                            List<FixedLengthDescriptor.FieldDefinition> fields,
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
        for (FixedLengthDescriptor.FieldDefinition field : fields) {
            String type = fieldTypes.get(field.getName());
            StructureElement fieldElement = StructureElement.builder()
                    .name(field.getName())
                    .type(type)
                    .build();

            item.addChild(fieldElement);
        }

        root.addChild(item);

        return root;
    }

    @Override
    public StructureElement mergeStructures(List<StructureElement> structures) throws AnalyzerException {
        if (structures == null || structures.isEmpty()) {
            throw new AnalyzerException("MERGE_ERROR", FileType.FIXED_LENGTH, "No structures to merge");
        }

        if (structures.size() == 1) {
            return structures.get(0);
        }

        log.debug("Merging {} fixed-length structures", structures.size());

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
            throw new AnalyzerException("MERGE_ERROR", FileType.FIXED_LENGTH,
                    "Failed to merge structures: " + e.getMessage(), e);
        }
    }

    @Override
    public Map<String, String> getAvailableOptions() {
        Map<String, String> options = new HashMap<>();
        options.put("descriptorFile", "Content of descriptor JSON file (default: null)");
        options.put("fieldDefinitions", "Inline field definitions in JSON array format (default: null)");
        options.put("encoding", "File encoding (default: UTF-8)");
        options.put("skipLines", "Number of lines to skip at the beginning (default: 0)");
        options.put("trimFields", "Whether to trim whitespace from fields (true/false, default: true)");
        options.put("recordLength", "Expected record length for validation (default: null)");
        return options;
    }
}
