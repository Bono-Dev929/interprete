# TP Intérprete - Conceptos y Paradigmas de Lenguajes de Programación (2026)

## Integrantes del Grupo
* **Lucas Bono**
* **Ramiro Hernan Magallan Gonzalez**

---

## 📹 Video de Demostración (Máx. 5 minutos)
Como requerimiento formal de la cátedra, se adjunta el enlace a la defensa en formato video donde se muestra el entorno corriendo, la compilación de la gramática y la ejecución en tiempo real de los casos de prueba (exitosos y con captura de errores sintácticos/semánticos):

* **Enlace al Video:** [https://drive.google.com/file/d/1GHtGiD-y2NHw-Ylu6QGhatlicf2D5ynN/view?usp=drive_link]

---

## Descripción del Lenguaje y Variante Asignada
Este proyecto consiste en el diseño e implementación de un intérprete para un lenguaje de programación propio inspirado en la sintaxis de **COBOL** (PseudoCobol). El código fuente se divide de manera estricta y formal en dos secciones esenciales: `DIVISION DATA.` para la definición e inicialización de variables, y `DIVISION PROCEDURE.` para el flujo de ejecución lógica.

### Variante Asignada: Estructura de Control Diferencial (`EVALUATE`)
Para cumplir con los requerimientos específicos de la variante sorteada, se integró la instrucción **`EVALUATE`** (el equivalente a un `switch-case` moderno). Su sintaxis sigue la filosofía del lenguaje utilizando un punto (`.`) obligatorio para el cierre de condiciones y bloques:
* `WHEN [valor] .`: Bloque que se ejecuta si coincide con la expresión evaluada.
* `STOP CASE .`: Control estricto de ruptura de flujo (evita que el código "chorree" hacia el siguiente caso).
* `WHEN OTHER .`: Bloque por defecto (default) en caso de que ninguna condición previa haya matcheado.

---

## Decisiones de Diseño y Arquitectura

1. **Sintaxis Estricta y Fin de Sentencia:** Cada instrucción operativa, declaración o estructura de control debe finalizar obligatoriamente con un espacio y un punto (` .`). Esto emula la robustez de COBOL y garantiza que el Lexer diferencie de forma unívoca el punto decimal de los números de tipo flotante (`V`) del punto de fin de instrucción.
2. **Manejo de Errores Profesional:** Se removieron los escuchadores por defecto de ANTLR (`parser.removeErrorListeners()`) para inyectar un `BaseErrorListener` personalizado. Esto permite interceptar fallos sintácticos en el acto, imprimiendo un mensaje prolijo y deteniendo la aplicación limpiamente mediante `System.exit(1)` sin ensuciar la consola con el stack-trace de Java.
3. **Análisis Semántico Eficiente:** El proyecto procesa el árbol de derivación de la siguiente manera:
   * **Fase Semántica (`SemanticAnalyzer`):** Controla que no se utilicen variables sin declarar, previene la redeclaración de identificadores en el mismo ámbito y valida de forma estricta la **compatibilidad de tipos** (Type Checking) antes de permitir la fase operativa.
   * **Fase de Ejecución (`Interpreter`):** Ejecuta las instrucciones del programa y protege al entorno ante errores críticos en tiempo de ejecución, como la **división por cero**.
4. **Tecnología Moderna (Java 17):** Aunque la sintaxis simula un lenguaje clásico, el motor fue desarrollado utilizando **Java 17** y gestionado mediante **Maven**. Se aprovecharon las expresiones `switch` modernas (`->`) y la inferencia de tipos locales (`var`) para asegurar un código limpio, legible y mantenible.

---

## Estructura de la Entrega (Archivos Incluidos)
Cumpliendo con las normativas de la cátedra, el repositorio contiene la estructura completa del proyecto **excluyendo los archivos autogenerados automáticamente por ANTLR** (filtrados mediante un archivo `.gitignore` estratégico):

```text
├── src/
│   ├── main/
│   │   ├── antlr4/
│   │   │   └── MiniLang.g4          # Gramática libre de contexto y reglas léxicas
│   │   └── java/
│   │       ├── Main.java            # Orquestador principal y setup del ErrorListener
│   │       ├── SemanticAnalyzer.java# Validaciones de existencia y control de tipos
│   │       ├── SymbolTable.java     # Tabla de símbolos para persistencia en memoria
│   │       └── Interpreter.java     # Motor de ejecución en tiempo real del AST
├── pom.xml                          # Configuración de dependencias (ANTLR 4 y Java 17)
├── .gitignore                       # Filtro de exclusión de la carpeta /target (código automático)
├── programa.txt                     # Programa de prueba válido estándar
└── 06-redeclarada.pcb               # Script de prueba para validar capturas semánticas
```

---

## Instrucciones de Compilación y Ejecución

Asegúrese de contar con un JDK de **Java 17** instalado y configurado en sus variables de entorno.

### 1. Compilación y Generación del Parser
Para limpiar el entorno y compilar la gramática `.g4` obligando a ANTLR a generar sus herramientas internas necesarias para el entorno local, ejecute:
```bash
mvn clean compile
```

### 2. Ejecución del Programa
Para encender el intérprete y procesar el archivo de pruebas por defecto (`programa.txt`), ejecute:
```bash
mvn exec:java
```

---

## Ejemplos de Uso e Intérprete en Acción

El repositorio cuenta con archivos de prueba reales para testear la solidez del sistema ante escenarios válidos y erróneos:

### Caso Válido (Lógica, Operaciones y EVALUATE):
```text
PROGRAM Control .
DIVISION DATA .
PIC I dia = 3 .
DIVISION PROCEDURE .
EVALUATE dia
    WHEN 1 .
        DISPLAY "Lunes" .
        STOP CASE .
    WHEN 3 .
        DISPLAY "Miercoles" .
        STOP CASE .
    WHEN OTHER .
        DISPLAY "Otro dia" .
END-EVALUATE .
STOP RUN .
```

### Caso Erróneo (Detección Semántica de Incompatibilidad de Tipos):
```text
PROGRAM ErrorSemantico .
DIVISION DATA .
PIC I cantidad = 10 .
DIVISION PROCEDURE .
MOVE "Texto Invalido" TO cantidad .
STOP RUN .
```
*Resultado esperado:* El motor semántico detiene el programa lanzando:  
`[ERROR SEMÁNTICO] Incompatibilidad de tipos: No se puede asignar un valor de tipo STRING a la variable 'cantidad' de tipo INT.`
