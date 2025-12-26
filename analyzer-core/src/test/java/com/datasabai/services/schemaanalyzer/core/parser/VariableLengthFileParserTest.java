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
 * Unit tests for {@link VariableLengthFileParser}.
 */
public class VariableLengthFileParserTest {

    private VariableLengthFileParser parser;

    @BeforeEach
    public void setUp() {
        parser = new VariableLengthFileParser();
    }

    @Test
    public void testGetSupportedFileType() {
        assertThat(parser.getSupportedFileType()).isEqualTo(FileType.VARIABLE_LENGTH);
    }

    @Test
    public void testCanParse_ValidContent() {
        FileAnalysisRequest request = FileAnalysisRequest.builder()
                .fileType(FileType.VARIABLE_LENGTH)
                .fileContent("001|Product A|19.99")
                .schemaName("TestSchema")
                .build();

        assertThat(parser.canParse(request)).isTrue();
    }

    @Test
    public void testCanParse_InvalidFileType() {
        FileAnalysisRequest request = FileAnalysisRequest.builder()
                .fileType(FileType.CSV)
                .fileContent("001|Product A|19.99")
                .schemaName("TestSchema")
                .build();

        assertThat(parser.canParse(request)).isFalse();
    }

    @Test
    public void testParse_ModeA_PipeDelimited() throws AnalyzerException {
        String fileContent = """
                001|Product A|19.99|true
                002|Product B|29.99|false
                003|Product C|39.99|true
                """;

        FileAnalysisRequest request = FileAnalysisRequest.builder()
                .fileType(FileType.VARIABLE_LENGTH)
                .fileContent(fileContent)
                .schemaName("Products")
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
        assertThat(item.getChildren()).hasSize(4);

        // Verify generated field names
        assertThat(item.findChild("field1")).isNotNull();
        assertThat(item.findChild("field1").getType()).isEqualTo("integer");

        assertThat(item.findChild("field2")).isNotNull();
        assertThat(item.findChild("field2").getType()).isEqualTo("string");

        assertThat(item.findChild("field3")).isNotNull();
        assertThat(item.findChild("field3").getType()).isEqualTo("number");

        assertThat(item.findChild("field4")).isNotNull();
        assertThat(item.findChild("field4").getType()).isEqualTo("boolean");
    }

    @Test
    public void testParse_ModeA_WithHeaders() throws AnalyzerException {
        String fileContent = """
                ID|Name|Price|InStock
                001|Product A|19.99|true
                002|Product B|29.99|false
                """;

        Map<String, String> options = new HashMap<>();
        options.put("hasHeader", "true");

        FileAnalysisRequest request = FileAnalysisRequest.builder()
                .fileType(FileType.VARIABLE_LENGTH)
                .fileContent(fileContent)
                .schemaName("Products")
                .parserOptions(options)
                .build();

        StructureElement root = parser.parse(request);

        StructureElement item = root.getChildren().get(0);
        assertThat(item.getChildren()).hasSize(4);

        // Verify header names used
        assertThat(item.findChild("ID")).isNotNull();
        assertThat(item.findChild("Name")).isNotNull();
        assertThat(item.findChild("Price")).isNotNull();
        assertThat(item.findChild("InStock")).isNotNull();
    }

    @Test
    public void testParse_ModeA_CustomDelimiter() throws AnalyzerException {
        String fileContent = """
                001;Product A;19.99
                002;Product B;29.99
                """;

        Map<String, String> options = new HashMap<>();
        options.put("delimiter", ";");

        FileAnalysisRequest request = FileAnalysisRequest.builder()
                .fileType(FileType.VARIABLE_LENGTH)
                .fileContent(fileContent)
                .schemaName("Products")
                .parserOptions(options)
                .build();

        StructureElement root = parser.parse(request);

        StructureElement item = root.getChildren().get(0);
        assertThat(item.getChildren()).hasSize(3);
    }

