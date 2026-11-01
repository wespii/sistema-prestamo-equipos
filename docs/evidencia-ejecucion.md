# Evidencia de ejecución

Fecha de verificación: **26 de octubre de 2026**. Ambiente: Windows 11, Eclipse Temurin JDK 21.0.12.1, Spring Boot 3.5.16, perfil `dev` con H2 local. Las pruebas se ejecutaron contra `http://localhost:8080` con datos semilla y registros de prueba de identificador único.

## Pruebas automatizadas

| Comprobación | Resultado | Evidencia |
| --- | --- | --- |
| Integración Spring Boot y MockMvc | 9 aprobadas, 0 fallidas, 0 errores | `PrestamoFlowTest` y [resumen JUnit](evidencia-junit.txt) |
| Recorrido HTTP real | 56 comprobaciones aprobadas, 0 fallidas | [salida estructurada](evidencia-api.json) y [script reproducible](../scripts/prueba_integral.py) |
| Interfaz base en Edge headless | 14 comprobaciones de consulta y flujo completo de crear equipo, solicitar, aprobar, entregar y devolver; sin errores JavaScript ni HTTP 4xx/5xx en ese flujo | [catálogo escritorio](capturas/catalogo-escritorio.png), [catálogo móvil](capturas/catalogo-movil.png), [bandeja del encargado](capturas/solicitudes-encargado.png) |
| Perfiles en Edge headless | Inicio de sesión estudiante y docente con HTTP 200, modal de solicitud con límites 7/30, fronteras de fecha validadas y selector administrativo con ambos perfiles; sin errores JavaScript/API observados | [perfil estudiante](capturas/perfil-estudiante.png), [perfil docente](capturas/perfil-docente.png) |

El recorrido HTTP ejercitó autenticación y permisos, categorías, alta y edición de equipos, catálogo con filtros, solicitudes válidas e inválidas, cancelación, rechazo, aprobación, entrega, prevención de doble préstamo, devolución sana, daño crítico, novedad menor, historial, indicadores y auditoría. También inició sesión como estudiante y docente, leyó los límites públicos, rechazó 8 días para estudiante y 31 para docente, y aceptó una solicitud docente de 30 días. El mayor tiempo observado fue **1.562 ms** en el primer inicio de sesión; los demás pasos estuvieron en 125 ms o menos en esta base pequeña. Estos tiempos describen este ambiente de práctica y no sustituyen una prueba de carga.

| Historias | Evidencia automática relevante |
| --- | --- |
| HU-01 a HU-04 | Crear/editar equipo, código único, filtro de catálogo y ficha de detalle en la prueba HTTP |
| HU-05 a HU-08 | Solicitud válida, fechas inválidas, cancelación propia e historial en prueba HTTP; propiedad entre usuarios en JUnit |
| HU-09 y HU-10 | Bandeja pendiente, aprobación, rechazo y observaciones en prueba HTTP y JUnit |
| HU-11 y HU-12 | Entrega solo aprobada, cambio a `PRESTADO` y rechazo de segunda entrega en ambas pruebas |
| HU-13 a HU-15 | Devolución `BUENO`, `NOVEDAD` y `DANADO`, estados finales y registro de novedad en ambas pruebas |
| HU-16 | Consulta de históricos e indicadores en prueba HTTP; detección de vencido con reloj controlado en JUnit |

## Verificación de la ampliación estudiante y docente

La suite JUnit comprobó los límites de ambos roles, su aplicación al crear la solicitud y su revalidación al aprobar o entregar cuando cambia el rol del titular. El backend arrancó con dos bases H2 locales existentes y migró las cuentas antiguas `SOLICITANTE` a `ESTUDIANTE` conservando el historial. La interfaz mostró el máximo de 7 días para estudiante y 30 para docente, leído de la configuración de la API. Los casos manuales PF-25 a PF-29 siguen pendientes de registro humano individual.

La interfaz se revisó a 1440 × 900 y 390 × 844 píxeles, sin desbordamiento horizontal en móvil. El flujo de navegador usó un equipo de prueba único y comprobó el estado final `DEVUELTO` del préstamo y `DISPONIBLE` del equipo.

Los casos de [pruebas funcionales manuales](pruebas-funcionales.md) conservan su estado **NO EJECUTADO** hasta completar y registrar una comprobación humana de cada caso y navegador. Las pruebas automáticas anteriores sí fueron ejecutadas; todavía no se ha hecho la revisión manual en Chrome y Firefox ni una auditoría formal WCAG.
