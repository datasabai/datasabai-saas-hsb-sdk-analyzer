# File Schema Analyzer Service

Service d'analyse de fichiers multiples types et génération de JSON Schemas pour BeanIO.

## 🏗️ Architecture Extensible

Ce service utilise le **Strategy Pattern** pour supporter différents types de fichiers de manière extensible :

```
FileSchemaAnalyzer
       ↓
 ParserFactory
       ↓
┌──────┴──────┬────────┬────────┬────────┐
│             │        │        │        │
XML         Excel     CSV      TXT     JSON
(✅)        (🔜)      (🔜)     (🔜)    (🔜)
```

### Statut d'Implémentation

| Type de Fichier | Statut | Parser | Description |
|-----------------|--------|--------|-------------|
| **XML** | ✅ Implémenté | `XmlFileParser` | Parse complet avec namespaces, attributs, arrays |
| **Excel** | 🔜 Stub | `ExcelFileParser` | Architecture prête, à implémenter |
| **CSV** | 🔜 Stub | `CsvFileParser` | Architecture prête, à implémenter |
| **TXT** | 🔜 Stub | - | À créer |
| **JSON** | 🔜 Stub | - | À créer |

## 📦 Structure du Projet

```
datasabai-saas-hsb-sdk-analyzer/
├── pom.xml                          (Parent POM, Java 21)
│
├── analyzer-core/                   (⚠️ Pure Java - NO Frameworks)
│   ├── pom.xml
│   └── src/main/java/...
│       ├── model/                   (Modèles communs)
│       │   ├── FileType.java        (Enum: XML, EXCEL, CSV, etc.)
│       │   ├── FileAnalysisRequest.java
│       │   ├── SchemaGenerationResult.java
│       │   ├── StructureElement.java (Élément générique)
│       │   └── ...
│       │
│       ├── parser/                  (Strategy Pattern)
│       │   ├── FileParser.java      (Interface générique)
│       │   ├── XmlFileParser.java   (✅ IMPLEMENTED)
│       │   ├── ExcelFileParser.java (🔜 STUB)
│       │   ├── CsvFileParser.java   (🔜 STUB)
│       │   └── ParserFactory.java   (Factory)
│       │
│       ├── generator/
│       │   ├── JsonSchemaGenerator.java
│       │   └── SchemaOptimizer.java
│       │
│       └── FileSchemaAnalyzer.java  (Service principal)
│
├── analyzer-quarkus-app/            (Application de développement)
│   └── src/main/java/...
│       └── AnalyzerResource.java    (REST endpoints)
│
└── analyzer-sdk-adapter/            (⚠️ Pure Java - NO Annotations)
    └── src/main/java/...
        └── FileSchemaAnalyzerAdapter.java (Implémente SdkModule)
```

## 🚀 Quick Start

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

L'application démarre sur [http://localhost:8080](http://localhost:8080)

### 3. Tester avec XML

```bash
curl -X POST http://localhost:8080/api/analyzer/analyze \
  -H "Content-Type: application/json" \
  -d '{
    "fileType": "XML",
    "fileContent": "<customer><id>123</id><name>John Doe</name></customer>",
    "schemaName": "Customer",
    "detectArrays": true,
    "optimizeForBeanIO": true
  }'
```

### 4. Vérifier les Types Supportés

```bash
curl http://localhost:8080/api/analyzer/supported-types
```

**Réponse :**
```json
{
  "available": ["XML"],
  "registered": ["XML", "EXCEL", "CSV"],
  "availableCount": 1,
  "registeredCount": 3
}
```

### 5. Essayer Excel ou CSV (retournera une erreur explicite)

```bash
curl -X POST http://localhost:8080/api/analyzer/analyze \
  -H "Content-Type: application/json" \
  -d '{
    "fileType": "EXCEL",
    "fileBytes": "...",
    "schemaName": "ExcelData"
  }'
```

