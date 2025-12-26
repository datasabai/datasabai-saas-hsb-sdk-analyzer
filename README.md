# File Schema Analyzer Service

A multi-format file analysis service that generates JSON Schemas for BeanIO configuration from CSV, JSON, Fixed-Length, and Variable-Length files.

## Architecture Overview

This service uses the **Strategy Pattern** to support different file types in an extensible manner:

```
FileSchemaAnalyzer
       ↓
 ParserFactory
       ↓
┌──────┴──────┬────────┬───────────────┬────────────────┐
│             │        │               │                │
CSV         JSON    Fixed-Length  Variable-Length
(✅)        (✅)       (✅)              (✅)
```

### Implementation Status

| File Type | Status | Parser | Description |
|-----------|--------|--------|-------------|
| **CSV** | ✅ Implemented | `CsvFileParser` | Delimited parsing with OpenCSV, header detection, type inference |
| **JSON** | ✅ Implemented | `JsonFileParser` | Recursive JSON parsing with Jackson, nested objects/arrays |
| **Fixed-Length** | ✅ Implemented | `FixedLengthFileParser` | Position-based parsing with external/inline descriptors |
| **Variable-Length** | ✅ Implemented | `VariableLengthFileParser` | Dual-mode: delimited fields or tag-value pairs |

## Project Structure

```
datasabai-saas-hsb-sdk-analyzer/
├── pom.xml                          (Parent POM, Java 21)
│
├── analyzer-core/                   (⚠️ Pure Java - NO Frameworks)
│   ├── pom.xml
│   └── src/main/java/...
│       ├── model/                   (Common models)
│       │   ├── FileType.java        (Enum: CSV, JSON, FIXED_LENGTH, VARIABLE_LENGTH)
│       │   ├── FileAnalysisRequest.java
│       │   ├── SchemaGenerationResult.java
│       │   ├── StructureElement.java (Generic element)
│       │   ├── FixedLengthDescriptor.java
│       │   └── ...
│       │
│       ├── parser/                  (Strategy Pattern)
│       │   ├── FileParser.java      (Generic interface)
│       │   ├── CsvFileParser.java   (✅ IMPLEMENTED)
│       │   ├── JsonFileParser.java  (✅ IMPLEMENTED)
│       │   ├── FixedLengthFileParser.java (✅ IMPLEMENTED)
│       │   ├── VariableLengthFileParser.java (✅ IMPLEMENTED)
│       │   ├── TypeInferenceUtil.java (Shared type inference)
│       │   └── ParserFactory.java   (Factory)
│       │
│       ├── generator/
│       │   ├── JsonSchemaGenerator.java
│       │   └── SchemaOptimizer.java
│       │
│       └── FileSchemaAnalyzer.java  (Main service)
│
├── analyzer-quarkus-app/            (Development application)
│   └── src/main/java/...
│       └── AnalyzerResource.java    (REST endpoints)
│
└── analyzer-sdk-adapter/            (⚠️ Pure Java - NO Annotations)
    └── src/main/java/...
        └── FileSchemaAnalyzerAdapter.java (Implements SdkModule)
```

## Quick Start

### 1. Build

```bash
cd datasabai-saas-hsb-sdk-analyzer
mvn clean install
```

### 2. Run Quarkus App (Dev Mode)

```bash
cd analyzer-quarkus-app
mvn quarkus:dev
```

