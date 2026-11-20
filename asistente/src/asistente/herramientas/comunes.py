from asistente import backend
from asistente.herramientas import registro
from asistente.herramientas.registro import (
    claims_del_token, generar_contrasena, herramienta, resultado,
)


@herramienta(
    "quien_soy",
    "Dice quién es el usuario actual (rol, userId, email) leyendo su sesión. "
    "Lo puede usar cualquiera; no necesita datos.",
    {"PUBLICO"}, "lectura",
)
async def quien_soy(token: str | None) -> dict:
    claims = claims_del_token(token)
    if not claims:
        return {"ok": True, "datos": "sin sesión"}
    return {"ok": True, "datos": {
        "rol": claims.get("rol"), "userId": claims.get("userId"), "email": claims.get("sub"),
    }}


@herramienta(
    "buscar_cursos",
    "Busca cursos publicados. Lo puede usar cualquiera. Datos opcionales: texto (parte del "
    "título) y especialidad_id.",
    {"PUBLICO"}, "lectura",
)
async def buscar_cursos(
    token: str | None, texto: str | None = None, especialidad_id: int | None = None
) -> dict:
    cursos = await backend.llamar("GET", "/api/courses", token, base_url=registro.BACKEND_URL)
    if not cursos.ok:
        return resultado(cursos)
    espec = await backend.llamar(
        "GET", "/api/specialties", token, base_url=registro.BACKEND_URL
    )
    nombres = {e["id"]: e["name"] for e in (espec.datos or [])} if espec.ok else {}
    filtrados = []
    for c in cursos.datos or []:
        if c.get("status") != "PUBLISHED":
            continue
        if texto and texto.lower() not in (c.get("title") or "").lower():
            continue
        if especialidad_id is not None and c.get("specialtyId") != especialidad_id:
            continue
        filtrados.append({**c, "especialidad": nombres.get(c.get("specialtyId"))})
    cursos.datos = filtrados
    return resultado(cursos)


@herramienta(
    "listar_especialidades",
    "Lista las especialidades disponibles. Lo puede usar cualquiera; no necesita datos.",
    {"PUBLICO"}, "lectura",
)
async def listar_especialidades(token: str | None) -> dict:
    return resultado(
        await backend.llamar("GET", "/api/specialties", token, base_url=registro.BACKEND_URL)
    )


def _deshacer_alumno(args: dict, res: dict) -> dict | None:
    if not res.get("ok"):
        return None
    id_ = (res.get("datos") or {}).get("id")
    if id_ is None:
        return None
    return {"metodo": "DELETE", "ruta": f"/students/{id_}",
            "descripcion": "Eliminar el alumno recién registrado"}


@herramienta(
    "registrar_alumno",
    "Registra un alumno nuevo. Lo usan visitantes sin sesión y administradores. "
    "Datos: nombre, apellido, dni, email. La contraseña se genera sola.",
    {"PUBLICO", "ADMINISTRADOR"}, "escritura", deshacer=_deshacer_alumno,
)
async def registrar_alumno(
    token: str | None, nombre: str, apellido: str, dni: str, email: str
) -> dict:
    contrasena = generar_contrasena()
    r = await backend.llamar(
        "POST", "/students", token, base_url=registro.BACKEND_URL,
        json={"name": nombre, "lastName": apellido, "dni": dni, "email": email,
              "password": contrasena},
    )
    res = resultado(r)
    if res["ok"]:
        res["contrasena"] = contrasena
    return res


@herramienta(
    "registrar_instructor",
    "Registra un instructor nuevo (registro público). Lo usan visitantes sin sesión. "
    "Datos: nombre, apellido, email. La contraseña se genera sola.",
    {"PUBLICO"}, "escritura",
)
async def registrar_instructor(
    token: str | None, nombre: str, apellido: str, email: str
) -> dict:
    contrasena = generar_contrasena()
    r = await backend.llamar(
        "POST", "/instructor", token, base_url=registro.BACKEND_URL,
        json={"name": nombre, "lastName": apellido, "email": email, "password": contrasena},
    )
    res = resultado(r)
    if res["ok"]:
        res["contrasena"] = contrasena
    return res
