package com.datasabai.services.schemaanalyzer.core.parser;

import com.datasabai.services.schemaanalyzer.core.model.FileType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Factory for creating file parsers based on file type.
 * <p>
 * This class implements the Factory Pattern to provide the appropriate
 * parser for each file type. It maintains a registry of parsers and
 * allows for dynamic registration of new parsers.
 * </p>
 *
 * <h3>Design Pattern: Factory + Strategy</h3>
 * <ul>
 *   <li>Factory Pattern: Creates parser instances based on file type</li>
 *   <li>Strategy Pattern: Each parser implements the same interface</li>
 *   <li>Open/Closed Principle: New file types can be added without modifying existing code</li>
 * </ul>
 *
 * <h3>Usage:</h3>
 * <pre>{@code
 * ParserFactory factory = new ParserFactory();
 * FileParser parser = factory.getParser(FileType.XML);
 * StructureElement structure = parser.parse(request);
 * }</pre>
 *
 * <h3>Adding New Parsers:</h3>
 * <pre>{@code
 * // Create your parser
 * MyFileParser myParser = new MyFileParser();
 *
 * // Register it
 * ParserFactory factory = new ParserFactory();
 * factory.registerParser(myParser);
 *
 * // Use it
 * FileParser parser = factory.getParser(FileType.MY_TYPE);
 * }</pre>
 */
public class ParserFactory {
    private static final Logger log = LoggerFactory.getLogger(ParserFactory.class);

    private final Map<FileType, FileParser> parsers;

    /**
     * Creates a new ParserFactory with default parsers registered.
     */
    public ParserFactory() {
        this.parsers = new ConcurrentHashMap<>();
        registerDefaultParsers();
    }

    /**
     * Registers default parsers for all known file types.
     * <p>
     * This includes both implemented parsers (XML) and stubs (Excel, CSV, etc.).
     * </p>
     */
    private void registerDefaultParsers() {
        // Implemented parsers
        registerParser(new XmlFileParser());
        log.info("Registered XmlFileParser for {}", FileType.XML);

        // Stub parsers (for future implementation)
        registerParser(new ExcelFileParser());
        log.debug("Registered ExcelFileParser (stub) for {}", FileType.EXCEL);

        registerParser(new CsvFileParser());
        log.debug("Registered CsvFileParser (stub) for {}", FileType.CSV);

        log.info("ParserFactory initialized with {} parsers", parsers.size());
    }

    /**
     * Registers a new parser.
     * <p>
     * If a parser for the same file type already exists, it will be replaced.
     * </p>
     *
     * @param parser parser to register
     * @throws IllegalArgumentException if parser is null
     */
    public void registerParser(FileParser parser) {
        if (parser == null) {
            throw new IllegalArgumentException("Parser cannot be null");
        }

        FileType fileType = parser.getSupportedFileType();
        if (fileType == null) {
            throw new IllegalArgumentException("Parser must support a file type");
        }

        FileParser previous = parsers.put(fileType, parser);
        if (previous != null) {
            log.warn("Replaced existing parser for {}: {} -> {}",
                    fileType, previous.getClass().getSimpleName(), parser.getClass().getSimpleName());
        }
    }

    /**
     * Gets a parser for the specified file type.
     *
     * @param fileType file type to get parser for
     * @return parser for the file type
     * @throws IllegalArgumentException if no parser is registered for the file type
     */
    public FileParser getParser(FileType fileType) {
        if (fileType == null) {
            throw new IllegalArgumentException("File type cannot be null");
        }

        FileParser parser = parsers.get(fileType);
        if (parser == null) {
            throw new IllegalArgumentException(
                    "No parser registered for file type: " + fileType +
                    ". Available types: " + getAvailableFileTypes()
            );
        }

        return parser;
    }

    /**
     * Checks if a parser is registered for the specified file type.
     *
     * @param fileType file type to check
     * @return true if a parser is registered
     */
    public boolean hasParser(FileType fileType) {
        return fileType != null && parsers.containsKey(fileType);
    }

    /**
     * Gets all registered file types.
     * <p>
     * This includes both implemented and stub parsers.
     * </p>
     *
     * @return list of all registered file types
     */
    public List<FileType> getRegisteredFileTypes() {
        return new ArrayList<>(parsers.keySet());
    }

    /**
     * Gets only file types with available (implemented) parsers.
     * <p>
     * This filters out stub implementations that throw UnsupportedOperationException.
     * Use this to show users which file types are actually ready to use.
     * </p>
     *
     * @return list of available file types
     */
    public List<FileType> getAvailableFileTypes() {
        List<FileType> available = new ArrayList<>();

        for (Map.Entry<FileType, FileParser> entry : parsers.entrySet()) {
            FileType fileType = entry.getKey();
            FileParser parser = entry.getValue();

            // Test if parser is implemented by checking class type
            // Implemented parsers have full functionality, stubs throw exceptions
            if (isParserImplemented(parser)) {
                available.add(fileType);
            }
        }

        return available;
    }

    /**
     * Checks if a parser is fully implemented (not a stub).
     *
     * @param parser parser to check
     * @return true if implemented
     */
    private boolean isParserImplemented(FileParser parser) {
        // Currently, only XmlFileParser is fully implemented
        // This can be enhanced to check more dynamically
        return parser instanceof XmlFileParser;
    }

    /**
     * Gets all registered parsers.
     *
     * @return map of file type to parser
     */
    public Map<FileType, FileParser> getAllParsers() {
        return new HashMap<>(parsers);
    }

    /**
     * Gets available parser options for a specific file type.
     *
     * @param fileType file type
     * @return map of option name to description
     */
    public Map<String, String> getParserOptions(FileType fileType) {
        FileParser parser = getParser(fileType);
        return parser.getAvailableOptions();
    }

    /**
     * Clears all registered parsers.
     * <p>
     * Used primarily for testing.
     * </p>
     */
    public void clear() {
        parsers.clear();
        log.debug("All parsers cleared");
    }

    /**
     * Resets to default parsers.
     * <p>
     * Clears all parsers and re-registers the default ones.
     * </p>
     */
    public void reset() {
        clear();
        registerDefaultParsers();
        log.debug("ParserFactory reset to defaults");
    }

    /**
     * Gets summary information about registered parsers.
     *
     * @return summary string
     */
    public String getSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("ParserFactory Summary:\n");
        sb.append("  Total registered: ").append(parsers.size()).append("\n");
        sb.append("  Available (implemented): ").append(getAvailableFileTypes().size()).append("\n");
        sb.append("\nRegistered parsers:\n");

        for (Map.Entry<FileType, FileParser> entry : parsers.entrySet()) {
            FileType type = entry.getKey();
            FileParser parser = entry.getValue();
            boolean implemented = isParserImplemented(parser);
            String status = implemented ? "✓ IMPLEMENTED" : "⧗ STUB";

            sb.append(String.format("  - %-10s : %-40s [%s]\n",
                    type.name(),
                    parser.getClass().getSimpleName(),
                    status
            ));
        }

        return sb.toString();
    }
}
