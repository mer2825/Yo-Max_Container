/* globals feather:false */

/* globals feather:false */
// La línea anterior es una directiva para herramientas de análisis de código, indicando que 'feather' es una variable global.

// Esta es una función autoejecutable (IIFE - Immediately Invoked Function Expression).
// Se usa para crear un ámbito privado y evitar contaminar el ámbito global.
(function () {
    'use strict' // Activa el modo estricto de JavaScript, que ayuda a escribir código más seguro.
    // Llama a la función replace() de la librería Feather Icons.
    // Esta función busca todos los elementos con el atributo `data-feather` y los reemplaza por el SVG del icono correspondiente.
    feather.replace({ 'aria-hidden': 'true' })
})()