**Réponse (HTTP 501):**
```json
{
  "error": "UNSUPPORTED_FILE_TYPE",
  "message": "Excel parsing not yet implemented. To add Excel support: ...",
  "fileType": "EXCEL",
  "availableTypes": ["XML"]
}
```

## 📚 Endpoints REST

| Endpoint | Méthode | Description |
|----------|---------|-------------|
| `/api/analyzer/analyze` | POST | Analyse depuis JSON |
| `/api/analyzer/analyze-file` | POST | Upload multipart |
| `/api/analyzer/supported-types` | GET | Types disponibles |
| `/api/analyzer/parser-options/{type}` | GET | Options par type |
| `/api/analyzer/validate-schema` | POST | Valide un JSON Schema |
| `/api/analyzer/health` | GET | Health check |

## 🔧 Utilisation Programmatique

### Via Analyzer Core (Pure Java)

```java
import com.datasabai.services.schemaanalyzer.core.*;
import com.datasabai.services.schemaanalyzer.core.model.*;

FileSchemaAnalyzer analyzer = new FileSchemaAnalyzer();

FileAnalysisRequest request = FileAnalysisRequest.builder()
    .fileType(FileType.XML)
    .fileContent("<customer><id>123</id></customer>")
    .schemaName("Customer")
    .detectArrays(true)
    .optimizeForBeanIO(true)
    .build();

SchemaGenerationResult result = analyzer.analyze(request);

if (result.isSuccess()) {
    System.out.println(result.getJsonSchemaAsString());
}
```

### Via SDK Adapter (Intégration HSB)

```java
import com.datasabai.services.schemaanalyzer.adapter.*;
import com.datasabai.hsb.sdk.*;

SdkModule<FileAnalysisRequest, SchemaGenerationResult> module =
    new FileSchemaAnalyzerAdapter();

FileAnalysisRequest request = FileAnalysisRequest.builder()
    .fileType(FileType.XML)
    .fileContent("<data>...</data>")
    .schemaName("Data")
    .build();

SdkContext context = new SdkContext();
context.setConfig("optimizeForBeanIO", "true");

SchemaGenerationResult result = module.execute(request, context);
```

## 🛠️ Ajouter un Nouveau Type de Fichier

### Exemple : Implémenter le Parser CSV

#### Étape 1 : Décommenter la Dépendance

Dans `analyzer-core/pom.xml` :

```xml
<!-- Décommenter -->
<dependency>
    <groupId>org.apache.commons</groupId>
    <artifactId>commons-csv</artifactId>
</dependency>
```

#### Étape 2 : Implémenter `CsvFileParser`

Actuellement dans [analyzer-core/src/main/java/com/datasabai/services/schemaanalyzer/core/parser/CsvFileParser.java](analyzer-core/src/main/java/com/datasabai/services/schemaanalyzer/core/parser/CsvFileParser.java):

```java
@Override
public StructureElement parse(FileAnalysisRequest request) throws AnalyzerException {
    // TODO: Implementation template provided in comments

    // 1. Get parser options
    String delimiter = request.getParserOption("delimiter", ",");
    boolean hasHeader = Boolean.parseBoolean(request.getParserOption("hasHeader", "true"));

    // 2. Configure CSV format
    CSVFormat format = CSVFormat.DEFAULT
            .withDelimiter(delimiter.charAt(0))
            .withFirstRecordAsHeader(hasHeader);

    // 3. Parse CSV
    Reader reader = new StringReader(request.getFileContent());
    CSVParser csvParser = new CSVParser(reader, format);

    // 4. Build StructureElement tree
    StructureElement root = new StructureElement();
    root.setName(request.getSchemaName());
    root.setType("object");

    // 5. Analyze columns and infer types
    // ... voir template dans le code

    return root;
}
```

#### Étape 3 : Implémenter `mergeStructures`

```java
@Override
public StructureElement mergeStructures(List<StructureElement> structures) {
    // Merge multiple CSV structures
    // Combine columns, refine types, mark optional columns
}
```

#### Étape 4 : Tester

