package com.datasabai.services.schemaanalyzer.core.parser;

import com.datasabai.services.schemaanalyzer.core.model.AnalyzerException;
import com.datasabai.services.schemaanalyzer.core.model.FileAnalysisRequest;
import com.datasabai.services.schemaanalyzer.core.model.FileType;
import com.datasabai.services.schemaanalyzer.core.model.StructureElement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for {@link FixedLengthFileParser}.
 */
public class FixedLengthFileParserTest {

    private FixedLengthFileParser parser;

    @BeforeEach
    public void setUp() {
        parser = new FixedLengthFileParser();
    }

    @Test
    public void testGetSupportedFileType() {
        assertThat(parser.getSupportedFileType()).isEqualTo(FileType.FIXED_LENGTH);
    }

    @Test
    public void testCanParse_ValidWithDescriptor() {
        String descriptor = """
                [
                  {"name": "id", "start": 0, "length": 5}
                ]
                """;

        Map<String, String> options = new HashMap<>();
        options.put("fieldDefinitions", descriptor);

        FileAnalysisRequest request = FileAnalysisRequest.builder()
                .fileType(FileType.FIXED_LENGTH)
                .fileContent("00001TestData")
                .schemaName("TestSchema")
                .parserOptions(options)
                .build();

        assertThat(parser.canParse(request)).isTrue();
    }

    @Test
    public void testCanParse_InvalidFileType() {
        FileAnalysisRequest request = FileAnalysisRequest.builder()
                .fileType(FileType.CSV)
                .fileContent("00001TestData")
                .schemaName("TestSchema")
                .build();

        assertThat(parser.canParse(request)).isFalse();
    }

    @Test
    public void testCanParse_NoDescriptor() {
        FileAnalysisRequest request = FileAnalysisRequest.builder()
                .fileType(FileType.FIXED_LENGTH)
                .fileContent("00001TestData")
                .schemaName("TestSchema")
                .build();

        assertThat(parser.canParse(request)).isFalse();
    }

    @Test
    public void testParse_BasicWithInlineDefinitions() throws AnalyzerException {
        String descriptor = """
                [
                  {"name": "id", "start": 0, "length": 5, "type": "integer"},
                  {"name": "name", "start": 5, "length": 20, "type": "string"},
                  {"name": "amount", "start": 25, "length": 10, "type": "number"}
                ]
                """;

        String fileContent = """
                00001Product A           00019.99
                00002Product B           00029.99
                00003Product C           00039.99
                """;

        Map<String, String> options = new HashMap<>();
        options.put("fieldDefinitions", descriptor);

        FileAnalysisRequest request = FileAnalysisRequest.builder()
                .fileType(FileType.FIXED_LENGTH)
                .fileContent(fileContent)
                .schemaName("Products")
                .parserOptions(options)
                .build();

        StructureElement root = parser.parse(request);

        // Verify root structure
        assertThat(root.getName()).isEqualTo("Products");
        assertThat(root.getType()).isEqualTo("array");
        assertThat(root.isArray()).isTrue();
        assertThat(root.getChildren()).hasSize(1);

        // Verify item structure
        StructureElement item = root.getChildren().get(0);
        assertThat(item.getName()).isEqualTo("item");
        assertThat(item.getType()).isEqualTo("object");
        assertThat(item.getChildren()).hasSize(3);

        // Verify fields
        assertThat(item.findChild("id")).isNotNull();
        assertThat(item.findChild("id").getType()).isEqualTo("integer");

        assertThat(item.findChild("name")).isNotNull();
        assertThat(item.findChild("name").getType()).isEqualTo("string");

        assertThat(item.findChild("amount")).isNotNull();
        assertThat(item.findChild("amount").getType()).isEqualTo("number");
    }

    @Test
    public void testParse_DescriptorFile() throws AnalyzerException {
        String descriptor = """
                [
                  {"name": "id", "start": 0, "length": 5},
                  {"name": "name", "start": 5, "length": 15}
                ]
                """;

        String fileContent = """
                00001Product A
                00002Product B
                """;

        Map<String, String> options = new HashMap<>();
        options.put("descriptorFile", descriptor);

        FileAnalysisRequest request = FileAnalysisRequest.builder()
                .fileType(FileType.FIXED_LENGTH)
                .fileContent(fileContent)
                .schemaName("Products")
                .parserOptions(options)
                .build();

        StructureElement root = parser.parse(request);

        StructureElement item = root.getChildren().get(0);
        assertThat(item.getChildren()).hasSize(2);
        assertThat(item.findChild("id")).isNotNull();
        assertThat(item.findChild("name")).isNotNull();
    }

