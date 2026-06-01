document.addEventListener('DOMContentLoaded', function() {
    // Lógica del Sidebar
    const openSidebarBtn = document.getElementById('open-sidebar');
    const closeSidebarBtn = document.getElementById('close-sidebar');
    const sidebar = document.getElementById('sidebar');
    const sidebarOverlay = document.getElementById('sidebar-overlay');

    if (openSidebarBtn) {
        openSidebarBtn.addEventListener('click', () => sidebar.classList.add('active'));
    }
    if (closeSidebarBtn) {
        closeSidebarBtn.addEventListener('click', () => sidebar.classList.remove('active'));
    }
    if (sidebarOverlay) {
        sidebarOverlay.addEventListener('click', () => sidebar.classList.remove('active'));
    }

    // Inicializar la conexión WebSocket global si las librerías están disponibles
    if (typeof SockJS !== 'undefined' && typeof Stomp !== 'undefined') {
        initializeGlobalWebSocket();
    }

    // Inicializar los dropdowns de Bootstrap explícitamente para evitar problemas de activación
    const dropdownToggles = document.querySelectorAll('.nav-link.dropdown-toggle');
    if (typeof bootstrap !== 'undefined' && dropdownToggles.length) {
        dropdownToggles.forEach(toggle => {
            new bootstrap.Dropdown(toggle);
            toggle.addEventListener('click', function(event) {
                event.preventDefault();
            });
        });
    }
});

/**
 * Inicializa una conexión WebSocket global para toda la aplicación.
 * Maneja la reconexión y la distribución de eventos.
 */
function initializeGlobalWebSocket() {
    let stompClient = null;

    function connect() {
        const socket = new SockJS('/ws');
        stompClient = Stomp.over(socket);
        stompClient.debug = null; // Desactivar logs de STOMP para una consola más limpia

        stompClient.connect({}, function (frame) {
            console.log('WebSocket Global Conectado: ' + frame);
            showNotification('Conectado para actualizaciones en tiempo real.', 'success');

            // Suscripción al topic de usuarios
            stompClient.subscribe('/topic/usuarios', function (message) {
                console.log('Mensaje recibido de /topic/usuarios:', message.body);
                const notification = JSON.parse(message.body);

                // 1. Comprobar si el usuario actual es el afectado (lógica de seguridad)
                if (typeof usuarioGlobal !== 'undefined' && usuarioGlobal.usuario === notification.username) {
                    console.log('El usuario afectado es el actual. Forzando cierre de sesión.');
                    Swal.fire({
                        title: 'Acción Remota',
                        text: 'Tu cuenta ha sido modificada. Se cerrará tu sesión por seguridad.',
                        icon: 'warning',
                        confirmButtonText: 'Aceptar',
                        allowOutsideClick: false,
                        allowEscapeKey: false
                    }).then(() => {
                        window.location.href = '/logout';
                    });
                    return; // Detener el procesamiento si el usuario actual es el afectado
                }

                // 2. Actualizar la vista si es relevante (lógica de UI)
                if ($('#tablaUsuarios').length) {
                    console.log('Tabla de usuarios encontrada. Notificando actualización...');
                    showNotification('La lista de usuarios ha sido actualizada.', 'info');
                    // Disparar un evento personalizado para que usuarios.js recargue la tabla
                    document.dispatchEvent(new Event('userTableUpdated'));
                }
            });

            // Aquí se pueden añadir más suscripciones a otros topics (ej. /topic/productos)

        }, function(error) {
            console.error('Error de conexión WebSocket Global:', error);
            tryToReconnect();
        });
    }

    function tryToReconnect() {
        console.log('Conexión WebSocket perdida. Intentando reconectar en 5 segundos...');
        showNotification('Conexión perdida. Intentando reconectar...', 'error');
        setTimeout(connect, 5000);
    }

    connect(); // Iniciar la primera conexión
}


/**
 * Muestra notificaciones toast globales.
 * @param {string} message - El mensaje a mostrar.
 * @param {string} type - El tipo de notificación: 'success', 'danger', 'warning', o 'info'.
 */
function showNotification(message, type = 'success') {
    const notificationContainer = document.getElementById('notification-container');
    if (!notificationContainer) {
        console.error('El contenedor de notificaciones #notification-container no se encontró.');
        return;
    }

    let toastClass;
    switch (type) {
        case 'success': toastClass = 'text-bg-success'; break;
        case 'danger': toastClass = 'text-bg-danger'; break;
        case 'warning': toastClass = 'text-bg-warning'; break;
        case 'info': toastClass = 'text-bg-info'; break;
        default: toastClass = 'text-bg-secondary';
    }

    const toastElement = document.createElement('div');
    toastElement.className = `toast align-items-center ${toastClass} border-0`;
    toastElement.setAttribute('role', 'alert');
    toastElement.setAttribute('aria-live', 'assertive');
    toastElement.setAttribute('aria-atomic', 'true');

    toastElement.innerHTML = `
        <div class="d-flex">
            <div class="toast-body">${message}</div>
            <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast" aria-label="Close"></button>
        </div>
    `;

    notificationContainer.appendChild(toastElement);

    const toast = new bootstrap.Toast(toastElement, { delay: 5000 });
    toast.show();

    toastElement.addEventListener('hidden.bs.toast', () => toastElement.remove());
}
