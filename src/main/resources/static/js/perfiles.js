$(document).ready(function() {
    let dataTable;
    let perfilModal;
    let permisosModal;

    const API_BASE = '/perfiles/api';
    const ENDPOINTS = {
        list: `${API_BASE}/listar`,
        save: `${API_BASE}/guardar`,
        get: (id) => `${API_BASE}/${id}`,
        delete: (id) => `${API_BASE}/eliminar/${id}`,
        toggleStatus: (id) => `${API_BASE}/cambiar-estado/${id}`,
        opciones: `${API_BASE}/opciones`,
        auditDetails: (id) => `${API_BASE}/audit-details/${id}`
    };
    const ADMIN_PROFILE_NAME = "Administrador";

    initializeDataTable();
    perfilModal = new bootstrap.Modal(document.getElementById('perfilModal'));
    permisosModal = new bootstrap.Modal(document.getElementById('permisosModal'));
    setupEventListeners();

    function initializeDataTable() {
        dataTable = $('#tablaPerfiles').DataTable({
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
                        if (data === 1) return '<span class="badge text-bg-success">Activo</span>';
                        if (data === 0) return '<span class="badge text-bg-warning">Inactivo</span>';
                        if (data === 2) return '<span class="badge text-bg-danger">Eliminado</span>';
                        return '';
                    }
                },
                {
                    data: null,
                    orderable: false,
                    searchable: false,
                    render: (data, type, row) => createActionButtons(row)
                }
            ],
            language: { url: "https://cdn.datatables.net/plug-ins/1.13.6/i18n/es-ES.json" }
        });
    }

    function createActionButtons(row) {
        let buttons = `<div class="d-flex gap-1">`;
        buttons += `<button data-id="${row.id}" class="btn btn-sm btn-secondary btn-detalles" title="Detalles de Auditoría" data-bs-toggle="modal" data-bs-target="#auditModal"><i class="bi bi-info-circle-fill"></i></button>`;

        if (row.estado !== 2) {
            const isNotAdmin = row.nombre.toLowerCase() !== ADMIN_PROFILE_NAME.toLowerCase();
            
            buttons += `<button data-id="${row.id}" class="btn btn-sm btn-info action-edit" title="Editar"><i class="bi bi-pencil-square"></i></button>`;
            buttons += `<button data-id="${row.id}" class="btn btn-sm btn-secondary action-permisos" title="Asignar Permisos"><i class="bi bi-shield-lock-fill"></i></button>`;

            if (isNotAdmin) {
                if (row.estado === 1) {
                    buttons += `<button data-id="${row.id}" class="btn btn-sm btn-warning action-status" title="Desactivar"><i class="bi bi-eye-slash-fill"></i></button>`;
                } else if (row.estado === 0) {
                    buttons += `<button data-id="${row.id}" class="btn btn-sm btn-success action-status" title="Activar"><i class="bi bi-eye-fill"></i></button>`;
                }
                buttons += `<button data-id="${row.id}" class="btn btn-sm btn-danger action-delete" title="Eliminar"><i class="bi bi-trash3-fill"></i></button>`;
            }
        } else {
            buttons += `<button class="btn btn-sm btn-secondary" disabled title="Perfil Eliminado"><i class="bi bi-person-x-fill"></i></button>`;
        }

        buttons += `</div>`;
        return buttons;
    }

    function setupEventListeners() {
        $('#btnNuevoRegistro').on('click', openModalForNew);
        $('#formPerfil').on('submit', savePerfil);
        $('#tablaPerfiles tbody').on('click', '.action-edit', handleEdit);
        $('#tablaPerfiles tbody').on('click', '.action-status', handleToggleStatus);
        $('#tablaPerfiles tbody').on('click', '.action-delete', handleDelete);
        $('#tablaPerfiles tbody').on('click', '.action-permisos', handlePermisos);
        $('#btnGuardarPermisos').on('click', savePermisos);
        $('#tablaPerfiles tbody').on('click', '.btn-detalles', showAuditDetails);
    }

    function showAuditDetails() {
        var perfilId = $(this).data('id');
        $('#auditModal #creadoPor, #auditModal #fechaCreacion, #auditModal #modificadoPor, #auditModal #fechaModificacion, #auditModal #ultimaAccion').text('Cargando...');
        $.ajax({
            url: ENDPOINTS.auditDetails(perfilId),
            type: 'GET',
            success: function(data) {
                $('#auditModal #creadoPor').text(data.creadoPorNombre);
                $('#auditModal #fechaCreacion').text(data.fechaCreacion);
                $('#auditModal #modificadoPor').text(data.modificadoPorNombre);
                $('#auditModal #fechaModificacion').text(data.fechaModificacion);
                $('#auditModal #ultimaAccion').text(data.ultimaAccion);
            },
            error: function(error) {
                $('#auditModal #creadoPor, #auditModal #modificadoPor, #auditModal #ultimaAccion').text('Error al cargar');
                $('#auditModal #fechaCreacion, #auditModal #fechaModificacion').text('-');
                console.error("Error al obtener detalles de auditoría:", error);
            }
        });
    }

    function openModalForNew() {
        clearForm(false);
        $('#modalTitle').text('Agregar Perfil');
        perfilModal.show();
    }

    async function handleEdit(e) {
        const id = $(this).data('id');
        try {
            const response = await fetch(ENDPOINTS.get(id));
            const result = await response.json();
            if (result.success) {
                const isReadOnly = result.data.nombre.toLowerCase() === ADMIN_PROFILE_NAME.toLowerCase();
                clearForm(isReadOnly);
                fillForm(result.data);
                $('#modalTitle').text(isReadOnly ? 'Detalles del Perfil Administrador' : 'Editar Perfil');
                perfilModal.show();
            } else {
                showNotification(result.message, 'error');
            }
        } catch (error) {
            showNotification('Error al cargar los datos del perfil.', 'error');
        }
    }

    async function savePerfil(e) {
        e.preventDefault();
        const perfilData = { id: $('#id').val() || null, nombre: $('#nombre').val(), descripcion: $('#descripcion').val() };
        try {
            const response = await fetch(ENDPOINTS.save, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(perfilData) });
            const result = await response.json();
            if (result.success) {
                perfilModal.hide();
                showNotification(result.message, 'success');
                dataTable.ajax.reload();
            } else {
                showNotification(result.message, 'error');
            }
        } catch (error) {
            showNotification('Error al guardar el perfil.', 'error');
        }
    }

    function handleDelete(e) {
        const id = $(this).data('id');
        Swal.fire({
            title: '¿Estás seguro?',
            text: "El perfil pasará a estado 'Eliminado'.",
            icon: 'warning',
            showCancelButton: true,
            confirmButtonColor: '#dc3545',
            cancelButtonColor: '#6c757d',
            confirmButtonText: 'Sí, eliminar',
            cancelButtonText: 'Cancelar'
        }).then(async (result) => {
            if (result.isConfirmed) {
                try {
                    const response = await fetch(ENDPOINTS.delete(id), { method: 'DELETE' });
                    const result = await response.json();
                    if (result.success) {
                        showNotification(result.message, 'success');
                        dataTable.ajax.reload();
                    } else {
                        showNotification(result.message, 'error');
                    }
                } catch (error) {
                    showNotification('Error al eliminar el perfil.', 'error');
                }
            }
        });
    }

    async function handleToggleStatus(e) {
        const id = $(this).data('id');
        try {
            const response = await fetch(ENDPOINTS.toggleStatus(id), { method: 'POST' });
            const result = await response.json();
            if (result.success) {
                showNotification(result.message, 'success');
                dataTable.ajax.reload();
            } else {
                showNotification(result.message, 'error');
            }
        } catch (error) {
            showNotification('Error al cambiar el estado.', 'error');
        }
    }

    async function handlePermisos(e) {
        const perfilId = $(this).data('id');
        $('#permisoPerfilId').val(perfilId);
        try {
            const perfilResponse = await fetch(ENDPOINTS.get(perfilId));
            const perfilResult = await perfilResponse.json();
            if (perfilResult.success) {
                const perfil = perfilResult.data;
                const isReadOnly = perfil.nombre.toLowerCase() === ADMIN_PROFILE_NAME.toLowerCase();
                $('#permisoPerfilNombre').text(perfil.nombre);
                $('#btnGuardarPermisos').toggle(!isReadOnly);

                const opcionesResponse = await fetch(ENDPOINTS.opciones);
                const opcionesResult = await opcionesResponse.json();
                if (opcionesResult.success) {
                    const listaOpcionesDiv = $('#listaOpciones');
                    listaOpcionesDiv.empty();
                    opcionesResult.data.forEach(opcion => {
                        const isChecked = perfil.opciones.includes(opcion.id);
                        listaOpcionesDiv.append(`
                            <div class="list-group-item">
                                <input class="form-check-input me-1" type="checkbox" value="${opcion.id}" id="opcion${opcion.id}" ${isChecked ? 'checked' : ''} ${isReadOnly ? 'disabled' : ''}>
                                <label class="form-check-label" for="opcion${opcion.id}">${opcion.nombre}</label>
                            </div>
                        `);
                    });
                    permisosModal.show();
                } else {
                    showNotification(opcionesResult.message, 'error');
                }
            } else {
                showNotification(perfilResult.message, 'error');
            }
        } catch (error) {
            showNotification('Error al cargar permisos.', 'error');
        }
    }

    async function savePermisos() {
        const perfilId = $('#permisoPerfilId').val();
        const opcionesSeleccionadas = Array.from($('#listaOpciones input:checked')).map(el => ({ id: $(el).val() }));
        try {
            const responseGet = await fetch(ENDPOINTS.get(perfilId));
            const resultGet = await responseGet.json();
            if (!resultGet.success) {
                showNotification(resultGet.message, 'error');
                return;
            }
            const perfilExistente = resultGet.data;
            const perfilActualizado = { ...perfilExistente, opciones: opcionesSeleccionadas };
            const response = await fetch(ENDPOINTS.save, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(perfilActualizado) });
            const result = await response.json();
            if (result.success) {
                permisosModal.hide();
                showNotification('Permisos actualizados correctamente', 'success');
                dataTable.ajax.reload();
            } else {
                showNotification(result.message, 'error');
            }
        } catch (error) {
            showNotification('Error al guardar permisos.', 'error');
        }
    }

    function fillForm(data) {
        $('#id').val(data.id);
        $('#nombre').val(data.nombre);
        $('#descripcion').val(data.descripcion);
    }

    function clearForm(isReadOnly) {
        $('#formPerfil')[0].reset();
        $('#id').val('');
        $('#nombre, #descripcion').prop('disabled', isReadOnly);
        $('#btnGuardarPerfil').toggle(!isReadOnly);
    }

    function showNotification(message, type = 'success') {
        const toastContainer = $('#notification-container');
        const toastClass = type === 'success' ? 'text-bg-success' : 'text-bg-danger';
        const toastHTML = `<div class="toast align-items-center ${toastClass} border-0" role="alert" aria-live="assertive" aria-atomic="true"><div class="d-flex"><div class="toast-body">${message}</div><button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast" aria-label="Close"></button></div></div>`;
        toastContainer.append(toastHTML);
        const toast = new bootstrap.Toast(toastContainer.children().last(), { delay: 3000 });
        toast.show();
    }
});