    @Test
    public void testParse_TypeInference() throws AnalyzerException {
        String descriptor = """
                [
                  {"name": "intField", "start": 0, "length": 5},
                  {"name": "numField", "start": 5, "length": 10},
                  {"name": "strField", "start": 15, "length": 10},
                  {"name": "boolField", "start": 25, "length": 5}
                ]
                """;

        String fileContent = """
                0012312.34     Hello     true
                0045645.67     World     false
                """;

        Map<String, String> options = new HashMap<>();
        options.put("fieldDefinitions", descriptor);

        FileAnalysisRequest request = FileAnalysisRequest.builder()
                .fileType(FileType.FIXED_LENGTH)
                .fileContent(fileContent)
                .schemaName("TypeTest")
                .parserOptions(options)
                .build();

        StructureElement root = parser.parse(request);
        StructureElement item = root.getChildren().get(0);

        // Type inference should work
        assertThat(item.findChild("intField").getType()).isEqualTo("integer");
        assertThat(item.findChild("numField").getType()).isEqualTo("number");
        assertThat(item.findChild("strField").getType()).isEqualTo("string");
        assertThat(item.findChild("boolField").getType()).isEqualTo("boolean");
    }

    @Test
    public void testParse_ExplicitTypesOverrideInference() throws AnalyzerException {
        String descriptor = """
                [
                  {"name": "field1", "start": 0, "length": 5, "type": "string"},
                  {"name": "field2", "start": 5, "length": 10, "type": "integer"}
                ]
                """;

        String fileContent = """
                12345     99.99
                """;

        Map<String, String> options = new HashMap<>();
        options.put("fieldDefinitions", descriptor);

        FileAnalysisRequest request = FileAnalysisRequest.builder()
                .fileType(FileType.FIXED_LENGTH)
                .fileContent(fileContent)
                .schemaName("Test")
                .parserOptions(options)
                .build();

        StructureElement root = parser.parse(request);
        StructureElement item = root.getChildren().get(0);

        // Explicit types should be used
        assertThat(item.findChild("field1").getType()).isEqualTo("string");
        assertThat(item.findChild("field2").getType()).isEqualTo("integer");
    }

    @Test
    public void testParse_TrimFields() throws AnalyzerException {
        String descriptor = """
                [
                  {"name": "id", "start": 0, "length": 10},
                  {"name": "name", "start": 10, "length": 15}
                ]
                """;

        String fileContent = """
                   123    Product A
                   456    Product B
                """;

        Map<String, String> options = new HashMap<>();
        options.put("fieldDefinitions", descriptor);
        options.put("trimFields", "true");

        FileAnalysisRequest request = FileAnalysisRequest.builder()
                .fileType(FileType.FIXED_LENGTH)
                .fileContent(fileContent)
                .schemaName("Products")
                .parserOptions(options)
                .build();

        StructureElement root = parser.parse(request);
        StructureElement item = root.getChildren().get(0);

        // Fields should be trimmed and types inferred correctly
        assertThat(item.findChild("id").getType()).isEqualTo("integer");
        assertThat(item.findChild("name").getType()).isEqualTo("string");
    }

    @Test
    public void testParse_SkipLines() throws AnalyzerException {
        String descriptor = """
                [
                  {"name": "id", "start": 0, "length": 5}
                ]
                """;

        String fileContent = """
                # Header line to skip
                # Another header
                00001
                00002
                """;

        Map<String, String> options = new HashMap<>();
        options.put("fieldDefinitions", descriptor);
        options.put("skipLines", "2");

        FileAnalysisRequest request = FileAnalysisRequest.builder()
                .fileType(FileType.FIXED_LENGTH)
                .fileContent(fileContent)
                .schemaName("Data")
                .parserOptions(options)
                .build();

        StructureElement root = parser.parse(request);

        // Should successfully parse after skipping 2 lines
        assertThat(root.getChildren()).hasSize(1);
    }

    @Test
    public void testParse_RecordLengthValidation() throws AnalyzerException {
        String descriptor = """
                [
                  {"name": "id", "start": 0, "length": 5},
                  {"name": "name", "start": 5, "length": 10}
                ]
                """;

        String fileContent = """
                00001Product A
                00002Product B
                """;

        Map<String, String> options = new HashMap<>();
        options.put("fieldDefinitions", descriptor);
        options.put("recordLength", "15");

        FileAnalysisRequest request = FileAnalysisRequest.builder()
                .fileType(FileType.FIXED_LENGTH)
                .fileContent(fileContent)
                .schemaName("Products")
                .parserOptions(options)
                .build();

        // Should not throw, but may log warnings about length mismatch
        StructureElement root = parser.parse(request);
        assertThat(root).isNotNull();
    }