The application starts on [http://localhost:8080](http://localhost:8080)

### 3. Test with CSV

```bash
curl -X POST http://localhost:8080/api/analyzer/analyze \
  -H "Content-Type: application/json" \
  -d '{
    "fileType": "CSV",
    "fileContent": "ID,Name,Price\n1,Product A,19.99\n2,Product B,29.99",
    "schemaName": "Product",
    "detectArrays": true,
    "optimizeForBeanIO": true
  }'
```

### 4. Test with JSON

```bash
curl -X POST http://localhost:8080/api/analyzer/analyze \
  -H "Content-Type: application/json" \
  -d '{
    "fileType": "JSON",
    "fileContent": "{\"id\": 123, \"name\": \"John Doe\", \"price\": 19.99}",
    "schemaName": "Product"
  }'
```

### 5. Test with Fixed-Length

```bash
curl -X POST http://localhost:8080/api/analyzer/analyze \
  -H "Content-Type: application/json" \
  -d '{
    "fileType": "FIXED_LENGTH",
    "fileContent": "00001Product A     00019.99\n00002Product B     00029.99",
    "schemaName": "Product",
    "parserOptions": {
      "fieldDefinitions": "[{\"name\":\"id\",\"start\":0,\"length\":5},{\"name\":\"name\",\"start\":5,\"length\":15},{\"name\":\"price\",\"start\":20,\"length\":8}]"
    }
  }'
```

### 6. Test with Variable-Length

```bash
curl -X POST http://localhost:8080/api/analyzer/analyze \
  -H "Content-Type: application/json" \
  -d '{
    "fileType": "VARIABLE_LENGTH",
    "fileContent": "001|Product A|19.99\n002|Product B|29.99",
    "schemaName": "Product"
  }'
```

### 7. Check Supported Types

```bash
curl http://localhost:8080/api/analyzer/supported-types
```

**Response:**
```json
{
  "available": ["CSV", "JSON", "FIXED_LENGTH", "VARIABLE_LENGTH"],
  "registered": ["CSV", "JSON", "FIXED_LENGTH", "VARIABLE_LENGTH"],
  "availableCount": 4,
  "registeredCount": 4
}
```

## REST Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/analyzer/analyze` | POST | Analyze from JSON |
| `/api/analyzer/analyze-file` | POST | Multipart upload |
| `/api/analyzer/supported-types` | GET | Available types |
| `/api/analyzer/parser-options/{type}` | GET | Options per type |
| `/api/analyzer/validate-schema` | POST | Validate JSON Schema |
| `/api/analyzer/health` | GET | Health check |

## Programmatic Usage

### Via Analyzer Core (Pure Java)

```java
import com.datasabai.services.schemaanalyzer.core.*;
import com.datasabai.services.schemaanalyzer.core.model.*;

FileSchemaAnalyzer analyzer = new FileSchemaAnalyzer();

// CSV Example
FileAnalysisRequest csvRequest = FileAnalysisRequest.builder()
    .fileType(FileType.CSV)
    .fileContent("ID,Name,Price\n1,Product A,19.99\n2,Product B,29.99")
    .schemaName("Product")
    .parserOption("delimiter", ",")
    .parserOption("hasHeader", "true")
    .detectArrays(true)
    .optimizeForBeanIO(true)
    .build();

SchemaGenerationResult result = analyzer.analyze(csvRequest);

if (result.isSuccess()) {
    System.out.println(result.getJsonSchemaAsString());
}

// JSON Example
FileAnalysisRequest jsonRequest = FileAnalysisRequest.builder()
    .fileType(FileType.JSON)
    .fileContent("{\"id\": 123, \"name\": \"Product A\", \"price\": 19.99}")
    .schemaName("Product")
    .build();

result = analyzer.analyze(jsonRequest);
```

### Via SDK Adapter (HSB Integration)

```java
import com.datasabai.services.schemaanalyzer.adapter.*;
import com.datasabai.hsb.sdk.core.*;

SdkModule<FileAnalysisRequest, SchemaGenerationResult> module =
    new FileSchemaAnalyzerAdapter();

FileAnalysisRequest request = FileAnalysisRequest.builder()
    .fileType(FileType.JSON)
    .fileContent("{\"id\": 123, \"name\": \"Product A\"}")
    .schemaName("Product")
    .build();

SdkContext context = SdkContext.builder()
    .config("optimizeForBeanIO", "true")
    .config("detectArrays", "true")
    .build();

SchemaGenerationResult result = module.execute(request, context);
```

## Parser Options

### CSV Parser Options

```java
FileAnalysisRequest request = FileAnalysisRequest.builder()
    .fileType(FileType.CSV)
    .parserOption("delimiter", ",")           // Column delimiter (default: ",")
    .parserOption("hasHeader", "true")        // First row is header (default: "true")
    .parserOption("encoding", "UTF-8")        // File encoding (default: "UTF-8")
    .parserOption("quoteChar", "\"")          // Quote character (default: "\"")
    .parserOption("escapeChar", "\\")         // Escape character (default: "\\")
    .parserOption("skipLines", "0")           // Lines to skip (default: "0")
    .parserOption("sampleRows", "100")        // Rows to sample (default: "100")
    .build();
```

### JSON Parser Options

```java
FileAnalysisRequest request = FileAnalysisRequest.builder()
    .fileType(FileType.JSON)
    .parserOption("strictMode", "true")       // Strict JSON validation (default: "true")
    .parserOption("allowComments", "false")   // Allow // and /* */ comments (default: "false")
    .parserOption("allowTrailingCommas", "false") // Allow trailing commas (default: "false")
    .build();
```

### Fixed-Length Parser Options

```java
// Option 1: Inline field definitions
String fieldDefs = "[" +
    "{\"name\":\"id\",\"start\":0,\"length\":5,\"type\":\"integer\"}," +
    "{\"name\":\"name\",\"start\":5,\"length\":20,\"type\":\"string\"}," +
    "{\"name\":\"price\",\"start\":25,\"length\":10,\"type\":\"number\"}" +
    "]";

FileAnalysisRequest request = FileAnalysisRequest.builder()
    .fileType(FileType.FIXED_LENGTH)
    .parserOption("fieldDefinitions", fieldDefs)  // Inline JSON definitions
    .parserOption("encoding", "UTF-8")            // File encoding (default: "UTF-8")
    .parserOption("skipLines", "0")               // Lines to skip (default: "0")
    .parserOption("trimFields", "true")           // Trim whitespace (default: "true")
    .parserOption("recordLength", "35")           // Expected record length (optional)
    .build();

// Option 2: External descriptor file
FileAnalysisRequest request2 = FileAnalysisRequest.builder()
    .fileType(FileType.FIXED_LENGTH)
    .parserOption("descriptorFile", descriptorJsonContent)  // External descriptor
    .build();
```

### Variable-Length Parser Options

```java
// Mode A: Delimited Fields
FileAnalysisRequest request = FileAnalysisRequest.builder()
    .fileType(FileType.VARIABLE_LENGTH)
    .parserOption("delimiter", "|")           // Field delimiter (default: "|")
    .parserOption("hasHeader", "false")       // First row is header (default: "false")
    .parserOption("encoding", "UTF-8")        // File encoding (default: "UTF-8")
    .parserOption("skipLines", "0")           // Lines to skip (default: "0")
    .parserOption("quoteChar", "\"")          // Quote character (default: "\"")
    .build();

// Mode B: Tag-Value Pairs (ID=001|NAME=Product A|PRICE=19.99)
FileAnalysisRequest request2 = FileAnalysisRequest.builder()
    .fileType(FileType.VARIABLE_LENGTH)
    .parserOption("tagValuePairs", "true")    // Enable tag-value mode (default: "false")
    .parserOption("tagValueDelimiter", "=")   // Tag-value delimiter (default: "=")
    .parserOption("delimiter", "|")           // Pair delimiter (default: "|")
    .build();
```

## Type Inference

All parsers use a shared type inference utility (`TypeInferenceUtil`) that detects:

- **integer**: Whole numbers (e.g., "123", "-456")
- **number**: Decimals (e.g., "19.99", "3.14")
- **boolean**: true/false values (case-insensitive)
- **string**: Default fallback type

Type inference is applied to:
- CSV columns
- JSON string values
- Fixed-length fields (when type not specified in descriptor)
- Variable-length fields

Type merging rules when analyzing multiple samples:
- integer + integer → integer
- integer + number → number
- any + string → string
- null + type → type

## Advanced Features

### Automatic Array Detection

```java
FileAnalysisRequest request = FileAnalysisRequest.builder()
    .fileType(FileType.CSV)
    .fileContent("ID,Name\n1,A\n2,B\n3,C")
    .schemaName("Products")
    .detectArrays(true)  // ← Enable array detection
    .build();

result.getDetectedArrayFields();  // ["Products"]
```

### BeanIO Optimization

```java
FileAnalysisRequest request = FileAnalysisRequest.builder()
    .optimizeForBeanIO(true)  // ← Add x-beanio-* hints
    .build();
```

**Generated JSON Schema:**
```json
{
  "x-beanio": {
    "streamFormat": "csv",
    "generatePOJO": true
  },
  "properties": {
    "id": {
      "type": "integer",
      "x-java-field": "id",
      "x-beanio-field": {
        "name": "id",
        "javaName": "id",
        "typeHandler": "java.lang.Integer"
      }
    }
  }
}
```

### Sample Merging

```java
FileAnalysisRequest request = FileAnalysisRequest.builder()
    .fileContent(mainContent)
    .addSampleFile(sample1)
    .addSampleFile(sample2)
    .build();

// Generated schema includes all fields found
// Optional fields are marked as such
```

## Architecture Rules

### ✅ analyzer-core (Pure Java)

**ALLOWED:**
- Jackson (JSON parsing)
- OpenCSV (CSV parsing)
- Apache Commons
- SLF4J

**FORBIDDEN:**
- Quarkus, Spring, CDI
- Framework annotations

### ✅ analyzer-quarkus-app (Total Freedom)

**ALLOWED:**
- Quarkus REST
- CDI
- Annotations

### ⚠️ analyzer-sdk-adapter (STRICT PURE JAVA)

**ALLOWED:**
- `sdk-core` dependency
- `analyzer-core` dependency
- Pure Java SE

**ABSOLUTELY FORBIDDEN:**
- `@ApplicationScoped`
- `@Inject`
- `@Path`
- Any framework annotations

## Generated JSON Schema Example

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "Product",
  "x-metadata": {
    "sourceType": "CSV",
    "generatedBy": "File Schema Analyzer"
  },
  "type": "array",
  "items": {
    "type": "object",
    "properties": {
      "ID": {
        "type": "integer",
        "x-java-field": "id"
      },
      "Name": {
        "type": "string",
        "x-java-field": "name"
      },
      "Price": {
        "type": "number",
        "x-java-field": "price"
      }
    },
    "required": ["ID", "Name", "Price"]
  },
  "x-beanio": {
    "streamFormat": "csv",
    "generatePOJO": true
  }
}
```

## Testing

```bash
# Unit tests
mvn test

