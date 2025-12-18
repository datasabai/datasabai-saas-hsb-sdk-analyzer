package com.datasabai.services.schemaanalyzer.core.model;

/**
 * Exception thrown during file schema analysis.
 * <p>
 * This exception encapsulates errors that occur during parsing,
 * schema generation, or validation processes.
 * </p>
 */
public class AnalyzerException extends RuntimeException {
    private final String errorCode;
    private final FileType fileType;

    public AnalyzerException(String message) {
        super(message);
        this.errorCode = "ANALYZER_ERROR";
        this.fileType = null;
    }

    public AnalyzerException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "ANALYZER_ERROR";
        this.fileType = null;
    }

    public AnalyzerException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.fileType = null;
    }

    public AnalyzerException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.fileType = null;
    }

    public AnalyzerException(String errorCode, FileType fileType, String message) {
        super(message);
        this.errorCode = errorCode;
        this.fileType = fileType;
    }

    public AnalyzerException(String errorCode, FileType fileType, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.fileType = fileType;
    }

    /**
     * Gets the error code.
     *
     * @return error code
     */
    public String getErrorCode() {
        return errorCode;
    }

    /**
     * Gets the file type related to this error.
     *
     * @return file type or null if not applicable
     */
    public FileType getFileType() {
        return fileType;
    }

    /**
     * Gets a formatted error message with code and file type.
     *
     * @return formatted error message
     */
    public String getFormattedMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(errorCode).append("]");
        if (fileType != null) {
            sb.append(" [").append(fileType.name()).append("]");
        }
        sb.append(" ").append(getMessage());
        return sb.toString();
    }

    @Override
    public String toString() {
        return getFormattedMessage();
    }
}