```java
@Test
void shouldParseCsvFile() {
    String csvContent = """
        id,name,price
        1,Product A,19.99
        2,Product B,29.99
        """;

    FileAnalysisRequest request = FileAnalysisRequest.builder()
        .fileType(FileType.CSV)
        .fileContent(csvContent)
        .schemaName("Products")
        .parserOption("delimiter", ",")
        .parserOption("hasHeader", "true")
        .build();

    FileSchemaAnalyzer analyzer = new FileSchemaAnalyzer();
    SchemaGenerationResult result = analyzer.analyze(request);

    assertThat(result.isSuccess()).isTrue();
}
```

#### Étape 5 : Vérification

```bash
# Rebuild
mvn clean install

# Test
curl -X POST http://localhost:8080/api/analyzer/analyze \
  -H "Content-Type: application/json" \
  -d '{
    "fileType": "CSV",
    "fileContent": "id,name,price\n1,Product A,19.99",
    "schemaName": "Products"
  }'
```

**C'est tout !** Le `ParserFactory` l'enregistre automatiquement.

## 🎯 Fonctionnalités Avancées

### Détection Automatique d'Arrays

```xml
<!-- Input XML -->
<orders>
    <order><id>1</id></order>
    <order><id>2</id></order>
</orders>
```

```java
FileAnalysisRequest request = FileAnalysisRequest.builder()
    .detectArrays(true)  // ← Active la détection
    .build();

result.getDetectedArrayFields();  // ["orders.order"]
```

### Fusion de Samples

```java
FileAnalysisRequest request = FileAnalysisRequest.builder()
    .fileContent(mainXml)
    .addSampleFile(sample1)
    .addSampleFile(sample2)
    .build();

// Le schema généré inclut tous les champs trouvés
// Les champs optionnels sont marqués comme tels
```

### Optimisation BeanIO

```java
FileAnalysisRequest request = FileAnalysisRequest.builder()
    .optimizeForBeanIO(true)  // ← Ajoute x-beanio-* hints
    .build();
```

**JSON Schema généré :**
```json
{
  "x-beanio": {
    "streamFormat": "xml",
    "generatePOJO": true
  },
  "properties": {
    "customerId": {
      "type": "integer",
      "x-java-field": "customerId",
      "x-beanio-field": {
        "name": "customer_id",
        "javaName": "customerId",
        "typeHandler": "java.lang.Integer"
      }
    }
  }
}
```

### Options Parser Spécifiques

#### XML
```java
.parserOption("preserveNamespaces", "true")
.parserOption("includeAttributes", "true")
.parserOption("detectCDATA", "true")
```

#### CSV (quand implémenté)
```java
.parserOption("delimiter", ";")
.parserOption("hasHeader", "true")
.parserOption("encoding", "UTF-8")
.parserOption("skipLines", "2")
```

#### Excel (quand implémenté)
```java
.parserOption("sheetName", "Data")
.parserOption("startRow", "1")
.parserOption("hasHeader", "true")
```

## 🔐 Règles d'Architecture

### ✅ analyzer-core (Pure Java)

**AUTORISÉ :**
- Jackson XML/CSV
- Apache POI (Excel)
- Apache Commons
- SLF4J

**INTERDIT :**
- Quarkus, Spring, CDI
- Annotations framework

### ✅ analyzer-quarkus-app (Liberté totale)

**AUTORISÉ :**
- Quarkus REST
- CDI
- Annotations

### ⚠️ analyzer-sdk-adapter (PURE JAVA STRICT)

**AUTORISÉ :**
- `sdk-core` dependency
- `analyzer-core` dependency
- Pure Java SE

**INTERDIT ABSOLUMENT :**
- `@ApplicationScoped`
- `@Inject`
- `@Path`
- Toute annotation framework

