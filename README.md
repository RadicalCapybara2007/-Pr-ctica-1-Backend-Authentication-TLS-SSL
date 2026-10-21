# Practicauno — API segura

Backend REST con Java 21 y Spring Boot. El servicio usa HTTPS con TLS 1.2/1.3 y autenticación JWT Bearer. No incluye frontend ni persistencia de usuarios: la cuenta de demostración se mantiene en memoria.

## Requisitos

- JDK 21
- Git
- Postman para las pruebas manuales (opcional)
- `keytool` incluido con el JDK

El proyecto incluye Maven Wrapper (`./mvnw`), por lo que no necesitas instalar Maven.

## Configuración local

Ejecuta estos pasos desde la carpeta raíz del proyecto.

### 1. Crear el certificado de desarrollo

El certificado PKCS12 se guarda en `.local/`, una carpeta excluida de Git. No agregues el keystore al repositorio.

```bash
mkdir -p .local
keytool -genkeypair \
  -alias practicauno-local \
  -keyalg RSA \
  -keysize 2048 \
  -storetype PKCS12 \
  -keystore .local/localhost.p12 \
  -validity 365 \
  -dname "CN=localhost, OU=Desarrollo, O=PracticaUno, C=MX" \
  -ext "SAN=dns:localhost,ip:127.0.0.1"
```

Cuando `keytool` lo solicite, define una contraseña para el keystore. Se pedirá de nuevo al arrancar la aplicación. No la escribas en archivos versionados.

Si necesitas verificar el archivo:

```bash
keytool -list -keystore .local/localhost.p12 -storetype PKCS12
```

### 2. Definir variables y arrancar

Las variables deben definirse en la misma terminal desde la que ejecutarás la aplicación. No compartas ni guardes sus valores en el repositorio.

#### Linux/macOS con fish

```fish
read -s -P 'Contraseña del keystore: ' SSL_KEY_STORE_PASSWORD
echo
set -gx SSL_KEY_STORE_PASSWORD $SSL_KEY_STORE_PASSWORD

read -s -P 'Contraseña del usuario demo: ' APP_AUTH_PASSWORD
echo
set -gx APP_AUTH_PASSWORD $APP_AUTH_PASSWORD

set -gx APP_AUTH_USERNAME demo
set -gx JWT_SECRET (openssl rand -base64 48)
./mvnw spring-boot:run
```

La contraseña del usuario debe tener al menos 8 caracteres para las pruebas. `JWT_SECRET` debe tener al menos 32 bytes; el comando `openssl` genera un valor aleatorio suficiente. La clave debe mantenerse igual mientras necesites validar los tokens que ya fueron emitidos. Reiniciar con otra clave invalida esos tokens.

#### Linux/macOS con Bash o Zsh

```bash
read -rsp "Contraseña del keystore: " SSL_KEY_STORE_PASSWORD; echo
export SSL_KEY_STORE_PASSWORD
read -rsp "Contraseña del usuario demo: " APP_AUTH_PASSWORD; echo
export APP_AUTH_PASSWORD
export APP_AUTH_USERNAME=demo
export JWT_SECRET="$(openssl rand -base64 48)"
./mvnw spring-boot:run
```

El servidor inicia en `https://localhost:8443`.

## API disponible

| Método | Ruta | Acceso | Uso |
|---|---|---|---|
| `POST` | `/api/auth/login` | Público | Validar usuario y obtener JWT |
| `GET` | `/api/secure` | JWT válido | Recurso protegido de demostración |
| `GET` | `/api/admin` | Rol `ADMIN` | Recurso de demostración para comprobar autorización |

La cuenta `demo` recibe el rol `USER`, así que puede acceder a `/api/secure`, pero no a `/api/admin`.

### Iniciar sesión y obtener token

Envía esta solicitud en Postman:

- Método: `POST`
- URL: `https://localhost:8443/api/auth/login`
- Authorization: `No Auth`
- Body → `raw` → `JSON`:

```json
{
  "username": "demo",
  "password": "<valor configurado en APP_AUTH_PASSWORD>"
}
```

Una respuesta correcta tiene estado `200 OK` y una estructura similar a:

```json
{
  "tokenType": "Bearer",
  "accessToken": "<token>",
  "expiresIn": 3600
}
```

Usa el valor de `accessToken` en las solicitudes protegidas. No compartas tokens en capturas, registros o repositorios.

### Probar con Postman

El certificado local es autofirmado. Para estas pruebas locales, desactiva **Settings → General → SSL certificate verification** en Postman o configura el certificado en Postman. No desactives la verificación TLS para conexiones de producción.

1. Envía el login anterior y copia `accessToken`.
2. Envía `GET https://localhost:8443/api/secure`.
3. En Authorization, elige el esquema `Bearer` y pega el token de acceso.
4. Envía `GET https://localhost:8443/api/admin` con el mismo token para probar la respuesta de autorización.

| Caso | Solicitud | Resultado esperado |
|---|---|---|
| Login válido | `POST /api/auth/login` con `demo` y su contraseña configurada | `200 OK`, respuesta con `accessToken` |
| Login inválido | Login con contraseña incorrecta | `401 Unauthorized` |
| Ruta sin token | `GET /api/secure` sin Authorization | `401 Unauthorized` |
| Token inválido | `GET /api/secure` con token inválido o vencido | `401 Unauthorized` |
| Token válido | `GET /api/secure` con el token de acceso JWT | `200 OK`, mensaje y usuario |
| Sin rol requerido | `GET /api/admin` con el token del usuario `demo` | `403 Forbidden` |

La respuesta para una ruta no autenticada indica que falta autenticación o que el token no es válido. Un usuario autenticado sin el rol exigido recibe un mensaje de acceso denegado.

### Comprobar TLS con curl

Con el servidor ejecutándose:

```bash
curl -k -v -o /dev/null https://localhost:8443/
```

Busca `TLSv1.2` o `TLSv1.3` y una respuesta HTTP. Es normal que `/` devuelva `401`, ya que no es una ruta pública del API. La opción `-k` omite la validación del certificado autofirmado y solo se usa aquí para desarrollo local.

## Pruebas automatizadas

Ejecuta desde la raíz:

```bash
./mvnw test
```

Las pruebas automatizadas desactivan TLS para usar MockMvc y suministran credenciales y una clave de prueba aisladas; no necesitan el keystore local. Cubren el inicio de la aplicación, login inválido, ausencia y rechazo de token, acceso autorizado con JWT y respuesta `403` por falta de rol.

## Seguridad y alcance

- El usuario es solo de demostración y se almacena en memoria. Se pierde al reiniciar el proceso y no debe usarse como gestión de usuarios de producción.
- La contraseña del usuario y la del keystore se reciben mediante variables de entorno. No guardes sus valores en `application.properties`.
- La clave JWT debe mantenerse secreta y tener al menos 32 bytes. Usa una clave estable y gestionada de forma segura en despliegues reales.
- El certificado autofirmado y el keystore local son únicamente para desarrollo. Producción requiere un certificado confiable y gestión segura de secretos.
- Spring Security protege por defecto las rutas no públicas; solo `/api/auth/login` es pública. No se habilita CORS porque este proyecto es exclusivamente backend y no incluye cliente web.
