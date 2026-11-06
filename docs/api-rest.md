# API REST

Base local: `http://localhost:8080/api`. Las respuestas son JSON; los listados devuelven arreglos directamente. Salvo el catálogo público y el inicio de sesión, enviar `Authorization: Bearer <token>`. Fechas de calendario: `yyyy-MM-dd`; marcas de tiempo de respuesta: ISO. Los errores tienen forma `{ "error": "...", "message": "..." }`.

## Rutas

| Método y ruta | Uso | Acceso principal |
| --- | --- | --- |
| `POST /auth/login` | Autenticar con `email`, `password`; devuelve `token` y `user` | Público |
| `GET /auth/me` | Datos del usuario autenticado | Autenticado |
| `GET /configuracion/prestamos` | Límites vigentes `{maxDiasEstudiante,maxDiasDocente}` | Público |
| `GET /categorias` | Categorías activas para catálogo | Público |
| `GET /equipos?texto=&categoriaId=&estado=` | Equipos disponibles y equipos con regreso futuro estimado | Público |
| `GET /equipos/{id}` | Ficha y estado de un equipo por ID; la interfaz solo enlaza fichas desde el catálogo filtrado | Público |
| `GET /equipos/{id}/imagen` | Foto del equipo | Público |
| `GET /equipos/{id}/historial` | Cambios e historial del equipo | Administrador o encargado |
| `GET /admin/usuarios` | Lista de cuentas | Administrador |
| `GET /admin/usuarios/{id}` | Detalle de cuenta | Administrador |
| `POST /admin/usuarios` | Crear cuenta | Administrador |
| `PUT /admin/usuarios/{id}` | Editar cuenta/estado | Administrador |
| `GET /admin/categorias` | Todas las categorías, incluidas las inactivas | Administrador |
| `GET /admin/categorias/{id}` | Detalle de categoría | Administrador |
| `POST /admin/categorias` | Crear categoría | Administrador |
| `PUT /admin/categorias/{id}` | Editar o desactivar categoría | Administrador |
| `GET /admin/equipos?texto=&categoriaId=&estado=` | Inventario con filtros opcionales | Administrador |
| `POST /admin/equipos` | Registrar equipo | Administrador |
| `PUT /admin/equipos/{id}` | Editar o inactivar equipo | Administrador |
| `PUT /admin/equipos/{id}/imagen` | Subir/reemplazar foto JPG, PNG o WebP (máximo 5 MB) | Administrador |
| `DELETE /admin/equipos/{id}/imagen` | Quitar foto cargada | Administrador |
| `POST /solicitudes` | Crear solicitud propia y validar duración máxima del perfil | Estudiante o docente |
| `GET /solicitudes/mias` | Solicitudes e historial propios | Estudiante o docente |
| `GET /solicitudes/{id}` | Solicitud propia o consulta del personal autorizado | Titular, encargado o administrador |
| `GET /solicitudes?estado=&usuarioId=&equipoId=` | Consulta general, con filtros opcionales | Encargado o administrador |
| `GET /solicitudes/pendientes` | Bandeja pendiente | Encargado o administrador |
| `POST /solicitudes/{id}/cancelar` | Cancelar solicitud propia pendiente | Estudiante o docente |
| `POST /solicitudes/{id}/decision` | Aprobar/rechazar con observación | Encargado |
| `POST /solicitudes/{id}/entregar` | Confirmar entrega; crea un préstamo y cambia solicitud a `ENTREGADA` | Encargado |
| `GET /prestamos?estado=&usuarioId=&equipoId=` | Préstamos, con filtros opcionales | Encargado o administrador |
| `GET /prestamos/mios` | Préstamos propios | Estudiante o docente |
| `POST /prestamos/{id}/devolver` | Registrar devolución y eventual novedad | Encargado |
| `GET /reportes/resumen` | Conteos de equipos, solicitudes y préstamos (incluye los que vencen en 3 días) | Encargado o administrador |
| `GET /reportes/detalle` | Registros que forman los indicadores | Encargado o administrador |
| `GET /auditoria` | Acciones y transiciones registradas | Administrador |

No hay borrado físico de equipos ni categorías con historial: se actualiza su estado a `INACTIVO` o `activo=false`.

## Cuerpos de solicitud

`POST /auth/login`:

```json
{"email":"estudiante@uniajc.edu.co","password":"Estudiante123!"}
```

La respuesta `user.rol` distingue `ESTUDIANTE` y `DOCENTE`. La cuenta docente de demostración usa `docente@uniajc.edu.co` y `Docente123!`.

`GET /configuracion/prestamos` devuelve, con la configuración predeterminada:

```json
{"maxDiasEstudiante":7,"maxDiasDocente":30}
```

