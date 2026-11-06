package co.edu.uniajc.prestamos;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import co.edu.uniajc.prestamos.config.DatosIniciales;
import co.edu.uniajc.prestamos.model.*;
import co.edu.uniajc.prestamos.model.Estados.RolCodigo;
import co.edu.uniajc.prestamos.repo.*;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(PrestamoFlowTest.FixedClockConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class PrestamoFlowTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired MutableClock clock;
    @Autowired DatosIniciales datosIniciales;
    @Autowired RolRepository roles;
    @Autowired UsuarioRepository usuarios;
    @Autowired EquipoRepository equipos;
    @Autowired SolicitudRepository solicitudes;
    @Autowired PasswordEncoder passwords;

    @BeforeEach
    void resetClock() { clock.set("2026-10-26T12:00:00Z"); }

    @Test
    void flujoCompletoConDanioYControlDeDobleEntrega() throws Exception {
        String solicitante = login("estudiante@uniajc.edu.co", "Estudiante123!");
        String encargado = login("encargado@uniajc.edu.co", "Encargado123!");
        String admin = login("admin@uniajc.edu.co", "Admin123!");
        long equipo = equipo("PORT-001");

        assertEquals(400, post("/api/solicitudes", solicitante,
            Map.of("equipoId", equipo, "fechaInicio", "2026-10-26", "fechaFin", "2026-10-26", "motivo", "Clase")).status);
        Result creada = post("/api/solicitudes", solicitante,
            Map.of("equipoId", equipo, "fechaInicio", "2026-10-26", "fechaFin", "2026-10-29", "motivo", "Clase"));
        assertEquals(201, creada.status);
        long solicitud = creada.body.get("id").asLong();

        assertEquals(403, post("/api/solicitudes/" + solicitud + "/decision", admin,
            Map.of("aprobada", true, "observacion", "Aprobada")).status);
        assertEquals(200, post("/api/solicitudes/" + solicitud + "/decision", encargado,
            Map.of("aprobada", true, "observacion", "Aprobada")).status);
        assertEquals("RESERVADO", get("/api/equipos/" + equipo, null).body.get("estado").asText());

        assertEquals(403, post("/api/solicitudes/" + solicitud + "/entregar", solicitante, null).status);
        Result entrega = post("/api/solicitudes/" + solicitud + "/entregar", encargado, null);
        assertEquals(200, entrega.status);
        long prestamo = entrega.body.get("id").asLong();
        assertEquals(409, post("/api/solicitudes/" + solicitud + "/entregar", encargado, null).status);
        assertEquals("PRESTADO", get("/api/equipos/" + equipo, null).body.get("estado").asText());

        Result devolucion = post("/api/prestamos/" + prestamo + "/devolver", encargado,
            Map.of("condicion", "DANADO", "observacion", "No enciende"));
        assertEquals(200, devolucion.status);
        assertEquals("DEVUELTO_CON_NOVEDAD", devolucion.body.get("estado").asText());
        assertEquals(1, devolucion.body.get("novedades").size());
        assertEquals("MANTENIMIENTO", get("/api/equipos/" + equipo, null).body.get("estado").asText());
        assertEquals(1, get("/api/prestamos?equipoId=" + equipo, admin).body.size());
        assertTrue(get("/api/equipos/" + equipo + "/historial", admin).body.size() >= 3);
    }

    @Test
    void soloElPropietarioCancelaSolicitudesPendientes() throws Exception {
        String admin = login("admin@uniajc.edu.co", "Admin123!");
        String primero = login("estudiante@uniajc.edu.co", "Estudiante123!");
        Result nuevoUsuario = post("/api/admin/usuarios", admin,
            Map.of("nombre", "Otra persona", "email", "otra@uniajc.edu.co", "password", "Otra12345!", "rol", "ESTUDIANTE", "activo", true));
        assertEquals(201, nuevoUsuario.status);
        String segundo = login("otra@uniajc.edu.co", "Otra12345!");
        long equipo = equipo("PROY-001");
        long solicitud = post("/api/solicitudes", primero,
            Map.of("equipoId", equipo, "fechaInicio", "2026-10-26", "fechaFin", "2026-10-27", "motivo", "Exposición")).body.get("id").asLong();
        assertEquals(200, get("/api/solicitudes/" + solicitud, primero).status);
        assertEquals(403, get("/api/solicitudes/" + solicitud, segundo).status);
        assertEquals(200, get("/api/admin/usuarios/" + nuevoUsuario.body.get("id").asLong(), admin).status);
        assertEquals(403, post("/api/solicitudes/" + solicitud + "/cancelar", segundo, null).status);
        assertEquals("CANCELADA", post("/api/solicitudes/" + solicitud + "/cancelar", primero, null).body.get("estado").asText());
        assertEquals(409, post("/api/solicitudes/" + solicitud + "/cancelar", primero, null).status);
        assertEquals(1, get("/api/solicitudes/mias", primero).body.size());
        assertEquals(0, get("/api/solicitudes/mias", segundo).body.size());
    }

    @Test
    void vencimientoYDevolucionSinNovedad() throws Exception {
        String solicitante = login("estudiante@uniajc.edu.co", "Estudiante123!");
        String encargado = login("encargado@uniajc.edu.co", "Encargado123!");
        String admin = login("admin@uniajc.edu.co", "Admin123!");
        long equipo = equipo("ACCE-001");
        long solicitud = post("/api/solicitudes", solicitante,
            Map.of("equipoId", equipo, "fechaInicio", "2026-10-26", "fechaFin", "2026-10-27", "motivo", "Clase híbrida")).body.get("id").asLong();
        post("/api/solicitudes/" + solicitud + "/decision", encargado, Map.of("aprobada", true));
        long prestamo = post("/api/solicitudes/" + solicitud + "/entregar", encargado, null).body.get("id").asLong();

        clock.set("2026-10-29T12:00:00Z");
        admin = login("admin@uniajc.edu.co", "Admin123!");
        encargado = login("encargado@uniajc.edu.co", "Encargado123!");
        assertEquals(1, get("/api/reportes/resumen", admin).body.get("prestamosVencidos").asInt());
        assertTrue(get("/api/prestamos?estado=ACTIVO", encargado).body.get(0).get("vencido").asBoolean());
        assertEquals("DEVUELTO", post("/api/prestamos/" + prestamo + "/devolver", encargado,
            Map.of("condicion", "BUENO", "observacion", "Sin daños")).body.get("estado").asText());
        assertEquals("DISPONIBLE", get("/api/equipos/" + equipo, null).body.get("estado").asText());
        assertEquals(0, get("/api/reportes/resumen", admin).body.get("prestamosVencidos").asInt());
    }

    @Test
    void rechazoYNovedadNoCritica() throws Exception {
        String solicitante = login("estudiante@uniajc.edu.co", "Estudiante123!");
        String encargado = login("encargado@uniajc.edu.co", "Encargado123!");
        long equipo = equipo("PORT-002");
        long rechazada = post("/api/solicitudes", solicitante,
            Map.of("equipoId", equipo, "fechaInicio", "2026-10-26", "fechaFin", "2026-10-28", "motivo", "Práctica")).body.get("id").asLong();
        assertEquals("RECHAZADA", post("/api/solicitudes/" + rechazada + "/decision", encargado,
            Map.of("aprobada", false, "observacion", "No procede")).body.get("estado").asText());
        assertEquals(409, post("/api/solicitudes/" + rechazada + "/entregar", encargado, null).status);
        long aprobada = post("/api/solicitudes", solicitante,
            Map.of("equipoId", equipo, "fechaInicio", "2026-10-26", "fechaFin", "2026-10-28", "motivo", "Práctica alterna")).body.get("id").asLong();
        post("/api/solicitudes/" + aprobada + "/decision", encargado, Map.of("aprobada", true));
        long prestamo = post("/api/solicitudes/" + aprobada + "/entregar", encargado, null).body.get("id").asLong();
        assertEquals(400, post("/api/prestamos/" + prestamo + "/devolver", encargado, Map.of("condicion", "NOVEDAD")).status);
        assertEquals("DEVUELTO_CON_NOVEDAD", post("/api/prestamos/" + prestamo + "/devolver", encargado,
            Map.of("condicion", "NOVEDAD", "observacion", "Rasguño superficial")).body.get("estado").asText());
        assertEquals("DISPONIBLE", get("/api/equipos/" + equipo, null).body.get("estado").asText());
    }

    @Test
    void aprobacionSinEntregaCaducaYLiberaEquipo() throws Exception {
        String solicitante = login("estudiante@uniajc.edu.co", "Estudiante123!");
        String encargado = login("encargado@uniajc.edu.co", "Encargado123!");
        long equipo = equipo("PORT-001");
        long solicitud = post("/api/solicitudes", solicitante,
            Map.of("equipoId", equipo, "fechaInicio", "2026-10-26", "fechaFin", "2026-10-31", "motivo", "Proyecto")).body.get("id").asLong();
        post("/api/solicitudes/" + solicitud + "/decision", encargado, Map.of("aprobada", true));
        assertEquals("RESERVADO", get("/api/equipos/" + equipo, null).body.get("estado").asText());

        clock.set("2026-10-28T13:00:00Z");
        solicitante = login("estudiante@uniajc.edu.co", "Estudiante123!");
        encargado = login("encargado@uniajc.edu.co", "Encargado123!");
        assertEquals("VENCIDA", get("/api/solicitudes/" + solicitud, solicitante).body.get("estado").asText());
        assertEquals("DISPONIBLE", get("/api/equipos/" + equipo, null).body.get("estado").asText());
        assertEquals(409, post("/api/solicitudes/" + solicitud + "/entregar", encargado, null).status);
    }

    @Test
    void rutasPublicasYErroresHttpTienenCodigosCoherentes() throws Exception {
        assertEquals(200, mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/favicon.svg"))
            .andReturn().getResponse().getStatus());
        assertEquals(404, get("/favicon.ico", null).status);
        assertEquals(405, mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/auth/login"))
            .andReturn().getResponse().getStatus());
        assertEquals(415, mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/auth/login")
            .contentType(MediaType.TEXT_PLAIN).content("texto"))
            .andReturn().getResponse().getStatus());
        assertEquals(400, get("/api/equipos?estado=INVALIDO", null).status);
    }

    @Test
    void estudianteYDocenteTienenLimitesDistintosYLoginPropio() throws Exception {
        Result configuracion = get("/api/configuracion/prestamos", null);
        assertEquals(200, configuracion.status);
        assertEquals(7, configuracion.body.get("maxDiasEstudiante").asInt());
        assertEquals(30, configuracion.body.get("maxDiasDocente").asInt());

        Result loginEstudiante = post("/api/auth/login", null,
            Map.of("email", "estudiante@uniajc.edu.co", "password", "Estudiante123!"));
        Result loginDocente = post("/api/auth/login", null,
            Map.of("email", "docente@uniajc.edu.co", "password", "Docente123!"));
        assertEquals(200, loginEstudiante.status);
        assertEquals(200, loginDocente.status);
        assertEquals("ESTUDIANTE", loginEstudiante.body.get("user").get("rol").asText());
        assertEquals("DOCENTE", loginDocente.body.get("user").get("rol").asText());
        String estudiante = loginEstudiante.body.get("token").asText();
        String docente = loginDocente.body.get("token").asText();

        long portatil = equipo("PORT-001");
        assertEquals(400, post("/api/solicitudes", estudiante,
            Map.of("equipoId", portatil, "fechaInicio", "2026-10-26", "fechaFin", "2026-11-03", "motivo", "Ocho días")).status);
        assertEquals(201, post("/api/solicitudes", estudiante,
            Map.of("equipoId", portatil, "fechaInicio", "2026-10-26", "fechaFin", "2026-11-02", "motivo", "Siete días")).status);
        assertEquals(200, get("/api/solicitudes/mias", estudiante).status);
        assertEquals(200, get("/api/prestamos/mios", estudiante).status);

        long proyector = equipo("PROY-001");
        assertEquals(400, post("/api/solicitudes", docente,
            Map.of("equipoId", proyector, "fechaInicio", "2026-10-26", "fechaFin", "2026-11-26", "motivo", "Treinta y un días")).status);
        assertEquals(201, post("/api/solicitudes", docente,
            Map.of("equipoId", proyector, "fechaInicio", "2026-10-26", "fechaFin", "2026-11-25", "motivo", "Treinta días")).status);
        assertEquals(200, get("/api/solicitudes/mias", docente).status);
        assertEquals(200, get("/api/prestamos/mios", docente).status);

        String admin = login("admin@uniajc.edu.co", "Admin123!");
        assertEquals(400, post("/api/admin/usuarios", admin,
            Map.of("nombre", "Rol anterior", "email", "anterior@uniajc.edu.co",
                "password", "Clave12345!", "rol", "SOLICITANTE", "activo", true)).status);
    }

    @Test
    void cambioDeRolRevalidaPlazoAlAprobarYEntregar() throws Exception {
        String docente = login("docente@uniajc.edu.co", "Docente123!");
        String encargado = login("encargado@uniajc.edu.co", "Encargado123!");
        String admin = login("admin@uniajc.edu.co", "Admin123!");
        long docenteId = get("/api/auth/me", docente).body.get("id").asLong();
        long equipo = equipo("PORT-001");
        long solicitud = post("/api/solicitudes", docente,
            Map.of("equipoId", equipo, "fechaInicio", "2026-10-26", "fechaFin", "2026-11-15", "motivo", "Clase extendida"))
            .body.get("id").asLong();

        assertEquals(200, put("/api/admin/usuarios/" + docenteId, admin,
            Map.of("nombre", "Docente Demo", "email", "docente@uniajc.edu.co", "rol", "ESTUDIANTE", "activo", true)).status);
        assertEquals(409, post("/api/solicitudes/" + solicitud + "/decision", encargado,
            Map.of("aprobada", true, "observacion", "Solicitud larga")).status);
        assertEquals(200, put("/api/admin/usuarios/" + docenteId, admin,
            Map.of("nombre", "Docente Demo", "email", "docente@uniajc.edu.co", "rol", "DOCENTE", "activo", true)).status);
        assertEquals(200, post("/api/solicitudes/" + solicitud + "/decision", encargado,
            Map.of("aprobada", true, "observacion", "Aprobada")).status);

        assertEquals(200, put("/api/admin/usuarios/" + docenteId, admin,
            Map.of("nombre", "Docente Demo", "email", "docente@uniajc.edu.co", "rol", "ESTUDIANTE", "activo", true)).status);
        assertEquals(409, post("/api/solicitudes/" + solicitud + "/entregar", encargado, null).status);
        assertEquals("RESERVADO", get("/api/equipos/" + equipo, null).body.get("estado").asText());
    }

    @Test
    void migracionDeSolicitanteConservaCuentaEHistorial() throws Exception {
        Rol anterior = roles.save(new Rol(RolCodigo.SOLICITANTE, "Solicitante anterior"));
        Usuario usuario = new Usuario();
        usuario.nombre = "Cuenta histórica";
        usuario.email = "historico@uniajc.edu.co";
        usuario.passwordHash = passwords.encode("Historico123!");
        usuario.rol = anterior;
        usuario = usuarios.save(usuario);

        Solicitud historial = new Solicitud();
        historial.solicitante = usuario;
        historial.equipo = equipos.findByCodigoIgnoreCase("PORT-001").orElseThrow();
        historial.fechaInicio = LocalDate.parse("2026-10-26");
        historial.fechaFin = LocalDate.parse("2026-10-27");
        historial.fechaSolicitud = LocalDateTime.parse("2026-10-26T07:00:00");
        historial.motivo = "Registro histórico";
        historial = solicitudes.save(historial);

        datosIniciales.run(null);
        assertEquals(RolCodigo.ESTUDIANTE, usuarios.findById(usuario.id).orElseThrow().rol.codigo);
        String token = login("historico@uniajc.edu.co", "Historico123!");
        assertEquals(1, get("/api/solicitudes/mias", token).body.size());
        assertEquals(historial.id.longValue(), get("/api/solicitudes/mias", token).body.get(0).get("id").asLong());
    }

    private long equipo(String codigo) throws Exception {
        Result catalogo = get("/api/equipos", null);
        assertEquals(200, catalogo.status);
        for (JsonNode e : catalogo.body) if (codigo.equals(e.get("codigo").asText())) return e.get("id").asLong();
        throw new AssertionError("No se encontró equipo " + codigo);
    }

    private String login(String email, String password) throws Exception {
        Result result = post("/api/auth/login", null, Map.of("email", email, "password", password));
        assertEquals(200, result.status);
        return result.body.get("token").asText();
    }

    private Result get(String path, String token) throws Exception {
        MockHttpServletRequestBuilder request = org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(path);
        if (token != null) request.header("Authorization", "Bearer " + token);
        var response = mvc.perform(request).andReturn().getResponse();
        return new Result(response.getStatus(), json.readTree(response.getContentAsString()));
    }

    private Result post(String path, String token, Object body) throws Exception {
        MockHttpServletRequestBuilder request = org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(path).contentType(MediaType.APPLICATION_JSON);
        if (token != null) request.header("Authorization", "Bearer " + token);
        if (body != null) request.content(json.writeValueAsString(body));
        var response = mvc.perform(request).andReturn().getResponse();
        return new Result(response.getStatus(), json.readTree(response.getContentAsString()));
    }

    private Result put(String path, String token, Object body) throws Exception {
        MockHttpServletRequestBuilder request = org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(path)
            .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body));
        if (token != null) request.header("Authorization", "Bearer " + token);
        var response = mvc.perform(request).andReturn().getResponse();
        return new Result(response.getStatus(), json.readTree(response.getContentAsString()));
    }

    private record Result(int status, JsonNode body) {}

    @TestConfiguration
    static class FixedClockConfig {
        @Bean @Primary
        MutableClock mutableClock() { return new MutableClock(); }
    }

    static class MutableClock extends Clock {
        private final AtomicReference<Instant> now = new AtomicReference<>(Instant.parse("2026-10-26T12:00:00Z"));
        private final ZoneId zone = ZoneId.of("America/Bogota");
        void set(String instant) { now.set(Instant.parse(instant)); }
        @Override public ZoneId getZone() { return zone; }
        @Override public Clock withZone(ZoneId zone) { return Clock.fixed(now.get(), zone); }
        @Override public Instant instant() { return now.get(); }
    }
}
