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
 *   <li>XML - Fully implemented</li>
 *   <li>EXCEL - Architecture ready, stub implementation</li>
 *   <li>CSV - Architecture ready, stub implementation</li>
 *   <li>TXT - Architecture ready, stub implementation</li>
 *   <li>JSON - Architecture ready, stub implementation</li>
 * </ul>
 */
public enum FileType {
    /**
     * XML files (.xml)
     * <p>Status: IMPLEMENTED</p>
     */
    XML("xml", "application/xml", List.of("xml")),

    /**
     * Excel files (.xls, .xlsx)
     * <p>Status: STUB - Not yet implemented</p>
     */
    EXCEL("excel", "application/vnd.ms-excel", List.of("xls", "xlsx")),

    /**
     * CSV files (.csv)
     * <p>Status: STUB - Not yet implemented</p>
     */
    CSV("csv", "text/csv", List.of("csv")),

    /**
     * Text files (.txt)
     * <p>Status: STUB - Not yet implemented</p>
     */
    TXT("txt", "text/plain", List.of("txt")),

    /**
     * JSON files (.json)
     * <p>Status: STUB - Not yet implemented</p>
     */
    JSON("json", "application/json", List.of("json"));

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
     * @return lowercase code (e.g., "xml", "excel")
     */
    public String getCode() {
        return code;
    }

    /**
     * Gets the MIME type for this file type.
     *
     * @return MIME type (e.g., "application/xml")
     */
    public String getMimeType() {
        return mimeType;
    }

    /**
     * Gets the list of file extensions supported by this file type.
     *
     * @return list of extensions without the dot (e.g., ["xml"])
     */
    public List<String> getExtensions() {
        return extensions;
    }

    /**
     * Finds a FileType by its file extension.
     *
     * @param extension file extension without the dot (e.g., "xml", "xlsx")
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
     * @param mimeType MIME type (e.g., "application/xml")
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

        // Handle common Excel MIME types
        if (normalizedMimeType.contains("spreadsheet") ||
            normalizedMimeType.contains("excel") ||
            normalizedMimeType.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")) {
            return EXCEL;
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
