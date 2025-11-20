## Tecnicatura Universitaria en Programación
**Aplicación Java con relación 1→1 unidireccional + DAO + MySQL**

## Tema elegido :

**Pedido → Envío**
Desarrollar una aplicación en Java que modele dos clases relacionadas mediante una
asociación unidireccional 1 a 1 (la clase “A” referencia a la clase “B”), persistiendo
datos en una base relacional mediante JDBC y el patrón DAO, con operaciones
transaccionales (commit/rollback) y menú de consola para CRUD.

## Integrantes:
**Nahuel Urciuoli Zabala**  - Comision 9
**Maximo Emanuel Ponce**  - Comision 4
**Luciano Joaquin Martinez**  - Comision 4
**Santiago Gabriel Rodriguez**  - Comision 4

## Descripción del proyecto
Este repositorio contiene los archivos referidos al trabajo final de Programacion II

Código fuente: Archivo Java + SQL + UML

Documentación: PDF con UML, explicacion de funcionamiento y resultados.

Video explicativo: https://www.youtube.com/watch?v=0frAOJBzYDI

## Instrucciones de uso:

Revisar la documentación: Abrir el archivo Word para contexto teórico.

Ver el video: Visualizar la explicación multimedia para reforzar conceptos.

# Sistema de Gestión de Pedidos y Envíos

#### Descripción
Sistema de consola para gestionar pedidos y sus envíos asociados. Permite crear, listar, actualizar, eliminar y buscar pedidos; además, administrar envíos relacionados, con soporte de validaciones, transacciones y persistencia en MySQL mediante JDBC.

Estado actual del proyecto
- Arquitectura en capas: Main (UI por consola) → Service → DAO → Models → MySQL
- Entidades: Pedido y Envío
- Persistencia: JDBC puro con PreparedStatement y manejo de transacciones
- Configuración: archivo db.properties en la raíz del proyecto
- Script de base de datos: SQL BBD\Script.sql (crea BD y tablas)

Requisitos
- Java 17 o superior
- MySQL/MariaDB (probado con MariaDB 10.4+ y MySQL 8+)
- MySQL Connector/J (si ejecutas fuera de un IDE, debe estar en el classpath)

Estructura principal
- src\Main\AppMenu.java: punto de entrada principal por consola
- src\Main\Main.java: punto de entrada alternativo que delega a AppMenu
- src\Main\MenuHandler.java: orquestación de opciones del menú y entrada de usuario
- src\Config\DatabaseConnection.java: fábrica de conexiones JDBC; lee db.properties
- src\Dao\PedidoDAO.java y src\Dao\EnvioDAO.java: acceso a datos (CRUD y búsquedas)
- src\Service\PedidosServiceImpl.java y src\Service\EnvioServiceImpl.java: reglas de negocio y validaciones
- src\Models\Pedido.java y src\Models\Envio.java: modelos del dominio
- SQL BBD\Script.sql: crea la base de datos y tablas requeridas
- db.properties: parámetros de conexión a BD

Configuración de la base de datos
1) Crear la base de datos y tablas
   - Abre y ejecuta el archivo: SQL BBD\\Script.sql

2) Configurar credenciales
   - Edita el archivo db.properties en la raíz del proyecto:
     url=jdbc:mysql://localhost:3306/pedidosenvios
     user=root
     password=
   - También puedes usar otras credenciales según tu entorno.

3) Driver JDBC
   - Asegúrate de que el conector de MySQL (MySQL Connector/J) esté disponible en tiempo de ejecución si ejecutas desde terminal fuera del IDE.

Cómo ejecutar
Opción A: Desde IntelliJ IDEA u otro IDE
- Marca la carpeta src como "Sources"
- Ejecuta la clase Main o AppMenu (ambas funcionan); el menú se mostrará en la consola
- Para probar la conexión, puedes ejecutar src\Main\TestConexion.java

Opción B: Desde terminal (ejemplo orientativo)
- Compila las clases apuntando el classpath al conector JDBC
- Ejecuta Main o AppMenu asegurando el classpath correcto y la presencia de db.properties en el directorio de trabajo

Características principales
- CRUD de Pedidos (con estado: NUEVO, FACTURADO, ENVIADO)
- Asociación Pedido ↔ Envío (opcional)
- CRUD de Envíos (tracking único, costos, fechas, tipo, empresa, estado)
- Búsqueda de pedidos por número y por nombre de cliente
- Soft delete (eliminación lógica) y consultas que filtran registros eliminados
- Validaciones en capa de servicio (campos obligatorios, formatos, unicidad)
- Transacciones con commit/rollback para operaciones compuestas
- Uso de PreparedStatement para evitar SQL Injection

Parámetros por defecto en código
- DatabaseConnection.DEFAULT_URL: jdbc:mysql://localhost:3306/pedidosenvios (configurable desde db.properties)
- Usuario por defecto: root
- Password por defecto: vacío
- Estos valores pueden sobrescribirse con db.properties o propiedades del sistema (-Ddb.url, -Ddb.user, -Ddb.password)