`POST /admin/usuarios` (en `PUT`, `password` puede omitirse si no se cambia):

```json
{"nombre":"María Pérez","email":"maria@uniajc.edu.co","password":"ClaveSegura123!","rol":"ESTUDIANTE","activo":true}
```

`POST /admin/categorias`:

```json
{"nombre":"Portátiles","activo":true}
```

`POST /admin/equipos` (`codigo` es opcional; si se omite, se genera automáticamente):

```json
{"nombre":"Portátil de práctica","descripcion":"Equipo para clase","caracteristicas":"16 GB RAM","estado":"DISPONIBLE","categoriaId":1}
```

La respuesta incluye `codigo` (`EQ-00001`, `EQ-00002`, ...), `fechaDisponibleEstimada` e `imagenUrl`. Las imágenes cargadas se guardan en la base junto con el equipo. Los equipos prestados usan la devolución prevista del préstamo activo; los reservados muestran el día siguiente a la fecha fin aprobada. El catálogo público incluye equipos disponibles y equipos prestados o reservados con fecha estrictamente futura. No incluye los que están inactivos, en mantenimiento o cuyo regreso ya pasó o no se puede estimar. La ficha pública aplica el mismo filtro, también si se consulta directamente por ID.

`POST /solicitudes`:

```json
{"equipoId":1,"fechaInicio":"2026-10-27","fechaFin":"2026-10-29","motivo":"Práctica de laboratorio"}
```

La fecha final debe ser posterior a la inicial. La diferencia máxima es **7 días para `ESTUDIANTE`** y **30 días para `DOCENTE`**; el máximo exacto sí se admite, y una solicitud más larga se rechaza con HTTP 400. Los valores pueden cambiar mediante `APP_MAX_DIAS_ESTUDIANTE` y `APP_MAX_DIAS_DOCENTE`; el frontend consulta `/configuracion/prestamos` para presentar el límite vigente. El servidor valida el límite aunque el cliente omita la comprobación de la interfaz. También lo revalida al aprobar y entregar según el rol actual del titular; si una solicitud antigua supera el plazo vigente, esas transiciones devuelven HTTP 409.

`POST /solicitudes/{id}/decision`:

```json
{"aprobada":true,"observacion":"Asignación confirmada"}
```

Después de la decisión, la solicitud queda `APROBADA` hasta que el encargado confirme la entrega. Entonces pasa a `ENTREGADA`, se activa el préstamo `ACTIVO` y el equipo cambia a `PRESTADO`.

`POST /prestamos/{id}/devolver`:

```json
{"condicion":"BUENO","observacion":"Se recibe completo y funcional"}
```

Para devolución, `condicion` admite `BUENO`, `NOVEDAD` o `DANADO`; describir el hallazgo en `observacion` cuando corresponda. `BUENO` devuelve el equipo a `DISPONIBLE` sin novedad; `NOVEDAD` registra un hallazgo no crítico y lo deja `DISPONIBLE`; `DANADO` registra daño y lo pasa a `MANTENIMIENTO`. Aprobar reserva el equipo y entregar lo marca `PRESTADO`.

## Ejemplo de consumo en PowerShell

Iniciar el servidor en otra terminal y ejecutar cada bloque en orden. Elegir el ID de un equipo disponible del catálogo y fechas futuras antes de crear la solicitud.

```powershell
$base = 'http://localhost:8080/api'
$login = Invoke-RestMethod -Method Post -Uri "$base/auth/login" -ContentType 'application/json' -Body '{"email":"estudiante@uniajc.edu.co","password":"Estudiante123!"}'
$estudianteHeaders = @{ Authorization = "Bearer $($login.token)" }
Invoke-RestMethod -Uri "$base/equipos?estado=DISPONIBLE"
```

```powershell
$inicio = (Get-Date).AddDays(1).ToString('yyyy-MM-dd')
$fin = (Get-Date).AddDays(3).ToString('yyyy-MM-dd')
$body = @{ equipoId = 1; fechaInicio = $inicio; fechaFin = $fin; motivo = 'Práctica de laboratorio' } | ConvertTo-Json
$solicitud = Invoke-RestMethod -Method Post -Uri "$base/solicitudes" -Headers $estudianteHeaders -ContentType 'application/json' -Body $body
$solicitud
```

Para continuar, iniciar como encargado, usar `$solicitud.id` en `/solicitudes/{id}/decision` y `/entregar`, y luego devolver el `$prestamo.id` en `/prestamos/{id}/devolver`. El [plan funcional](pruebas-funcionales.md) indica los resultados esperados y cómo registrar evidencia.

Los códigos esperados incluyen 200/201 para éxito, 400 para datos inválidos, 401 sin autenticación, 403 por rol, 404 para recurso ausente y 409 para conflicto de estado o disponibilidad.
