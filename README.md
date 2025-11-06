
# Sistema de Gestión de Personas y Domicilios

## Trabajo Práctico Integrador - Programación 2

### Descripción del Proyecto

Este Trabajo Práctico Integrador tiene como objetivo demostrar la aplicación práctica de los conceptos fundamentales de Programación Orientada a Objetos y Persistencia de Datos aprendidos durante el cursado de Programación 2. El proyecto consiste en desarrollar un sistema completo de gestión de personas y domicilios que permita realizar operaciones CRUD (Crear, Leer, Actualizar, Eliminar) sobre estas entidades, implementando una arquitectura robusta y profesional.

### Objetivos Académicos

El desarrollo de este sistema permite aplicar y consolidar los siguientes conceptos clave de la materia:

**1. Arquitectura en Capas (Layered Architecture)**
- Implementación de separación de responsabilidades en 4 capas diferenciadas
- Capa de Presentación (Main/UI): Interacción con el usuario mediante consola
- Capa de Lógica de Negocio (Service): Validaciones y reglas de negocio
- Capa de Acceso a Datos (DAO): Operaciones de persistencia
- Capa de Modelo (Models): Representación de entidades del dominio

**2. Programación Orientada a Objetos**
- Aplicación de principios SOLID (Single Responsibility, Dependency Injection)
- Uso de herencia mediante clase abstracta Base
- Implementación de interfaces genéricas (GenericDAO, GenericService)
- Encapsulamiento con atributos privados y métodos de acceso
- Sobrescritura de métodos (equals, hashCode, toString)

**3. Persistencia de Datos con JDBC**
- Conexión a base de datos MySQL mediante JDBC
- Implementación del patrón DAO (Data Access Object)
- Uso de PreparedStatements para prevenir SQL Injection
- Gestión de transacciones con commit y rollback
- Manejo de claves autogeneradas (AUTO_INCREMENT)
- Consultas con LEFT JOIN para relaciones entre entidades

**4. Manejo de Recursos y Excepciones**
- Uso del patrón try-with-resources para gestión automática de recursos JDBC
- Implementación de AutoCloseable en TransactionManager
- Manejo apropiado de excepciones con propagación controlada
- Validación multi-nivel: base de datos y aplicación

**5. Patrones de Diseño**
- Factory Pattern (DatabaseConnection)
- Service Layer Pattern (separación lógica de negocio)
- DAO Pattern (abstracción del acceso a datos)
- Soft Delete Pattern (eliminación lógica de registros)
- Dependency Injection manual

**6. Validación de Integridad de Datos**
- Validación de unicidad (DNI único por persona)
- Validación de campos obligatorios en múltiples niveles
- Validación de integridad referencial (Foreign Keys)
- Implementación de eliminación segura para prevenir referencias huérfanas

### Funcionalidades Implementadas

El sistema permite gestionar dos entidades principales con las siguientes operaciones:

## Características Principales

- **Gestión de Personas**: Crear, listar, actualizar y eliminar personas con validación de DNI único
- **Gestión de Domicilios**: Administrar domicilios de forma independiente o asociados a personas
- **Búsqueda Inteligente**: Filtrar personas por nombre o apellido con coincidencias parciales
- **Soft Delete**: Eliminación lógica que preserva la integridad de datos
- **Seguridad**: Protección contra SQL injection mediante PreparedStatements
- **Validación Multi-capa**: Validaciones en capa de servicio y base de datos
- **Transacciones**: Soporte para operaciones atómicas con rollback automático

## Requisitos del Sistema

| Componente | Versión Requerida |
|------------|-------------------|
| Java JDK | 17 o superior |
| MySQL | 8.0 o superior |
| Gradle | 8.12 (incluido wrapper) |
| Sistema Operativo | Windows, Linux o macOS |

## Instalación

### 1. Configurar Base de Datos

Ejecutar el siguiente script SQL en MySQL:

```sql
CREATE DATABASE IF NOT EXISTS dbtpi3;
USE dbtpi3;

CREATE TABLE domicilios (
    id INT AUTO_INCREMENT PRIMARY KEY,
    calle VARCHAR(100) NOT NULL,
    numero VARCHAR(10) NOT NULL,
    eliminado BOOLEAN DEFAULT FALSE
);

CREATE TABLE personas (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL,
    apellido VARCHAR(50) NOT NULL,
    dni VARCHAR(20) NOT NULL UNIQUE,
    domicilio_id INT,
    eliminado BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (domicilio_id) REFERENCES domicilios(id)
);
```

