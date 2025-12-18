package com.datasabai.services.schemaanalyzer.core.parser;

import com.datasabai.services.schemaanalyzer.core.model.AnalyzerException;
import com.datasabai.services.schemaanalyzer.core.model.FileAnalysisRequest;
import com.datasabai.services.schemaanalyzer.core.model.FileType;
import com.datasabai.services.schemaanalyzer.core.model.StructureElement;

import java.util.List;

/**
 * Interface for file parsers following the Strategy Pattern.
 * <p>
 * Each file type (XML, Excel, CSV, etc.) has its own parser implementation.
 * This allows the system to be easily extended with new file types
 * without modifying existing code.
 * </p>
 *
 * <h3>Implementation Guide:</h3>
 * <ol>
 *   <li>Create a class implementing this interface</li>
 *   <li>Implement {@link #getSupportedFileType()} to return the file type</li>
 *   <li>Implement {@link #parse(FileAnalysisRequest)} to parse files and build StructureElement tree</li>
 *   <li>Implement {@link #canParse(FileAnalysisRequest)} to validate the file content</li>
 *   <li>Implement {@link #mergeStructures(List)} to combine multiple sample files</li>
 *   <li>Register the parser in {@link ParserFactory}</li>
 * </ol>
 *
 * <h3>Example Implementation:</h3>
 * <pre>{@code
 * public class MyFileParser implements FileParser {
 *     @Override
 *     public FileType getSupportedFileType() {
 *         return FileType.MY_TYPE;
 *     }
 *
 *     @Override
 *     public StructureElement parse(FileAnalysisRequest request) {
 *         // Parse file and build structure
 *         return rootElement;
 *     }
 *
 *     @Override
 *     public boolean canParse(FileAnalysisRequest request) {
 *         // Validate file content
 *         return true;
 *     }
 *
 *     @Override
 *     public StructureElement mergeStructures(List<StructureElement> structures) {
 *         // Merge multiple samples
 *         return mergedStructure;
 *     }
 * }
 * }</pre>
 *
 * @see XmlFileParser
 * @see ParserFactory
 */
public interface FileParser {

    /**
     * Gets the file type supported by this parser.
     *
     * @return supported file type
     */
    FileType getSupportedFileType();

    /**
     * Parses the file and returns its structure representation.
     * <p>
     * The implementation should:
     * <ul>
     *   <li>Read the file content from the request</li>
     *   <li>Parse it according to the file type format</li>
     *   <li>Build a {@link StructureElement} tree representing the structure</li>
     *   <li>Infer types for elements (string, integer, boolean, etc.)</li>
     *   <li>Detect arrays if {@code request.isDetectArrays()} is true</li>
     *   <li>Use parser options from {@code request.getParserOptions()}</li>
     * </ul>
     * </p>
     *
     * @param request analysis request containing file content and options
     * @return root structure element representing the file structure
     * @throws AnalyzerException if parsing fails
     */
    StructureElement parse(FileAnalysisRequest request) throws AnalyzerException;

    /**
     * Checks if this parser can parse the given request.
     * <p>
     * This method should perform basic validation:
     * <ul>
     *   <li>Check if file type matches</li>
     *   <li>Validate that content/bytes are provided</li>
     *   <li>Optionally validate file format (e.g., valid XML, CSV header)</li>
     * </ul>
     * </p>
     *
     * @param request analysis request
     * @return true if this parser can handle the request
     */
    boolean canParse(FileAnalysisRequest request);

    /**
     * Merges multiple structure elements from sample files.
     * <p>
     * This method combines structures from multiple sample files to create
     * a more accurate and complete schema. The implementation should:
     * <ul>
     *   <li>Union all elements from all structures</li>
     *   <li>Refine types (if all samples have integer, keep integer)</li>
     *   <li>Mark fields as optional if not present in all samples</li>
     *   <li>Detect arrays consistently across samples</li>
     *   <li>Preserve the most detailed structure</li>
     * </ul>
     * </p>
     *
     * @param structures list of structures from different sample files
     * @return merged structure element
     * @throws AnalyzerException if merging fails
     */
    StructureElement mergeStructures(List<StructureElement> structures) throws AnalyzerException;

    /**
     * Gets a description of available parser options for this file type.
     * <p>
     * Used for documentation and validation.
     * </p>
     *
     * @return map of option name to description
     */
    default java.util.Map<String, String> getAvailableOptions() {
        return java.util.Map.of();
    }

    /**
     * Validates parser options from the request.
     * <p>
     * Override this method to validate custom parser options.
     * </p>
     *
     * @param request analysis request
     * @throws IllegalArgumentException if options are invalid
     */
    default void validateOptions(FileAnalysisRequest request) {
        // Default: no validation
    }
}