    @Test
    public void testMergeStructures_SingleStructure() throws AnalyzerException {
        String descriptor = """
                [
                  {"name": "id", "start": 0, "length": 5}
                ]
                """;

        String fileContent = "00001";

        Map<String, String> options = new HashMap<>();
        options.put("fieldDefinitions", descriptor);

        FileAnalysisRequest request = FileAnalysisRequest.builder()
                .fileType(FileType.FIXED_LENGTH)
                .fileContent(fileContent)
                .schemaName("Data")
                .parserOptions(options)
                .build();

        StructureElement structure = parser.parse(request);
        StructureElement merged = parser.mergeStructures(List.of(structure));

        assertThat(merged.getName()).isEqualTo("Data");
        assertThat(merged.getType()).isEqualTo("array");
    }

    @Test
    public void testMergeStructures_MultipleStructures() throws AnalyzerException {
        String descriptor1 = """
                [
                  {"name": "id", "start": 0, "length": 5},
                  {"name": "name", "start": 5, "length": 10}
                ]
                """;

        String descriptor2 = """
                [
                  {"name": "id", "start": 0, "length": 5},
                  {"name": "name", "start": 5, "length": 10},
                  {"name": "price", "start": 15, "length": 10}
                ]
                """;

        String fileContent1 = "00001Product A";
        String fileContent2 = "ABC  Product B00099.99";

        Map<String, String> options1 = new HashMap<>();
        options1.put("fieldDefinitions", descriptor1);

        Map<String, String> options2 = new HashMap<>();
        options2.put("fieldDefinitions", descriptor2);

        FileAnalysisRequest request1 = FileAnalysisRequest.builder()
                .fileType(FileType.FIXED_LENGTH)
                .fileContent(fileContent1)
                .schemaName("Data")
                .parserOptions(options1)
                .build();

        FileAnalysisRequest request2 = FileAnalysisRequest.builder()
                .fileType(FileType.FIXED_LENGTH)
                .fileContent(fileContent2)
                .schemaName("Data")
                .parserOptions(options2)
                .build();

        StructureElement structure1 = parser.parse(request1);
        StructureElement structure2 = parser.parse(request2);

        StructureElement merged = parser.mergeStructures(List.of(structure1, structure2));

        assertThat(merged.getType()).isEqualTo("array");
        StructureElement item = merged.getChildren().get(0);
        assertThat(item.getChildren()).hasSize(3);

        // id: integer + string → string
        assertThat(item.findChild("id").getType()).isEqualTo("string");

        // name: string in both
        assertThat(item.findChild("name").getType()).isEqualTo("string");

        // price: only in second
        assertThat(item.findChild("price").getType()).isEqualTo("number");
    }

    @Test
    public void testParse_MissingDescriptor() {
        FileAnalysisRequest request = FileAnalysisRequest.builder()
                .fileType(FileType.FIXED_LENGTH)
                .fileContent("00001TestData")
                .schemaName("Test")
                .build();

        assertThatThrownBy(() -> parser.parse(request))
                .isInstanceOf(AnalyzerException.class)
                .hasMessageContaining("descriptorFile or fieldDefinitions must be provided");
    }

    @Test
    public void testParse_InvalidDescriptorJson() {
        Map<String, String> options = new HashMap<>();
        options.put("fieldDefinitions", "{invalid json}");

        FileAnalysisRequest request = FileAnalysisRequest.builder()
                .fileType(FileType.FIXED_LENGTH)
                .fileContent("00001TestData")
                .schemaName("Test")
                .parserOptions(options)
                .build();

        assertThatThrownBy(() -> parser.parse(request))
                .isInstanceOf(AnalyzerException.class)
                .hasMessageContaining("Invalid descriptor");
    }

    @Test
    public void testParse_OverlappingFields() {
        String descriptor = """
                [
                  {"name": "field1", "start": 0, "length": 10},
                  {"name": "field2", "start": 5, "length": 10}
                ]
                """;

        Map<String, String> options = new HashMap<>();
        options.put("fieldDefinitions", descriptor);

        FileAnalysisRequest request = FileAnalysisRequest.builder()
                .fileType(FileType.FIXED_LENGTH)
                .fileContent("TestData")
                .schemaName("Test")
                .parserOptions(options)
                .build();

        assertThatThrownBy(() -> parser.parse(request))
                .isInstanceOf(AnalyzerException.class)
                .hasMessageContaining("overlap");
    }

    @Test
    public void testMergeStructures_EmptyList() {
        assertThatThrownBy(() -> parser.mergeStructures(List.of()))
                .isInstanceOf(AnalyzerException.class)
                .hasMessageContaining("No structures to merge");
    }

    @Test
    public void testMergeStructures_NullList() {
        assertThatThrownBy(() -> parser.mergeStructures(null))
                .isInstanceOf(AnalyzerException.class)
                .hasMessageContaining("No structures to merge");
    }

    @Test
    public void testGetAvailableOptions() {
        Map<String, String> options = parser.getAvailableOptions();

        assertThat(options).isNotNull();
        assertThat(options).containsKeys(
                "descriptorFile",
                "fieldDefinitions",
                "encoding",
                "skipLines",
                "trimFields",
                "recordLength"
        );
    }
}
