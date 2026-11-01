# Matriz de trazabilidad

Fuente: `02_Requerimientos_Sistema_Prestamo_Equipos_Tecnologicos.docx`, versión 1.0 (05/10/2026). Una solicitud corresponde a un equipo, conforme a HU-05 y al modelo lógico del documento.

Las **16 historias están implementadas** en frontend y backend. Esta matriz vincula cada una con sus requerimientos y con un caso observable en [pruebas-funcionales.md](pruebas-funcionales.md). Los casos manuales son un **plan de verificación** y siguen sin ejecutarse. Las pruebas automáticas realizadas y sus resultados se documentan en [evidencia-ejecucion.md](evidencia-ejecucion.md).

| HU | Comportamiento esperado | RF relacionados | Caso funcional | Evidencia manual |
| --- | --- | --- | --- | --- |
| HU-01 | Registrar equipo | RF-02, RF-03, RF-15, RF-17 | PF-01 | No ejecutado |
| HU-02 | Editar equipo | RF-03, RF-15, RF-17 | PF-02 | No ejecutado |
| HU-03 | Consultar y filtrar catálogo | RF-04, RF-05 | PF-03 | No ejecutado |
| HU-04 | Consultar detalle y estado | RF-06 | PF-04 | No ejecutado |
| HU-05 | Crear solicitud con equipo, fechas y motivo como estudiante o docente | RF-05, RF-07 | PF-05, PF-25, PF-26 | No ejecutado |
| HU-06 | Rechazar fechas, duración por perfil o disponibilidad inválidas | RF-05, RF-07 | PF-06, PF-25, PF-26 | No ejecutado |
| HU-07 | Cancelar solicitud pendiente propia | RF-07, RF-14, RF-15, RF-17 | PF-07 | No ejecutado |
| HU-08 | Consultar solicitudes propias e historial según perfil | RF-14, RF-15 | PF-08, PF-27 | No ejecutado |
| HU-09 | Consultar bandeja pendiente | RF-08, RF-15 | PF-09 | No ejecutado |
| HU-10 | Aprobar o rechazar con observación | RF-05, RF-09, RF-15, RF-17 | PF-10 | No ejecutado |
| HU-11 | Registrar entrega de aprobada | RF-10, RF-15, RF-17 | PF-11 | No ejecutado |
| HU-12 | Marcar equipo prestado y evitar simultaneidad | RF-05, RF-10, RF-17 | PF-12 | No ejecutado |
| HU-13 | Registrar devolución y fecha real | RF-12, RF-15, RF-17 | PF-13 | No ejecutado |
| HU-14 | Registrar novedad o daño | RF-13, RF-15, RF-17 | PF-14 | No ejecutado |
| HU-15 | Dejar equipo disponible o en mantenimiento | RF-12, RF-13, RF-17 | PF-15 | No ejecutado |
| HU-16 | Consultar activos, vencidos e histórico | RF-11, RF-14, RF-16 | PF-16 | No ejecutado |

## Reglas de negocio especialmente sensibles

| Regla | Comprobación | Casos |
| --- | --- | --- |
| RN-01 | Solo un equipo `DISPONIBLE` puede iniciar nuevo préstamo | PF-05, PF-10, PF-11 |
| RN-02 | La entrega exige solicitud `APROBADA` | PF-11, PF-18 |
| RN-03 | Fecha final posterior a fecha inicial | PF-06 |
| RN-04 | Ningún equipo puede estar en dos préstamos activos | PF-12, PF-19 |
| RN-05 | Daño crítico puede llevar a `MANTENIMIENTO` | PF-14, PF-15 |
| RN-06 | Solo el encargado decide, entrega y recibe | PF-17 |
| RN-07 | El historial no se elimina al devolver | PF-08, PF-16 |

## Ampliación de perfiles solicitada después del documento original

El documento define `SOLICITANTE` como rol general. La ampliación separa ese comportamiento en `ESTUDIANTE` y `DOCENTE`, sin cambiar el número de historias originales. Ambos pueden solicitar y revisar registros propios; `ESTUDIANTE` admite como máximo 7 días y `DOCENTE` 30 días, con límites configurables. Los usuarios existentes de `SOLICITANTE` se migran a `ESTUDIANTE` preservando el historial. El backend revalida el límite al crear, aprobar y entregar. Los casos [PF-25 a PF-29](pruebas-funcionales.md) verifican acceso, límites, migración y cambios de rol.

## Cobertura de requisitos complementarios

| Requisitos | Verificación prevista |
| --- | --- |
| RF-01, RF-15 | PF-17 y PF-27: usuarios, autenticación y permisos por rol |
| RF-02 | PF-01 y PF-20: categoría activa y administración |
| RF-16 | PF-16: indicadores por estado, activos y vencidos |
| RF-17 | PF-21: auditoría de transiciones |
| RNF-01, RNF-02 | PF-17, PF-22: autorización, entradas inválidas y datos personales |
| RNF-03, RNF-04, RNF-06 | PF-23: mensajes, teclado y diseño adaptable |
| RNF-05 | PF-24: medición en ambiente de práctica |
| RNF-07 a RNF-11 | Revisión de estructura, configuración y esquema; consultar README |
| RNF-12 | `mvn test` y registro manual de resultados PF-01 a PF-24 |

El documento original no define un RF exclusivo para cancelar solicitudes. La operación queda cubierta por HU-07 y se vincula aquí con solicitudes, historial, autorización y auditoría. Tampoco fija un plazo de caducidad: el proyecto adopta **48 horas configurables** para una aprobación sin entrega.
