document.addEventListener("DOMContentLoaded", function () {
  const form = document.getElementById("formIniciarSesion");
  const recaptchaError = document.getElementById("recaptcha-error");
  const btnIngresar = document.getElementById("btnIngresar");
  const contadorSpan = document.getElementById("contador-bloqueo");
  const bloqueoAlert = document.getElementById("bloqueo-alert");

  // Lógica de cuenta regresiva para el bloqueo
  if (contadorSpan) {
    let segundosRestantes = parseInt(contadorSpan.textContent, 10);

    // Deshabilitar el botón y campos si está bloqueado
    if (segundosRestantes > 0) {
        btnIngresar.disabled = true;
        document.getElementById("nombreUsuario").readOnly = true;
        document.getElementById("contrasena").readOnly = true;

        // Formatear al cargar la página por si viene de recarga
        actualizarTextoContador(segundosRestantes);
    }

    const intervalo = setInterval(() => {
      segundosRestantes--;

      if (segundosRestantes <= 0) {
        clearInterval(intervalo);
        if(bloqueoAlert) bloqueoAlert.style.display = 'none';

        // Habilitar de nuevo al terminar
        btnIngresar.disabled = false;
        document.getElementById("nombreUsuario").readOnly = false;
        document.getElementById("contrasena").readOnly = false;

        // Limpiar contraseña
        document.getElementById("contrasena").value = "";
      } else {
        actualizarTextoContador(segundosRestantes);
      }
    }, 1000);
  }

  function actualizarTextoContador(segundosRestantes) {
      let minutos = Math.floor(segundosRestantes / 60);
      let segundos = segundosRestantes % 60;
      let texto = "";

      if(minutos > 0) {
         texto = minutos + " minutos y " + segundos + " segundos";
      } else {
         texto = segundos + " segundos";
      }

      contadorSpan.textContent = texto;
  }

  form.addEventListener("submit", function (e) {
    // 1. Recolección de datos
    const usuario = document.getElementById("nombreUsuario").value.trim();
    const contrasena = document.getElementById("contrasena").value.trim();

    // Resetear el mensaje de error del reCAPTCHA por si estaba visible
    recaptchaError.style.display = 'none';

    // 2. Validación simple de campos vacíos
    if (!usuario || !contrasena) {
      alert("Por favor, ingresa tu Usuario y Contraseña.");
      e.preventDefault(); // Detiene el envío si faltan datos
      return;
    }

    // Validación del lado del cliente de reCAPTCHA
    const recaptchaResponse = grecaptcha.getResponse();
    if (recaptchaResponse.length === 0) {
      // Mostrar el mensaje estético debajo del reCAPTCHA en lugar del alert feo
      recaptchaError.style.display = 'block';
      e.preventDefault();
      return;
    }

    // El formulario se envía al servidor (a /login)
  });
});