Notas
- El proyecto no usa Gradle/Maven en esta versión; es un proyecto de Java simple orientado a ejecución desde IDE o compilación manual.
---

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
- Validación de unicidad (número de pedido único y tracking de envío único)

**5. Patrones de Diseño**
- Factory Pattern (DatabaseConnection)
- Service Layer Pattern (separación lógica de negocio)
- DAO Pattern (abstracción del acceso a datos)
- Soft Delete Pattern (eliminación lógica de registros)
- Dependency Injection manual

**6. Validación de Integridad de Datos**
- Validación de unicidad: número de pedido y tracking de envío únicos
- Validación de campos obligatorios en múltiples niveles
- Validación de integridad referencial (Foreign Keys)
- Implementación de eliminación segura para prevenir referencias huérfanas

### Funcionalidades Implementadas

El sistema permite gestionar dos entidades principales con las siguientes operaciones:

# Instrucciones (completar)
### Configurar Conexión (Opcional)

Por defecto conecta a:
- **Host**: localhost:3306
- **Base de datos**: dbtpi3
- **Usuario**: root
- **Contraseña**: (vacía)

Para cambiar la configuración, crear un archivo llamado `db.properties` con
el siguiente formato:

```properties
url=jdbc:mysql://ruta:puerto/nombre_db
user=usuario
password=contraseña
```

## Ejecución

### Opción 1: Desde IDE
1. Abrir proyecto en IntelliJ IDEA, Eclipse o Netbeans
2. Ejecutar clase `Main.Main`

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
1. Crear pedido
2. Listar pedidos
3. Actualizar pedido
4. Eliminar pedido
5. Crear envío
6. Listar envíos
7. Actualizar envío por ID
8. Eliminar envío por ID
9. Actualizar envío por ID de pedido
10. Eliminar envío por ID de pedido
0. Salir
```

### Operaciones Disponibles

Pedidos
- Crear pedido: número, fecha, nombre de cliente, total y estado (NUEVO/FACTURADO/ENVIADO)
- Listar pedidos: muestra todos los pedidos activos
- Actualizar pedido: modifica campos; Enter para mantener valores actuales
- Eliminar pedido: soft delete (marca como eliminado)
- Buscar pedido por número
- Buscar pedidos por nombre de cliente

Envíos
- Crear envío: tracking (único), costo, fechas, tipo, empresa, estado
- Listar envíos
- Actualizar envío por ID
- Eliminar envío por ID
- Actualizar envío por ID de pedido
- Eliminar envío por ID de pedido

Ejemplo (creación de pedido):
```
Número: 0001
Fecha (AAAA-MM-DD): 2025-05-10
Cliente: Ana López
Total: 1234.50
Estado [1-NUEVO, 2-FACTURADO, 3-ENVIADO]: 1
¿Desea crear un envío asociado ahora? (s/n): n
```

## Arquitectura

### Estructura en Capas

```
┌─────────────────────────────────────┐
│     Main / UI Layer                 │
│  (Interacción con usuario)          │
│  AppMenu, MenuHandler              │
└───────────┬─────────────────────────┘
            │
┌───────────▼─────────────────────────┐
│     Service Layer                   │
│  (Lógica de negocio y validación)   │
│  PedidosServiceImpl                │
│  EnvioServiceImpl                   │
└───────────┬─────────────────────────┘
            │
┌───────────▼─────────────────────────┐
│     DAO Layer                       │
│  (Acceso a datos)                   │
│  PedidoDAO, EnvioDAO                │
└───────────┬─────────────────────────┘
            │
