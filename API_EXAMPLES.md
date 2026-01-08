# API Examples - File Schema Analyzer

Ce document fournit des exemples d'utilisation de l'API REST pour analyser différents types de fichiers et générer des JSON Schemas.

## Table des matières

1. [Endpoints disponibles](#endpoints-disponibles)
2. [Analyser un fichier CSV](#analyser-un-fichier-csv)
3. [Analyser un fichier JSON](#analyser-un-fichier-json)
4. [Analyser un fichier Fixed-Length](#analyser-un-fichier-fixed-length)
5. [Analyser un fichier Variable-Length](#analyser-un-fichier-variable-length)

---

## Endpoints disponibles

| Endpoint | Méthode | Description |
|----------|---------|-------------|
| `/api/analyzer/analyze` | POST | Analyse depuis un JSON request body |
| `/api/analyzer/analyze-file` | POST | Analyse depuis un fichier uploadé (multipart) |
| `/api/analyzer/supported-types` | GET | Liste des types de fichiers supportés |
| `/api/analyzer/parser-options/{type}` | GET | Options disponibles pour un type de fichier |
| `/api/analyzer/health` | GET | Health check |

---

## Analyser un fichier CSV

### Via JSON Request

```bash
curl -X POST http://localhost:8080/api/analyzer/analyze \
  -H "Content-Type: application/json" \
  -d '{
    "fileType": "CSV",
    "schemaName": "Products",
    "fileContent": "ID,Name,Price,InStock\n1,Product A,19.99,true\n2,Product B,29.99,false",
    "detectArrays": true,
    "optimizeForBeanIO": true
  }'
```

### Via Multipart Upload

```bash
curl -X POST http://localhost:8080/api/analyzer/analyze-file \
  -F "file=@products.csv" \
  -F "schemaName=Products" \
  -F "fileType=CSV" \
  -F "detectArrays=true" \
  -F "optimizeForBeanIO=true"
```

### Avec options de parser supplémentaires

```bash
curl -X POST http://localhost:8080/api/analyzer/analyze-file \
  -F "file=@products.csv" \
  -F "schemaName=Products" \
  -F "fileType=CSV" \
  -F 'parserOptions={"delimiter":",","hasHeader":"true","skipLines":"0"}'
```

---

## Analyser un fichier JSON

### Via Multipart Upload

```bash
curl -X POST http://localhost:8080/api/analyzer/analyze-file \
  -F "file=@data.json" \
  -F "schemaName=UserData" \
  -F "fileType=JSON"
```

---

## Analyser un fichier Fixed-Length

Les fichiers Fixed-Length nécessitent un **descripteur** qui définit les positions et longueurs des champs.

### Format du descripteur

Le descripteur est un tableau JSON avec les champs suivants :

```json
[
  {
    "name": "id",           // Nom du champ (requis)
    "start": 0,             // Position de départ (0-based, requis)
    "length": 5,            // Longueur du champ (requis)
    "type": "integer",      // Type de données (optionnel: integer, number, string, boolean)
    "trim": true            // Trim whitespace (optionnel, défaut: true)
  },
  {
    "name": "name",
    "start": 5,
    "length": 20,
    "type": "string",
    "trim": true
  },
  {
    "name": "amount",
    "start": 25,
    "length": 10,
    "type": "number",
    "trim": true
  }
]
```

### Option 1: Descripteur inline (fieldDefinitions)

```bash
curl -X POST http://localhost:8080/api/analyzer/analyze-file \
  -F "file=@data.txt" \
  -F "schemaName=Transactions" \
  -F "fileType=FIXED_LENGTH" \
  -F 'fieldDefinitions=[
    {"name":"id","start":0,"length":5,"type":"integer"},
    {"name":"name","start":5,"length":20,"type":"string"},
    {"name":"amount","start":25,"length":10,"type":"number"}
  ]'
```

### Option 2: Fichier descripteur séparé (descriptorFile)

Créez un fichier `descriptor.json` :

```json
[
  {"name": "id", "start": 0, "length": 5, "type": "integer"},
  {"name": "name", "start": 5, "length": 20, "type": "string"},
  {"name": "amount", "start": 25, "length": 10, "type": "number"}
]
```

Puis uploadez-le avec le fichier de données :

```bash
curl -X POST http://localhost:8080/api/analyzer/analyze-file \
  -F "file=@data.txt" \
  -F "descriptorFile=@descriptor.json" \
  -F "schemaName=Transactions" \
  -F "fileType=FIXED_LENGTH"
```

### Exemple de fichier Fixed-Length

**data.txt** :
```
00001Product A           00019.99
00002Product B           00029.99
00003Product C           00039.99
```

**Avec le descripteur ci-dessus, cela générera :**
- `id`: "00001", "00002", "00003" (type: integer si spécifié, sinon string)
- `name`: "Product A", "Product B", "Product C" (type: string)
- `amount`: "00019.99", "00029.99", "00039.99" (type: number si spécifié, sinon string)

### Options supplémentaires pour Fixed-Length

```bash
curl -X POST http://localhost:8080/api/analyzer/analyze-file \
  -F "file=@data.txt" \
  -F "descriptorFile=@descriptor.json" \
  -F "schemaName=Transactions" \
  -F "fileType=FIXED_LENGTH" \
  -F 'parserOptions={
    "skipLines":"2",
    "trimFields":"true",
    "recordLength":"35"
  }'
```

**Options disponibles :**
- `skipLines`: Nombre de lignes à ignorer au début (défaut: 0)
- `trimFields`: Trim whitespace des champs (défaut: true)
- `recordLength`: Longueur attendue des enregistrements pour validation

---

## Analyser un fichier Variable-Length

### Mode A: Champs délimités

```bash
curl -X POST http://localhost:8080/api/analyzer/analyze-file \
  -F "file=@data.txt" \
  -F "schemaName=Orders" \
  -F "fileType=VARIABLE_LENGTH" \
  -F 'parserOptions={"delimiter":"|","mode":"A"}'
```

**Exemple de fichier (data.txt):**
```
1|Product A|19.99|true
2|Product B|29.99|false
```

### Mode B: Tag-Value pairs

```bash
curl -X POST http://localhost:8080/api/analyzer/analyze-file \
  -F "file=@data.txt" \
  -F "schemaName=Orders" \
  -F "fileType=VARIABLE_LENGTH" \
  -F 'parserOptions={"delimiter":"=","tagDelimiter":"|","mode":"B"}'
```

**Exemple de fichier (data.txt):**
```
ID=1|NAME=Product A|PRICE=19.99|INSTOCK=true
ID=2|NAME=Product B|PRICE=29.99|INSTOCK=false
```

---

## Définir les types dans le descripteur

### Pourquoi définir les types ?

Par défaut, tous les champs sont inférés comme `"string"` car le système ne peut pas déterminer de manière fiable si "123456789" est un entier ou un numéro de commande pouvant contenir "1234AZ443".

**Vous pouvez cependant définir explicitement les types dans le descripteur** pour les fichiers Fixed-Length :

```json
[
  {"name": "orderId", "start": 0, "length": 10, "type": "string"},
  {"name": "customerId", "start": 10, "length": 8, "type": "integer"},
  {"name": "amount", "start": 18, "length": 12, "type": "number"},
  {"name": "isPaid", "start": 30, "length": 5, "type": "boolean"},
  {"name": "orderDate", "start": 35, "length": 10, "type": "string"}
]
```

**Types supportés :**
- `string` - Chaîne de caractères (défaut)
- `integer` - Nombre entier
- `number` - Nombre décimal
- `boolean` - Booléen (true/false)
- `null` - Valeur nulle

### Exemple complet avec types explicites

**descriptor.json:**
```json
[
  {"name": "accountNumber", "start": 0, "length": 16, "type": "string", "trim": true},
  {"name": "balance", "start": 16, "length": 12, "type": "number", "trim": true},
  {"name": "isActive", "start": 28, "length": 5, "type": "boolean", "trim": true},
  {"name": "transactionCount", "start": 33, "length": 6, "type": "integer", "trim": true}
]
```

**Requête cURL:**
```bash
curl -X POST http://localhost:8080/api/analyzer/analyze-file \
  -F "file=@accounts.txt" \
  -F "descriptorFile=@descriptor.json" \
  -F "schemaName=BankAccounts" \
  -F "fileType=FIXED_LENGTH"
```

**Le JSON Schema généré respectera les types définis dans le descripteur !**

---

## Vérifier les options disponibles

Pour voir les options disponibles pour un type de fichier :

```bash
curl http://localhost:8080/api/analyzer/parser-options/FIXED_LENGTH
```

**Réponse :**
```json
{
  "fileType": "FIXED_LENGTH",
  "options": {
    "descriptorFile": "Content of descriptor JSON file (default: null)",
    "fieldDefinitions": "Inline field definitions in JSON array format (default: null)",
    "encoding": "File encoding (default: UTF-8)",
    "skipLines": "Number of lines to skip at the beginning (default: 0)",
    "trimFields": "Whether to trim whitespace from fields (true/false, default: true)",
    "recordLength": "Expected record length for validation (default: null)"
  }
}
```

---

## WebUI - Exemple HTML

Voici un exemple de formulaire HTML pour uploader un fichier Fixed-Length avec un descripteur :

```html
<!DOCTYPE html>
<html>
<head>
    <title>File Schema Analyzer</title>
</head>
<body>
    <h1>Analyze Fixed-Length File</h1>

    <form id="analyzeForm">
        <div>
            <label>Data File:</label>
            <input type="file" name="file" id="dataFile" required>
        </div>

        <div>
            <label>Descriptor File (JSON):</label>
            <input type="file" name="descriptorFile" id="descriptorFile">
        </div>

        <div>
            <label>Schema Name:</label>
            <input type="text" name="schemaName" value="MySchema">
        </div>

        <div>
            <label>Or use inline field definitions:</label>
            <textarea name="fieldDefinitions" rows="10" cols="50" placeholder='[{"name":"id","start":0,"length":5,"type":"integer"}]'></textarea>
        </div>

        <button type="submit">Analyze</button>
    </form>

    <div id="result"></div>

    <script>
        document.getElementById('analyzeForm').addEventListener('submit', async (e) => {
            e.preventDefault();

            const formData = new FormData();
            formData.append('file', document.getElementById('dataFile').files[0]);
            formData.append('schemaName', e.target.schemaName.value);
            formData.append('fileType', 'FIXED_LENGTH');

            if (document.getElementById('descriptorFile').files[0]) {
                formData.append('descriptorFile', document.getElementById('descriptorFile').files[0]);
            }

            if (e.target.fieldDefinitions.value.trim()) {
                formData.append('fieldDefinitions', e.target.fieldDefinitions.value);
            }

            try {
                const response = await fetch('http://localhost:8080/api/analyzer/analyze-file', {
                    method: 'POST',
                    body: formData
                });

                const result = await response.json();
                document.getElementById('result').innerHTML = '<pre>' + JSON.stringify(result, null, 2) + '</pre>';
            } catch (error) {
                document.getElementById('result').innerHTML = 'Error: ' + error.message;
            }
        });
    </script>
</body>
</html>
```

---

## Notes importantes

1. **Types par défaut** : Si aucun type n'est spécifié dans le descripteur, tous les champs seront de type `"string"` par défaut
2. **Enrichissement ultérieur** : Les types peuvent être enrichis dans une étape de processing ultérieure selon les règles métier
3. **Validation** : Le descripteur Fixed-Length est validé pour détecter les chevauchements de champs et les erreurs de configuration
4. **Formats supportés** : CSV, JSON, FIXED_LENGTH, VARIABLE_LENGTH

---

## Prochaines étapes

Pour déployer l'application :

```bash
# Compiler
mvn clean package

# Déployer vers Azure DevOps
mvn deploy

# Lancer l'application Quarkus en mode dev
cd analyzer-quarkus-app
mvn quarkus:dev
```

L'API sera disponible sur `http://localhost:8080`
