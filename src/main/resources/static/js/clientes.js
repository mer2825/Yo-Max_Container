$(document).ready(function() {
    let dataTable;
    let clienteModal;
    let currentFilter = null; // Para mantener el filtro actual al recargar la tabla
    let documentSearchedAndValid = false; // Nueva bandera para la validación del documento

    const API_BASE = '/clientes/api';
    const ENDPOINTS = {
        list: `${API_BASE}/listar`,
        save: `${API_BASE}/guardar`,
        get: (id) => `${API_BASE}/${id}`,
        delete: (id) => `${API_BASE}/eliminar/${id}`,
        toggleStatus: (id) => `${API_BASE}/cambiar-estado/${id}`,
        buscarDoc: (tipo, numero) => `${API_BASE}/buscar-o-crear?tipo=${tipo}&numero=${numero}`
    };

    // Inicializar el overlay de carga una sola vez al cargar la página
    const loadingOverlay = $('<div id="loading-overlay"><div class="spinner-border text-primary" role="status"><span class="visually-hidden">Cargando...</span></div></div>');
    $('body').append(loadingOverlay);

    initializeDataTable();
    clienteModal = new bootstrap.Modal(document.getElementById('clienteModal'));
    setupEventListeners();

    function actualizarValidacionDocumento() {
        const tipoDocumento = $('#tipoDocumento').val();
        const numeroDocumentoInput = $('#numeroDocumento');

        if (tipoDocumento === 'dni') {
            numeroDocumentoInput.attr('maxlength', 8);
        } else { // Asumimos RUC u otro
            numeroDocumentoInput.attr('maxlength', 11);
        }
        numeroDocumentoInput.val(''); // Limpiar el campo al cambiar de tipo
    }

    // Función para mostrar/ocultar el campo de dirección
    function toggleDireccionField() {
        const tipoDocumento = $('#tipoDocumento').val();
        const direccionGroup = $('#direccion-group');

        if (tipoDocumento === 'ruc') {
            direccionGroup.show();
        } else {
            direccionGroup.hide();
            $('#direccion').val(''); // Limpia el campo si se oculta
        }
    }

    // Función genérica para llamadas a la API
    async function apiCall(url, method = 'GET', data = null, successMessage = null) {
        showLoading(true);
        try {
            const options = {
                method: method,
                headers: {
                    'Content-Type': 'application/json',
                },
            };
            if (data) {
                options.body = JSON.stringify(data);
            }

            const response = await fetch(url, options);
            const result = await response.json();

            if (!response.ok) {
                const errorMessage = result.message || `Error del servidor (${response.status})`;
                showNotification(errorMessage, 'error');
                return null; // Indicar que la llamada falló
            }

            if (result.success) {
                if (successMessage) {
                    showNotification(successMessage, 'success');
                }
                return result; // Devolver el resultado exitoso
            } else {
                showNotification(result.message, 'error');
                return null; // Indicar que la operación lógica falló
            }
        } catch (error) {
            console.error(`Error en la llamada a la API (${url}):`, error);
            showNotification('Error de conexión con el servidor.', 'error');
            return null; // Indicar un error de conexión
        } finally {
            showLoading(false);
        }
    }

    function initializeDataTable() {
        if (dataTable) {
            dataTable.destroy();
        }

        let ajaxUrl = ENDPOINTS.list;
        if (currentFilter) { // Usar currentFilter para mantener el estado
            ajaxUrl += `?tipoDocumento=${currentFilter}`;
        }

        dataTable = $('#tablaClientes').DataTable({
            responsive: true,
            processing: true,
            ajax: { url: ajaxUrl, dataSrc: 'data' },
            columns: [
                { data: 'id' },
                { data: 'nombre' },
                { data: 'tipoDocumento' },
                { data: 'numeroDocumento' },
                { data: 'direccion' },
                { data: 'telefono' },
                { data: 'email' },
                {
                    data: 'estado',
                    render: (data) => data === 1 ? '<span class="badge text-bg-success">Activo</span>' : '<span class="badge text-bg-danger">Inactivo</span>'
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
        const statusIcon = row.estado === 1 ? '<i class="bi bi-eye-slash-fill"></i>' : '<i class="bi bi-eye-fill"></i>';
        const statusTitle = row.estado === 1 ? 'Desactivar' : 'Activar';
        return `
            <div class="d-flex gap-1">
                <button data-id="${row.id}" class="btn btn-sm btn-info action-edit" title="Editar"><i class="bi bi-pencil-square"></i></button>
                <button data-id="${row.id}" class="btn btn-sm btn-warning action-status" title="${statusTitle}">${statusIcon}</button>
                <button data-id="${row.id}" class="btn btn-sm btn-danger action-delete" title="Eliminar"><i class="bi bi-trash3-fill"></i></button>
            </div>
        `;
    }

    function setupEventListeners() {
        $('#btnNuevoRegistro').on('click', openModalForNew);
        $('#formCliente').on('submit', saveCliente);
        $('#tablaClientes tbody').on('click', '.action-edit', handleEdit);
        $('#tablaClientes tbody').on('click', '.action-status', handleToggleStatus);
        $('#tablaClientes tbody').on('click', '.action-delete', handleDelete);
        $('#btnBuscarDoc').on('click', handleBuscarDoc);

        $('#btnFiltroTodos').on('click', () => filterClients(null, $('#btnFiltroTodos')));
        $('#btnFiltroDNI').on('click', () => filterClients('DNI', $('#btnFiltroDNI')));
        $('#btnFiltroRUC').on('click', () => filterClients('RUC', $('#btnFiltroRUC')));

        $('#numeroDocumento').on('input', function() {
            documentSearchedAndValid = false;
            // Aplicar siempre la validación de solo números
            this.value = this.value.replace(/[^0-9]/g, '');
        });

        $('#tipoDocumento').on('change', function() {
            documentSearchedAndValid = false;
            toggleDireccionField();
            actualizarValidacionDocumento();
        });
    }

    function filterClients(filterType, clickedButton) {
        currentFilter = filterType;
        $('.btn-group button').removeClass('active');
        clickedButton.addClass('active');
        initializeDataTable();
    }

    function openModalForNew() {
        clearForm();
        $('#modalTitle').text('Agregar Cliente');
        documentSearchedAndValid = false;
        toggleDireccionField();
        actualizarValidacionDocumento();
        clienteModal.show();
    }

    async function handleEdit(e) {
        const id = $(this).data('id');
        const result = await apiCall(ENDPOINTS.get(id));
        if (result) {
            fillForm(result.data);
            $('#modalTitle').text('Editar Cliente');
            documentSearchedAndValid = true;
            toggleDireccionField();
            actualizarValidacionDocumento();
            clienteModal.show();
        }
    }

    async function saveCliente(e) {
        e.preventDefault();
        const id = $('#id').val();
        const numeroDocumento = $('#numeroDocumento').val();
        const tipoDocumento = $('#tipoDocumento').val();

        if (!id && numeroDocumento && !documentSearchedAndValid) {
            showNotification('Por favor, busque y valide el documento antes de guardar.', 'error');
            return;
        }

        const clienteData = {
            id: id || null,
            tipoDocumento: tipoDocumento.toUpperCase(),
            numeroDocumento: numeroDocumento,
            nombre: $('#nombre').val(),
            direccion: $('#direccion').val(),
            telefono: $('#telefono').val(),
            email: $('#email').val(),
        };

        const result = await apiCall(ENDPOINTS.save, 'POST', clienteData, 'Cliente guardado exitosamente.');
        if (result) {
            clienteModal.hide();
            dataTable.ajax.reload();
        }
    }

    async function handleToggleStatus(e) {
        const id = $(this).data('id');
        const result = await apiCall(ENDPOINTS.toggleStatus(id), 'POST', null, 'Estado del cliente actualizado.');
        if (result) {
            dataTable.ajax.reload();
        }
    }

    function handleDelete(e) {
        const id = $(this).data('id');
        Swal.fire({
            title: '¿Estás seguro?',
            text: "El cliente será marcado como eliminado lógicamente.",
            icon: 'warning',
            showCancelButton: true,
            confirmButtonColor: '#dc3545',
            cancelButtonColor: '#6c757d',
            confirmButtonText: 'Sí, eliminar',
            cancelButtonText: 'Cancelar'
        }).then(async (result) => {
            if (result.isConfirmed) {
                const apiResult = await apiCall(ENDPOINTS.delete(id), 'DELETE', null, 'Cliente eliminado lógicamente.');
                if (apiResult) {
                    dataTable.ajax.reload();
                }
            }
        });
    }

    async function handleBuscarDoc() {
        const tipo = $('#tipoDocumento').val();
        const numero = $('#numeroDocumento').val();
        if (!numero) {
            showNotification('Por favor, ingrese un número de documento.', 'error');
            return;
        }

        const result = await apiCall(ENDPOINTS.buscarDoc(tipo, numero));

        if (result) {
            if (result.isNewClient === false) {
                showNotification(`El cliente con documento ${numero} ya está registrado.`, 'error');
                documentSearchedAndValid = false;
            } else if (result.cliente) {
                fillForm(result.cliente);
                showNotification('Nuevo cliente encontrado. Complete y guarde los datos.', 'success');
                documentSearchedAndValid = true;
            } else {
                showNotification(result.message || 'No se encontraron datos para ese documento.', 'error');
                documentSearchedAndValid = false;
            }
        } else {
            documentSearchedAndValid = false;
        }
        toggleDireccionField();
    }

    function fillForm(data) {
        $('#id').val(data.id || '');
        $('#tipoDocumento').val(data.tipoDocumento ? data.tipoDocumento.toLowerCase() : 'dni');
        $('#numeroDocumento').val(data.numeroDocumento);
        $('#nombre').val(data.nombre);
        $('#direccion').val(data.direccion || '');
        $('#telefono').val(data.telefono || '');
        $('#email').val(data.email || '');
    }

    function clearForm() {
        $('#formCliente')[0].reset();
        $('#id').val('');
        $('#tipoDocumento').val('dni');
        documentSearchedAndValid = false;
        toggleDireccionField();
        actualizarValidacionDocumento();
    }

    function showNotification(message, type = 'success') {
        const toastContainer = $('#notification-container');
        if (!toastContainer.length) return;

        let toastClass = 'text-bg-success';
        if (type === 'error') {
            toastClass = 'text-bg-danger';
        } else if (type === 'info') {
            toastClass = 'text-bg-info';
        }

        const toastHTML = `<div class="toast align-items-center ${toastClass} border-0" role="alert" aria-live="assertive" aria-atomic="true"><div class="d-flex"><div class="toast-body">${message}</div><button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast" aria-label="Close"></button></div></div>`;
        toastContainer.append(toastHTML);
        const toast = new bootstrap.Toast(toastContainer.children().last(), { delay: 5000 });
        toast.show();
    }

    function showLoading(show) {
        if (show) {
            loadingOverlay.css('display', 'flex');
            setTimeout(() => {
                loadingOverlay.addClass('loading-overlay-visible');
            }, 10);
        } else {
            loadingOverlay.removeClass('loading-overlay-visible');
            loadingOverlay.one('transitionend', function() {
                if (!loadingOverlay.hasClass('loading-overlay-visible')) {
                    loadingOverlay.css('display', 'none');
                }
            });
        }
    }
});