    @Test
    public void testParse_ModeA_TabDelimited() throws AnalyzerException {
        String fileContent = "001\tProduct A\t19.99\n002\tProduct B\t29.99";

        Map<String, String> options = new HashMap<>();
        options.put("delimiter", "\t");

        FileAnalysisRequest request = FileAnalysisRequest.builder()
                .fileType(FileType.VARIABLE_LENGTH)
                .fileContent(fileContent)
                .schemaName("Products")
                .parserOptions(options)
                .build();

        StructureElement root = parser.parse(request);

        StructureElement item = root.getChildren().get(0);
        assertThat(item.getChildren()).hasSize(3);
    }

    @Test
    public void testParse_ModeA_QuotedFields() throws AnalyzerException {
        String fileContent = """
                001|"Product A|Special"|19.99
                002|"Product B|Premium"|29.99
                """;

        Map<String, String> options = new HashMap<>();
        options.put("quoteChar", "\"");

        FileAnalysisRequest request = FileAnalysisRequest.builder()
                .fileType(FileType.VARIABLE_LENGTH)
                .fileContent(fileContent)
                .schemaName("Products")
                .parserOptions(options)
                .build();

        StructureElement root = parser.parse(request);

        StructureElement item = root.getChildren().get(0);
        assertThat(item.getChildren()).hasSize(3);
    }

    @Test
    public void testParse_ModeB_TagValuePairs() throws AnalyzerException {
        String fileContent = """
                ID=001|NAME=Product A|PRICE=19.99|INSTOCK=true
                ID=002|NAME=Product B|PRICE=29.99|INSTOCK=false
                ID=003|NAME=Product C|PRICE=39.99|INSTOCK=true
                """;

        Map<String, String> options = new HashMap<>();
        options.put("tagValuePairs", "true");

        FileAnalysisRequest request = FileAnalysisRequest.builder()
                .fileType(FileType.VARIABLE_LENGTH)
                .fileContent(fileContent)
                .schemaName("Products")
                .parserOptions(options)
                .build();

        StructureElement root = parser.parse(request);

        // Verify root structure
        assertThat(root.getName()).isEqualTo("Products");
        assertThat(root.getType()).isEqualTo("array");
        assertThat(root.isArray()).isTrue();

        // Verify item structure
        StructureElement item = root.getChildren().get(0);
        assertThat(item.getType()).isEqualTo("object");
        assertThat(item.getChildren()).hasSize(4);

        // Verify tag names used
        assertThat(item.findChild("ID")).isNotNull();
        assertThat(item.findChild("ID").getType()).isEqualTo("integer");

        assertThat(item.findChild("NAME")).isNotNull();
        assertThat(item.findChild("NAME").getType()).isEqualTo("string");

        assertThat(item.findChild("PRICE")).isNotNull();
        assertThat(item.findChild("PRICE").getType()).isEqualTo("number");

        assertThat(item.findChild("INSTOCK")).isNotNull();
        assertThat(item.findChild("INSTOCK").getType()).isEqualTo("boolean");
    }

    @Test
    public void testParse_ModeB_ColonDelimiter() throws AnalyzerException {
        String fileContent = """
                ID:001|NAME:Product A|PRICE:19.99
                ID:002|NAME:Product B|PRICE:29.99
                """;

        Map<String, String> options = new HashMap<>();
        options.put("tagValuePairs", "true");
        options.put("tagValueDelimiter", ":");

        FileAnalysisRequest request = FileAnalysisRequest.builder()
                .fileType(FileType.VARIABLE_LENGTH)
                .fileContent(fileContent)
                .schemaName("Products")
                .parserOptions(options)
                .build();

        StructureElement root = parser.parse(request);

        StructureElement item = root.getChildren().get(0);
        assertThat(item.getChildren()).hasSize(3);
        assertThat(item.findChild("ID")).isNotNull();
        assertThat(item.findChild("NAME")).isNotNull();
        assertThat(item.findChild("PRICE")).isNotNull();
    }

