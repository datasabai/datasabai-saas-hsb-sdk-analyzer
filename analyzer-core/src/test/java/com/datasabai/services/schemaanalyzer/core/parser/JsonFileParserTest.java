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
 * Unit tests for {@link JsonFileParser}.
 */
public class JsonFileParserTest {

    private JsonFileParser parser;

    @BeforeEach
    public void setUp() {
        parser = new JsonFileParser();
    }

    @Test
    public void testGetSupportedFileType() {
        assertThat(parser.getSupportedFileType()).isEqualTo(FileType.JSON);
    }

    @Test
    public void testCanParse_ValidJson() {
        FileAnalysisRequest request = FileAnalysisRequest.builder()
                .fileType(FileType.JSON)
                .fileContent("{\"key\": \"value\"}")
                .schemaName("TestSchema")
                .build();

        assertThat(parser.canParse(request)).isTrue();
    }

    @Test
    public void testCanParse_ValidJsonArray() {
        FileAnalysisRequest request = FileAnalysisRequest.builder()
                .fileType(FileType.JSON)
                .fileContent("[{\"key\": \"value\"}]")
                .schemaName("TestSchema")
                .build();

        assertThat(parser.canParse(request)).isTrue();
    }

    @Test
    public void testCanParse_InvalidFileType() {
        FileAnalysisRequest request = FileAnalysisRequest.builder()
                .fileType(FileType.CSV)
                .fileContent("{\"key\": \"value\"}")
                .schemaName("TestSchema")
                .build();

        assertThat(parser.canParse(request)).isFalse();
    }

    @Test
    public void testCanParse_NullContent() {
        FileAnalysisRequest request = FileAnalysisRequest.builder()
                .fileType(FileType.JSON)
                .fileBytes(new byte[]{1})
                .schemaName("TestSchema")
                .build();

        request.setFileContent(null);

        assertThat(parser.canParse(request)).isFalse();
    }

    @Test
    public void testCanParse_NotJson() {
        FileAnalysisRequest request = FileAnalysisRequest.builder()
                .fileType(FileType.JSON)
                .fileContent("not json content")
                .schemaName("TestSchema")
                .build();

        assertThat(parser.canParse(request)).isFalse();
    }

    @Test
    public void testParse_SimpleObject() throws AnalyzerException {
        String jsonContent = """
                {
                  "id": 123,
                  "name": "Product A",
                  "price": 19.99,
                  "inStock": true
                }
                """;

        FileAnalysisRequest request = FileAnalysisRequest.builder()
                .fileType(FileType.JSON)
                .fileContent(jsonContent)
                .schemaName("Product")
                .build();

        StructureElement root = parser.parse(request);

        // Verify root structure
        assertThat(root.getName()).isEqualTo("Product");
        assertThat(root.getType()).isEqualTo("object");
        assertThat(root.getChildren()).hasSize(4);

        // Verify fields
        assertThat(root.findChild("id")).isNotNull();
        assertThat(root.findChild("id").getType()).isEqualTo("integer");

        assertThat(root.findChild("name")).isNotNull();
        assertThat(root.findChild("name").getType()).isEqualTo("string");

        assertThat(root.findChild("price")).isNotNull();
        assertThat(root.findChild("price").getType()).isEqualTo("number");

        assertThat(root.findChild("inStock")).isNotNull();
        assertThat(root.findChild("inStock").getType()).isEqualTo("boolean");
    }

    @Test
    public void testParse_ArrayOfObjects() throws AnalyzerException {
        String jsonContent = """
                [
                  {
                    "id": 1,
                    "name": "Product A"
                  },
                  {
                    "id": 2,
                    "name": "Product B"
                  }
                ]
                """;

        FileAnalysisRequest request = FileAnalysisRequest.builder()
                .fileType(FileType.JSON)
                .fileContent(jsonContent)
                .schemaName("Products")
                .build();

        StructureElement root = parser.parse(request);

        // Verify root is array
        assertThat(root.getName()).isEqualTo("Products");
        assertThat(root.getType()).isEqualTo("array");
        assertThat(root.isArray()).isTrue();
        assertThat(root.getChildren()).hasSize(1);

        // Verify item structure
        StructureElement item = root.getChildren().get(0);
        assertThat(item.getName()).isEqualTo("item");
        assertThat(item.getType()).isEqualTo("object");
        assertThat(item.getChildren()).hasSize(2);

        assertThat(item.findChild("id")).isNotNull();
        assertThat(item.findChild("id").getType()).isEqualTo("integer");

        assertThat(item.findChild("name")).isNotNull();
        assertThat(item.findChild("name").getType()).isEqualTo("string");
    }

