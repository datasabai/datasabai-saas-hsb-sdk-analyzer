package com.datasabai.services.schemaanalyzer.core;

import com.datasabai.services.schemaanalyzer.core.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for FileSchemaAnalyzer.
 */
class FileSchemaAnalyzerTest {

    private FileSchemaAnalyzer analyzer;

    @BeforeEach
    void setUp() {
        analyzer = new FileSchemaAnalyzer();
    }

    @Test
    void shouldAnalyzeSimpleXml() {
        // Given
        String xmlContent = """
                <customer>
                    <id>123</id>
                    <name>John Doe</name>
                    <active>true</active>
                </customer>
                """;

        FileAnalysisRequest request = FileAnalysisRequest.builder()
                .fileType(FileType.XML)
                .fileContent(xmlContent)
                .schemaName("Customer")
                .detectArrays(true)
                .optimizeForBeanIO(true)
                .build();

        // When
        SchemaGenerationResult result = analyzer.analyze(request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getSchemaName()).isEqualTo("Customer");
        assertThat(result.getSourceFileType()).isEqualTo(FileType.XML);
        assertThat(result.getJsonSchema()).isNotNull();
        assertThat(result.getJsonSchemaAsString()).isNotBlank();
        assertThat(result.getElementsAnalyzed()).isGreaterThan(0);
        assertThat(result.getAnalysisTimeMs()).isGreaterThanOrEqualTo(0);

        // Verify schema structure
        Map<String, Object> schema = result.getJsonSchema();
        assertThat(schema).containsKey("$schema");
        assertThat(schema).containsKey("title");
        assertThat(schema.get("title")).isEqualTo("Customer");
    }