    @Test
    public void testParse_ModeB_CustomPairDelimiter() throws AnalyzerException {
        String fileContent = """
                ID=001;NAME=Product A;PRICE=19.99
                ID=002;NAME=Product B;PRICE=29.99
                """;

        Map<String, String> options = new HashMap<>();
        options.put("tagValuePairs", "true");
        options.put("delimiter", ";");

        FileAnalysisRequest request = FileAnalysisRequest.builder()
                .fileType(FileType.VARIABLE_LENGTH)
                .fileContent(fileContent)
                .schemaName("Products")
                .parserOptions(options)
                .build();

        StructureElement root = parser.parse(request);

        StructureElement item = root.getChildren().get(0);
        assertThat(item.getChildren()).hasSize(3);
    }

    @Test
    public void testParse_SkipLines() throws AnalyzerException {
        String fileContent = """
                # Header comment
                # Another comment
                001|Product A|19.99
                002|Product B|29.99
                """;

        Map<String, String> options = new HashMap<>();
        options.put("skipLines", "2");

        FileAnalysisRequest request = FileAnalysisRequest.builder()
                .fileType(FileType.VARIABLE_LENGTH)
                .fileContent(fileContent)
                .schemaName("Products")
                .parserOptions(options)
                .build();

        StructureElement root = parser.parse(request);

        // Should successfully parse after skipping 2 lines
        assertThat(root.getChildren()).hasSize(1);
    }

    @Test
    public void testMergeStructures_SingleStructure() throws AnalyzerException {
        String fileContent = "001|Product A";

        FileAnalysisRequest request = FileAnalysisRequest.builder()
                .fileType(FileType.VARIABLE_LENGTH)
                .fileContent(fileContent)
                .schemaName("Products")
                .build();

        StructureElement structure = parser.parse(request);
        StructureElement merged = parser.mergeStructures(List.of(structure));

        assertThat(merged.getName()).isEqualTo("Products");
        assertThat(merged.getType()).isEqualTo("array");
    }

    @Test
    public void testMergeStructures_MultipleStructures() throws AnalyzerException {
        String content1 = """
                001|Product A
                002|Product B
                """;

        String content2 = """
                003|Product C|39.99
                004|Product D|49.99
                """;

        String content3 = """
                ABC|Product E|59.99|true
                DEF|Product F|69.99|false
                """;

        FileAnalysisRequest request1 = FileAnalysisRequest.builder()
                .fileType(FileType.VARIABLE_LENGTH)
                .fileContent(content1)
                .schemaName("Products")
                .build();

        FileAnalysisRequest request2 = FileAnalysisRequest.builder()
                .fileType(FileType.VARIABLE_LENGTH)
                .fileContent(content2)
                .schemaName("Products")
                .build();

        FileAnalysisRequest request3 = FileAnalysisRequest.builder()
                .fileType(FileType.VARIABLE_LENGTH)
                .fileContent(content3)
                .schemaName("Products")
                .build();

        StructureElement structure1 = parser.parse(request1);
        StructureElement structure2 = parser.parse(request2);
        StructureElement structure3 = parser.parse(request3);

        StructureElement merged = parser.mergeStructures(List.of(structure1, structure2, structure3));

        assertThat(merged.getType()).isEqualTo("array");
        StructureElement item = merged.getChildren().get(0);
        assertThat(item.getChildren()).hasSize(4);

        // field1: integer + integer + string → string
        assertThat(item.findChild("field1").getType()).isEqualTo("string");

        // field2: string in all
        assertThat(item.findChild("field2").getType()).isEqualTo("string");

        // field3: number in content2 and content3
        assertThat(item.findChild("field3").getType()).isEqualTo("number");

        // field4: boolean only in content3
        assertThat(item.findChild("field4").getType()).isEqualTo("boolean");
    }

    @Test
    public void testParse_EmptyFile() {
        FileAnalysisRequest request = FileAnalysisRequest.builder()
                .fileType(FileType.VARIABLE_LENGTH)
                .fileBytes(new byte[]{1})
                .schemaName("Empty")
                .build();

        request.setFileContent("");

        assertThatThrownBy(() -> parser.parse(request))
                .isInstanceOf(AnalyzerException.class)
                .hasMessageContaining("No file content provided");
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
                "delimiter",
                "encoding",
                "hasHeader",
                "skipLines",
                "quoteChar",
                "tagValuePairs",
                "tagValueDelimiter"
        );
    }
}
