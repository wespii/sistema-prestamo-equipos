# Plan de pruebas funcionales

Estado inicial de la evidencia manual: **no ejecutado**. Quien pruebe debe anotar fecha, perfil, entorno, resultado (`APROBADO` / `FALLIDO`), respuesta observada y enlace o captura. No se debe reemplazar el resultado esperado por una afirmación de ejecución.

Usar la base semilla o crear datos equivalentes. Para pruebas que alteran el inventario, empezar con un equipo `DISPONIBLE` sin préstamos activos y restablecer los datos antes de otro caso. Las fechas en la API se envían en formato ISO.

## Casos por historia de usuario

| ID | HU | Preparación y pasos | Resultado esperado |
| --- | --- | --- | --- |
| PF-01 | HU-01 | Iniciar como administrador. Crear una categoría activa y un equipo con código único, nombre, descripción, características y estado disponible. | El equipo aparece en el catálogo y detalle con su categoría; el código duplicado se rechaza. |
| PF-02 | HU-02 | Como administrador, cambiar nombre, descripción y características del equipo creado. | La ficha conserva el mismo ID/código y muestra los nuevos datos; otro rol no puede editarla. |
| PF-03 | HU-03 | Abrir catálogo y filtrar por texto, categoría y estado. | Cada filtro limita el conjunto de resultados; el estado visible coincide con la disponibilidad. |
| PF-04 | HU-04 | Abrir la ficha de un equipo disponible y luego la de uno en mantenimiento. | Se muestran código, categoría, características, estado y disponibilidad coherentes. La información restringida solo aparece al rol permitido. |
| PF-05 | HU-05 | Como estudiante o docente, elegir equipo disponible, fecha inicial anterior a la final y motivo no vacío; enviar. | Se crea solicitud propia `PENDIENTE` con las fechas y motivo enviados; confirmación visible. |
| PF-06 | HU-06 | Enviar fecha final igual o anterior a la inicial; repetir con equipo no disponible o campos vacíos. | Se rechazan las entradas con mensaje claro y HTTP 400/409 según el caso; no se crea solicitud inválida. Los límites propios de cada perfil se comprueban en PF-25 y PF-26. |
| PF-07 | HU-07 | Crear solicitud pendiente propia, cancelarla y luego intentar cancelar una ajena o ya aprobada. | La propia pasa a `CANCELADA`; las otras no cambian y devuelven 403 o 409. |
| PF-08 | HU-08 | Como solicitante, consultar mis solicitudes después de crear, cancelar y devolver préstamos. | Aparecen estados e historial propios; no se exponen solicitudes de otro usuario. |
| PF-09 | HU-09 | Como encargado, abrir bandeja con solicitudes pendientes, aprobadas y rechazadas. | La vista de pendientes muestra solo `PENDIENTE` con datos para tomar la decisión. |
| PF-10 | HU-10 | Como encargado, aprobar una pendiente con observación; rechazar otra. | Cada una cambia al estado correspondiente y guarda encargado, fecha y observación; la aprobación verifica disponibilidad de nuevo. |
| PF-11 | HU-11 | Intentar entregar una pendiente y luego entregar una aprobada. | La pendiente se rechaza sin préstamo; la aprobada crea un único préstamo `ACTIVO` con fecha y encargado. |
| PF-12 | HU-12 | Tras PF-11, volver a consultar equipo y solicitar/aprobar/entregar otra solicitud para el mismo equipo. | El equipo figura `PRESTADO`; ninguna ruta permite un segundo préstamo activo. |
| PF-13 | HU-13 | Como encargado, devolver el préstamo activo sin daños, consignando condición y observación. | Se guarda fecha de devolución real; préstamo `DEVUELTO`; el equipo queda `DISPONIBLE`. |
| PF-14 | HU-14 | En otra devolución, registrar `NOVEDAD` no crítica con descripción. | La novedad se asocia al préstamo, usuario y fecha, permanece visible en historial y el equipo queda `DISPONIBLE`. |
| PF-15 | HU-15 | Comparar devoluciones con `BUENO`, `NOVEDAD` y `DANADO`. | `BUENO` → `DEVUELTO` y `DISPONIBLE`; `NOVEDAD` → `DEVUELTO_CON_NOVEDAD` y `DISPONIBLE`; `DANADO` → `DEVUELTO_CON_NOVEDAD` y `MANTENIMIENTO`. |
| PF-16 | HU-16 | Como administrador, consultar activos, vencidos, históricos e indicadores tras entregar y devolver. | Los contadores y listas reflejan los registros; los devueltos siguen consultables. |

## Casos transversales y negativos

