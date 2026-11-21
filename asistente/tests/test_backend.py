import httpx
import respx

from asistente.backend import llamar

BASE = "http://backend.test"


@respx.mock
async def test_reenvia_token():
    ruta = respx.get(f"{BASE}/api/students").mock(return_value=httpx.Response(200, json=[]))
    res = await llamar("GET", "/api/students", "Bearer abc", base_url=BASE)
    assert res.ok and res.status == 200 and res.datos == []
    assert ruta.calls.last.request.headers["Authorization"] == "Bearer abc"


@respx.mock
async def test_token_sin_prefijo_y_sin_token():
    ruta = respx.get(f"{BASE}/x").mock(return_value=httpx.Response(200, json={}))
    await llamar("GET", "/x", "abc", base_url=BASE)
    assert ruta.calls.last.request.headers["Authorization"] == "Bearer abc"
    await llamar("GET", "/x", None, base_url=BASE)
    assert "Authorization" not in ruta.calls.last.request.headers
    await llamar("GET", "/x", "", base_url=BASE)
    assert "Authorization" not in ruta.calls.last.request.headers


@respx.mock
async def test_problem_detail_a_resultado():
    respx.get(f"{BASE}/x").mock(
        return_value=httpx.Response(
            403, json={"code": "authorization.denied", "detail": "No tiene permisos", "title": "Forbidden"}
        )
    )
    res = await llamar("GET", "/x", "t", base_url=BASE)
    assert (res.ok, res.status, res.code, res.mensaje) == (
        False, 403, "authorization.denied", "No tiene permisos",
    )


@respx.mock
async def test_errores_de_validacion_se_agregan():
    respx.post(f"{BASE}/x").mock(
        return_value=httpx.Response(
            400,
            json={
                "code": "validation.error",
                "title": "Bad Request",
                "errors": [{"field": "email", "message": "inválido"}, {"field": "dni", "message": "requerido"}],
            },
        )
    )
    res = await llamar("POST", "/x", "t", json={"a": 1}, base_url=BASE)
    assert res.mensaje == "Bad Request email: inválido; dni: requerido"


@respx.mock
async def test_error_no_json():
    respx.get(f"{BASE}/x").mock(return_value=httpx.Response(502, text="<html>bad gateway</html>"))
    res = await llamar("GET", "/x", "t", base_url=BASE)
    assert not res.ok and res.status == 502 and res.code is None
    assert res.mensaje == "Error del backend (502)"


@respx.mock
async def test_204_sin_cuerpo():
    respx.delete(f"{BASE}/x").mock(return_value=httpx.Response(204))
    res = await llamar("DELETE", "/x", "t", base_url=BASE)
    assert res.ok is True and res.status == 204 and res.datos is None


@respx.mock
async def test_params_y_json():
    ruta = respx.post(f"{BASE}/x").mock(return_value=httpx.Response(201, json={"id": 1}))
    res = await llamar("POST", "/x", "t", json={"n": "a"}, params={"q": "1"}, base_url=BASE)
    assert res.datos == {"id": 1}
    req = ruta.calls.last.request
    assert req.url.params["q"] == "1" and b'"n"' in req.content


@respx.mock
async def test_backend_caido():
    respx.get(f"{BASE}/x").mock(side_effect=httpx.ConnectError("boom"))
    res = await llamar("GET", "/x", "t", base_url=BASE)
    assert (res.ok, res.status, res.mensaje) == (False, 0, "No se pudo conectar con el backend")


@respx.mock
async def test_401_mensaje_sesion():
    respx.get(f"{BASE}/x").mock(return_value=httpx.Response(401, json={"detail": "Bad creds", "code": "auth.invalid"}))
    res = await llamar("GET", "/x", "t", base_url=BASE)
    assert res.status == 401 and res.mensaje == "Tu sesión venció o no iniciaste sesión."
