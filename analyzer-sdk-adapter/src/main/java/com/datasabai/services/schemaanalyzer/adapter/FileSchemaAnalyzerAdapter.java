package com.datasabai.services.schemaanalyzer.adapter;

import com.datasabai.hsb.sdk.SdkContext;
import com.datasabai.hsb.sdk.SdkModule;
import com.datasabai.services.schemaanalyzer.core.FileSchemaAnalyzer;
import com.datasabai.services.schemaanalyzer.core.model.AnalyzerException;
import com.datasabai.services.schemaanalyzer.core.model.FileAnalysisRequest;
import com.datasabai.services.schemaanalyzer.core.model.SchemaGenerationResult;

import java.util.HashMap;
import java.util.Map;

/**
 * SDK Adapter for the File Schema Analyzer service.
 * <p>
 * <b>PURE JAVA IMPLEMENTATION - NO FRAMEWORK ANNOTATIONS</b>
 * </p>
 * <p>
 * This adapter integrates the File Schema Analyzer with the Datasabai HSB SDK
 * following the "Model B - Adapters" architecture. It implements the
 * {@link SdkModule} interface to provide CLI, REST, and UI access through
 * the SDK runtime.
 * </p>
 *
 * <h3>Architecture:</h3>
 * <pre>
 * SDK Runtime (Quarkus)
 *        ↓
 * FileSchemaAnalyzerAdapter (Pure Java)
 *        ↓
 * FileSchemaAnalyzer (Pure Java)
 *        ↓
 * Parsers (Pure Java)
 * </pre>
 *
 * <h3>Configuration via SdkContext:</h3>
 * <ul>
 *   <li><b>detectArrays</b>: Override default array detection (true/false)</li>
 *   <li><b>optimizeForBeanIO</b>: Override BeanIO optimization (true/false)</li>
 *   <li><b>parserOptions.*</b>: Parser-specific options</li>
 *     <ul>
 *       <li>parserOptions.preserveNamespaces (XML)</li>
 *       <li>parserOptions.delimiter (CSV)</li>
 *       <li>parserOptions.sheetName (Excel)</li>
 *     </ul>
 * </ul>
 *
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * // From SDK Runtime
 * SdkModule<FileAnalysisRequest, SchemaGenerationResult> module =
 *     new FileSchemaAnalyzerAdapter();
 *
 * FileAnalysisRequest request = FileAnalysisRequest.builder()
 *     .fileType(FileType.XML)
 *     .fileContent("<customer><id>123</id></customer>")
 *     .schemaName("Customer")
 *     .build();
 *
 * SdkContext context = new SdkContext();
 * SchemaGenerationResult result = module.execute(request, context);
 * }</pre>
 *
 * <h3>Thread Safety:</h3>
 * <p>
 * This adapter is thread-safe. Multiple threads can call {@link #execute(FileAnalysisRequest, SdkContext)}
 * concurrently.
 * </p>
 *
 * @see SdkModule
 * @see FileSchemaAnalyzer
 */
public class FileSchemaAnalyzerAdapter implements SdkModule<FileAnalysisRequest, SchemaGenerationResult> {

    private final FileSchemaAnalyzer analyzer;

    /**
     * Creates a new adapter with a default FileSchemaAnalyzer instance.
     * <p>
     * NO DEPENDENCY INJECTION - Pure constructor initialization.
     * </p>
     */
    public FileSchemaAnalyzerAdapter() {
        this.analyzer = new FileSchemaAnalyzer();
    }

    /**
     * Creates a new adapter with a custom FileSchemaAnalyzer instance.
     * <p>
     * Useful for testing or custom configurations.
     * </p>
     *
     * @param analyzer file schema analyzer instance
     */
    public FileSchemaAnalyzerAdapter(FileSchemaAnalyzer analyzer) {
        if (analyzer == null) {
            throw new IllegalArgumentException("FileSchemaAnalyzer cannot be null");
        }
        this.analyzer = analyzer;
    }

    @Override
    public String name() {
        return "file-schema-analyzer";
    }