| ID | Preparación y pasos | Resultado esperado |
| --- | --- | --- |
| PF-17 | Probar sin token, con token de estudiante, docente y encargado las rutas de usuarios/inventario, decisión y préstamo; crear un usuario por administración si existe la interfaz. | HTTP 401 sin autenticación, 403 para rol insuficiente y éxito solo para el rol autorizado. Las contraseñas nunca aparecen en respuestas. |
| PF-18 | Intentar entregar solicitud `RECHAZADA`, `CANCELADA` o inexistente. | No se crea préstamo; respuesta 404 si falta recurso o 409 para estado incompatible. |
| PF-19 | Enviar dos entregas casi simultáneas para el mismo equipo. | Como máximo una termina con préstamo `ACTIVO`; la otra falla de forma controlada. |
| PF-20 | Como administrador crear/editar categorías; intentar código de equipo duplicado y categoría inexistente/inactiva. | Se preservan unicidad e integridad referencial; entradas inválidas no crean filas parciales. |
| PF-21 | Crear equipo, solicitar, decidir, entregar, devolver y consultar auditoría como administrador. | Cada transición relevante registra actor, acción, entidad, referencia y fecha. |
| PF-22 | Enviar HTML/JavaScript en campos de texto y parámetros inesperados; consultar listados con solicitante. | Texto se presenta escapado, no se ejecutan scripts, no hay datos personales ajenos y se rechazan entradas inválidas. |
| PF-23 | Recorrer catálogo, formularios y acciones solo con teclado; probar anchuras móvil y escritorio en Chrome, Edge y Firefox. | Foco visible, etiquetas comprensibles, mensajes accionables, sin desbordamiento que impida el uso. |
| PF-24 | Medir varias consultas y operaciones CRUD en el ambiente de práctica. | Respuesta normal inferior a 3 segundos; registrar hardware, volumen de datos y resultados para contextualizar la medición. |

## Casos de la ampliación estudiante y docente

| ID | Preparación y pasos | Resultado esperado |
| --- | --- | --- |
| PF-25 | Entrar como `ESTUDIANTE`. Solicitar un equipo disponible por exactamente 7 días y otro por 8 días, calculados como `fechaFin - fechaInicio`. | La solicitud de 7 días se crea; la de 8 días recibe HTTP 400 y mensaje que indique el límite. La interfaz informa el mismo máximo que `GET /api/configuracion/prestamos`. |
| PF-26 | Entrar como `DOCENTE`. Solicitar un equipo disponible por exactamente 30 días y otro por 31 días. | La de 30 días se crea; la de 31 días recibe HTTP 400. El docente ve su perfil y límite de 30 días en la interfaz. |
| PF-27 | Iniciar sesión con las cuentas demo estudiante y docente; desde administración crear una cuenta de cada rol y consultar `/auth/me`, `/solicitudes/mias` y `/prestamos/mios`. | Cada cuenta conserva su rol, puede solicitar y solo ve su propio historial; no puede decidir, entregar ni devolver. No se permite crear un nuevo usuario con `SOLICITANTE`. |
| PF-28 | En una copia de base anterior con usuario `SOLICITANTE` y préstamos existentes, iniciar la nueva versión y entrar con esa cuenta. | La cuenta queda en `ESTUDIANTE`, conserva historial y aplica máximo de 7 días a solicitudes nuevas. No se altera el historial previo. |
| PF-29 | Escenario A: crear solicitud pendiente de 20 días como docente, cambiar la cuenta a estudiante e intentar aprobar. Escenario B: crear y aprobar otra solicitud de 20 días como docente, cambiar la cuenta a estudiante antes de entregar e intentar entregar. | La duración se revalida con el rol vigente; ambas transiciones rechazan con HTTP 409 y no crean préstamo. En A el equipo no se reserva; en B la aprobación previa conserva su estado hasta caducar, sin permitir la entrega. |

## Registro de ejecución

| Caso | Fecha y entorno | Resultado | Evidencia / observación |
| --- | --- | --- | --- |
| PF-01 | Pendiente | NO EJECUTADO | — |
| PF-02 | Pendiente | NO EJECUTADO | — |
| PF-03 | Pendiente | NO EJECUTADO | — |
| PF-04 | Pendiente | NO EJECUTADO | — |
| PF-05 | Pendiente | NO EJECUTADO | — |
| PF-06 | Pendiente | NO EJECUTADO | — |
| PF-07 | Pendiente | NO EJECUTADO | — |
| PF-08 | Pendiente | NO EJECUTADO | — |
| PF-09 | Pendiente | NO EJECUTADO | — |
| PF-10 | Pendiente | NO EJECUTADO | — |
| PF-11 | Pendiente | NO EJECUTADO | — |
| PF-12 | Pendiente | NO EJECUTADO | — |
| PF-13 | Pendiente | NO EJECUTADO | — |
| PF-14 | Pendiente | NO EJECUTADO | — |
| PF-15 | Pendiente | NO EJECUTADO | — |
| PF-16 | Pendiente | NO EJECUTADO | — |
| PF-17 | Pendiente | NO EJECUTADO | — |
| PF-18 | Pendiente | NO EJECUTADO | — |
| PF-19 | Pendiente | NO EJECUTADO | — |
| PF-20 | Pendiente | NO EJECUTADO | — |
| PF-21 | Pendiente | NO EJECUTADO | — |
| PF-22 | Pendiente | NO EJECUTADO | — |
| PF-23 | Pendiente | NO EJECUTADO | — |
| PF-24 | Pendiente | NO EJECUTADO | — |
| PF-25 | Pendiente | NO EJECUTADO | — |
| PF-26 | Pendiente | NO EJECUTADO | — |
| PF-27 | Pendiente | NO EJECUTADO | — |
| PF-28 | Pendiente | NO EJECUTADO | — |
| PF-29 | Pendiente | NO EJECUTADO | — |

Las pruebas automatizadas de servicios pueden verificar parte de estas reglas, pero no sustituyen las comprobaciones de interfaz, accesibilidad y compatibilidad de navegador indicadas aquí.
