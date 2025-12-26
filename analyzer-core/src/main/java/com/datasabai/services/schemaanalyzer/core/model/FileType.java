package com.datasabai.services.schemaanalyzer.core.model;

import java.util.Arrays;
import java.util.List;

/**
 * Supported file types for schema analysis.
 * <p>
 * This enum defines all file types that can be analyzed by the File Schema Analyzer.
 * Each type has its own parser implementation following the Strategy Pattern.
 * </p>
 *
 * <h3>Current Implementation Status:</h3>
 * <ul>
 *   <li>CSV - Fully implemented with OpenCSV</li>
 *   <li>JSON - Fully implemented with Jackson</li>
 *   <li>FIXED_LENGTH - Fully implemented with structure descriptor support</li>
 *   <li>VARIABLE_LENGTH - Fully implemented with delimiter and tag-value parsing</li>
 * </ul>
 */
public enum FileType {
    /**
     * CSV files (.csv)
     * <p>Status: IMPLEMENTED - Uses OpenCSV for parsing</p>
     */
    CSV("csv", "text/csv", List.of("csv")),

    /**
     * JSON files (.json)
     * <p>Status: IMPLEMENTED - Uses Jackson for parsing</p>
     */
    JSON("json", "application/json", List.of("json")),

    /**
     * Fixed-length files (.txt, .dat, .fix)
     * <p>Status: IMPLEMENTED - Requires field position descriptor</p>
     */
    FIXED_LENGTH("fixed-length", "text/plain", List.of("txt", "dat", "fix")),

    /**
     * Variable-length files (.txt, .dat, .var)
     * <p>Status: IMPLEMENTED - Supports delimited fields and tag-value pairs</p>
     */
    VARIABLE_LENGTH("variable-length", "text/plain", List.of("txt", "dat", "var"));

    private final String code;
    private final String mimeType;
    private final List<String> extensions;

    FileType(String code, String mimeType, List<String> extensions) {
        this.code = code;
        this.mimeType = mimeType;
        this.extensions = extensions;
    }

    /**
     * Gets the technical code for this file type.
     *
     * @return lowercase code (e.g., "csv", "json", "fixed-length")
     */
    public String getCode() {
        return code;
    }

    /**
     * Gets the MIME type for this file type.
     *
     * @return MIME type (e.g., "text/csv", "application/json")
     */
    public String getMimeType() {
        return mimeType;
    }

    /**
     * Gets the list of file extensions supported by this file type.
     *
     * @return list of extensions without the dot (e.g., ["csv"], ["json"])
     */
    public List<String> getExtensions() {
        return extensions;
    }

    /**
     * Finds a FileType by its file extension.
     *
     * @param extension file extension without the dot (e.g., "csv", "json", "txt")
     * @return matching FileType
     * @throws IllegalArgumentException if no matching FileType is found
     */
    public static FileType fromExtension(String extension) {
        if (extension == null || extension.isBlank()) {
            throw new IllegalArgumentException("Extension cannot be null or blank");
        }

        String normalizedExtension = extension.toLowerCase().trim();
        // Remove leading dot if present
        if (normalizedExtension.startsWith(".")) {
            normalizedExtension = normalizedExtension.substring(1);
        }

        for (FileType type : values()) {
            if (type.extensions.contains(normalizedExtension)) {
                return type;
            }
        }

        throw new IllegalArgumentException(
                "Unsupported file extension: " + extension + ". Supported: " + getAllExtensions()
        );
    }

    /**
     * Finds a FileType by its MIME type.
     *
     * @param mimeType MIME type (e.g., "text/csv", "application/json")
     * @return matching FileType
     * @throws IllegalArgumentException if no matching FileType is found
     */
    public static FileType fromMimeType(String mimeType) {
        if (mimeType == null || mimeType.isBlank()) {
            throw new IllegalArgumentException("MIME type cannot be null or blank");
        }

        String normalizedMimeType = mimeType.toLowerCase().trim();

        for (FileType type : values()) {
            if (type.mimeType.equalsIgnoreCase(normalizedMimeType)) {
                return type;
            }
        }

        throw new IllegalArgumentException(
                "Unsupported MIME type: " + mimeType + ". Supported: " + getAllMimeTypes()
        );
    }

    /**
     * Gets all supported file extensions across all file types.
     *
     * @return list of all supported extensions
     */
    public static List<String> getAllExtensions() {
        return Arrays.stream(values())
                .flatMap(type -> type.extensions.stream())
                .toList();
    }

    /**
     * Gets all supported MIME types across all file types.
     *
     * @return list of all supported MIME types
     */
    public static List<String> getAllMimeTypes() {
        return Arrays.stream(values())
                .map(FileType::getMimeType)
                .toList();
    }

    /**
     * Checks if this file type has a specific extension.
     *
     * @param extension extension to check (without dot)
     * @return true if this file type supports the extension
     */
    public boolean hasExtension(String extension) {
        if (extension == null) {
            return false;
        }
        String normalized = extension.toLowerCase().trim();
        if (normalized.startsWith(".")) {
            normalized = normalized.substring(1);
        }
        return extensions.contains(normalized);
    }
}
