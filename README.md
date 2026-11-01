# Sistema de préstamo de equipos tecnológicos

Proyecto académico basado en el documento de requerimientos de la Institución Universitaria Antonio José Camacho (versión 1.0, 05/10/2026). Permite consultar el inventario, solicitar un equipo, decidir la solicitud, registrar entrega y devolución, reportar daños y conservar el historial.

## Tecnologías y estructura

- Java 21, Spring Boot, Spring Web, Spring Security, Spring Data JPA y Bean Validation.
- Interfaz HTML5, CSS3 y JavaScript ES6 servida por la misma aplicación. Consume la API REST con `fetch`.
- H2 persistente para desarrollo local y PostgreSQL configurable por ambiente.
- Inicio de sesión JWT; perfiles `ADMINISTRADOR`, `ENCARGADO`, `ESTUDIANTE` y `DOCENTE`.

```text
src/main/java/co/edu/uniajc/prestamos/   API, servicios, persistencia y seguridad
src/main/resources/static/                interfaz web
src/main/resources/application*.yml       configuración por ambiente
src/test/                                 pruebas automatizadas
docs/                                     arquitectura, trazabilidad y plan funcional
```

El [documento de requerimientos original](docs/02_Requerimientos_Sistema_Prestamo_Equipos_Tecnologicos.docx) se incluye en `docs/`. La [matriz de trazabilidad](docs/trazabilidad.md) relaciona las **16 historias de usuario implementadas** con sus requisitos y pruebas.

Para preparar una exposición y ubicar cada pantalla en el código, consulta la [guía de exposición del proyecto](docs/guia_exposicion.docx).

## Ejecutar en local

Requisito: **JDK 21**. El proyecto incluye Maven Wrapper, así que no hace falta instalar Maven por separado. Desde esta carpeta, en PowerShell:

```powershell
.\scripts\iniciar-windows.ps1
```

El script encuentra el JDK 21 preparado en este equipo o usa `JAVA_HOME` en otro equipo. También acepta `-Modo Pruebas` y `-Modo Paquete`. Si Java 21 ya está configurado, puede usarse directamente `.\mvnw.cmd spring-boot:run`. Tras empaquetar, el ejecutable queda en `target/sistema-prestamo-equipos-1.0.0.jar` y se inicia con `java -jar target/sistema-prestamo-equipos-1.0.0.jar`.