# Tests with coverage
mvn test jacoco:report

# Integration tests (Quarkus)
cd analyzer-quarkus-app
mvn verify
```

**Test Coverage:**
- analyzer-core: 93 tests passing
- analyzer-sdk-adapter: 15 tests passing

## Troubleshooting

### Error "Parser cannot handle the provided file content"

Check that your file content is valid:

**CSV:**
```csv
ID,Name,Price
1,Product A,19.99
2,Product B,29.99
```

**JSON:**
```json
{
  "id": 123,
  "name": "Product A"
}
```

**Fixed-Length:** Ensure field definitions match content length
**Variable-Length:** Ensure delimiter is consistent

### Error "No parser registered for file type"

The file type is not in the `FileType` enum. Supported types:
- CSV
- JSON
- FIXED_LENGTH
- VARIABLE_LENGTH

### Fixed-Length "Field definitions required"

Fixed-length files require either:
- `fieldDefinitions` parser option (inline JSON)
- `descriptorFile` parser option (external descriptor)

Example:
```java
.parserOption("fieldDefinitions", "[{\"name\":\"id\",\"start\":0,\"length\":5}]")
```

## Documentation

### Javadoc

```bash
mvn javadoc:javadoc
# Open: target/site/apidocs/index.html
```

### Key Classes

- [FileSchemaAnalyzer.java](analyzer-core/src/main/java/com/datasabai/services/schemaanalyzer/core/FileSchemaAnalyzer.java) : Main service (8-step analysis)
- [FileParser.java](analyzer-core/src/main/java/com/datasabai/services/schemaanalyzer/core/parser/FileParser.java) : Strategy interface
- [ParserFactory.java](analyzer-core/src/main/java/com/datasabai/services/schemaanalyzer/core/parser/ParserFactory.java) : Factory pattern
- [TypeInferenceUtil.java](analyzer-core/src/main/java/com/datasabai/services/schemaanalyzer/core/parser/TypeInferenceUtil.java) : Shared type inference
- [FileSchemaAnalyzerAdapter.java](analyzer-sdk-adapter/src/main/java/com/datasabai/services/schemaanalyzer/adapter/FileSchemaAnalyzerAdapter.java) : SDK integration

## Design Patterns

| Pattern | Where | Why |
|---------|-------|-----|
| **Strategy** | `FileParser` | Different parsing algorithms |
| **Factory** | `ParserFactory` | Parser creation by type |
| **Builder** | `FileAnalysisRequest`, etc. | Fluent construction |
| **Adapter** | `FileSchemaAnalyzerAdapter` | SDK integration |

## Dependencies

### Core Dependencies
- **OpenCSV 5.9** : CSV parsing
- **Jackson 2.18.2** : JSON parsing
- **JSON Schema Generator 4.33.1** : Schema generation
- **SLF4J** : Logging
- **Apache Commons Lang3** : Utilities

### Test Dependencies
- **JUnit Jupiter 5.11.4** : Testing framework
- **AssertJ 3.27.3** : Fluent assertions

## Contributing

To add a new file parser:

1. Fork the project
2. Create a branch: `git checkout -b feature/add-my-parser`
3. Implement `MyFileParser implements FileParser`
4. Add comprehensive tests
5. Register in `ParserFactory.registerDefaultParsers()`
6. Update `FileType` enum if needed
7. Commit: `git commit -m 'Add my parser implementation'`
8. Push: `git push origin feature/add-my-parser`
9. Create a Pull Request

## License

Copyright © 2025 Datasabai

## Contact

- **Service** : File Schema Analyzer
- **Version** : 1.0.0-SNAPSHOT
- **Organization** : Datasabai

---

**Extensible Architecture • Pure Java • SDK Integration • Production Ready**
