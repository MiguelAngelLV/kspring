## KSpring

KSpring es una biblioteca diseñada para automatizar dos tareas muy «aburridas y repetitivas» a la hora
de trabajar con Spring Boot y Kotlin: Generar clientes para test's y crear documentación para Swagger UI.

Esta diseña para **mi estructura**, por lo cual, algunos métodos, sistemas de conversión u otros pueden no adaptarse a tus necesidades. 
Te animo a que envies pull requests con tus aportaciones.


#### Generación de Clientes para Test's

Cuando realizamos los test's de nuestra API de Spring Boot, es habitual acceder a los EndPoints usando
MockMVC, lo cual nos obliga a escribir manualmente las URL's de los EndPoints, convertir de POJO a JSON y viceversa, entre otras muchas tareas.

El módulo Spring2MVC de KSpring escaneará todos tus RestControllers y generará una nueva clase con el prefijo «Api» que incluirá todos
los métodos y se ocupará de la transformación de JSON. Así msimo, también incluye varios métodos de ayuda para verificar tipo de respuesta (como errores) entre otros.


De este Controller
```kotlin

@REST("/json/users")
class UsersController : Controller() {

    @GetMapping("/logged")
    fun logged(): User {
        return login.user
    }


    @PostMapping("/login")
    fun login(@RequestBody data: Login): User {


        login.user = User()

        login.user = ds.find(User::class.java)
                .field("username").equalIgnoreCase(data.username)
                .field("password").equal(data.password.toSHA256())
                .get() ?: throw UnauthorizedError()

        return login.user


    }
}

```

Spring2MVC generará esta clase de ayuda:

```kotlin

class ApiUsers(mockMvc: MockMvc, mapper: ObjectMapper) : ApiBase(mockMvc, mapper) {
    fun logged(): ApiResponse<User> {
        val call = _get("/json/users/logged")
        val perform = mockMvc.perform(call)
        val result = perform.andReturn()
        return ApiResponse(result, jacksonTypeRef(), mapper)
    }

    fun login(data: Login): ApiResponse<User> {
        val call = _post("/json/users/login", data)
        val perform = mockMvc.perform(call)
        val result = perform.andReturn()
        return ApiResponse(result, jacksonTypeRef(), mapper)
    }

}

```

Esto nos permitirá mantener el código siempre actualizado y limpio.


#### Generación de Documentación para Swagger UI

Swagger UI requiere de añadir numerosas anotaciones a nuestro código, las cuales, además de ser tediosas de escribir, dejan el código bastante menos legibles. 
Por ello, usando Spring2Swagger, podremos usar una documentación semejante a Javadoc para documentar nuestros EndPoints y S2S creará subclases con las anotaciones pertinentes
para que Swagger pueda usarlas sin tener que escribirlas nosotros «ni verlas».


```kotlin


/**
 *
 * @Summary Usuarios
 * @Description Gestión de los usuarios
 **/
@REST("/json/users")
class UsersController : Controller() {


    /**
     * @Summary Usuario actual
     * @Description Devuelve el usuario actualmente identificado
     * @Response 200 El usuario identificado
     */
    @GetMapping("/logged")
    fun logged(): User {
        return login.user
    }


    /**
     * @Summary Identificación
     * @Description Permite indentificarse el sistema
     * @Response 200 El usuario se ha identificado correctamente
     * @Response 401 Datos de usuario incorrectos
     */
    @PostMapping("/login")
    fun login(@RequestBody data: Login): User {


        login.user = User()

        login.user = ds.find(User::class.java)
                .field("username").equalIgnoreCase(data.username)
                .field("password").equal(data.password.toSHA256())
                .get() ?: throw UnauthorizedError()

        return login.user


    }


    /**
     * @Summary Salir
     * @Description Cierra la sesión actual
     * @Response 200 Devuelve el usuario invitado
     */
    @GetMapping("/logout")
    fun logout(): User {
        login.user = User()
        return login.user
    }

}


``` 

Se convertirá en esto:

```kotlin


@RestController
@RequestMapping("/json/users")
@Api(
        value = "/json/users",
        tags = ["Usuarios"],
        description = "Gestión de los usuarios"
)
class UsersControllerSwagger : UsersController() {
    @ApiOperation(
            value = "Usuario actual",
            notes = "Devuelve el usuario actualmente identificado"
    )
    @ApiResponses(ApiResponse(code = 200, message = "El usuario identificado"))
    override fun logged(): User = super.logged()

    @ApiOperation(
            value = "Identificación",
            notes = "Permite indentificarse el sistema"
    )
    @ApiResponses(ApiResponse(code = 200, message = "El usuario se ha identificado correctamente"), ApiResponse(code = 401, message = "Datos de usuario incorrectos"))
    override fun login(@NotNull @RequestBody data: Login): User = super.login(data)

    @ApiOperation(
            value = "Salir",
            notes = "Cierra la sesión actual"
    )
    @ApiResponses(ApiResponse(code = 200, message = "Devuelve el usuario invitado"))
    override fun logout(): User = super.logout()

}

```

Para asegurar que se no se pasa por alto ninguna anotación, el KSpring dará error de compilación en caso de no encontrar la pertinente documentación para uno de los EndPoints.



#### Uso

Primero debemos añadir al gradle build los plugin AllOpen y KAPT

```groovy

apply plugin: 'kotlin-kapt'


allOpen {
    annotation("org.malv.kspring.spring2swagger.REST")
}

```

Después, añadimos jitpack a los repositorios y la dependencias

```groovy
repositories {
    mavenCentral()
    maven { url 'https://jitpack.io' }

}

dependencies {
    ...
    kapt 'com.github.MiguelAngelLV:kspring:-SNAPSHOT'
    implementation 'com.github.MiguelAngelLV:kspring:-SNAPSHOT'

    ...
}

```

Después, tenemos que reemplazar en nuestros controlladores las anotaciones **@RestController** y **@RequestMapping** por **@REST**, y finalmente, añadir la documentación.


En el caso de usar IntelliJ, este no genera automáticamente las clases y necesitaremos ejecutar manualmente gradle para proceda a la generación de las clases, tanto la primera 
como cada vez que realicemos cambios en la interfaz de la misma (documentación, nuevos métodos, cambio de parámetros...)

