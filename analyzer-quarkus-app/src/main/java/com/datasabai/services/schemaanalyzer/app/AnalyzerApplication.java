package com.datasabai.services.schemaanalyzer.app;

import com.datasabai.services.schemaanalyzer.core.FileSchemaAnalyzer;
import com.datasabai.services.schemaanalyzer.core.model.FileType;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Application configuration and CDI beans.
 */
@ApplicationScoped
public class AnalyzerApplication {
    private static final Logger log = LoggerFactory.getLogger(AnalyzerApplication.class);

    /**
     * Produces a singleton FileSchemaAnalyzer instance.
     */
    @Produces
    @Singleton
    public FileSchemaAnalyzer fileSchemaAnalyzer() {
        log.info("Initializing FileSchemaAnalyzer");

        FileSchemaAnalyzer analyzer = new FileSchemaAnalyzer();

        // Log supported types
        List<FileType> availableTypes = analyzer.getAvailableFileTypes();
        List<FileType> registeredTypes = analyzer.getRegisteredFileTypes();

        log.info("File Schema Analyzer initialized");
        log.info("Available file types (implemented): {}", availableTypes);
        log.info("Registered file types (including stubs): {}", registeredTypes);

        if (availableTypes.size() < registeredTypes.size()) {
            log.info("Some file types are registered but not yet implemented:");
            for (FileType type : registeredTypes) {
                if (!availableTypes.contains(type)) {
                    log.info("  - {} (STUB)", type);
                }
            }
        }

        return analyzer;
    }
}