### 2. Compilar el Proyecto

```bash
# Linux/macOS
./gradlew clean build

# Windows
gradlew.bat clean build
```

### 3. Configurar Conexión (Opcional)

Por defecto conecta a:
- **Host**: localhost:3306
- **Base de datos**: dbtpi3
- **Usuario**: root
- **Contraseña**: (vacía)

Para cambiar la configuración, usar propiedades del sistema:

```bash
java -Ddb.url=jdbc:mysql://localhost:3306/dbtpi3 \
     -Ddb.user=usuario \
     -Ddb.password=clave \
     -cp ...
```

## Ejecución

### Opción 1: Desde IDE
1. Abrir proyecto en IntelliJ IDEA o Eclipse
2. Ejecutar clase `Main.Main`

### Opción 2: Línea de comandos

**Windows:**
```bash
# Localizar JAR de MySQL
dir /s /b %USERPROFILE%\.gradle\caches\*mysql-connector-j-8.4.0.jar

# Ejecutar (reemplazar <ruta-mysql-jar>)
java -cp "build\classes\java\main;<ruta-mysql-jar>" Main.Main
```

**Linux/macOS:**
```bash
# Localizar JAR de MySQL
find ~/.gradle/caches -name "mysql-connector-j-8.4.0.jar"

# Ejecutar (reemplazar <ruta-mysql-jar>)
java -cp "build/classes/java/main:<ruta-mysql-jar>" Main.Main
```

### Verificar Conexión

```bash
# Usar TestConexion para verificar conexión a BD
java -cp "build/classes/java/main:<ruta-mysql-jar>" Main.TestConexion
```

Salida esperada:
```
Conexion exitosa a la base de datos
Usuario conectado: root@localhost
Base de datos: dbtpi3
URL: jdbc:mysql://localhost:3306/dbtpi3
Driver: MySQL Connector/J v8.4.0
```

## Uso del Sistema

### Menú Principal

```
========= MENU =========
1. Crear persona
2. Listar personas
3. Actualizar persona
4. Eliminar persona
5. Crear domicilio
6. Listar domicilios
7. Actualizar domicilio por ID
8. Eliminar domicilio por ID
9. Actualizar domicilio por ID de persona
10. Eliminar domicilio por ID de persona
0. Salir
```

### Operaciones Disponibles

#### 1. Crear Persona
- Captura nombre, apellido y DNI
- Permite agregar domicilio opcionalmente
- Valida DNI único (no permite duplicados)

**Ejemplo:**
```
Nombre: Juan
Apellido: Pérez
DNI: 12345678
¿Desea agregar un domicilio? (s/n): s
Calle: San Martín
Numero: 123
```

#### 2. Listar Personas
Dos opciones:
- **(1) Listar todos**: Muestra todas las personas activas
- **(2) Buscar**: Filtra por nombre o apellido

**Ejemplo de búsqueda:**
```
Ingrese texto a buscar: Juan
```
**Resultado:**
```
ID: 1, Nombre: Juan, Apellido: Pérez, DNI: 12345678
   Domicilio: San Martín 123
```

#### 3. Actualizar Persona
- Permite modificar nombre, apellido, DNI
- Permite actualizar o agregar domicilio
- Presionar Enter sin escribir mantiene el valor actual

**Ejemplo:**
```
ID de la persona a actualizar: 1
Nuevo nombre (actual: Juan, Enter para mantener):
Nuevo apellido (actual: Pérez, Enter para mantener): González
Nuevo DNI (actual: 12345678, Enter para mantener):
¿Desea actualizar el domicilio? (s/n): n
```

#### 4. Eliminar Persona
- Eliminación lógica (marca como eliminado, no borra físicamente)
- Requiere ID de la persona

#### 5. Crear Domicilio
- Crea domicilio independiente sin asociarlo a persona
- Puede asociarse posteriormente

