package co.edu.uniajc.prestamos.api;

import co.edu.uniajc.prestamos.api.ApiDtos.*;
import co.edu.uniajc.prestamos.service.CatalogoService;
import co.edu.uniajc.prestamos.service.UsuarioService;
import co.edu.uniajc.prestamos.security.AuthService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class AdminController {
    private final UsuarioService usuarios;
    private final CatalogoService catalogo;
    private final AuthService auth;

    public AdminController(UsuarioService usuarios, CatalogoService catalogo, AuthService auth) {
        this.usuarios = usuarios;
        this.catalogo = catalogo;
        this.auth = auth;
    }

    @GetMapping("/usuarios")
    public List<UsuarioView> usuarios() { return usuarios.list(); }

    @GetMapping("/usuarios/{id}")
    public UsuarioView usuario(@PathVariable Long id) { return usuarios.get(id); }

    @PostMapping("/usuarios")
    public ResponseEntity<UsuarioView> crearUsuario(@Valid @RequestBody UsuarioInput input) {
        UsuarioView result = usuarios.create(input, auth.currentUser());
        return ResponseEntity.created(URI.create("/api/admin/usuarios/" + result.id())).body(result);
    }

    @PutMapping("/usuarios/{id}")
    public UsuarioView actualizarUsuario(@PathVariable Long id, @Valid @RequestBody UsuarioInput input) {
        return usuarios.update(id, input, auth.currentUser());
    }

    @GetMapping("/categorias")
    public List<CategoriaView> categorias() { return catalogo.categorias(true); }

    @GetMapping("/categorias/{id}")
    public CategoriaView categoria(@PathVariable Long id) { return catalogo.categoria(id); }

    @PostMapping("/categorias")
    public ResponseEntity<CategoriaView> crearCategoria(@Valid @RequestBody CategoriaInput input) {
        CategoriaView result = catalogo.crearCategoria(input, auth.currentUser());
        return ResponseEntity.created(URI.create("/api/admin/categorias/" + result.id())).body(result);
    }

    @PutMapping("/categorias/{id}")
    public CategoriaView actualizarCategoria(@PathVariable Long id, @Valid @RequestBody CategoriaInput input) {
        return catalogo.actualizarCategoria(id, input, auth.currentUser());
    }

    @GetMapping("/equipos")
    public List<EquipoView> equipos(@RequestParam(required = false) String texto,
                                    @RequestParam(required = false) Long categoriaId,
                                    @RequestParam(required = false) co.edu.uniajc.prestamos.model.Estados.EquipoEstado estado) {
        return catalogo.equiposAdministracion(texto, categoriaId, estado);
    }

    @PostMapping("/equipos")
    public ResponseEntity<EquipoView> crearEquipo(@Valid @RequestBody EquipoInput input) {
        EquipoView result = catalogo.crearEquipo(input, auth.currentUser());
        return ResponseEntity.created(URI.create("/api/equipos/" + result.id())).body(result);
    }

    @PutMapping("/equipos/{id}")
    public EquipoView actualizarEquipo(@PathVariable Long id, @Valid @RequestBody EquipoInput input) {
        return catalogo.actualizarEquipo(id, input, auth.currentUser());
    }

    @PutMapping(value = "/equipos/{id}/imagen", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public EquipoView guardarImagen(@PathVariable Long id, @RequestParam("archivo") MultipartFile archivo)
            throws java.io.IOException {
        return catalogo.guardarImagen(id, archivo.getBytes(), archivo.getContentType(), auth.currentUser());
    }

    @DeleteMapping("/equipos/{id}/imagen")
    public EquipoView eliminarImagen(@PathVariable Long id) {
        return catalogo.eliminarImagen(id, auth.currentUser());
    }
}
