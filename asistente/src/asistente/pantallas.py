"""Qué pantalla del front acompaña a cada acción del asistente.

El servicio no conoce las rutas del front: devuelve un nombre de pantalla (y un id cuando
hace falta) y el front lo traduce a su ruta. La tabla es fija, así el modelo no decide
adónde se navega: solo puede pedir `ir_a` con una pantalla de su rol.
"""
from asistente.herramientas.registro import Rol

# Pantallas que cada rol puede abrir. Las que llevan id se marcan en CON_ID.
PANTALLAS: dict[Rol, dict[str, str]] = {
    "PUBLICO": {
        "ingresar": "Iniciar sesión",
        "registrarse": "Crear una cuenta",
    },
    "ALUMNO": {
        "catalogo": "Catálogo de cursos",
        "curso": "Detalle de un curso",
        "mis_inscripciones": "Mis cursos (en los que estoy inscripto)",
    },
    "INSTRUCTOR": {
        "mis_cursos": "Mis cursos",
        "nuevo_curso": "Formulario para crear un curso",
        "editar_curso": "Formulario para editar un curso",
        "alumnos_curso": "Alumnos de un curso",
        "solicitudes": "Mis solicitudes de aprobación",
        "recaudacion": "Mi recaudación",
    },
    "ADMINISTRADOR": {
        "solicitudes": "Solicitudes de aprobación pendientes",
        "cursos": "Todos los cursos",
        "instructores": "Instructores",
        "administradores": "Administradores",
        "recaudacion": "Recaudación",
    },
}
CON_ID = frozenset({"curso", "editar_curso", "alumnos_curso"})

# herramienta -> (pantalla, argumento que trae el id o None)
POR_HERRAMIENTA: dict[str, tuple[str, str | None]] = {
    "cursos_disponibles": ("catalogo", None),
    "buscar_cursos": ("catalogo", None),
    "ver_curso": ("curso", "curso_id"),
    "inscribirme": ("mis_inscripciones", None),
    "mis_inscripciones": ("mis_inscripciones", None),
    "mis_cursos": ("mis_cursos", None),
    "ver_mi_curso": ("editar_curso", "curso_id"),
    "alumnos_de_mi_curso": ("alumnos_curso", "curso_id"),
    "crear_curso": ("mis_cursos", None),
    "editar_curso": ("mis_cursos", None),
    "borrar_curso": ("mis_cursos", None),
    "pedir_aprobacion": ("solicitudes", None),
    "mis_solicitudes": ("solicitudes", None),
    "mi_recaudacion": ("recaudacion", None),
    "listar_instructores": ("instructores", None),
    "ver_instructor": ("instructores", None),
    "crear_instructor": ("instructores", None),
    "borrar_instructor": ("instructores", None),
    "asignar_especialidad": ("instructores", None),
    "quitar_especialidad": ("instructores", None),
    "listar_administradores": ("administradores", None),
    "crear_administrador": ("administradores", None),
    "borrar_administrador": ("administradores", None),
    "solicitudes_pendientes": ("solicitudes", None),
    "aprobar_solicitud": ("solicitudes", None),
    "rechazar_solicitud": ("solicitudes", None),
    "listar_cursos_admin": ("cursos", None),
    "ver_curso_admin": ("cursos", None),
    "reporte_recaudacion": ("recaudacion", None),
    "reporte_recaudacion_por_curso": ("recaudacion", None),
    "reporte_inscripciones": ("recaudacion", None),
}


def del_rol(rol: Rol | None) -> dict[str, str]:
    return PANTALLAS[rol or "PUBLICO"]


def armar(rol: Rol | None, nombre: str, id_: object = None) -> dict | None:
    """{"nombre": ..., "id": ...} si la pantalla es del rol y tiene el id que necesita."""
    if nombre not in del_rol(rol):
        return None
    if nombre in CON_ID:
        if isinstance(id_, bool) or not isinstance(id_, int | str) or not str(id_).isdigit():
            return None
        return {"nombre": nombre, "id": int(id_)}
    return {"nombre": nombre, "id": None}


def de_herramienta(rol: Rol | None, herramienta: str, args: dict) -> dict | None:
    entrada = POR_HERRAMIENTA.get(herramienta)
    if entrada is None:
        return None
    nombre, clave_id = entrada
    return armar(rol, nombre, args.get(clave_id) if clave_id else None)


IR_A = "ir_a"


def esquema_ir_a(rol: Rol | None) -> dict:
    opciones = del_rol(rol)
    lista = "; ".join(f"{k}: {v}" for k, v in opciones.items())
    return {"type": "function", "function": {
        "name": IR_A,
        "description": (
            "Abre una pantalla de la app para el usuario (por ejemplo 'llevame a mis "
            f"solicitudes'). Pantallas: {lista}. Las de un curso necesitan curso_id."),
        "parameters": {
            "type": "object",
            "properties": {
                "pantalla": {"type": "string", "enum": list(opciones)},
                "curso_id": {"type": "integer"},
            },
            "required": ["pantalla"],
        },
    }}