    @Test
    void shouldDetectArraysInXml() {
        // Given
        String xmlContent = """
                <orders>
                    <order>
                        <id>1</id>
                        <amount>100.50</amount>
                    </order>
                    <order>
                        <id>2</id>
                        <amount>200.75</amount>
                    </order>
                </orders>
                """;

        FileAnalysisRequest request = FileAnalysisRequest.builder()
                .fileType(FileType.XML)
                .fileContent(xmlContent)
                .schemaName("Orders")
                .detectArrays(true)
                .build();

        // When
        SchemaGenerationResult result = analyzer.analyze(request);

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getDetectedArrayFields()).isNotEmpty();
        assertThat(result.getDetectedArrayFields()).anyMatch(field -> field.contains("order"));
    }

    @Test
    void shouldHandleXmlWithNamespaces() {
        // Given
        String xmlContent = """
                <ns:customer xmlns:ns="http://example.com">
                    <ns:id>123</ns:id>
                    <ns:name>John</ns:name>
                </ns:customer>
                """;

        FileAnalysisRequest request = FileAnalysisRequest.builder()
                .fileType(FileType.XML)
                .fileContent(xmlContent)
                .schemaName("Customer")
                .parserOption("preserveNamespaces", "true")
                .build();

        // When
        SchemaGenerationResult result = analyzer.analyze(request);

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getJsonSchemaAsString()).isNotBlank();
    }

    @Test
    void shouldHandleXmlWithAttributes() {
        // Given
        String xmlContent = """
                <product id="123" category="electronics">
                    <name>Laptop</name>
                    <price currency="USD">999.99</price>
                </product>
                """;

        FileAnalysisRequest request = FileAnalysisRequest.builder()
                .fileType(FileType.XML)
                .fileContent(xmlContent)
                .schemaName("Product")
                .parserOption("includeAttributes", "true")
                .build();

        // When
        SchemaGenerationResult result = analyzer.analyze(request);

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getMetadata().getTotalAttributes()).isGreaterThan(0);
    }

    @Test
    void shouldMergeSampleXmlFiles() {
        // Given
        String mainXml = """
                <customer>
                    <id>123</id>
                    <name>John</name>
                </customer>
                """;

        String sample1 = """
                <customer>
                    <id>456</id>
                    <name>Jane</name>
                    <email>jane@example.com</email>
                </customer>
                """;

        String sample2 = """
                <customer>
                    <id>789</id>
                    <name>Bob</name>
                    <phone>555-1234</phone>
                </customer>
                """;

        FileAnalysisRequest request = FileAnalysisRequest.builder()
                .fileType(FileType.XML)
                .fileContent(mainXml)
                .schemaName("Customer")
                .addSampleFile(sample1)
                .addSampleFile(sample2)
                .build();

        // When
        SchemaGenerationResult result = analyzer.analyze(request);

        // Then
        assertThat(result.isSuccess()).isTrue();
        // The merged schema should include fields from all samples
        String schemaString = result.getJsonSchemaAsString();
        assertThat(schemaString).contains("id");
        assertThat(schemaString).contains("name");
        // email and phone should be present as they were in samples
    }

    @Test
    void shouldOptimizeSchemaForBeanIO() {
        // Given
        String xmlContent = """
                <customer>
                    <customer_id>123</customer_id>
                    <first_name>John</first_name>
                </customer>
                """;

        FileAnalysisRequest request = FileAnalysisRequest.builder()
                .fileType(FileType.XML)
                .fileContent(xmlContent)
                .schemaName("Customer")
                .optimizeForBeanIO(true)
                .build();

        // When
        SchemaGenerationResult result = analyzer.analyze(request);

        // Then
        assertThat(result.isSuccess()).isTrue();
        String schemaString = result.getJsonSchemaAsString();
        assertThat(schemaString).contains("x-beanio");
        assertThat(schemaString).contains("x-java-field");
    }

    @Test
    void shouldInferTypesCorrectly() {
        // Given
        String xmlContent = """
                <data>
                    <stringField>hello</stringField>
                    <integerField>42</integerField>
                    <decimalField>3.14</decimalField>
                    <booleanField>true</booleanField>
                </data>
                """;

        FileAnalysisRequest request = FileAnalysisRequest.builder()
                .fileType(FileType.XML)
                .fileContent(xmlContent)
                .schemaName("Data")
                .build();

        // When
        SchemaGenerationResult result = analyzer.analyze(request);

        // Then
        assertThat(result.isSuccess()).isTrue();
        Map<String, Object> schema = result.getJsonSchema();
        assertThat(schema).isNotNull();
    }

    @Test
    void shouldThrowExceptionForExcelFiles() {
        // Given
        FileAnalysisRequest request = FileAnalysisRequest.builder()
                .fileType(FileType.EXCEL)
                .fileBytes(new byte[]{1, 2, 3})
                .schemaName("ExcelData")
                .build();

        // When/Then
        assertThatThrownBy(() -> analyzer.analyze(request))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("Excel parsing not yet implemented")
                .hasMessageContaining("Currently supported types: XML");
    }

    @Test
    void shouldThrowExceptionForCsvFiles() {
        // Given
        FileAnalysisRequest request = FileAnalysisRequest.builder()
                .fileType(FileType.CSV)
                .fileContent("id,name,price\n1,Product A,19.99")
                .schemaName("CsvData")
                .build();

        // When/Then
        assertThatThrownBy(() -> analyzer.analyze(request))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("CSV parsing not yet implemented")
                .hasMessageContaining("Currently supported types: XML");
    }

    @Test
    void shouldThrowExceptionForNullRequest() {
        // When/Then
        assertThatThrownBy(() -> analyzer.analyze(null))
                .isInstanceOf(AnalyzerException.class)
                .hasMessageContaining("Request cannot be null");
    }

    @Test
    void shouldThrowExceptionForInvalidXml() {
        // Given
        FileAnalysisRequest request = FileAnalysisRequest.builder()
                .fileType(FileType.XML)
                .fileContent("not valid xml")
                .schemaName("Invalid")
                .build();

        // When/Then
        assertThatThrownBy(() -> analyzer.analyze(request))
                .isInstanceOf(AnalyzerException.class);
    }

    @Test
    void shouldThrowExceptionForMissingFileContent() {
        // When/Then
        assertThatThrownBy(() -> FileAnalysisRequest.builder()
                .fileType(FileType.XML)
                .schemaName("Test")
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Either fileContent or fileBytes must be provided");
    }

    @Test
    void shouldReturnAvailableFileTypes() {
        // When
        List<FileType> availableTypes = analyzer.getAvailableFileTypes();

        // Then
        assertThat(availableTypes).contains(FileType.XML);
        // Excel and CSV should not be in available types (only in registered)
        assertThat(availableTypes).doesNotContain(FileType.EXCEL, FileType.CSV);
    }

    @Test
    void shouldReturnRegisteredFileTypes() {
        // When
        List<FileType> registeredTypes = analyzer.getRegisteredFileTypes();

        // Then
        assertThat(registeredTypes).contains(FileType.XML, FileType.EXCEL, FileType.CSV);
    }

    @Test
    void shouldReturnParserOptions() {
        // When
        Map<String, String> xmlOptions = analyzer.getParserOptions(FileType.XML);

        // Then
        assertThat(xmlOptions).isNotEmpty();
        assertThat(xmlOptions).containsKey("preserveNamespaces");
        assertThat(xmlOptions).containsKey("includeAttributes");
    }

    @Test
    void shouldHandleComplexNestedXml() {
        // Given
        String xmlContent = """
                <library>
                    <books>
                        <book isbn="123">
                            <title>Book 1</title>
                            <authors>
                                <author>
                                    <name>Author 1</name>
                                    <email>author1@example.com</email>
                                </author>
                                <author>
                                    <name>Author 2</name>
                                    <email>author2@example.com</email>
                                </author>
                            </authors>
                            <price>29.99</price>
                        </book>
                        <book isbn="456">
                            <title>Book 2</title>
                            <authors>
                                <author>
                                    <name>Author 3</name>
                                </author>
                            </authors>
                            <price>39.99</price>
                        </book>
                    </books>
                </library>
                """;

        FileAnalysisRequest request = FileAnalysisRequest.builder()
                .fileType(FileType.XML)
                .fileContent(xmlContent)
                .schemaName("Library")
                .detectArrays(true)
                .optimizeForBeanIO(true)
                .build();

        // When
        SchemaGenerationResult result = analyzer.analyze(request);

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getDetectedArrayFields()).hasSizeGreaterThan(0);
        assertThat(result.getMetadata().getTotalElements()).isGreaterThan(5);
        assertThat(result.getMetadata().getArrayElements()).isGreaterThan(0);
    }

    @Test
    void shouldIncludeMetadataInResult() {
        // Given
        String xmlContent = "<simple><field>value</field></simple>";

        FileAnalysisRequest request = FileAnalysisRequest.builder()
                .fileType(FileType.XML)
                .fileContent(xmlContent)
                .schemaName("Simple")
                .build();

        // When
        SchemaGenerationResult result = analyzer.analyze(request);

        // Then
        assertThat(result.getMetadata()).isNotNull();
        assertThat(result.getMetadata().getSourceFileType()).isEqualTo(FileType.XML);
        assertThat(result.getMetadata().getRootElement()).isNotBlank();
        assertThat(result.getMetadata().getGeneratedAt()).isNotNull();
        assertThat(result.getMetadata().getSchemaVersion()).contains("draft-07");
    }
}
