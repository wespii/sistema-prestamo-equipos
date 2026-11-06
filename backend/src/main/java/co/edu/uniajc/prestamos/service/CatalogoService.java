package co.edu.uniajc.prestamos.service;

import co.edu.uniajc.prestamos.api.ApiDtos.*;
import co.edu.uniajc.prestamos.api.ApiException;
import co.edu.uniajc.prestamos.model.Categoria;
import co.edu.uniajc.prestamos.model.Equipo;
import co.edu.uniajc.prestamos.model.Solicitud;
import co.edu.uniajc.prestamos.model.Usuario;
import co.edu.uniajc.prestamos.model.Estados.EquipoEstado;
import co.edu.uniajc.prestamos.model.Estados.PrestamoEstado;
import co.edu.uniajc.prestamos.model.Estados.SolicitudEstado;
import co.edu.uniajc.prestamos.repo.CategoriaRepository;
import co.edu.uniajc.prestamos.repo.EquipoRepository;
import co.edu.uniajc.prestamos.repo.PrestamoRepository;
import co.edu.uniajc.prestamos.repo.SolicitudRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CatalogoService {
    private static final int MAX_IMAGEN_BYTES = 5 * 1024 * 1024;

    private final CategoriaRepository categorias;
    private final EquipoRepository equipos;
    private final SolicitudRepository solicitudes;
    private final PrestamoRepository prestamos;
    private final AuditService audit;
    private final Clock clock;

    public CatalogoService(CategoriaRepository categorias, EquipoRepository equipos,
                           SolicitudRepository solicitudes, PrestamoRepository prestamos,
                           AuditService audit, Clock clock) {
        this.categorias = categorias;
        this.equipos = equipos;
        this.solicitudes = solicitudes;
        this.prestamos = prestamos;
        this.audit = audit;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<CategoriaView> categorias(boolean incluirInactivas) {
        return categorias.findAll(Sort.by("nombre")).stream().filter(c -> incluirInactivas || c.activo)
            .map(CatalogoService::view).toList();
    }

    @Transactional(readOnly = true)
    public CategoriaView categoria(Long id) {
        return view(categorias.findById(id).orElseThrow(() -> ApiException.notFound("Categoría")));
    }

    @Transactional
    public CategoriaView crearCategoria(CategoriaInput input, Usuario actor) {
        String nombre = input.nombre().trim();
        if (categorias.findByNombreIgnoreCase(nombre).isPresent()) throw ApiException.conflict("La categoría ya existe");
        Categoria c = new Categoria();
        c.nombre = nombre;
        c.activo = !Boolean.FALSE.equals(input.activo());
        c = categorias.save(c);
        audit.record(actor, "CREAR", "CATEGORIA", c.id, c.nombre);
        return view(c);
    }

    @Transactional
    public CategoriaView actualizarCategoria(Long id, CategoriaInput input, Usuario actor) {
        Categoria c = categorias.findById(id).orElseThrow(() -> ApiException.notFound("Categoría"));
        String nombre = input.nombre().trim();
        categorias.findByNombreIgnoreCase(nombre).filter(other -> !other.id.equals(id)).ifPresent(other -> {
            throw ApiException.conflict("La categoría ya existe");
        });
        c.nombre = nombre;
        if (input.activo() != null) c.activo = input.activo();
        audit.record(actor, "ACTUALIZAR", "CATEGORIA", c.id, c.nombre + "; activo=" + c.activo);
        return view(c);
    }

    /** Catálogo que ve el público: disponibles y equipos con regreso futuro conocido. */
    @Transactional(readOnly = true)
    public List<EquipoView> equipos(String texto, Long categoriaId, EquipoEstado estado) {
        LocalDate hoy = LocalDate.now(clock);
        return buscarEquipos(texto, categoriaId, estado).stream().map(this::view)
            .filter(item -> item.estado() == EquipoEstado.DISPONIBLE
                || (item.estado() == EquipoEstado.PRESTADO || item.estado() == EquipoEstado.RESERVADO)
                    && item.fechaDisponibleEstimada() != null && item.fechaDisponibleEstimada().isAfter(hoy))
            .toList();
    }

    /** Inventario completo, reservado a las pantallas y reportes internos. */
    @Transactional(readOnly = true)
    public List<EquipoView> equiposAdministracion(String texto, Long categoriaId, EquipoEstado estado) {
        return buscarEquipos(texto, categoriaId, estado).stream().map(this::view).toList();
    }

    private List<Equipo> buscarEquipos(String texto, Long categoriaId, EquipoEstado estado) {
        Specification<Equipo> spec = (root, query, cb) -> cb.conjunction();
        if (texto != null && !texto.isBlank()) {
            String q = "%" + texto.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("nombre")), q),
                cb.like(cb.lower(root.get("codigo")), q),
                cb.like(cb.lower(root.get("descripcion")), q)));
        }
        if (categoriaId != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("categoria").get("id"), categoriaId));
        if (estado != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("estado"), estado));
        return equipos.findAll(spec, Sort.by("nombre"));
    }

    @Transactional(readOnly = true)
    public EquipoView equipo(Long id) {
        return view(equipos.findById(id).orElseThrow(() -> ApiException.notFound("Equipo")));
    }

    @Transactional(readOnly = true)
    public ImagenEquipo imagen(Long id) {
        Equipo e = equipos.findById(id).orElseThrow(() -> ApiException.notFound("Equipo"));
        if (e.imagen == null || e.imagen.length == 0) throw ApiException.notFound("Imagen del equipo");
        return new ImagenEquipo(e.imagen, e.imagenContentType == null ? "image/jpeg" : e.imagenContentType);
    }

    @Transactional
    public EquipoView guardarImagen(Long id, byte[] bytes, String contentType, Usuario actor) {
        if (bytes == null || bytes.length == 0) throw ApiException.badRequest("Selecciona una imagen.");
        if (bytes.length > MAX_IMAGEN_BYTES) throw ApiException.badRequest("La imagen no puede superar 5 MB.");
        if (contentType == null || !List.of("image/jpeg", "image/png", "image/webp").contains(contentType))
            throw ApiException.badRequest("Usa una imagen JPG, PNG o WebP.");
        Equipo e = equipos.findLockedById(id).orElseThrow(() -> ApiException.notFound("Equipo"));
        e.imagen = bytes.clone();
        e.imagenContentType = contentType;
        audit.record(actor, "ACTUALIZAR_IMAGEN", "EQUIPO", e.id, "Imagen " + contentType + " (" + bytes.length + " bytes)");
        return view(e);
    }

    @Transactional
    public EquipoView eliminarImagen(Long id, Usuario actor) {
        Equipo e = equipos.findLockedById(id).orElseThrow(() -> ApiException.notFound("Equipo"));
        e.imagen = null;
        e.imagenContentType = null;
        audit.record(actor, "ELIMINAR_IMAGEN", "EQUIPO", e.id, "Imagen del equipo eliminada");
        return view(e);
    }

    @Transactional
    public EquipoView crearEquipo(EquipoInput input, Usuario actor) {
        EquipoEstado estado = input.estado() == null ? EquipoEstado.DISPONIBLE : input.estado();
        if (estado == EquipoEstado.PRESTADO || estado == EquipoEstado.RESERVADO) throw ApiException.badRequest("Ese estado solo se asigna mediante el préstamo");
        String codigoIngresado = input.codigo() == null || input.codigo().isBlank()
            ? null : input.codigo().trim().toUpperCase();
        if (codigoIngresado != null && equipos.existsByCodigoIgnoreCase(codigoIngresado))
            throw ApiException.conflict("El código de equipo ya existe");
        Equipo e = new Equipo();
        e.codigo = codigoIngresado == null ? "TMP-" + UUID.randomUUID() : codigoIngresado;
        apply(e, input);
        e.estado = estado;
        e = equipos.saveAndFlush(e);
        if (codigoIngresado == null) e.codigo = codigoAutomatico(e.id);
        audit.record(actor, "CREAR", "EQUIPO", e.id, "Estado inicial " + e.estado + "; código " + e.codigo);
        return view(e);
    }

    private String codigoAutomatico(Long id) {
        long consecutivo = id;
        String codigo = "EQ-" + String.format(java.util.Locale.ROOT, "%05d", consecutivo);
        while (equipos.existsByCodigoIgnoreCase(codigo)) {
            codigo = "EQ-" + String.format(java.util.Locale.ROOT, "%05d", ++consecutivo);
        }
        return codigo;
    }

    @Transactional
    public EquipoView actualizarEquipo(Long id, EquipoInput input, Usuario actor) {
        Equipo e = equipos.findLockedById(id).orElseThrow(() -> ApiException.notFound("Equipo"));
        String codigo = input.codigo() == null || input.codigo().isBlank()
            ? e.codigo : input.codigo().trim().toUpperCase();
        equipos.findByCodigoIgnoreCase(codigo).filter(other -> !other.id.equals(id)).ifPresent(other -> {
            throw ApiException.conflict("El código de equipo ya existe");
        });
        EquipoEstado nuevoEstado = input.estado() == null ? e.estado : input.estado();
        if ((e.estado == EquipoEstado.PRESTADO || e.estado == EquipoEstado.RESERVADO) && nuevoEstado != e.estado)
            throw ApiException.conflict("No puede cambiar manualmente el estado de un equipo comprometido");
        if ((nuevoEstado == EquipoEstado.PRESTADO || nuevoEstado == EquipoEstado.RESERVADO) && nuevoEstado != e.estado)
            throw ApiException.badRequest("Ese estado solo se asigna mediante el préstamo");
        if (prestamos.existsBySolicitudEquipoIdAndEstado(id, PrestamoEstado.ACTIVO) && nuevoEstado != EquipoEstado.PRESTADO)
            throw ApiException.conflict("El equipo tiene un préstamo activo");
        EquipoEstado previo = e.estado;
        apply(e, input);
        e.codigo = codigo;
        e.estado = nuevoEstado;
        audit.record(actor, "ACTUALIZAR", "EQUIPO", e.id, "Estado " + previo + " → " + e.estado);
        return view(e);
    }

    private void apply(Equipo e, EquipoInput input) {
        Categoria c = categorias.findById(input.categoriaId()).orElseThrow(() -> ApiException.notFound("Categoría"));
        if (!c.activo && (e.categoria == null || !e.categoria.id.equals(c.id)))
            throw ApiException.badRequest("La categoría está inactiva");
        e.nombre = input.nombre().trim();
        e.descripcion = input.descripcion().trim();
        e.caracteristicas = input.caracteristicas() == null ? "" : input.caracteristicas().trim();
        e.categoria = c;
    }

    private EquipoView view(Equipo e) {
        LocalDate fechaEstimada = null;
        if (e.estado == EquipoEstado.PRESTADO) {
            fechaEstimada = prestamos.findFirstBySolicitudEquipoIdAndEstadoOrderByFechaDevolucionEsperadaAsc(
                e.id, PrestamoEstado.ACTIVO).map(p -> p.fechaDevolucionEsperada).orElse(null);
        } else if (e.estado == EquipoEstado.RESERVADO) {
            fechaEstimada = solicitudes.findFirstByEquipoIdAndEstadoOrderByFechaFinDesc(
                e.id, SolicitudEstado.APROBADA).map(s -> s.fechaFin.plusDays(1)).orElse(null);
        }
        String imagenUrl = e.imagenContentType == null ? null : "/api/equipos/" + e.id + "/imagen";
        return new EquipoView(e.id, e.codigo, e.nombre, e.descripcion, e.caracteristicas,
            e.estado, e.categoria.id, e.categoria.nombre, e.estado == EquipoEstado.DISPONIBLE,
            fechaEstimada, imagenUrl);
    }

    public static CategoriaView view(Categoria c) { return new CategoriaView(c.id, c.nombre, c.activo); }

    public record ImagenEquipo(byte[] datos, String contentType) {}
}