    @Test
    public void testParse_NestedObjects() throws AnalyzerException {
        String jsonContent = """
                {
                  "product": {
                    "id": 1,
                    "details": {
                      "name": "Laptop",
                      "brand": "TechCorp"
                    }
                  }
                }
                """;

        FileAnalysisRequest request = FileAnalysisRequest.builder()
                .fileType(FileType.JSON)
                .fileContent(jsonContent)
                .schemaName("Root")
                .build();

        StructureElement root = parser.parse(request);

        assertThat(root.getName()).isEqualTo("Root");
        assertThat(root.getType()).isEqualTo("object");

        StructureElement product = root.findChild("product");
        assertThat(product).isNotNull();
        assertThat(product.getType()).isEqualTo("object");

        assertThat(product.findChild("id")).isNotNull();
        assertThat(product.findChild("id").getType()).isEqualTo("integer");

        StructureElement details = product.findChild("details");
        assertThat(details).isNotNull();
        assertThat(details.getType()).isEqualTo("object");

        assertThat(details.findChild("name")).isNotNull();
        assertThat(details.findChild("brand")).isNotNull();
    }

    @Test
    public void testParse_NestedArrays() throws AnalyzerException {
        String jsonContent = """
                {
                  "products": [
                    {
                      "name": "Product A",
                      "tags": ["electronics", "gadget"]
                    }
                  ]
                }
                """;

        FileAnalysisRequest request = FileAnalysisRequest.builder()
                .fileType(FileType.JSON)
                .fileContent(jsonContent)
                .schemaName("Root")
                .build();

        StructureElement root = parser.parse(request);

        assertThat(root.getType()).isEqualTo("object");

        StructureElement products = root.findChild("products");
        assertThat(products).isNotNull();
        assertThat(products.getType()).isEqualTo("array");
        assertThat(products.isArray()).isTrue();

        StructureElement productItem = products.getChildren().get(0);
        assertThat(productItem.getType()).isEqualTo("object");

        StructureElement tags = productItem.findChild("tags");
        assertThat(tags).isNotNull();
        assertThat(tags.getType()).isEqualTo("array");
        assertThat(tags.isArray()).isTrue();

        StructureElement tagItem = tags.getChildren().get(0);
        assertThat(tagItem.getName()).isEqualTo("item");
        assertThat(tagItem.getType()).isEqualTo("string");
    }

    @Test
    public void testParse_MixedTypes() throws AnalyzerException {
        String jsonContent = """
                {
                  "intValue": 42,
                  "floatValue": 3.14,
                  "stringValue": "hello",
                  "boolValue": true,
                  "nullValue": null,
                  "arrayValue": [1, 2, 3],
                  "objectValue": {"key": "value"}
                }
                """;

        FileAnalysisRequest request = FileAnalysisRequest.builder()
                .fileType(FileType.JSON)
                .fileContent(jsonContent)
                .schemaName("Mixed")
                .build();

        StructureElement root = parser.parse(request);

        assertThat(root.findChild("intValue").getType()).isEqualTo("integer");
        assertThat(root.findChild("floatValue").getType()).isEqualTo("number");
        assertThat(root.findChild("stringValue").getType()).isEqualTo("string");
        assertThat(root.findChild("boolValue").getType()).isEqualTo("boolean");
        assertThat(root.findChild("nullValue").getType()).isEqualTo("null");
        assertThat(root.findChild("arrayValue").getType()).isEqualTo("array");
        assertThat(root.findChild("objectValue").getType()).isEqualTo("object");
    }

    @Test
    public void testParse_TypeInferenceForStrings() throws AnalyzerException {
        String jsonContent = """
                {
                  "email": "user@example.com",
                  "date": "2024-12-26",
                  "number": "12345",
                  "text": "regular text"
                }
                """;

        FileAnalysisRequest request = FileAnalysisRequest.builder()
                .fileType(FileType.JSON)
                .fileContent(jsonContent)
                .schemaName("StringTypes")
                .build();

        StructureElement root = parser.parse(request);

        // TypeInferenceUtil should refine string types
        assertThat(root.findChild("email").getType()).isEqualTo("string");
        assertThat(root.findChild("date").getType()).isEqualTo("string");
        assertThat(root.findChild("number").getType()).isEqualTo("integer");
        assertThat(root.findChild("text").getType()).isEqualTo("string");
    }

