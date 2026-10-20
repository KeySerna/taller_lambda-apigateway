# Introducción a microservicios con Java, AWS Lambda y API Gateway

Microservicio REST mínimo que devuelve el cuadrado de un número entero. La lógica es un
método estático de Java que se ejecuta como función de AWS Lambda y se expone por HTTP
mediante Amazon API Gateway.

```
GET https://<api-id>.execute-api.<region>.amazonaws.com/Beta?value=14   ->   196
```

## Estructura del proyecto

```
pom.xml
src/main/java/co/edu/escuelaing/services/MathServices.java      # el servicio
src/main/java/co/edu/escuelaing/services/SquareRequest.java     # POJO de entrada
src/main/java/co/edu/escuelaing/services/SquareResponse.java    # POJO de salida
src/test/java/co/edu/escuelaing/services/MathServicesTest.java  # pruebas unitarias
```

```java
public class MathServices {
    public static Integer square(Integer i) {
        return i * i;
    }
}
```

## Compilación

Requisitos: JDK 17 o superior y Maven 3.

```bash
mvn package
```

Este comando ejecuta las pruebas y genera `target/math-services-1.0-SNAPSHOT.jar`, que es el
archivo que se sube a Lambda.

> El tutorial original usa Java 8. AWS Lambda ya no ofrece el runtime de Java 8, así que el
> proyecto compila para Java 17 y funciona con los runtimes **Java 17** o **Java 21**.

## 0. Entrar a AWS (AWS Academy)

1. Entra a AWS Academy → tu curso → **Modules** → **Launch AWS Academy Learner Lab**.
2. Haz clic en **Start Lab** y espera a que el círculo junto a **AWS** se ponga verde.
3. Haz clic en **AWS** para abrir la consola.
4. Verifica que la región (arriba a la derecha) sea **us-east-1 (N. Virginia)**.

## 1. Crear la función Lambda

1. En el buscador de la consola escribe **Lambda** y ábrelo.
2. Haz clic en **Create function** → **Author from scratch**.
3. **Function name:** `square`.
4. **Runtime:** **Java 21** (o Java 17).
5. Despliega **Additional settings** → en el rol de ejecución elige **Custom execution role** /
   **Use an existing role** → selecciona `LabRole`.
   (En consolas antiguas la opción se llama **Change default execution role**. Fuera de AWS
   Academy puedes dejar que Lambda cree un rol nuevo.)
6. No actives **Durable execution** ni **EC2 capacity provider**.
7. Haz clic en **Create function** (en la consola nueva puede llamarse **Create Event Function**).

### Subir el código

1. En la pestaña **Code**, haz clic en **Upload from** → **.zip or .jar file**.
2. Selecciona `target/math-services-1.0-SNAPSHOT.jar` y haz clic en **Save**.
3. Más abajo, en **Runtime settings**, haz clic en **Edit** y en **Handler** escribe:
   ```
   co.edu.escuelaing.services.MathServices::square
   ```
   El formato es `{nombre completo de la clase}::{nombre del método}`. Haz clic en **Save**.

### Probar la función

1. Abre la pestaña **Test**.
2. Selecciona **Create new event**, con nombre `testSquare`.
3. Borra el JSON del cuadro de texto y escribe solo el número `5`.
4. Haz clic en **Save** y luego en **Test**.
5. Resultado esperado: `25`.

Evento de prueba `testSquare` con el valor `5` como entrada:

![Evento de prueba testSquare](images/lambda-test-event.png)

Ejecución exitosa de la función con runtime Java 21, respuesta `25`:

![Resultado del test en Lambda](images/lambda-test-success.png)

Detalle de la ejecución (duración, memoria y logs):

![Detalle de la ejecución](images/lambda-test-details.png)

Observa que la entrada es el valor `5` y no un objeto JSON. Esto es importante para el mapeo
que se configura en API Gateway.

## 2. Exponer la función con API Gateway

### Crear la API

1. En el buscador de la consola escribe **API Gateway** y ábrelo.
2. En **REST API** (no la opción *private*) haz clic en **Build**.
3. Selecciona **New API**, con nombre `mathServices` y **API endpoint type: Regional**.
4. Haz clic en **Create API**.

### Crear el método GET

1. Con el recurso `/` seleccionado, haz clic en **Create method**.
2. **Method type:** `GET`.
3. **Integration type:** **Lambda function**.
4. **Lambda proxy integration:** déjala **desactivada** (necesitamos una plantilla de mapeo propia).
5. **Lambda function:** selecciona `square` (región us-east-1).
6. Haz clic en **Create method**.

### Declarar el parámetro `value`

1. Abre la pestaña **Method request** → **Edit**.
2. En **URL query string parameters** haz clic en **Add query string**, con nombre `value`.
3. Haz clic en **Save**.

### Mapear el parámetro hacia la Lambda

1. Abre la pestaña **Integration request** → **Edit**.
2. En **URL query string parameters** agrega uno con nombre `value` y en **Mapped from** escribe:
   ```
   method.request.querystring.value
   ```