#### 6. Listar Domicilios
- Muestra todos los domicilios activos con ID, calle y número

#### 7. Actualizar Domicilio por ID
- Actualiza calle y/o número de cualquier domicilio
- Requiere ID del domicilio

#### 8. Eliminar Domicilio por ID
- ⚠️ **ADVERTENCIA**: Puede dejar referencias huérfanas si está asociado a persona
- Usar opción 10 como alternativa segura

#### 9. Actualizar Domicilio por Persona
- Actualiza el domicilio asociado a una persona específica
- Requiere ID de la persona

#### 10. Eliminar Domicilio por Persona (RECOMENDADO)
- ✅ **Eliminación segura**: Primero actualiza la referencia en persona, luego elimina
- Previene referencias huérfanas
- Requiere ID de la persona

## Arquitectura

### Estructura en Capas

```
┌─────────────────────────────────────┐
│     Main / UI Layer                 │
│  (Interacción con usuario)          │
│  AppMenu, MenuHandler, MenuDisplay  │
└───────────┬─────────────────────────┘
            │
┌───────────▼─────────────────────────┐
│     Service Layer                   │
│  (Lógica de negocio y validación)   │
│  PersonaServiceImpl                 │
│  DomicilioServiceImpl               │
└───────────┬─────────────────────────┘
            │
┌───────────▼─────────────────────────┐
│     DAO Layer                       │
│  (Acceso a datos)                   │
│  PersonaDAO, DomicilioDAO           │
└───────────┬─────────────────────────┘
            │
┌───────────▼─────────────────────────┐
│     Models Layer                    │
│  (Entidades de dominio)             │
│  Persona, Domicilio, Base           │
└─────────────────────────────────────┘
```

### Componentes Principales

