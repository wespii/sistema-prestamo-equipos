package co.edu.uniajc.prestamos.model;

public final class Estados {
    private Estados() {}

    // SOLICITANTE permanece solo para leer y migrar registros creados por versiones anteriores.
    public enum RolCodigo { ADMINISTRADOR, ENCARGADO, ESTUDIANTE, DOCENTE, SOLICITANTE }
    public enum EquipoEstado { DISPONIBLE, RESERVADO, PRESTADO, MANTENIMIENTO, INACTIVO }
    public enum SolicitudEstado { PENDIENTE, APROBADA, RECHAZADA, CANCELADA, VENCIDA }
    public enum PrestamoEstado { ACTIVO, DEVUELTO, DEVUELTO_CON_NOVEDAD }
    public enum CondicionDevolucion { BUENO, NOVEDAD, DANADO }
}