3. En **Mapping templates** haz clic en **Add mapping template**:
   - **Content type:** `application/json`
   - **Template body** (primero, como prueba, una constante): `7`
4. Haz clic en **Save**.
5. Abre la pestaña **Test** y haz clic en **Test**. La respuesta siempre será `49`,
   porque la entrada es una constante. Esto muestra que el cuerpo de la plantilla es
   exactamente lo que recibe la Lambda.

   ![Prueba del método GET con la plantilla constante](images/apigw-test-constant.png)

6. Vuelve a **Integration request** → **Edit** y cambia el cuerpo de la plantilla por:
   ```
   $input.params("value")
   ```
   La variable `$input` da acceso al payload y a los parámetros de la petición
   ([referencia de plantillas de mapeo](https://docs.aws.amazon.com/apigateway/latest/developerguide/api-gateway-mapping-template-reference.html)).
7. Haz clic en **Save**.
8. En la pestaña **Test**, en **Query strings** escribe `value=14` y haz clic en **Test**.
   Respuesta esperada: `196`.

   ![Prueba del método GET con value=14](images/apigw-test-value.png)

9. Si omites `value` (por ejemplo, escribiendo solo `14` en **Query strings**), la Lambda
   recibe una entrada vacía y no la puede convertir a `Integer`, así que responde con error:
   ```json
   {"errorMessage":"An error occurred during JSON parsing","errorType":"java.lang.RuntimeException","stackTrace":[]}
   ```
   El status sigue siendo `200` porque, sin proxy integration, API Gateway devuelve 200 por
   defecto aunque la Lambda falle (no hay *integration responses* configuradas para errores).

   ![Prueba del método GET sin el parámetro value](images/apigw-test-sin-value.png)

## 3. Publicar el servicio

1. Haz clic en **Deploy API**.
2. **Stage:** **New stage**, con nombre `Beta`. Haz clic en **Deploy**.
3. Copia la **Invoke URL** que aparece y ábrela en el navegador agregando el parámetro:
   ```bash
   curl "https://<api-id>.execute-api.us-east-1.amazonaws.com/Beta?value=14"
   # 196
   ```

   ![Servicio publicado en el stage Beta con value=14](images/beta-url-value.png)

4. Sin el parámetro (`.../Beta`) el servicio publicado devuelve el mismo error de parseo:

   ![Servicio publicado sin el parámetro value](images/beta-url-sin-value.png)

**Limpieza:** al terminar, elimina la función Lambda y la API para evitar cobros innecesarios.

## Entradas y salidas más complejas

El primer argumento de la función es el payload de la petición, después del mapeo. En Java,
los tipos de entrada y salida pueden ser:

- tipos simples de Java (`String`, `int`, `Integer`, ...),
- tipos de eventos predefinidos de la librería `aws-lambda-java-events`
  (por ejemplo `S3Event` o `APIGatewayProxyRequestEvent`),
- POJOs propios, que Lambda serializa y deserializa a JSON automáticamente.

Las plantillas de mapeo permiten transformar el mensaje que se envía a la función y el que
esta devuelve.

### Ejemplo con POJOs

`MathServices.squareJson` recibe y devuelve objetos propios en lugar de un número suelto:

```java
public static SquareResponse squareJson(SquareRequest request) {
    Integer value = request.getValue();
    return new SquareResponse(value, square(value));
}
```

`SquareRequest` tiene el campo `value` y `SquareResponse` los campos `value` y `square`. Ambas
clases necesitan un constructor público sin argumentos y getters/setters, porque Lambda
los usa para convertir el JSON en objeto y el objeto en JSON.

**Lambda**

1. Recompila con `mvn package`.
2. Crea una nueva función `squareJson` (mismo runtime y rol `LabRole`) y sube el mismo `.jar`.
3. **Handler:** `co.edu.escuelaing.services.MathServices::squareJson`
4. Crea un evento de prueba con este JSON:
   ```json
   {"value": 5}
   ```
   Respuesta esperada:
   ```json
   {"value": 5, "square": 25}
   ```

**API Gateway**

1. En la API `mathServices`, selecciona `/` y haz clic en **Create resource**, con nombre `json`.
2. Con `/json` seleccionado, crea un método `GET` → **Lambda function** → `squareJson`
   (sin proxy integration).
3. En **Method request** agrega el query string `value`.
4. En **Integration request** mapea `value` a `method.request.querystring.value` y agrega una
   plantilla `application/json` que arme un objeto JSON en vez de pasar un número:
   ```
   {"value": $input.params("value")}
   ```
5. Prueba con `value=14`. Respuesta esperada: `{"value":14,"square":196}`.
6. **Deploy API** al stage `Beta` y llama:
   ```bash
   curl "https://<api-id>.execute-api.us-east-1.amazonaws.com/Beta/json?value=14"
   # {"value":14,"square":196}
   ```
