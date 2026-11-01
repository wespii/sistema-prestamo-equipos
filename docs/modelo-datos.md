# Modelo de datos lógico

El esquema lo genera JPA a partir de las entidades de `src/main/java/co/edu/uniajc/prestamos/model/`. En desarrollo H2 conserva los datos en archivo; el perfil PostgreSQL usa las mismas entidades.

```mermaid
erDiagram
  ROL ||--o{ USUARIO : asigna
  CATEGORIA_EQUIPO ||--o{ EQUIPO : clasifica
  USUARIO ||--o{ SOLICITUD : crea
  EQUIPO ||--o{ SOLICITUD : solicitado_en
  SOLICITUD ||--o| DECISION_SOLICITUD : recibe
  USUARIO ||--o{ DECISION_SOLICITUD : decide
  SOLICITUD ||--o| PRESTAMO : origina
  USUARIO ||--o{ PRESTAMO : entrega_recibe
  PRESTAMO ||--o{ NOVEDAD : registra
  USUARIO ||--o{ NOVEDAD : reporta

  ROL {
    long id PK
    string codigo UK
    string nombre
  }
  USUARIO {
    long id PK
    string nombre
    string email UK
    string password_hash
    boolean activo
    long rol_id FK
  }
  CATEGORIA_EQUIPO {
    long id PK
    string nombre UK
    boolean activo
  }
  EQUIPO {
    long id PK
    string codigo UK
    string nombre
    string descripcion
    string caracteristicas
    string estado
    long categoria_id FK
    long version
  }
  SOLICITUD {
    long id PK
    long solicitante_id FK
    long equipo_id FK
    date fecha_inicio
    date fecha_fin
    string motivo
    string estado
    datetime fecha_solicitud
  }
  DECISION_SOLICITUD {
    long id PK
    long solicitud_id FK,UK
    long encargado_id FK
    boolean aprobada
    string observacion
    datetime fecha
  }
  PRESTAMO {
    long id PK
    long solicitud_id FK,UK
    datetime fecha_entrega
    date fecha_devolucion_esperada
    datetime fecha_devolucion_real
    string observacion_devolucion
    string estado
    long encargado_entrega_id FK
    long encargado_devolucion_id FK
  }
  NOVEDAD {
    long id PK
    long prestamo_id FK
    string tipo
    string descripcion
    datetime fecha
    long usuario_registra_id FK
  }
  AUDITORIA {
    long id PK
    string usuario
    string accion
    string entidad
    string referencia
    datetime fecha
    string detalle
  }
```

`AUDITORIA` almacena el identificador legible del actor y los datos de la acción como registro histórico; no depende de una FK a `USUARIO`. La tabla `PRESTAMO` guarda dos referencias a usuario, encargado de entrega y de devolución, resumidas en el diagrama.

## Restricciones importantes

- `USUARIO.email`, `CATEGORIA_EQUIPO.nombre` y `EQUIPO.codigo` son únicos.
- `DECISION_SOLICITUD.solicitud_id` y `PRESTAMO.solicitud_id` son únicos: cada solicitud tiene como máximo una decisión y un préstamo.
- Las claves foráneas impiden préstamos, solicitudes o novedades sin sus registros relacionados.
- Un equipo con historial se inactiva; no se borra. El historial de préstamos persiste tras devolverlo.
- La aplicación comprueba fechas y disponibilidad en servicios transaccionales. El estado actual del equipo y la unicidad por solicitud sustentan la regla de un solo préstamo activo por equipo.
- `ROL.codigo` distingue `ESTUDIANTE` y `DOCENTE`. El límite de días se calcula al crear la solicitud a partir del rol del usuario y de la configuración del ambiente; no se almacena una copia del límite en cada solicitud. Los registros históricos mantienen sus fechas originales.
- Las cuentas anteriores con `SOLICITANTE` se reasignan a `ESTUDIANTE` en la migración de inicio. Las relaciones de `USUARIO` con solicitudes y préstamos no cambian.
