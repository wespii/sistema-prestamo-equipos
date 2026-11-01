"""Prueba de aceptación de la API contra una instancia local con datos semilla.

Uso: python scripts/prueba_integral.py --url http://localhost:8080
El script crea registros con un sufijo único y no modifica las cuentas semilla.
"""

from __future__ import annotations

import argparse
import json
import os
import sys
import time
import urllib.error
import urllib.request
import uuid
from datetime import date, timedelta
from pathlib import Path


class Api:
    def __init__(self, base_url: str):
        self.base_url = base_url.rstrip("/")

    def request(self, method: str, path: str, token: str | None = None, body=None):
        headers = {"Accept": "application/json"}
        if token:
            headers["Authorization"] = f"Bearer {token}"
        data = None
        if body is not None:
            data = json.dumps(body).encode("utf-8")
            headers["Content-Type"] = "application/json"
        request = urllib.request.Request(
            self.base_url + path, data=data, headers=headers, method=method
        )
        started = time.monotonic()
        try:
            with urllib.request.urlopen(request, timeout=15) as response:
                raw = response.read()
                status = response.status
        except urllib.error.HTTPError as error:
            raw = error.read()
            status = error.code
        elapsed_ms = round((time.monotonic() - started) * 1000)
        try:
            payload = json.loads(raw) if raw else None
        except json.JSONDecodeError:
            payload = raw.decode("utf-8", errors="replace")
        return status, payload, elapsed_ms


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--url", default=os.getenv("APP_URL", "http://localhost:8080"))
    parser.add_argument("--output", type=Path)
    args = parser.parse_args()
    api = Api(args.url)
    results = []

    def check(name, method, path, token=None, body=None, expected=(200,), verify=None):
        status, payload, elapsed_ms = api.request(method, path, token, body)
        ok = status in expected and (verify(payload) if verify else True)
        results.append({"caso": name, "http": status, "ms": elapsed_ms, "resultado": "APROBADO" if ok else "FALLIDO"})
        print(f"{'OK' if ok else 'ERROR'} {name}: HTTP {status} ({elapsed_ms} ms)")
        if not ok:
            raise AssertionError(f"{name}: esperaba HTTP {expected}, recibido {status}: {payload}")
        return payload

    admin = check("login administrador", "POST", "/api/auth/login", body={
        "email": os.getenv("SMOKE_ADMIN_EMAIL", "admin@uniajc.edu.co"),
        "password": os.getenv("SMOKE_ADMIN_PASSWORD", "Admin123!"),
    }, verify=lambda p: bool(p.get("token")))["token"]
    encargado = check("login encargado", "POST", "/api/auth/login", body={
        "email": os.getenv("SMOKE_ENCARGADO_EMAIL", "encargado@uniajc.edu.co"),
        "password": os.getenv("SMOKE_ENCARGADO_PASSWORD", "Encargado123!"),
    }, verify=lambda p: bool(p.get("token")))["token"]
    estudiante_demo = check("login estudiante", "POST", "/api/auth/login", body={
        "email": os.getenv("SMOKE_ESTUDIANTE_EMAIL", "estudiante@uniajc.edu.co"),
        "password": os.getenv("SMOKE_ESTUDIANTE_PASSWORD", "Estudiante123!"),
    }, verify=lambda p: p.get("user", {}).get("rol") == "ESTUDIANTE" and bool(p.get("token")))["token"]
    docente = check("login docente", "POST", "/api/auth/login", body={
        "email": os.getenv("SMOKE_DOCENTE_EMAIL", "docente@uniajc.edu.co"),
        "password": os.getenv("SMOKE_DOCENTE_PASSWORD", "Docente123!"),
    }, verify=lambda p: p.get("user", {}).get("rol") == "DOCENTE" and bool(p.get("token")))["token"]
    limits = check("límites por perfil", "GET", "/api/configuracion/prestamos",
                   verify=lambda p: p.get("maxDiasEstudiante") == 7 and p.get("maxDiasDocente") == 30)
    check("reporte sin autenticación", "GET", "/api/reportes/resumen", expected=(401,))
    check("reporte con rol insuficiente", "GET", "/api/reportes/resumen", encargado, expected=(403,))
    check("docente sin acceso administrativo", "GET", "/api/admin/usuarios", docente, expected=(403,))

    suffix = uuid.uuid4().hex[:8]
    category = check("crear categoría", "POST", "/api/admin/categorias", admin,
                     {"nombre": f"Prueba {suffix}", "activo": True}, expected=(201,),
                     verify=lambda p: bool(p.get("id")))
    category_id = category["id"]

    def create_equipment(index):
        return check(f"crear equipo {index}", "POST", "/api/admin/equipos", admin, {
            "codigo": f"QA-{suffix}-{index}", "nombre": f"Portátil QA {index}",
            "descripcion": "Equipo para validar el ciclo completo",
            "caracteristicas": "16 GB RAM", "estado": "DISPONIBLE", "categoriaId": category_id,
        }, expected=(201,), verify=lambda p: p.get("estado") == "DISPONIBLE")

    equipment = create_equipment(1)
    equipment_id = equipment["id"]
    equipment = check("editar equipo", "PUT", f"/api/admin/equipos/{equipment_id}", admin, {
        "codigo": equipment["codigo"], "nombre": "Portátil QA actualizado",
        "descripcion": "Descripción actualizada para validar HU-02",
        "caracteristicas": "32 GB RAM", "estado": "DISPONIBLE", "categoriaId": category_id,
    }, verify=lambda p: p["id"] == equipment_id and p["nombre"] == "Portátil QA actualizado"
        and p["caracteristicas"] == "32 GB RAM")
    check("código de equipo único", "POST", "/api/admin/equipos", admin, {
        "codigo": equipment["codigo"], "nombre": "Duplicado", "descripcion": "Prueba",
        "caracteristicas": "Prueba", "estado": "DISPONIBLE", "categoriaId": category_id,
    }, expected=(400, 409))
    check("catálogo filtrado", "GET", f"/api/equipos?texto=QA-{suffix}&categoriaId={category_id}", admin,
          verify=lambda p: any(e["id"] == equipment_id for e in p))
    check("detalle equipo", "GET", f"/api/equipos/{equipment_id}", admin,
          verify=lambda p: p["codigo"] == equipment["codigo"])

    start = (date.today() + timedelta(days=1)).isoformat()
    end = (date.today() + timedelta(days=3)).isoformat()
    student_over = (date.fromisoformat(start) + timedelta(days=limits["maxDiasEstudiante"] + 1)).isoformat()
    teacher_max = (date.fromisoformat(start) + timedelta(days=limits["maxDiasDocente"])).isoformat()
    teacher_over = (date.fromisoformat(start) + timedelta(days=limits["maxDiasDocente"] + 1)).isoformat()
    check("estudiante excede máximo", "POST", "/api/solicitudes", estudiante_demo, {
        "equipoId": equipment_id, "fechaInicio": start, "fechaFin": student_over,
        "motivo": "Prueba de límite estudiante",
    }, expected=(400,))
    check("docente excede máximo", "POST", "/api/solicitudes", docente, {
        "equipoId": equipment_id, "fechaInicio": start, "fechaFin": teacher_over,
        "motivo": "Prueba de límite docente",
    }, expected=(400,))
    teacher_request = check("docente puede solicitar 30 días", "POST", "/api/solicitudes", docente, {
        "equipoId": equipment_id, "fechaInicio": start, "fechaFin": teacher_max,
        "motivo": "Proyecto académico docente",
    }, expected=(201,), verify=lambda p: p.get("estado") == "PENDIENTE")
    check("docente cancela solicitud propia", "POST",
          f"/api/solicitudes/{teacher_request['id']}/cancelar", docente,
          verify=lambda p: p.get("estado") == "CANCELADA")

    user = check("crear usuario estudiante", "POST", "/api/admin/usuarios", admin, {
        "nombre": "Estudiante Prueba", "email": f"qa-{suffix}@ejemplo.test",
        "password": "Prueba123!", "rol": "ESTUDIANTE", "activo": True,
    }, expected=(201,), verify=lambda p: p.get("rol") == "ESTUDIANTE" and "password" not in p)
    student = check("login nuevo estudiante", "POST", "/api/auth/login", body={
        "email": user["email"], "password": "Prueba123!"
    }, verify=lambda p: bool(p.get("token")))["token"]
    check("solicitante sin acceso administrativo", "GET", "/api/admin/usuarios", student, expected=(403,))

    invalid = {"equipoId": equipment_id, "fechaInicio": start, "fechaFin": start,
               "motivo": "Fecha inválida"}
    check("rechazar fechas incoherentes", "POST", "/api/solicitudes", student, invalid,
          expected=(400, 409))
    request_body = {"equipoId": equipment_id, "fechaInicio": start, "fechaFin": end,
                    "motivo": "Clase de programación"}
    cancellation = check("crear solicitud para cancelar", "POST", "/api/solicitudes", student,
                         request_body, expected=(201,), verify=lambda p: p["estado"] == "PENDIENTE")
    check("cancelar solicitud propia", "POST", f"/api/solicitudes/{cancellation['id']}/cancelar",
          student, expected=(200,), verify=lambda p: p["estado"] == "CANCELADA")
    check("cancelar solicitud dos veces", "POST", f"/api/solicitudes/{cancellation['id']}/cancelar",
          student, expected=(400, 409))

    rejected = check("crear solicitud para rechazo", "POST", "/api/solicitudes", student,
                     request_body, expected=(201,), verify=lambda p: p["estado"] == "PENDIENTE")
    check("rechazar solicitud", "POST", f"/api/solicitudes/{rejected['id']}/decision", encargado,
          {"aprobada": False, "observacion": "No corresponde al préstamo solicitado"},
          verify=lambda p: p["estado"] == "RECHAZADA" and bool(p.get("observacionDecision")))
    check("rechazada no se puede entregar", "POST", f"/api/solicitudes/{rejected['id']}/entregar",
          encargado, expected=(400, 409))

    request = check("crear solicitud válida", "POST", "/api/solicitudes", student,
                    request_body, expected=(201,), verify=lambda p: p["estado"] == "PENDIENTE")
    request_id = request["id"]
    check("historial de solicitudes propias", "GET", "/api/solicitudes/mias", student,
          verify=lambda p: any(s["id"] == request_id for s in p))
    check("bandeja de pendientes", "GET", "/api/solicitudes/pendientes", encargado,
          verify=lambda p: any(s["id"] == request_id for s in p))
    check("entrega sin aprobación", "POST", f"/api/solicitudes/{request_id}/entregar",
          encargado, expected=(400, 409))
    check("aprobar solicitud", "POST", f"/api/solicitudes/{request_id}/decision", encargado,
          {"aprobada": True, "observacion": "Disponible"},
          verify=lambda p: p["estado"] == "APROBADA")
    loan = check("entregar equipo", "POST", f"/api/solicitudes/{request_id}/entregar",
                 encargado, expected=(200, 201), verify=lambda p: p["estado"] == "ACTIVO")
    loan_id = loan["id"]
    check("evitar segunda entrega", "POST", f"/api/solicitudes/{request_id}/entregar",
          encargado, expected=(400, 409))
    check("estado prestado", "GET", f"/api/equipos/{equipment_id}", student,
          verify=lambda p: p["estado"] == "PRESTADO" and not p["disponible"])
    check("evitar solicitud de equipo prestado", "POST", "/api/solicitudes", student,
          request_body, expected=(400, 409))
    check("préstamo visible en historial propio", "GET", "/api/prestamos/mios", student,
          verify=lambda p: any(item["id"] == loan_id for item in p))
    check("devolución con daño", "POST", f"/api/prestamos/{loan_id}/devolver", encargado,
          {"condicion": "DANADO", "observacion": "Pantalla rota"},
          verify=lambda p: p["estado"] == "DEVUELTO_CON_NOVEDAD")
    check("equipo en mantenimiento", "GET", f"/api/equipos/{equipment_id}", admin,
          verify=lambda p: p["estado"] == "MANTENIMIENTO")
    check("historial equipo", "GET", f"/api/equipos/{equipment_id}/historial", admin,
          verify=lambda p: bool(p))

    equipment2 = create_equipment(2)
    body2 = {**request_body, "equipoId": equipment2["id"]}
    request2 = check("segunda solicitud", "POST", "/api/solicitudes", student, body2,
                     expected=(201,))
    check("aprobar segunda solicitud", "POST", f"/api/solicitudes/{request2['id']}/decision",
          encargado, {"aprobada": True, "observacion": "Disponible"})
    loan2 = check("entregar segundo equipo", "POST", f"/api/solicitudes/{request2['id']}/entregar",
                  encargado, expected=(200, 201))
    check("devolución sin daños", "POST", f"/api/prestamos/{loan2['id']}/devolver", encargado,
          {"condicion": "BUENO", "observacion": "Sin daños"},
          verify=lambda p: p["estado"] == "DEVUELTO")
    check("equipo de nuevo disponible", "GET", f"/api/equipos/{equipment2['id']}", admin,
          verify=lambda p: p["estado"] == "DISPONIBLE")
    equipment3 = create_equipment(3)
    body3 = {**request_body, "equipoId": equipment3["id"]}
    request3 = check("solicitud para novedad menor", "POST", "/api/solicitudes", student,
                     body3, expected=(201,))
    check("aprobar solicitud con novedad menor", "POST",
          f"/api/solicitudes/{request3['id']}/decision", encargado,
          {"aprobada": True, "observacion": "Disponible"})
    loan3 = check("entregar equipo con novedad menor", "POST",
                  f"/api/solicitudes/{request3['id']}/entregar", encargado)
    check("devolución con novedad menor", "POST",
          f"/api/prestamos/{loan3['id']}/devolver", encargado,
          {"condicion": "NOVEDAD", "observacion": "Carcasa con marca superficial"},
          verify=lambda p: p["estado"] == "DEVUELTO_CON_NOVEDAD"
              and len(p.get("novedades") or []) == 1)
    check("novedad menor mantiene disponibilidad", "GET", f"/api/equipos/{equipment3['id']}", admin,
          verify=lambda p: p["estado"] == "DISPONIBLE" and p["disponible"])
    check("reporte de indicadores", "GET", "/api/reportes/resumen", admin,
          verify=lambda p: "equiposPorEstado" in p and "prestamosActivos" in p
              and isinstance(p.get("prestamosVencidos"), int) and p["prestamosVencidos"] >= 0)
    check("auditoría con eventos", "GET", "/api/auditoria", admin,
          verify=lambda p: bool(p))
    check("historial de préstamos", "GET", "/api/prestamos", admin,
          verify=lambda p: any(item["id"] == loan_id for item in p))

    report = {"entorno": args.url, "casos": results,
              "aprobados": sum(r["resultado"] == "APROBADO" for r in results),
              "fallidos": sum(r["resultado"] == "FALLIDO" for r in results)}
    if args.output:
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"Resultado: {report['aprobados']} aprobados, {report['fallidos']} fallidos")
    return 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except Exception as exc:
        print(f"Prueba detenida: {exc}", file=sys.stderr)
        sys.exit(1)
