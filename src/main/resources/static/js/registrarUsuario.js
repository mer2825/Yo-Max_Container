document.addEventListener("DOMContentLoaded", function () {
    const form = document.getElementById("formRegistrarUsuario");
    const contrasena = document.getElementById("contrasena");
    const confirmacion = document.getElementById("confirmacion");
    const modal = document.getElementById("modalExito");
    const cerrarModal = document.getElementById("cerrarModal");

    const mensajeError = document.createElement("p");
    mensajeError.style.color = "red";
    mensajeError.style.fontSize = "0.9rem";
    mensajeError.style.marginTop = "5px";
    confirmacion.parentNode.insertBefore(mensajeError, confirmacion.nextSibling);

    function esContrasenaSegura(pwd) {
        const regex = /^(?=.*[A-Z])(?=.*[!@#$%^&*(),.?":{}|<>])[A-Za-z\d!@#$%^&*(),.?":{}|<>]{8,20}$/;
        return regex.test(pwd);
    }

    function esCorreoValido(email) {
        const regexCorreo = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        return regexCorreo.test(email);
    }

    function esTelefonoValido(telefono) {
        const regexTelefono = /^[0-9]{9}$/;
        return regexTelefono.test(telefono);
    }

    const ayudaContrasena = document.getElementById("ayudaContrasena");

    contrasena.addEventListener("input", function () {
      if (!esContrasenaSegura(contrasena.value)) {
        ayudaContrasena.style.color = "red";
      } else {
        ayudaContrasena.style.color = "blue";
      }
    });

    confirmacion.addEventListener("input", function () {
        if (contrasena.value === confirmacion.value) {
            confirmacion.style.borderColor = "green";
            mensajeError.textContent = "";
        } else {
            confirmacion.style.borderColor = "red";
            mensajeError.textContent = "Las contraseñas no coinciden.";
        }
    });

    form.addEventListener("submit", function (e) {
        e.preventDefault();

        const usuario = {
        nombre: document.getElementById("nombreUsuario").value.trim(),
        correo: document.getElementById("correoElectronico").value.trim(),
        telefono: document.getElementById("numeroTelefonico").value.trim(),
        contrasena: contrasena.value.trim()
    };

    if (!esCorreoValido(usuario.correo)) {
      alert("Ingrese un correo electrónico válido.");
      return;
    }

    if (!esTelefonoValido(usuario.telefono)) {
      alert("Ingrese un número de teléfono válido de 9 dígitos.");
      return;
    }

    if (!esContrasenaSegura(contrasena.value)) {
      alert("La contraseña no es segura. Debe tener entre 8 y 20 caracteres, al menos una mayúscula y un carácter especial.");
      return;
    }

    if (contrasena.value !== confirmacion.value) {
      alert("Las contraseñas no coinciden.");
      return;
    }

    localStorage.setItem("usuarioRegistrado", JSON.stringify(usuario));
    modal.style.display = "flex";
    form.reset();

    });

    cerrarModal.addEventListener("click", function () {
        window.location.href = "iniciarSesion.html";
    });
});