## 📊 JSON Schema Généré (Exemple)

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "Customer",
  "x-metadata": {
    "sourceType": "XML",
    "generatedBy": "File Schema Analyzer"
  },
  "type": "object",
  "properties": {
    "id": {
      "type": "integer",
      "x-java-field": "id"
    },
    "name": {
      "type": "string",
      "x-java-field": "name"
    },
    "orders": {
      "type": "array",
      "items": {
        "type": "object",
        "properties": {
          "orderId": {
            "type": "integer"
          },
          "amount": {
            "type": "number"
          }
        }
      }
    }
  },
  "required": ["id", "name"],
  "x-beanio": {
    "streamFormat": "xml",
    "generatePOJO": true
  }
}
```

## 🧪 Tests

```bash
# Tests unitaires
mvn test

# Tests avec couverture
mvn test jacoco:report

# Tests d'intégration Quarkus
cd analyzer-quarkus-app
mvn verify
```

## 🐛 Troubleshooting

### Excel/CSV ne fonctionne pas

**C'est normal !** Seul XML est implémenté.

```bash
curl http://localhost:8080/api/analyzer/supported-types
# → available: ["XML"]
```

Pour ajouter Excel/CSV, voir section "Ajouter un Nouveau Type de Fichier".

### Erreur "Parser cannot handle the provided file content"

Vérifiez que le XML est bien formé :

```xml
<!-- ✅ Bon -->
<root>
    <child>value</child>
</root>

<!-- ❌ Mauvais -->
<root>
    <child>value
</root>
```

### Erreur "No parser registered for file type"

Le type de fichier n'est pas dans l'enum `FileType`. Ajoutez-le :

```java
// Dans FileType.java
TXT("txt", "text/plain", List.of("txt")),
```

## 📖 Documentation

### Javadoc

```bash
mvn javadoc:javadoc
# Ouvrir: target/site/apidocs/index.html
```

### Architecture Détaillée

Voir les Javadocs des classes principales :
- [FileSchemaAnalyzer.java](analyzer-core/src/main/java/com/datasabai/services/schemaanalyzer/core/FileSchemaAnalyzer.java) : Service principal (8 steps)
- [FileParser.java](analyzer-core/src/main/java/com/datasabai/services/schemaanalyzer/core/parser/FileParser.java) : Interface Strategy
- [ParserFactory.java](analyzer-core/src/main/java/com/datasabai/services/schemaanalyzer/core/parser/ParserFactory.java) : Factory Pattern
- [FileSchemaAnalyzerAdapter.java](analyzer-sdk-adapter/src/main/java/com/datasabai/services/schemaanalyzer/adapter/FileSchemaAnalyzerAdapter.java) : SDK Integration

## 🎓 Design Patterns Utilisés

| Pattern | Où | Pourquoi |
|---------|-----|----------|
| **Strategy** | `FileParser` | Différents algorithmes de parsing |
| **Factory** | `ParserFactory` | Création de parsers selon type |
| **Builder** | `FileAnalysisRequest`, etc. | Construction fluide |
| **Adapter** | `FileSchemaAnalyzerAdapter` | Intégration SDK |

## 📝 TODO / Roadmap

- [ ] Implémenter `ExcelFileParser` (Apache POI)
- [ ] Implémenter `CsvFileParser` (Apache Commons CSV)
- [ ] Implémenter `JsonFileParser` (Jackson)
- [ ] Implémenter `TxtFileParser` (Fixed-length)
- [ ] Support des schémas XSD pour XML
- [ ] Support des formats Avro/Parquet
- [ ] UI web pour upload et visualisation

## 🤝 Contribution

Pour ajouter un nouveau type de fichier :

1. Fork le projet
2. Créer une branche : `git checkout -b feature/add-json-parser`
3. Implémenter `MyFileParser implements FileParser`
4. Ajouter tests
5. Commit : `git commit -m 'Add JSON parser implementation'`
6. Push : `git push origin feature/add-json-parser`
7. Créer une Pull Request

## 📄 Licence

Copyright © 2025 Datasabai

## 📞 Contact

- **Service** : File Schema Analyzer
- **Version** : 1.0.0-SNAPSHOT
- **Organisation** : Datasabai

---

**Architecture extensible • Pure Java • Intégration SDK • Production Ready**
