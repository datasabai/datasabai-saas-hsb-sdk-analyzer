package com.datasabai.services.schemaanalyzer.adapter;

import com.datasabai.hsb.sdk.SdkContext;
import com.datasabai.services.schemaanalyzer.core.model.FileAnalysisRequest;
import com.datasabai.services.schemaanalyzer.core.model.FileType;
import com.datasabai.services.schemaanalyzer.core.model.SchemaGenerationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for FileSchemaAnalyzerAdapter.
 */
class FileSchemaAnalyzerAdapterTest {

    private FileSchemaAnalyzerAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new FileSchemaAnalyzerAdapter();
    }

    @Test
    void shouldHaveCorrectName() {
        assertThat(adapter.name()).isEqualTo("file-schema-analyzer");
    }

    @Test
    void shouldHaveDescription() {
        assertThat(adapter.description()).isNotBlank();
        assertThat(adapter.description()).contains("schema");
    }

    @Test
    void shouldHaveVersion() {
        assertThat(adapter.version()).isNotBlank();
    }

    @Test
    void shouldHaveInputType() {
        assertThat(adapter.getInputType()).isEqualTo(FileAnalysisRequest.class);
    }

    @Test
    void shouldHaveOutputType() {
        assertThat(adapter.getOutputType()).isEqualTo(SchemaGenerationResult.class);
    }

    @Test
    void shouldHaveConfigurationSchema() {
        Map<String, String> schema = adapter.getConfigurationSchema();

        assertThat(schema).isNotEmpty();
        assertThat(schema).containsKey("detectArrays");
        assertThat(schema).containsKey("optimizeForBeanIO");
        assertThat(schema).containsKey("parserOptions.preserveNamespaces");
        assertThat(schema).containsKey("parserOptions.delimiter");
    }

    @Test
    void shouldExecuteXmlAnalysis() throws Exception {
        // Given
        String xmlContent = """
                <customer>
                    <id>123</id>
                    <name>John Doe</name>
                </customer>
                """;

        FileAnalysisRequest request = FileAnalysisRequest.builder()
                .fileType(FileType.XML)
                .fileContent(xmlContent)
                .schemaName("Customer")
                .build();

        SdkContext context = new SdkContext();

        // When
        SchemaGenerationResult result = adapter.execute(request, context);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getSchemaName()).isEqualTo("Customer");
        assertThat(result.getJsonSchema()).isNotNull();
    }

    @Test
    void shouldApplyConfigurationFromContext() throws Exception {
        // Given
        String xmlContent = "<simple><field>value</field></simple>";

        FileAnalysisRequest request = FileAnalysisRequest.builder()
                .fileType(FileType.XML)
                .fileContent(xmlContent)
                .schemaName("Simple")
                .build();

        SdkContext context = new SdkContext();
        context.setConfig("detectArrays", "false");
        context.setConfig("optimizeForBeanIO", "false");
        context.setConfig("parserOptions.preserveNamespaces", "false");

        // When
        SchemaGenerationResult result = adapter.execute(request, context);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        // Configuration should have been applied to the request
    }

    @Test
    void shouldThrowExceptionForNullInput() {
        SdkContext context = new SdkContext();

        assertThatThrownBy(() -> adapter.execute(null, context))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Input request cannot be null");
    }

    @Test
    void shouldThrowExceptionForExcelFiles() {
        // Given
        FileAnalysisRequest request = FileAnalysisRequest.builder()
                .fileType(FileType.EXCEL)
                .fileBytes(new byte[]{1, 2, 3})
                .schemaName("ExcelData")
                .build();

        SdkContext context = new SdkContext();

        // When/Then
        assertThatThrownBy(() -> adapter.execute(request, context))
                .isInstanceOf(Exception.class)
                .hasMessageContaining("not yet supported")
                .hasMessageContaining("Available types");
    }

    @Test
    void shouldThrowExceptionForCsvFiles() {
        // Given
        FileAnalysisRequest request = FileAnalysisRequest.builder()
                .fileType(FileType.CSV)
                .fileContent("id,name\n1,Product")
                .schemaName("CsvData")
                .build();

        SdkContext context = new SdkContext();

        // When/Then
        assertThatThrownBy(() -> adapter.execute(request, context))
                .isInstanceOf(Exception.class)
                .hasMessageContaining("not yet supported");
    }

    @Test
    void shouldHandleInvalidXml() {
        // Given
        FileAnalysisRequest request = FileAnalysisRequest.builder()
                .fileType(FileType.XML)
                .fileContent("not valid xml")
                .schemaName("Invalid")
                .build();

        SdkContext context = new SdkContext();

        // When/Then
        assertThatThrownBy(() -> adapter.execute(request, context))
                .isInstanceOf(Exception.class)
                .hasMessageContaining("failed");
    }

    @Test
    void shouldWorkWithoutContext() throws Exception {
        // Given
        String xmlContent = "<simple><field>value</field></simple>";

        FileAnalysisRequest request = FileAnalysisRequest.builder()
                .fileType(FileType.XML)
                .fileContent(xmlContent)
                .schemaName("Simple")
                .build();

        // When - execute without context (null)
        SchemaGenerationResult result = adapter.execute(request, null);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    void shouldExposeAnalyzerInstance() {
        assertThat(adapter.getAnalyzer()).isNotNull();
    }

    @Test
    void shouldRejectNullAnalyzerInConstructor() {
        assertThatThrownBy(() -> new FileSchemaAnalyzerAdapter(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("FileSchemaAnalyzer cannot be null");
    }

    @Test
    void shouldHandleComplexXmlWithContext() throws Exception {
        // Given
        String xmlContent = """
                <library>
                    <books>
                        <book isbn="123">
                            <title>Book 1</title>
                            <price>29.99</price>
                        </book>
                        <book isbn="456">
                            <title>Book 2</title>
                            <price>39.99</price>
                        </book>
                    </books>
                </library>
                """;

        FileAnalysisRequest request = FileAnalysisRequest.builder()
                .fileType(FileType.XML)
                .fileContent(xmlContent)
                .schemaName("Library")
                .build();

        SdkContext context = new SdkContext();
        context.setConfig("detectArrays", "true");
        context.setConfig("optimizeForBeanIO", "true");
        context.setConfig("parserOptions.includeAttributes", "true");

        // When
        SchemaGenerationResult result = adapter.execute(request, context);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getDetectedArrayFields()).isNotEmpty();
    }
}