    @Override
    public String description() {
        return "Analyzes file schemas (XML, Excel, CSV, etc.) and generates JSON Schemas for BeanIO configuration";
    }

    @Override
    public String version() {
        return "1.0.0-SNAPSHOT";
    }

    @Override
    public SchemaGenerationResult execute(FileAnalysisRequest input, SdkContext context) throws Exception {
        if (input == null) {
            throw new IllegalArgumentException("Input request cannot be null");
        }

        // Apply configuration from SdkContext if provided
        if (context != null) {
            applyContextConfiguration(input, context);
        }

        try {
            // Execute analysis
            return analyzer.analyze(input);

        } catch (AnalyzerException e) {
            // Wrap in Exception for SDK compatibility
            throw new Exception(
                    "File schema analysis failed: " + e.getMessage(),
                    e
            );

        } catch (UnsupportedOperationException e) {
            // Handle stub parsers
            throw new Exception(
                    "File type not yet supported: " + e.getMessage() +
                    " Available types: " + analyzer.getAvailableFileTypes(),
                    e
            );
        }
    }

    @Override
    public Class<FileAnalysisRequest> getInputType() {
        return FileAnalysisRequest.class;
    }

    @Override
    public Class<SchemaGenerationResult> getOutputType() {
        return SchemaGenerationResult.class;
    }

    @Override
    public Map<String, String> getConfigurationSchema() {
        Map<String, String> schema = new HashMap<>();

        // General configuration
        schema.put("detectArrays", "Enable automatic array detection (true/false, default: true)");
        schema.put("optimizeForBeanIO", "Optimize schema for BeanIO generation (true/false, default: true)");

        // Parser-specific options
        schema.put("parserOptions.preserveNamespaces", "XML: Preserve namespace prefixes (true/false, default: true)");
        schema.put("parserOptions.includeAttributes", "XML: Include XML attributes (true/false, default: true)");
        schema.put("parserOptions.delimiter", "CSV: Column delimiter (default: ,)");
        schema.put("parserOptions.hasHeader", "CSV: First row is header (true/false, default: true)");
        schema.put("parserOptions.sheetName", "Excel: Sheet name to parse (default: first sheet)");
        schema.put("parserOptions.startRow", "Excel: Row index where data starts (default: 0)");

        return schema;
    }

    /**
     * Applies configuration from SdkContext to the request.
     * <p>
     * This method reads configuration values from the context and applies them
     * to the request if they are not already set.
     * </p>
     *
     * @param request request to configure
     * @param context SDK context with configuration
     */
    private void applyContextConfiguration(FileAnalysisRequest request, SdkContext context) {
        // Apply detectArrays if configured
        String detectArraysStr = context.getConfig("detectArrays");
        if (detectArraysStr != null) {
            boolean detectArrays = Boolean.parseBoolean(detectArraysStr);
            request.setDetectArrays(detectArrays);
        }

        // Apply optimizeForBeanIO if configured
        String optimizeForBeanIOStr = context.getConfig("optimizeForBeanIO");
        if (optimizeForBeanIOStr != null) {
            boolean optimizeForBeanIO = Boolean.parseBoolean(optimizeForBeanIOStr);
            request.setOptimizeForBeanIO(optimizeForBeanIO);
        }

        // Apply parser options
        Map<String, String> parserOptions = new HashMap<>();
        if (request.getParserOptions() != null) {
            parserOptions.putAll(request.getParserOptions());
        }

        // Extract all parserOptions.* from context
        context.getAllConfigs().forEach((key, value) -> {
            if (key.startsWith("parserOptions.")) {
                String optionName = key.substring("parserOptions.".length());
                parserOptions.put(optionName, value);
            }
        });

        if (!parserOptions.isEmpty()) {
            request.setParserOptions(parserOptions);
        }
    }

    /**
     * Gets the underlying FileSchemaAnalyzer instance.
     * <p>
     * Useful for advanced usage or testing.
     * </p>
     *
     * @return file schema analyzer
     */
    public FileSchemaAnalyzer getAnalyzer() {
        return analyzer;
    }
}