    @Test
    public void testMergeStructures_SingleStructure() throws AnalyzerException {
        String jsonContent = """
                {
                  "id": 1,
                  "name": "Product A"
                }
                """;

        FileAnalysisRequest request = FileAnalysisRequest.builder()
                .fileType(FileType.JSON)
                .fileContent(jsonContent)
                .schemaName("Product")
                .build();

        StructureElement structure = parser.parse(request);
        StructureElement merged = parser.mergeStructures(List.of(structure));

        assertThat(merged.getName()).isEqualTo("Product");
        assertThat(merged.getType()).isEqualTo("object");
    }

    @Test
    public void testMergeStructures_MultipleObjects() throws AnalyzerException {
        String json1 = """
                {
                  "id": 1,
                  "name": "Product A"
                }
                """;

        String json2 = """
                {
                  "id": 2,
                  "name": "Product B",
                  "price": 29.99
                }
                """;

        String json3 = """
                {
                  "id": "ABC",
                  "name": "Product C",
                  "price": 39.99,
                  "inStock": true
                }
                """;

        FileAnalysisRequest request1 = FileAnalysisRequest.builder()
                .fileType(FileType.JSON)
                .fileContent(json1)
                .schemaName("Product")
                .build();

        FileAnalysisRequest request2 = FileAnalysisRequest.builder()
                .fileType(FileType.JSON)
                .fileContent(json2)
                .schemaName("Product")
                .build();

        FileAnalysisRequest request3 = FileAnalysisRequest.builder()
                .fileType(FileType.JSON)
                .fileContent(json3)
                .schemaName("Product")
                .build();

        StructureElement structure1 = parser.parse(request1);
        StructureElement structure2 = parser.parse(request2);
        StructureElement structure3 = parser.parse(request3);

        StructureElement merged = parser.mergeStructures(List.of(structure1, structure2, structure3));

        assertThat(merged.getType()).isEqualTo("object");
        assertThat(merged.getChildren()).hasSize(4);

        // id: integer + integer + string → string
        assertThat(merged.findChild("id").getType()).isEqualTo("string");

        // name: string in all
        assertThat(merged.findChild("name").getType()).isEqualTo("string");

        // price: number in json2 and json3
        assertThat(merged.findChild("price").getType()).isEqualTo("number");

        // inStock: boolean only in json3
        assertThat(merged.findChild("inStock").getType()).isEqualTo("boolean");
    }

    @Test
    public void testParse_AllowComments() throws AnalyzerException {
        String jsonContent = """
                {
                  // This is a comment
                  "id": 123,
                  /* Multi-line
                     comment */
                  "name": "Product A"
                }
                """;

        Map<String, String> options = new HashMap<>();
        options.put("allowComments", "true");

        FileAnalysisRequest request = FileAnalysisRequest.builder()
                .fileType(FileType.JSON)
                .fileContent(jsonContent)
                .schemaName("Product")
                .parserOptions(options)
                .build();

        StructureElement root = parser.parse(request);

        assertThat(root.getChildren()).hasSize(2);
        assertThat(root.findChild("id")).isNotNull();
        assertThat(root.findChild("name")).isNotNull();
    }

    @Test
    public void testParse_EmptyJson() {
        FileAnalysisRequest request = FileAnalysisRequest.builder()
                .fileType(FileType.JSON)
                .fileBytes(new byte[]{1})
                .schemaName("Empty")
                .build();

        request.setFileContent("");

        assertThatThrownBy(() -> parser.parse(request))
                .isInstanceOf(AnalyzerException.class)
                .hasMessageContaining("No file content provided");
    }

    @Test
    public void testParse_MalformedJson() {
        FileAnalysisRequest request = FileAnalysisRequest.builder()
                .fileType(FileType.JSON)
                .fileContent("{invalid json}")
                .schemaName("Invalid")
                .build();

        assertThatThrownBy(() -> parser.parse(request))
                .isInstanceOf(AnalyzerException.class)
                .hasMessageContaining("Failed to parse JSON");
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
                "strictMode",
                "allowComments",
                "allowTrailingCommas"
        );
    }
}
