$(document).ready(function() {
    let dataTable;
    let categoriaModal;

    const API_BASE = '/categorias/api';
    const ENDPOINTS = {
        list: `${API_BASE}/listar`,
        save: `${API_BASE}/guardar`,
        get: (id) => `${API_BASE}/${id}`,
        delete: (id) => `${API_BASE}/eliminar/${id}`,
        toggleStatus: (id) => `${API_BASE}/cambiar-estado/${id}`
    };

    initializeDataTable();
    categoriaModal = new bootstrap.Modal(document.getElementById('categoriaModal'));
    setupEventListeners();

    function initializeDataTable() {
        dataTable = $('#tablaCategorias').DataTable({
            responsive: true,
            processing: true,
            ajax: { url: ENDPOINTS.list, dataSrc: 'data' },
            columns: [
                { data: 'id' },
                { data: 'nombre' },
                { data: 'descripcion' },
                {
                    data: 'estado',
                    render: function(data) {
                        if (data === 1) {
                            return '<span class="badge text-bg-success">Activo</span>';
                        } else if (data === 0) {
                            return '<span class="badge text-bg-warning">Inactivo</span>';
                        } else if (data === 2) {
                            return '<span class="badge text-bg-danger">Eliminado</span>';
                        }
                        return ''; // En caso de un estado desconocido
                    }
                },
                {
                    data: null,
                    orderable: false,
                    searchable: false,
                    render: (data, type, row) => createActionButtons(row)
                }
            ],
            language: { url: "//cdn.datatables.net/plug-ins/1.13.6/i18n/es-ES.json" }
        });
    }

    function createActionButtons(row) {
        let buttons = `
            <div class="d-flex gap-1">
        `;

        if (row.estado !== 2) { // Si la categoría no está eliminada lógicamente
            buttons += `<button data-id="${row.id}" class="btn btn-sm btn-info action-edit" title="Editar"><i class="bi bi-pencil-square"></i></button>`;

            if (row.estado === 1) { // Si está activa, mostrar botón para desactivar
                buttons += `<button data-id="${row.id}" class="btn btn-sm btn-warning action-status" title="Desactivar"><i class="bi bi-eye-slash-fill"></i></button>`;
            } else if (row.estado === 0) { // Si está inactiva, mostrar botón para activar
                buttons += `<button data-id="${row.id}" class="btn btn-sm btn-success action-status" title="Activar"><i class="bi bi-eye-fill"></i></button>`;
            }
            buttons += `<button data-id="${row.id}" class="btn btn-sm btn-danger action-delete" title="Eliminar"><i class="bi bi-trash3-fill"></i></button>`;
        } else {
            buttons += `<button class="btn btn-sm btn-secondary" disabled title="Categoría Eliminada"><i class="bi bi-folder-x"></i></button>`;
        }

        buttons += `</div>`;
        return buttons;
    }

    function setupEventListeners() {
        $('#btnNuevoRegistro').on('click', openModalForNew);
        $('#formCategoria').on('submit', saveCategoria);
        $('#tablaCategorias tbody').on('click', '.action-edit', handleEdit);
        $('#tablaCategorias tbody').on('click', '.action-status', handleToggleStatus);
        $('#tablaCategorias tbody').on('click', '.action-delete', handleDelete);
    }

    function openModalForNew() {
        clearForm();
        $('#modalTitle').text('Agregar Categoría');
        categoriaModal.show();
    }

    async function handleEdit(e) {
        const id = $(this).data('id');
        try {
            const response = await fetch(ENDPOINTS.get(id));
            const result = await response.json();
            if (result.success) {
                fillForm(result.data);
                $('#modalTitle').text('Editar Categoría');
                categoriaModal.show();
            } else {
                showNotification(result.message, 'error');
            }
        } catch (error) {
            showNotification('Error al cargar los datos de la categoría.', 'error');
        }
    }

    // Función auxiliar para extraer el mensaje de error de la respuesta HTTP
    async function extractErrorMessage(response, defaultMessage) {
        let userFriendlyMessage = defaultMessage; // Mensaje que se mostrará al usuario
        let debugDetails = `HTTP Status: ${response.status}`; // Detalles para la consola

        try {
            const errorData = await response.json();
            if (errorData && errorData.message) {
                userFriendlyMessage = errorData.message; // Usar el mensaje del backend si existe
                debugDetails += ` | Backend Message: ${errorData.message}`; // Añadir al log de depuración
            } else {
                debugDetails += ` | Full JSON: ${JSON.stringify(errorData)}`; // Logear JSON completo si no hay 'message'
            }
        } catch (jsonParseError) {
            // Si la respuesta no es JSON, o el parseo falla, obtener el texto plano
            const rawText = await response.text();
            debugDetails += ` | Raw Response (no JSON): ${rawText.substring(0, 200)}`; // Logear texto crudo para depuración
        }
        console.error(`Frontend Error Handler - ${debugDetails}`); // Siempre logear detalles completos para depuración
        return userFriendlyMessage; // Retornar solo el mensaje amigable para mostrar al usuario
    }

    async function saveCategoria(e) {
        e.preventDefault();
        const categoriaData = {
            id: $('#id').val() || null, // Asegurarse de enviar el ID
            nombre: $('#nombre').val(),
            descripcion: $('#descripcion').val(),
        };

        try {
            const response = await fetch(ENDPOINTS.save, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(categoriaData)
            });
            
            if (!response.ok) {
                const displayMessage = await extractErrorMessage(response, 'Ocurrió un error inesperado al guardar la categoría.');
                showNotification(displayMessage, 'error');
                return;
            }

            const result = await response.json();
            if (result.success) {
                categoriaModal.hide();
                showNotification(result.message, 'success');
                localStorage.setItem('categoriaActualizada', Date.now());
                dataTable.ajax.reload();
            } else {
                showNotification(result.message || 'Error desconocido al guardar la categoría.', 'error');
            }
        } catch (error) {
            console.error("Error de conexión en saveCategoria (frontend):", error);
            showNotification('Error de conexión al guardar la categoría.', 'error');
        }
    }

    function handleDelete(e) {
        const id = $(this).data('id');
        Swal.fire({
            title: '¿Estás seguro?',
            text: "La categoría pasará a estado 'Eliminado' (no se borrará permanentemente).",
            icon: 'warning',
            showCancelButton: true,
            confirmButtonColor: '#dc3545',
            cancelButtonColor: '#6c754d',
            confirmButtonText: 'Sí, eliminar',
            cancelButtonText: 'Cancelar'
        }).then(async (result) => {
            if (result.isConfirmed) {
                try {
                    const response = await fetch(ENDPOINTS.delete(id), { method: 'DELETE' });
                    
                    if (!response.ok) {
                        const displayMessage = await extractErrorMessage(response, 'Ocurrió un error inesperado al eliminar la categoría.');
                        showNotification(displayMessage, 'error');
                        return;
                    }

                    const result = await response.json();
                    if (result.success) {
                        showNotification(result.message, 'success');
                        localStorage.setItem('categoriaActualizada', Date.now());
                        dataTable.ajax.reload();
                    } else {
                        showNotification(result.message || 'Error desconocido al eliminar la categoría.', 'error');
                    }
                } catch (error) {
                    console.error("Error de conexión en handleDelete (frontend):", error);
                    showNotification('Error de conexión al eliminar la categoría.', 'error');
                }
            }
        });
    }

    async function handleToggleStatus(e) {
        const id = $(this).data('id');
        try {
            const response = await fetch(ENDPOINTS.toggleStatus(id), { method: 'POST' });
            
            if (!response.ok) {
                const displayMessage = await extractErrorMessage(response, 'Ocurrió un error inesperado al cambiar el estado de la categoría.');
                showNotification(displayMessage, 'error');
                return;
            }

            const result = await response.json();
            if (result.success) {
                showNotification(result.message, 'success');
                localStorage.setItem('categoriaActualizada', Date.now());
                dataTable.ajax.reload();
            } else {
                showNotification(result.message || 'Error desconocido al cambiar el estado de la categoría.', 'error');
            }
        } catch (error) {
            console.error("Error de conexión en handleToggleStatus (frontend):", error);
            showNotification('Error de conexión al cambiar el estado de la categoría.', 'error');
        }
    }

    function fillForm(data) {
        $('#id').val(data.id);
        $('#nombre').val(data.nombre);
        $('#descripcion').val(data.descripcion);
    }

    function clearForm() {
        $('#formCategoria')[0].reset();
        $('#id').val('');
    }

    function showNotification(message, type = 'success') {
        const toastContainer = $('#notification-container');
        if (!toastContainer.length) return;
        const toastClass = type === 'success' ? 'text-bg-success' : 'text-bg-danger';
        const toastHTML = `<div class="toast align-items-center ${toastClass} border-0" role="alert" aria-live="assertive" aria-atomic="true"><div class="d-flex"><div class="toast-body">${message}</div><button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast" aria-label="Close"></button></div></div>`;
        toastContainer.append(toastHTML);
        const toast = new bootstrap.Toast(toastContainer.children().last(), { delay: 5000 });
        toast.show();
    }
});