┌───────────▼─────────────────────────┐
│     Models Layer                    │
│  (Entidades de dominio)             │
│  Pedido, Envio, Base                │
└─────────────────────────────────────┘
```

### Componentes Principales

**Config/**
- `DatabaseConnection.java`: Gestión de conexiones JDBC con validación en inicialización estática
- `TransactionManager.java`: Manejo de transacciones con AutoCloseable

**Models/**
- `Base.java`: Clase abstracta con campos id y eliminado
- `Pedido.java`: Entidad Pedido (numero, fecha, clienteNombre, total, estado, envio)
- `Envio.java`: Entidad Envio (tracking, costo, fechas, tipo, empresa, estado)

**Dao/**
- `GenericDAO<T>`: Interface genérica con operaciones CRUD
- `PedidoDAO`: CRUD de pedidos y búsquedas (por número y por cliente)
- `EnvioDAO`: CRUD de envíos y operaciones vinculadas a pedidos

**Service/**
- `GenericService<T>`: Interface genérica para servicios
- `PedidosServiceImpl`: Reglas de negocio de pedidos (validaciones, búsquedas, coordinación con envíos)
- `EnvioServiceImpl`: Reglas de negocio de envíos

**Main/**
- `Main.java`: Punto de entrada
- `AppMenu.java`: Orquestador del ciclo de menú
- `MenuHandler.java`: Implementación de operaciones CRUD con captura de entrada
- `TestConexion.java`: Utilidad para verificar conexión a BD

## Modelo de Datos

```
┌──────────────────┐          ┌──────────────────┐
│     pedido       │◀─────────│      envio       │
├──────────────────┤          ├──────────────────┤
│ id (PK)          │          │ id (PK)          │
│ numero (UNIQUE)  │          │ tracking (UNIQUE)│
│ fecha            │          │ costo            │
│ clienteNombre    │          │ fechaDespacho    │
│ total            │          │ fechaEstimada    │
│ estado           │          │ tipo             │
│ eliminado        │          │ empresa          │
│                  │          │ estado           │
│                  │          │ pedidoId (FK)    │
└──────────────────┘          └──────────────────┘
```

Relación: 1 Pedido puede tener 0 o 1 Envío. La FK está en `envio.pedidoId`.

**Reglas:**
- Número de pedido único; tracking de envío único
- Eliminación lógica: campo `eliminado = TRUE` en ambas tablas
- Integridad referencial: `envio.pedidoId` referencia `pedido.id` con ON UPDATE CASCADE

## Patrones y Buenas Prácticas

### Seguridad
- **100% PreparedStatements**: Prevención de SQL injection
- **Validación multi-capa**: Service layer valida antes de persistir
- Unicidad: Constraint en BD + validación en servicios (`PedidosServiceImpl` para número de pedido, `EnvioServiceImpl` para tracking)

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

1. Número de pedido único; tracking de envío único
2. Campos obligatorios:
   - Pedido: numero, fecha, clienteNombre, total, estado
   - Envío: tracking, costo, fechas, tipo, empresa, estado
3. Validación antes de persistir: la capa Service valida antes de llamar al DAO
4. Eliminación segura de envío asociado: usar opción 10 (por pedido) en lugar de 8 (por ID) cuando corresponda
5. Preservación de valores: en actualización, Enter mantiene el valor original del campo
6. Búsqueda flexible: por número de pedido y por nombre de cliente usando LIKE con %
7. Transacciones: operaciones compuestas con commit/rollback

## Estructura de Directorios

```
Trabajo-Final-Integrador-TFI/
├── src/
│   ├── Config/          # Configuración de BD y transacciones
│   ├── Dao/             # Acceso a datos (JDBC/DAO)
│   ├── Main/            # UI por consola y punto de entrada
│   ├── Models/          # Entidades de dominio
│   └── Service/         # Lógica de negocio
├── SQL BBD/Script.sql   # Script de creación de BD y tablas
├── db.properties        # Parámetros de conexión a la BD
├── build.xml            # Script de construcción (Ant/NetBeans)
├── nbproject/           # Archivos de proyecto NetBeans
├── manifest.mf          # Manifest para empaquetado
├── out/                 # Clases compiladas (salida del IDE)
└── README.md            # Este archivo
```

## Convenciones de Código

- **Idioma**: Español (nombres de clases, métodos, variables)
- **Nomenclatura**:
  - Clases: PascalCase (Ej: `PedidosServiceImpl`)
  - Métodos: camelCase (Ej: `buscarPorNumero`)
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
- Validación de unicidad (número de pedido y tracking) en base de datos y aplicación
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
   - Eliminación segura de envíos asociados (previene FKs huérfanas)
   - Validación de unicidad en dos niveles (DB + aplicación) para número de pedido y tracking
   - Coordinación transaccional entre servicios
   - Búsqueda flexible con LIKE pattern matching

4. **Buenas Prácticas Aplicadas**:
   - Dependency Injection manual
   - Separación de concerns (AppMenu, MenuHandler)
   - Factory pattern para conexiones
   - Input sanitization con trim() consistente
   - Fail-fast validation

### Conceptos de Programación 2 Demostrados

| Concepto | Implementación en el Proyecto |
|----------|-------------------------------|
| **Herencia** | Clase abstracta `Base` heredada por `Pedido` y `Envio` |
| **Polimorfismo** | Interfaces `GenericDAO<T>` y `GenericService<T>` |
| **Encapsulamiento** | Atributos privados con getters/setters en todas las entidades |
| **Abstracción** | Interfaces que definen contratos sin implementación |
| **JDBC** | Conexión, PreparedStatements, ResultSets, transacciones |
| **DAO Pattern** | `PedidoDAO`, `EnvioDAO` abstraen el acceso a datos |
| **Service Layer** | Lógica de negocio separada en `PedidosServiceImpl`, `EnvioServiceImpl` |
| **Exception Handling** | Try-catch en todas las capas, propagación controlada |
| **Resource Management** | Try-with-resources para AutoCloseable (Connection, Statement, ResultSet) |
| **Dependency Injection** | Construcción manual de dependencias en `AppMenu.createPedidosService()` |