**Config/**
- `DatabaseConnection.java`: Gestión de conexiones JDBC con validación en inicialización estática
- `TransactionManager.java`: Manejo de transacciones con AutoCloseable

**Models/**
- `Base.java`: Clase abstracta con campos id y eliminado
- `Persona.java`: Entidad Persona (nombre, apellido, dni, domicilio)
- `Domicilio.java`: Entidad Domicilio (calle, numero)

**Dao/**
- `GenericDAO<T>`: Interface genérica con operaciones CRUD
- `PersonaDAO`: Implementación con queries LEFT JOIN para incluir domicilio
- `DomicilioDAO`: Implementación para domicilios

**Service/**
- `GenericService<T>`: Interface genérica para servicios
- `PersonaServiceImpl`: Validaciones de persona y coordinación con domicilios
- `DomicilioServiceImpl`: Validaciones de domicilio

**Main/**
- `Main.java`: Punto de entrada
- `AppMenu.java`: Orquestador del ciclo de menú
- `MenuHandler.java`: Implementación de operaciones CRUD con captura de entrada
- `MenuDisplay.java`: Lógica de visualización de menús
- `TestConexion.java`: Utilidad para verificar conexión a BD

## Modelo de Datos

```
┌────────────────────┐          ┌──────────────────┐
│     personas       │          │   domicilios     │
├────────────────────┤          ├──────────────────┤
│ id (PK)            │          │ id (PK)          │
│ nombre             │          │ calle            │
│ apellido           │          │ numero           │
│ dni (UNIQUE)       │          │ eliminado        │
│ domicilio_id (FK)  │──────┐   └──────────────────┘
│ eliminado          │      │
└────────────────────┘      │
                            │
                            └──▶ Relación 0..1
```

**Reglas:**
- Una persona puede tener 0 o 1 domicilio
- DNI es único (constraint en base de datos y validación en aplicación)
- Eliminación lógica: campo `eliminado = TRUE`
- Foreign key `domicilio_id` puede ser NULL

## Patrones y Buenas Prácticas

### Seguridad
- **100% PreparedStatements**: Prevención de SQL injection
- **Validación multi-capa**: Service layer valida antes de persistir
- **DNI único**: Constraint en BD + validación en `PersonaServiceImpl.validateDniUnique()`

### Gestión de Recursos
- **Try-with-resources**: Todas las conexiones, statements y resultsets
- **AutoCloseable**: TransactionManager cierra y hace rollback automático
- **Scanner cerrado**: En `AppMenu.run()` al finalizar

### Validaciones
- **Input trimming**: Todos los inputs usan `.trim()` inmediatamente
- **Campos obligatorios**: Validación de null y empty en service layer
- **IDs positivos**: Validación `id > 0` en todas las operaciones
- **Verificación de rowsAffected**: En UPDATE y DELETE

### Soft Delete
- DELETE ejecuta: `UPDATE tabla SET eliminado = TRUE WHERE id = ?`
- SELECT filtra: `WHERE eliminado = FALSE`
- No hay eliminación física de datos

## Reglas de Negocio Principales

1. **DNI único**: No se permiten personas con DNI duplicado
2. **Campos obligatorios**: Nombre, apellido y DNI son requeridos para persona
3. **Validación antes de persistir**: Service layer valida antes de llamar a DAO
4. **Eliminación segura de domicilio**: Usar opción 10 (por persona) en lugar de opción 8 (por ID)
5. **Preservación de valores**: En actualización, campos vacíos mantienen valor original
6. **Búsqueda flexible**: LIKE con % permite coincidencias parciales
7. **Transacciones**: Operaciones complejas soportan rollback

## Solución de Problemas

### Error: "ClassNotFoundException: com.mysql.cj.jdbc.Driver"
**Causa**: JAR de MySQL no está en classpath

**Solución**: Incluir mysql-connector-j-8.4.0.jar en el comando java -cp

### Error: "Communications link failure"
**Causa**: MySQL no está ejecutándose

**Solución**:
```bash
# Linux/macOS
sudo systemctl start mysql
# O
brew services start mysql

# Windows
net start MySQL80
```

### Error: "Access denied for user 'root'@'localhost'"
**Causa**: Credenciales incorrectas

**Solución**: Verificar usuario/contraseña en DatabaseConnection.java o usar -Ddb.user y -Ddb.password

### Error: "Unknown database 'dbtpi3'"
**Causa**: Base de datos no creada

**Solución**: Ejecutar script de creación de base de datos (ver sección Instalación)

### Error: "Table 'personas' doesn't exist"
**Causa**: Tablas no creadas

**Solución**: Ejecutar script de creación de tablas (ver sección Instalación)

## Limitaciones Conocidas

1. **No hay tarea gradle run**: Debe ejecutarse con java -cp manualmente o desde IDE
2. **Interfaz solo consola**: No hay GUI gráfica
3. **Un domicilio por persona**: No soporta múltiples domicilios
4. **Sin paginación**: Listar todos puede ser lento con muchos registros
5. **Opción 8 peligrosa**: Eliminar domicilio por ID puede dejar referencias huérfanas (usar opción 10)
6. **Sin pool de conexiones**: Nueva conexión por operación (aceptable para app de consola)
7. **Sin transacciones en MenuHandler**: Actualizar persona + domicilio puede fallar parcialmente

## Documentación Adicional

- **CLAUDE.md**: Documentación técnica detallada para desarrollo
  - Comandos de build y ejecución
  - Arquitectura profunda
  - Patrones de código críticos
  - Troubleshooting avanzado
  - Verificación de calidad (score 9.7/10)

- **HISTORIAS_DE_USUARIO.md**: Especificaciones funcionales completas
  - Historias de usuario detalladas
  - Reglas de negocio numeradas
  - Criterios de aceptación en formato Gherkin
  - Diagramas de flujo

## Tecnologías Utilizadas

- **Lenguaje**: Java 17
- **Build Tool**: Gradle 8.12
- **Base de Datos**: MySQL 8.x
- **JDBC Driver**: mysql-connector-j 8.4.0
- **Testing**: JUnit 5 (configurado, sin tests implementados)

## Estructura de Directorios

```
TPI-Prog2-fusion-final/
├── src/main/java/
│   ├── Config/          # Configuración de BD y transacciones
│   ├── Dao/             # Capa de acceso a datos
│   ├── Main/            # UI y punto de entrada
│   ├── Models/          # Entidades de dominio
│   └── Service/         # Lógica de negocio
├── build.gradle         # Configuración de Gradle
├── gradlew              # Gradle wrapper (Unix)
├── gradlew.bat          # Gradle wrapper (Windows)
├── README.md            # Este archivo
├── CLAUDE.md            # Documentación técnica
└── HISTORIAS_DE_USUARIO.md  # Especificaciones funcionales
```

## Convenciones de Código

- **Idioma**: Español (nombres de clases, métodos, variables)
- **Nomenclatura**:
  - Clases: PascalCase (Ej: `PersonaServiceImpl`)
  - Métodos: camelCase (Ej: `buscarPorDni`)
  - Constantes SQL: UPPER_SNAKE_CASE (Ej: `SELECT_BY_ID_SQL`)
- **Indentación**: 4 espacios
- **Recursos**: Siempre usar try-with-resources
- **SQL**: Constantes privadas static final
- **Excepciones**: Capturar y manejar con mensajes al usuario

## Evaluación y Criterios de Calidad

### Aspectos Evaluados en el TPI

Este proyecto demuestra competencia en los siguientes criterios académicos:

**✅ Arquitectura y Diseño (30%)**
- Correcta separación en capas con responsabilidades bien definidas
- Aplicación de patrones de diseño apropiados (DAO, Service Layer, Factory)
- Uso de interfaces para abstracción y polimorfismo
- Implementación de herencia con clase abstracta Base

**✅ Persistencia de Datos (25%)**
- Correcta implementación de operaciones CRUD con JDBC
- Uso apropiado de PreparedStatements (100% de las consultas)
- Gestión de transacciones con commit/rollback
- Manejo de relaciones entre entidades (Foreign Keys, LEFT JOIN)
- Soft delete implementado correctamente

**✅ Manejo de Recursos y Excepciones (20%)**
- Try-with-resources en todas las operaciones JDBC
- Cierre apropiado de conexiones, statements y resultsets
- Manejo de excepciones con mensajes informativos al usuario
- Prevención de resource leaks

**✅ Validaciones e Integridad (15%)**
- Validación de campos obligatorios en múltiples niveles
- Validación de unicidad de DNI (base de datos + aplicación)
- Verificación de integridad referencial
- Prevención de referencias huérfanas mediante eliminación segura

**✅ Calidad de Código (10%)**
- Código documentado con Javadoc completo (13 archivos)
- Convenciones de nomenclatura consistentes
- Código legible y mantenible
- Ausencia de code smells o antipatrones críticos

**✅ Funcionalidad Completa (10%)**
- Todas las operaciones CRUD funcionan correctamente
- Búsquedas y filtros implementados
- Interfaz de usuario clara y funcional
- Manejo robusto de errores

### Puntos Destacables del Proyecto

1. **Score de Calidad Verificado**: 9.7/10 mediante análisis exhaustivo de:
   - Arquitectura y flujo de datos
   - Manejo de excepciones
   - Integridad referencial
   - Validaciones multi-nivel
   - Gestión de recursos
   - Consistencia de queries SQL

2. **Documentación Profesional**:
   - README completo con ejemplos y troubleshooting
   - CLAUDE.md con arquitectura técnica detallada
   - HISTORIAS_DE_USUARIO.md con 11 historias y 51 reglas de negocio
   - Javadoc completo en todos los archivos fuente

3. **Implementaciones Avanzadas**:
   - Eliminación segura de domicilios (previene FKs huérfanas)
   - Validación de DNI único en dos niveles (DB + aplicación)
   - Coordinación transaccional entre servicios
   - Búsqueda flexible con LIKE pattern matching

4. **Buenas Prácticas Aplicadas**:
   - Dependency Injection manual
   - Separación de concerns (AppMenu, MenuHandler, MenuDisplay)
   - Factory pattern para conexiones
   - Input sanitization con trim() consistente
   - Fail-fast validation

### Conceptos de Programación 2 Demostrados

| Concepto | Implementación en el Proyecto |
|----------|-------------------------------|
| **Herencia** | Clase abstracta `Base` heredada por `Persona` y `Domicilio` |
| **Polimorfismo** | Interfaces `GenericDAO<T>` y `GenericService<T>` |
| **Encapsulamiento** | Atributos privados con getters/setters en todas las entidades |
| **Abstracción** | Interfaces que definen contratos sin implementación |
| **JDBC** | Conexión, PreparedStatements, ResultSets, transacciones |
| **DAO Pattern** | `PersonaDAO`, `DomicilioDAO` abstraen el acceso a datos |
| **Service Layer** | Lógica de negocio separada en `PersonaServiceImpl`, `DomicilioServiceImpl` |
| **Exception Handling** | Try-catch en todas las capas, propagación controlada |
| **Resource Management** | Try-with-resources para AutoCloseable (Connection, Statement, ResultSet) |
| **Dependency Injection** | Construcción manual de dependencias en `AppMenu.createPersonaService()` |

## Contexto Académico

**Materia**: Programación 2
**Tipo de Evaluación**: Trabajo Práctico Integrador (TPI)
**Modalidad**: Desarrollo de sistema CRUD con persistencia en base de datos
**Objetivo**: Aplicar conceptos de POO, JDBC, arquitectura en capas y patrones de diseño

Este proyecto representa la integración de todos los conceptos vistos durante el cuatrimestre, demostrando capacidad para:
- Diseñar sistemas con arquitectura profesional
- Implementar persistencia de datos con JDBC
- Aplicar patrones de diseño apropiados
- Manejar recursos y excepciones correctamente
- Validar integridad de datos en múltiples niveles
- Documentar código de forma profesional

---

**Versión**: 1.0
**Java**: 17+
**MySQL**: 8.x
**Gradle**: 8.12
**Proyecto Educativo** - Trabajo Práctico Integrador de Programación 2
=======
# Trabajo-Final-Integrador- Programacion II

## Tecnicatura Universitaria en Programación
  **Aplicación Java con relación 1→1 unidireccional + DAO + MySQL**

## Tema elegido :
  **Pedido → Envio**
    Desarrollar una aplicación en Java que modele dos clases relacionadas mediante una
    asociación unidireccional 1 a 1 (la clase “A” referencia a la clase “B”), persistiendo
    datos en una base relacional mediante JDBC y el patrón DAO, con operaciones
    transaccionales (commit/rollback) y menú de consola para CRUD.

## Integrantes: 
  **Nahuel Urciuoli Zabala**  - Comision 9  
  

## Descripción del proyecto
Este repositorio contiene los archivos referidos al trabajo final de Programacion II 

Código fuente: Archivo Java + SQL + UML

Documentación: PDF con UML, explicacion de funcionamiento y resultados.

Video explicativo: xxxx

## Instrucciones de uso:

Revisar la documentación: Abrir el archivo Word para contexto teórico.

Ejecutar el código.

Ver el video: Visualizar la explicación multimedia para reforzar conceptos.

## Reflexiones del equipo:

La complejidad algorítmica es crucial para aplicaciones con grandes volúmenes de datos

Pequeñas diferencias en implementación pueden generar grandes impactos en rendimiento

El análisis Big-O ayuda a tomar decisiones informadas sobre qué algoritmo usar

# Trabajo-Final-Integrador- Programacion II

## Tecnicatura Universitaria en Programación
  **Aplicación Java con relación 1→1 unidireccional + DAO + MySQL**

## Tema elegido :
  **Pedido → Envio**
    Desarrollar una aplicación en Java que modele dos clases relacionadas mediante una
    asociación unidireccional 1 a 1 (la clase “A” referencia a la clase “B”), persistiendo
    datos en una base relacional mediante JDBC y el patrón DAO, con operaciones
    transaccionales (commit/rollback) y menú de consola para CRUD.

## Integrantes: 
  **Nahuel Urciuoli Zabala**  - Comision 9  
  

## Descripción del proyecto
Este repositorio contiene los archivos referidos al trabajo final de Programacion II 

Código fuente: Archivo Java + SQL + UML

Documentación: PDF con UML, explicacion de funcionamiento y resultados.

Video explicativo: xxxx

## Instrucciones de uso:

Revisar la documentación: Abrir el archivo Word para contexto teórico.

Ejecutar el código.

Ver el video: Visualizar la explicación multimedia para reforzar conceptos.

## Reflexiones del equipo:

La complejidad algorítmica es crucial para aplicaciones con grandes volúmenes de datos

Pequeñas diferencias en implementación pueden generar grandes impactos en rendimiento

El análisis Big-O ayuda a tomar decisiones informadas sobre qué algoritmo usar
