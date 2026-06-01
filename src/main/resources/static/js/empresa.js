
$(document).ready(function() {

    // Función para formatear los resultados del Select2 (con imagen)
    function formatProductResult(product) {
        if (!product.id) {
            return product.text; // Texto por defecto para el placeholder
        }

        // Obtener la url de las imágenes correspondientes
        const imageUrl = $(product.element).data('image-url');
        const defaultImage = '/images/placeholder.png'; // Imagen por defecto si no hay foto

        if (imageUrl) {
            return $(
                `<span class="d-flex align-items-center"><img src="${imageUrl}" class="me-2" style="width: 30px; height: 30px; object-fit: cover; border-radius: 3px;" /><span>${product.text}</span></span>`
            );
        } else {
            return $(
                `<span class="d-flex align-items-center"><img src="${defaultImage}" class="me-2" style="width: 30px; height: 30px; object-fit: cover; border-radius: 3px;" /><span>${product.text}</span></span>`
            );
        }
    }

    // Función para formatear las selecciones del Select2 (con imagen)
    function formatProductSelection(product) {
        if (!product.id) {
            return product.text; // Texto por defecto para el placeholder
        }

        const imageUrl = $(product.element).data('image-url');
        const defaultImage = '/images/placeholder.png';

        if (imageUrl) {
            return $(
                `<span class="d-flex align-items-center"><img src="${imageUrl}" class="me-2" style="width: 24px; height: 24px; object-fit: cover; border-radius: 3px;" /><span>${product.text}</span></span>`
            );
        } else {
            return $(
                `<span class="d-flex align-items-center"><img src="${defaultImage}" class="me-2" style="width: 24px; height: 24px; object-fit: cover; border-radius: 3px;" /><span>${product.text}</span></span>`
            );
        }
    }

    // Inicializar el selector múltiple de Select2
    $('#productosDestacados').select2({
        theme: "bootstrap-5",
        placeholder: "Selecciona los productos a destacar",
        closeOnSelect: false, // Mantener abierto para selección múltiple
        templateResult: formatProductResult, // Usar la función de formato para los resultados
        templateSelection: formatProductSelection, // Usar la función de formato para las selecciones
        escapeMarkup: function (markup) { return markup; } // Permitir HTML en los resultados
    });

    // Intercepta el envío del formulario para manejarlo con AJAX
    $('#formEmpresa').on('submit', function(e) {
        e.preventDefault(); // Previene la recarga de la página
        saveEmpresa();
    });

    // Función para guardar los datos de la empresa vía AJAX
    async function saveEmpresa() {
        let logoUrl = $('#logoUrl').val(); // Obtener la URL original

        // 1. Si hay un nuevo archivo de logo, súbelo primero
        const logoFile = $('#logoFile')[0].files[0];
        if (logoFile) {
            const formData = new FormData();
            formData.append('logoFile', logoFile);

            try {
                const response = await fetch('/empresa/api/subir-logo', {
                    method: 'POST',
                    body: formData
                });
                const result = await response.json();

                if (result.success) {
                    logoUrl = result.imageUrl; // Actualizar logoUrl con la nueva
                } else {
                    showNotification('Error al subir el nuevo logo: ' + result.message, 'error');
                    return; // Detener el proceso de guardado si la subida falla
                }
            } catch (error) {
                showNotification('Error de conexión al subir el logo.', 'error');
                return; // Detener
            }
        }

        // 2. Recopilar el resto de los datos del formulario
        const nombreEmpresa = $('#nombre').val();
        const productosIds = $('#productosDestacados').val();
        const productosDestacados = productosIds ? productosIds.map(id => ({ id: parseInt(id) })) : [];

        const empresaData = {
            id: $('#id').val(),
            nombre: nombreEmpresa,
            direccion: $('#direccion').val(),
            telefono: $('#telefono').val(),
            email: $('#email').val(),
            nosotros: $('#nosotros').val(),
            logoUrl: logoUrl, // Usar la URL del logo (antigua o nueva)
            productosDestacados: productosDestacados
        };

        // 3. Guardar toda la información de la empresa
        fetch('/empresa/api/guardar', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(empresaData)
        })
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                showNotification(data.message, 'success');

                // 4. Actualizar la UI (logos y valor oculto) DESPUÉS de guardar
                if (logoUrl) {
                    $('#logoUrl').val(logoUrl);
                    $('#currentLogoPreview').attr('src', logoUrl);
                    $('#sidebar-empresa-logo').attr('src', logoUrl);
                }
                // Actualizar el nombre de la empresa en la barra lateral
                $('#sidebar-empresa-nombre').text(nombreEmpresa);

                // 5. Enviar señal a otras pestañas
                localStorage.setItem('empresaActualizada', Date.now());

                // Limpiar el input de archivo para evitar re-subidas accidentales
                $('#logoFile').val('');
            } else {
                showNotification(data.message || 'Error al guardar.', 'error');
            }
        })
        .catch(error => {
            console.error('Error:', error);
            showNotification('Error de conexión al guardar los cambios.', 'error');
        });
    }

    // Lógica para la PREVISUALIZACIÓN del logo
    $('#logoFile').on('change', function(event) {
        const file = event.target.files[0];
        if (file) {
            // Usar URL.createObjectURL para mostrar una vista previa local sin subir el archivo
            const previewUrl = URL.createObjectURL(file);
            $('#currentLogoPreview').attr('src', previewUrl);
            showNotification('Logo listo para subir. Haz clic en "Guardar Cambios" para aplicar todo.', 'info');
        }
    });

    // Función para mostrar notificaciones (Toast)
    function showNotification(message, type = 'success') {
        const toastContainer = $('#notification-container');
        if (!toastContainer.length) return;

        const toastClass = (type === 'success') ? 'text-bg-success' : (type === 'error' ? 'text-bg-danger' : 'text-bg-info');

        const toastHTML = `
            <div class="toast align-items-center ${toastClass} border-0" role="alert" aria-live="assertive" aria-atomic="true">
                <div class="d-flex">
                    <div class="toast-body">
                        ${message}
                    </div>
                    <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast" aria-label="Close"></button>
                </div>
            </div>`;

        const toastElement = $(toastHTML);
        toastContainer.append(toastElement);

        const toast = new bootstrap.Toast(toastElement, { delay: 5000 });
        toast.show();
    }
});
