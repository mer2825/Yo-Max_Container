// Define el paquete al que pertenece la clase.
package com.example.acceso;

// Importaciones de clases necesarias de Spring Boot.
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// @SpringBootApplication: Es una anotación de conveniencia que combina tres anotaciones:
// 1. @Configuration: Marca la clase como una fuente de definiciones de beans.
// 2. @EnableAutoConfiguration: Le dice a Spring Boot que configure automáticamente la aplicación
//    basándose en las dependencias que tienes en tu `pom.xml`.
// 3. @ComponentScan: Le dice a Spring que busque otros componentes, configuraciones y servicios
//    en el paquete 'com.example.acceso' y sus subpaquetes.
@SpringBootApplication
public class AccesoApplication {

	// El método main es el punto de entrada estándar de cualquier aplicación Java.
	// Es lo primero que se ejecuta al iniciar el programa.
	public static void main(String[] args) {
		// SpringApplication.run(...) es el método que inicia toda la aplicación Spring
		// Boot.
		// Realiza varias tareas, como crear el contexto de la aplicación,
		// escanear componentes y arrancar el servidor web embebido (Tomcat por
		// defecto).
		SpringApplication.run(AccesoApplication.class, args);
		System.out.println("CONEXIÓN EXITOSA");
	}
}
