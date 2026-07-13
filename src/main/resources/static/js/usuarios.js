$(document).ready(function() {
    let dataTable;
    let usuarioModal;

    const API_BASE = '/usuarios/api';
    const ENDPOINTS = {
        list: `${API_BASE}/listar`,
        save: `${API_BASE}/guardar`,
        get: (id) => `${API_BASE}/${id}`,
        delete: (id) => `${API_BASE}/eliminar/${id}`,
        toggleStatus: (id) => `${API_BASE}/cambiar-estado/${id}`,
        perfiles: `${API_BASE}/perfiles`
    };

    initializeDataTable();
    usuarioModal = new bootstrap.Modal(document.getElementById('usuarioModal'));
    setupEventListeners();

    // Escuchar el evento personalizado para recargar la tabla
    document.addEventListener('userTableUpdated', function() {
        console.log('Evento userTableUpdated recibido. Recargando tabla de usuarios...');
        if (dataTable) {
            dataTable.ajax.reload(null, false); // Recarga la tabla sin resetear la paginación
        }
    });

    function initializeDataTable() {
        dataTable = $('#tablaUsuarios').DataTable({
            responsive: true,
            processing: true,
            ajax: { url: ENDPOINTS.list, dataSrc: 'data' },
            columns: [
                { data: 'id' },
                { data: 'nombre' },
                { data: 'usuario' },
                { data: 'perfil.nombre' },
                { data: 'correo' },
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
            buttons += `<button data-id="${row.id}" class="btn btn-sm btn-info action-edit" title="Editar"><i class="bi bi-pencil-square"></i></button>`;
            if (row.estado === 1) {
                buttons += `<button data-id="${row.id}" class="btn btn-sm btn-warning action-status" title="Desactivar"><i class="bi bi-eye-slash-fill"></i></button>`;
            } else if (row.estado === 0) {
                buttons += `<button data-id="${row.id}" class="btn btn-sm btn-success action-status" title="Activar"><i class="bi bi-eye-fill"></i></button>`;
            }
            buttons += `<button data-id="${row.id}" class="btn btn-sm btn-danger action-delete" title="Eliminar"><i class="bi bi-trash3-fill"></i></button>`;
        } else {
            buttons += `<button class="btn btn-sm btn-secondary" disabled title="Usuario Eliminado"><i class="bi bi-person-x-fill"></i></button>`;
        }
        buttons += `</div>`;
        return buttons;
    }

    function setupEventListeners() {
        $('#btnNuevoRegistro').on('click', openModalForNew);
        $('#formUsuario').on('submit', saveUsuario);
        $('#tablaUsuarios tbody').on('click', '.action-edit', handleEdit);
        $('#tablaUsuarios tbody').on('click', '.action-status', handleToggleStatus);
        $('#tablaUsuarios tbody').on('click', '.action-delete', handleDelete);

        // Validación en tiempo real para la contraseña
        $('#clave').on('input', function() {
            validatePasswordRealTime($(this));
        });

        // Validación en tiempo real para el nombre (solo letras y espacios, sin números)
        $('#nombre').on('input', function() {
            const value = $(this).val();
            // Eliminar cualquier caracter que no sea letra o espacio
            const sanitized = value.replace(/[^a-zA-ZáéíóúÁÉÍÓÚñÑ\s]/g, '');
            if (value !== sanitized) {
                $(this).val(sanitized);
                showFieldError('nombre', 'El nombre solo puede contener letras y espacios.');
            } else {
                $('#nombre').removeClass('is-invalid');
                $('#nombre-feedback').text('');
            }
        });

        // Validación en tiempo real para el usuario (solo letras, números y guión bajo)
        $('#usuario').on('input', function() {
            const value = $(this).val();
            // Eliminar cualquier caracter especial (excepto guión bajo)
            const sanitized = value.replace(/[^a-zA-Z0-9_]/g, '');
            if (value !== sanitized) {
                $(this).val(sanitized);
                showFieldError('usuario', 'El usuario solo puede contener letras, números y guión bajo.');
            } else {
                $('#usuario').removeClass('is-invalid');
                $('#usuario-feedback').text('');
            }
        });
    }

    function validatePasswordRealTime(passwordInput) {
        const password = passwordInput.val();
        const feedbackElement = $('#clave-feedback');
        let messages = [];

        if (password.length === 0) {
            passwordInput.removeClass('is-invalid');
            feedbackElement.text('');
            return;
        }

        if (password.length < 8) {
            messages.push('La contraseña debe tener como mínimo 8 caracteres.');
        }
        if (!/[A-Z]/.test(password)) {
            messages.push('Debe contener al menos una letra mayúscula.');
        }
        if (!/[a-z]/.test(password)) {
            messages.push('Debe contener al menos una letra minúscula.');
        }
        if (!/\d/.test(password)) {
            messages.push('Debe contener al menos un número.');
        }
        if (!/[!@#$%^&*()_+\-=\[\]{};':"|,.<>/?]/.test(password)) {
            messages.push('Debe contener al menos un carácter especial.');
        }

        if (messages.length > 0) {
            passwordInput.addClass('is-invalid');
            feedbackElement.html(messages.join('<br>')); // Usar .html() para mostrar múltiples mensajes
        } else {
            passwordInput.removeClass('is-invalid');
            feedbackElement.text('');
        }
    }

    async function cargarPerfiles(selectedProfileId = null) {
        const select = $('#id_perfil');
        select.empty().append('<option value="">Cargando...</option>');
        try {
            const response = await fetch(ENDPOINTS.perfiles);
            const result = await response.json();
            if (result.success && result.data) {
                select.empty().append('<option value="">Seleccione un perfil</option>');
                result.data.forEach(perfil => {
                    select.append(`<option value="${perfil.id}">${perfil.nombre}</option>`);
                });
                if (selectedProfileId) {
                    select.val(selectedProfileId);
                }
            }
        } catch (error) {
            select.empty().append('<option value="">Error al cargar perfiles</option>');
        }
    }

    function openModalForNew() {
        clearForm();
        $('#modalTitle').text('Agregar Usuario');
        $('#clave').attr('placeholder', 'Mínimo 8 caracteres, mayúscula, minúscula, número, especial');
        cargarPerfiles();
        usuarioModal.show();
    }

    async function handleEdit(e) {
        const id = $(this).data('id');
        try {
            const response = await fetch(ENDPOINTS.get(id));
            const result = await response.json();
            if (result.success) {
                clearForm();
                fillForm(result.data);
                $('#modalTitle').text('Editar Usuario');
                $('#clave').attr('placeholder', 'Dejar en blanco para no cambiar');
                await cargarPerfiles(result.data.perfil.id);
                usuarioModal.show();
            } else {
                showNotification(result.message, 'error');
            }
        } catch (error) {
            showNotification('Error al cargar los datos del usuario.', 'error');
        }
    }

    async function saveUsuario(e) {
        e.preventDefault();
        if (!validateForm()) {
            showNotification('Por favor, corrija los errores del formulario.', 'error');
            return;
        }

        const isEditing = !!$('#id').val();
        const clave = $('#clave').val();

        const usuarioData = {
            id: $('#id').val() || null,
            nombre: $('#nombre').val(),
            usuario: $('#usuario').val(),
            correo: $('#correo').val(),
            perfil: { id: $('#id_perfil').val() }
        };

        // Solo incluir la clave si es un usuario nuevo o si se ha modificado en la edición
        if (!isEditing || (isEditing && clave)) {
            usuarioData.clave = clave;
        }

        try {
            const response = await fetch(ENDPOINTS.save, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(usuarioData) });
            const result = await response.json();
            if (result.success) {
                usuarioModal.hide();
                showNotification(result.message, 'success');
                // La tabla se recargará automáticamente gracias al evento WebSocket
            } else {
                if (result.errors) {
                    handleServerErrors(result.errors);
                }
                showNotification(result.message || 'Error al guardar.', 'error');
            }
        } catch (error) {
            showNotification('Error de conexión al guardar el usuario.', 'error');
        }
    }

    function validateForm() {
        let isValid = true;
        clearFieldErrors();

        // Validar nombre (solo letras y espacios)
        const nombre = $('#nombre').val();
        if (!nombre || nombre.trim() === '') {
            showFieldError('nombre', 'El nombre es obligatorio.');
            isValid = false;
        } else if (!/^[a-zA-ZáéíóúÁÉÍÓÚñÑ\s]+$/.test(nombre)) {
            showFieldError('nombre', 'El nombre solo puede contener letras y espacios. No se permiten números ni caracteres especiales.');
            isValid = false;
        }
        // Validar usuario (solo letras, números y guión bajo)
        const usuario = $('#usuario').val();
        if (!usuario || usuario.trim() === '') {
            showFieldError('usuario', 'El nombre de usuario es obligatorio.');
            isValid = false;
        } else if (!/^[a-zA-Z][a-zA-Z0-9_]*$/.test(usuario)) {
            showFieldError('usuario', 'El usuario debe comenzar con una letra y solo puede contener letras, números y guión bajo.');
            isValid = false;
        }
        // Validar correo
        if (!$('#correo').val()) {
            showFieldError('correo', 'El correo es obligatorio.');
            isValid = false;
        }
        // Validar perfil
        if (!$('#id_perfil').val()) {
            showFieldError('id_perfil', 'Debe seleccionar un perfil.');
            isValid = false;
        }

        // Validar contraseña
        const isNewUser = !$('#id').val();
        const claveInput = $('#clave');
        const clave = claveInput.val();
        let passwordErrors = [];

        if (isNewUser && !clave) {
            passwordErrors.push('La contraseña es obligatoria para nuevos usuarios.');
        } else if (clave) { // Solo validar si se proporciona una clave (para nuevo usuario o si se modificó en edición)
            if (clave.length < 8) {
                passwordErrors.push('La contraseña debe tener como mínimo 8 caracteres.');
            }
            if (!/[A-Z]/.test(clave)) {
                passwordErrors.push('Debe contener al menos una letra mayúscula.');
            }
            if (!/[a-z]/.test(clave)) {
                passwordErrors.push('Debe contener al menos una letra minúscula.');
            }
            if (!/\d/.test(clave)) {
                passwordErrors.push('Debe contener al menos un número.');
            }
            if (!/[!@#$%^&*()_+\-=\[\]{};':"|,.<>/?]/.test(clave)) {
                passwordErrors.push('Debe contener al menos un carácter especial.');
            }
        }

        if (passwordErrors.length > 0) {
            showFieldError('clave', passwordErrors.join('<br>'));
            isValid = false;
        }
        
        return isValid;
    }

    function handleDelete(e) {
        const id = $(this).data('id');
        Swal.fire({
            title: '¿Estás seguro?',
            text: "El usuario pasará a estado 'Eliminado' (no se borrará permanentemente).",
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
                    } else {
                        showNotification(result.message, 'error');
                    }
                } catch (error) {
                    showNotification('Error al eliminar el usuario.', 'error');
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
            } else {
                showNotification(result.message, 'error');
            }
        } catch (error) {
            showNotification('Error al cambiar el estado.', 'error');
        }
    }

    function fillForm(data) {
        $('#id').val(data.id);
        $('#nombre').val(data.nombre);
        $('#usuario').val(data.usuario);
        $('#correo').val(data.correo);
        // No rellenamos la clave por seguridad
    }

    function clearForm() {
        $('#formUsuario')[0].reset();
        $('#id').val('');
        clearFieldErrors();
        // También limpiar el feedback de la contraseña
        $('#clave-feedback').text('');
        $('#clave').removeClass('is-invalid');
    }

    function showFieldError(field, message) {
        const fieldElement = $(`#${field}`);
        fieldElement.addClass('is-invalid');
        const feedbackElement = $(`#${field}-feedback`);
        if (feedbackElement.length) {
            // Si el mensaje es un array, lo unimos con <br>
            if (Array.isArray(message)) {
                feedbackElement.html(message.join('<br>'));
            } else {
                feedbackElement.text(message);
            }
        } else {
            // Si no existe un div de feedback específico, lo creamos
            const displayMessage = Array.isArray(message) ? message.join('<br>') : message;
            fieldElement.after(`<div id="${field}-feedback" class="invalid-feedback">${displayMessage}</div>`);
        }
    }

    function clearFieldErrors() {
        $('.form-control').removeClass('is-invalid');
        $('.invalid-feedback').text('');
    }

    function handleServerErrors(errors) {
        Object.keys(errors).forEach(field => {
            // errors[field] ahora es una lista de strings
            showFieldError(field, errors[field]);
        });
    }
});