package co.edu.uniajc.prestamos.service;

import co.edu.uniajc.prestamos.api.ApiDtos.*;
import co.edu.uniajc.prestamos.api.ApiException;
import co.edu.uniajc.prestamos.model.Categoria;
import co.edu.uniajc.prestamos.model.Equipo;
import co.edu.uniajc.prestamos.model.Usuario;
import co.edu.uniajc.prestamos.model.Estados.EquipoEstado;
import co.edu.uniajc.prestamos.model.Estados.PrestamoEstado;
import co.edu.uniajc.prestamos.repo.CategoriaRepository;
import co.edu.uniajc.prestamos.repo.EquipoRepository;
import co.edu.uniajc.prestamos.repo.PrestamoRepository;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CatalogoService {
    private final CategoriaRepository categorias;
    private final EquipoRepository equipos;
    private final PrestamoRepository prestamos;
    private final AuditService audit;

    public CatalogoService(CategoriaRepository categorias, EquipoRepository equipos,
                           PrestamoRepository prestamos, AuditService audit) {
        this.categorias = categorias;
        this.equipos = equipos;
        this.prestamos = prestamos;
        this.audit = audit;
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

    @Transactional(readOnly = true)
    public List<EquipoView> equipos(String texto, Long categoriaId, EquipoEstado estado) {
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
        return equipos.findAll(spec, Sort.by("nombre")).stream().map(CatalogoService::view).toList();
    }

    @Transactional(readOnly = true)
    public EquipoView equipo(Long id) {
        return view(equipos.findById(id).orElseThrow(() -> ApiException.notFound("Equipo")));
    }

    @Transactional
    public EquipoView crearEquipo(EquipoInput input, Usuario actor) {
        String codigo = input.codigo().trim().toUpperCase();
        if (equipos.existsByCodigoIgnoreCase(codigo)) throw ApiException.conflict("El código de equipo ya existe");
        EquipoEstado estado = input.estado() == null ? EquipoEstado.DISPONIBLE : input.estado();
        if (estado == EquipoEstado.PRESTADO || estado == EquipoEstado.RESERVADO) throw ApiException.badRequest("Ese estado solo se asigna mediante el préstamo");
        Equipo e = new Equipo();
        apply(e, input, codigo);
        e.estado = estado;
        e = equipos.save(e);
        audit.record(actor, "CREAR", "EQUIPO", e.id, "Estado inicial " + e.estado);
        return view(e);
    }

    @Transactional
    public EquipoView actualizarEquipo(Long id, EquipoInput input, Usuario actor) {
        Equipo e = equipos.findLockedById(id).orElseThrow(() -> ApiException.notFound("Equipo"));
        String codigo = input.codigo().trim().toUpperCase();
        equipos.findAll().stream().filter(other -> other.codigo.equalsIgnoreCase(codigo) && !other.id.equals(id)).findAny().ifPresent(other -> {
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
        apply(e, input, codigo);
        e.estado = nuevoEstado;
        audit.record(actor, "ACTUALIZAR", "EQUIPO", e.id, "Estado " + previo + " → " + e.estado);
        return view(e);
    }

    private void apply(Equipo e, EquipoInput input, String codigo) {
        Categoria c = categorias.findById(input.categoriaId()).orElseThrow(() -> ApiException.notFound("Categoría"));
        if (!c.activo && (e.categoria == null || !e.categoria.id.equals(c.id)))
            throw ApiException.badRequest("La categoría está inactiva");
        e.codigo = codigo;
        e.nombre = input.nombre().trim();
        e.descripcion = input.descripcion().trim();
        e.caracteristicas = input.caracteristicas() == null ? "" : input.caracteristicas().trim();
        e.categoria = c;
    }

    public static EquipoView view(Equipo e) {
        return new EquipoView(e.id, e.codigo, e.nombre, e.descripcion, e.caracteristicas,
            e.estado, e.categoria.id, e.categoria.nombre, e.estado == EquipoEstado.DISPONIBLE);
    }

    public static CategoriaView view(Categoria c) { return new CategoriaView(c.id, c.nombre, c.activo); }
}