Abrir [http://localhost:8080](http://localhost:8080). El perfil por defecto es `dev`: crea las tablas con JPA y usa una base H2 persistente en `./data/prestamos`. No hace falta instalar una base de datos para la demostración local. Al detener y volver a iniciar, los datos permanecen en ese archivo.

La consola web H2 está deshabilitada. Los datos locales se almacenan en `data/prestamos.mv.db`, archivo excluido de Git.

### Cuentas de demostración (`dev`)

| Rol | Correo | Contraseña |
| --- | --- | --- |
| Administrador | `admin@uniajc.edu.co` | `Admin123!` |
| Encargado | `encargado@uniajc.edu.co` | `Encargado123!` |
| Estudiante | `estudiante@uniajc.edu.co` | `Estudiante123!` |
| Docente | `docente@uniajc.edu.co` | `Docente123!` |

Estas cuentas son solo para desarrollo y demostración. Se crean como datos semilla en `dev`, junto con categorías y equipos. No se deben usar en un despliegue real.

### Perfiles y duración del préstamo

Estudiantes y docentes pueden consultar el catálogo, crear solicitudes y ver únicamente sus propios historiales. El préstamo estudiantil admite **hasta 7 días** entre inicio y devolución esperada; el docente admite **hasta 30 días**. Se calcula `fechaFin - fechaInicio`: exactamente 7 o 30 días se acepta, respectivamente; 8 o 31 se rechaza. La interfaz consulta `GET /api/configuracion/prestamos` para mostrar los límites, y el backend vuelve a validarlos en cada solicitud. El administrador gestiona cuentas y equipos; el encargado decide solicitudes, entrega y recibe equipos. Estos plazos son una ampliación pedida para este proyecto y no estaban fijados por el documento original.

Los valores predeterminados se cambian con `APP_MAX_DIAS_ESTUDIANTE` y `APP_MAX_DIAS_DOCENTE` antes de iniciar el servidor. Ambos deben ser positivos y el máximo docente debe superar el estudiantil. En una base H2 anterior, los usuarios existentes con rol `SOLICITANTE` pasan a `ESTUDIANTE` conservando su historial; no se crean nuevas cuentas con el rol anterior. El límite se revalida al aprobar y entregar: una solicitud anterior que exceda el máximo vigente no puede avanzar y debe cancelarse para crear otra con fechas válidas.

### Flujo de demostración

1. Sin iniciar sesión, consultar y filtrar el catálogo y abrir una ficha de equipo.
2. Entrar como estudiante y crear una solicitud de hasta 7 días; repetir como docente con una de hasta 30 días. Probar que 8 y 31 días, respectivamente, se rechazan.
3. Entrar como encargado; abrir pendientes, aprobar con observación y registrar la entrega. El equipo pasa a `PRESTADO`.
4. Devolver el equipo. En buen estado vuelve a `DISPONIBLE`; una novedad no crítica queda registrada y el equipo sigue `DISPONIBLE`; un daño lo pasa a `MANTENIMIENTO`.
5. Entrar como administrador y consultar inventario, usuarios, indicadores, historial y auditoría.

## PostgreSQL

Crear previamente una base llamada `prestamos` y un usuario con permisos sobre ella. El perfil `prod` usa PostgreSQL y crea/actualiza el esquema mediante JPA para facilitar la instalación académica. En PowerShell:

```powershell
$env:SPRING_PROFILES_ACTIVE = 'prod'
$env:DB_URL = 'jdbc:postgresql://localhost:5432/prestamos'
$env:DB_USER = 'usuario_bd'
$env:DB_PASSWORD = 'contraseña_bd'
$env:APP_JWT_SECRET = 'reemplazar-por-un-secreto-largo-y-aleatorio'
$env:APP_BOOTSTRAP_ADMIN_EMAIL = 'admin@mi-institucion.edu.co'
$env:APP_BOOTSTRAP_ADMIN_PASSWORD = 'contraseña-inicial-privada'
$env:APP_BOOTSTRAP_ADMIN_NAME = 'Administrador inicial'
.\mvnw.cmd spring-boot:run
```

`APP_JWT_SECRET` y la contraseña inicial deben ser privadas. Si la base está vacía, las tres variables `APP_BOOTSTRAP_ADMIN_*` crean el administrador inicial; el perfil `prod` no carga las cuentas de demostración. Después del primer arranque, retirar la contraseña inicial del entorno y cambiarla desde la administración si la aplicación lo permite. El perfil `prod` deshabilita la consola H2. También se puede ajustar `APP_TIMEZONE` (predeterminado `America/Bogota`), `APP_JWT_HOURS` (8), `APP_APPROVAL_HOURS` (48), `APP_MAX_DIAS_ESTUDIANTE` (7) y `APP_MAX_DIAS_DOCENTE` (30). Para acceso externo, publicar la aplicación detrás de HTTPS y limitar el origen permitido para solicitudes del navegador.

## Pruebas y evidencia

```powershell
.\mvnw.cmd test
.\mvnw.cmd clean package
```

Las pruebas automáticas validan reglas del backend y generan reportes en `target/surefire-reports/`. La [evidencia de ejecución](docs/evidencia-ejecucion.md) registra los resultados de integración y del recorrido HTTP completo. El [plan manual](docs/pruebas-funcionales.md) contiene pasos y resultados esperados; sus casos conservan su estado hasta que se registre una comprobación manual específica. La [arquitectura](docs/arquitectura.md) y el [modelo de datos](docs/modelo-datos.md) explican el diseño y las decisiones tomadas en los puntos abiertos de la especificación.

Con el servidor iniciado en perfil `dev`, Python 3.10 o posterior puede ejecutar la prueba integral HTTP:

```powershell
python scripts/prueba_integral.py --url http://localhost:8080 --output docs/evidencia-api.json
```

El script usa las cuentas semilla. Se pueden sustituir con `SMOKE_ADMIN_EMAIL`, `SMOKE_ADMIN_PASSWORD`, `SMOKE_ENCARGADO_EMAIL`, `SMOKE_ENCARGADO_PASSWORD`, `SMOKE_ESTUDIANTE_EMAIL`, `SMOKE_ESTUDIANTE_PASSWORD`, `SMOKE_DOCENTE_EMAIL` y `SMOKE_DOCENTE_PASSWORD`. Verifica autenticación, permisos, los límites de ambos perfiles, inventario, solicitudes, entrega, devolución, reportes y auditoría. Crea usuarios, categorías y equipos de prueba con un sufijo único; esos registros quedan en la base H2 para poder inspeccionar la evidencia. Sale con código 1 ante el primer fallo y con 0 si todos sus controles pasan. El JSON de `--output` se genera al terminar correctamente en `docs/evidencia-api.json`; ese archivo se puede conservar como evidencia de la ejecución.

Las [capturas de escritorio](docs/capturas/catalogo-escritorio.png), [móvil](docs/capturas/catalogo-movil.png), [perfil estudiante](docs/capturas/perfil-estudiante.png), [perfil docente](docs/capturas/perfil-docente.png) y [bandeja del encargado](docs/capturas/solicitudes-encargado.png) muestran la interfaz probada.

## Alcance y reglas importantes

- Cada solicitud corresponde a un equipo, según HU-05 y el modelo de datos. Se puede crear otra solicitud para otro equipo.
- Solo el estudiante o docente titular puede cancelar una solicitud propia `PENDIENTE`.
- Solo el encargado puede aprobar/rechazar y registrar entrega/devolución.
- La entrega exige aprobación y disponibilidad confirmada en el backend. No puede haber dos préstamos activos para un equipo.
- El historial y la auditoría permanecen después de la devolución.
- RFID, biometría, cobros, facturación, contabilidad e integración institucional externa están fuera del alcance definido.

## API REST

La interfaz usa respuestas JSON; los errores devuelven `error` y `message`. Las fechas se envían en formato ISO. Las rutas y ejemplos de uso se detallan en [API REST](docs/api-rest.md).
