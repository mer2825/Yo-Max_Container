/**
 * Script para la gestión de productos con Bootstrap 5 y galería de imágenes.
 * Archivo: src/main/resources/static/js/productos.js
 */
$(document).ready(function() {
    let dataTable;
    let productoModal;
    let imagenesParaEliminar = [];
    let newFiles = []; // Almacena los nuevos archivos de imagen para la subida
    let sortableGaleria;

    const API_BASE = '/productos/api';
    const ENDPOINTS = {
        list: `${API_BASE}/listar`,
        save: `${API_BASE}/guardar`,
        get: (id) => `${API_BASE}/${id}`,
        delete: (id) => `${API_BASE}/eliminar/${id}`,
        categories: `${API_BASE}/categorias`,
        toggleStatus: (id) => `${API_BASE}/cambiar-estado/${id}`,
        uploadImages: `${API_BASE}/subir-imagenes`,
        deleteImageBatch: `${API_BASE}/imagenes/eliminar-batch`,
        updateImageOrder: `${API_BASE}/imagenes/actualizar-orden`
    };

    initializeDataTable();
    productoModal = new bootstrap.Modal(document.getElementById('productoModal'));
    loadCategories();
    setupEventListeners();

    function initializeDataTable() {
        if (dataTable) dataTable.destroy();
        dataTable = $('#tablaProductos').DataTable({
            responsive: true,
            processing: true,
            ajax: { url: ENDPOINTS.list, dataSrc: 'data' },
            columns: [
                { data: 'id' },
                {
                    data: 'imagenes',
                    render: function(data) {
                        if (data && data.length > 0) {
                            return `<img src="${data[0].url}" alt="Producto" class="img-thumbnail" style="width: 50px; height: 50px; object-fit: cover;">`;
                        }
                        return '<div class="img-thumbnail" style="width: 50px; height: 50px; background-color: #f0f0f0;"></div>';
                    }
                },
                { data: 'nombre' },
                { data: 'categoria.nombre' },
                { data: 'precio', render: $.fn.dataTable.render.number('.', ',', 2, 'S/ ') },
                { data: 'stock' },
                { data: 'stockMinimo' },
                { data: 'descripcion', render: (data) => data && data.length > 50 ? data.substr(0, 50) + '...' : data },
                {
                    data: 'estado',
                    render: (data) => {
                        if (data === 1) return '<span class="badge text-bg-success">Activo</span>';
                        if (data === 0) return '<span class="badge text-bg-warning">Inactivo</span>';
                        return '<span class="badge text-bg-danger">Eliminado</span>';
                    }
                },
                { data: null, orderable: false, searchable: false, render: (data, type, row) => createActionButtons(row) }
            ],
            language: { url: "https://cdn.datatables.net/plug-ins/1.13.6/i18n/es-ES.json" }
        });
    }

    function createActionButtons(row) {
        const isEliminado = row.estado === 2;
        const statusIcon = row.estado === 1 ? '<i class="bi bi-eye-slash-fill"></i>' : '<i class="bi bi-eye-fill"></i>';
        const statusTitle = row.estado === 1 ? 'Desactivar' : 'Activar';

        return `
            <div class="d-flex gap-1">
                <button data-id="${row.id}" class="btn btn-sm btn-info action-edit" title="Editar" ${isEliminado ? 'disabled' : ''}><i class="bi bi-pencil-square"></i></button>
                <button data-id="${row.id}" class="btn btn-sm btn-warning action-status" title="${statusTitle}" ${isEliminado ? 'disabled' : ''}>${statusIcon}</button>
                <button data-id="${row.id}" class="btn btn-sm btn-danger action-delete" title="Eliminar" ${isEliminado ? 'disabled' : ''}><i class="bi bi-trash3-fill"></i></button>
                <button data-id="${row.id}" class="btn btn-sm btn-secondary btn-detalles" title="Detalles de Auditoría" data-bs-toggle="modal" data-bs-target="#auditModal"><i class="bi bi-info-circle-fill"></i></button>
            </div>
        `;
    }

    function setupEventListeners() {
        $('#btnNuevoRegistro').on('click', openModalForNew);
        $('#formProducto').on('submit', saveProducto);
        $('#tablaProductos tbody').on('click', '.action-edit', handleEdit);
        $('#tablaProductos tbody').on('click', '.action-status', handleToggleStatus);
        $('#tablaProductos tbody').on('click', '.action-delete', handleDelete);
        $('#galeria-imagenes').on('click', '.btn-delete-img', handleDeleteImagen);
        $('#imagenFile').on('change', handleFileSelectionAndPreview);
        $('#galeria-preview').on('click', '.btn-remove-preview', handleRemovePreview);
    }

    function loadCategories() {
        fetch(ENDPOINTS.categories)
            .then(response => response.json())
            .then(data => {
                if (data.success) {
                    const select = $('#id_categoria');
                    select.empty().append('<option value="">Seleccione una categoría...</option>');
                    data.data.forEach(category => select.append(`<option value="${category.id}">${category.nombre}</option>`));
                }
            });
    }

    async function saveProducto(e) {
        if (e) e.preventDefault();
        showLoading(true);

        try {
            if (imagenesParaEliminar.length > 0) {
                const deleteResponse = await fetch(ENDPOINTS.deleteImageBatch, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(imagenesParaEliminar)
                });
                if (!deleteResponse.ok) throw new Error('Error al eliminar imágenes marcadas.');
            }

            const productoData = {
                id: $('#id').val() || null,
                nombre: $('#nombre').val(),
                descripcion: $('#descripcion').val(),
                precio: $('#precio').val(),
                stock: $('#stock').val(),
                stockMinimo: $('#stockMinimo').val(),
                categoria: { id: $('#id_categoria').val() }
            };

            const saveResponse = await fetch(ENDPOINTS.save, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(productoData)
            });
            const saveResult = await saveResponse.json();

            if (!saveResult.success) {
                throw new Error(saveResult.message || 'Error al guardar el producto.');
            }

            const productoId = saveResult.producto.id;
            const filesToUpload = newFiles.filter(f => f !== null);

            if (filesToUpload.length > 0) {
                const formData = new FormData();
                formData.append('id', productoId);
                filesToUpload.forEach(file => formData.append('files', file));

                const uploadResponse = await fetch(ENDPOINTS.uploadImages, { method: 'POST', body: formData });
                const uploadResult = await uploadResponse.json();
                if (!uploadResult.success) {
                    showNotification('Producto guardado, pero hubo un error al subir las nuevas imágenes.', 'warning');
                }
            }
            
            hideModal();
            showNotification(saveResult.message, 'success');
            localStorage.setItem('productoActualizado', Date.now()); // Señal para el catálogo
            dataTable.ajax.reload();

        } catch (error) {
            showNotification(error.message, 'danger');
        } finally {
            showLoading(false);
        }
    }

    function handleFileSelectionAndPreview(e) {
        const files = e.target.files;
        if (!files) return;

        for (const file of files) {
            if (!newFiles.some(f => f && f.name === file.name && f.size === file.size)) {
                const fileIndex = newFiles.length;
                newFiles.push(file);
                
                const reader = new FileReader();
                reader.onload = function(event) {
                    const thumb = `
                        <div id="preview-thumb-${fileIndex}" class="position-relative">
                            <img src="${event.target.result}" class="img-thumbnail" style="width: 100px; height: 100px; object-fit: cover;">
                            <button class="btn btn-danger btn-sm position-absolute top-0 end-0 btn-remove-preview" data-index="${fileIndex}" title="Quitar imagen">
                                <i class="bi bi-x-lg"></i>
                            </button>
                        </div>
                    `;
                    $('#galeria-preview').append(thumb);
                }
                reader.readAsDataURL(file);
            }
        }
        $('#galeria-preview-container').toggle(newFiles.filter(f => f).length > 0);
        $(this).val(''); // Reset input para permitir re-seleccionar
    }

    function handleRemovePreview(e) {
        const indexToRemove = $(this).data('index');
        if (indexToRemove !== undefined && newFiles[indexToRemove]) {
            newFiles[indexToRemove] = null; // Marcar como nulo en lugar de eliminar para no afectar índices
            $(`#preview-thumb-${indexToRemove}`).remove();
        }
        $('#galeria-preview-container').toggle(newFiles.filter(f => f).length > 0);
    }

    function handleEdit(e) {
        const id = $(this).data('id');
        showLoading(true);
        fetch(ENDPOINTS.get(id))
            .then(response => response.json())
            .then(result => {
                if (result.success) {
                    openModalForEdit(result.data);
                } else {
                    showNotification('Error al cargar producto.', 'danger');
                }
            })
            .finally(() => showLoading(false));
    }

    function handleDelete(e) {
        const id = $(this).data('id');
        Swal.fire({
            title: '¿Estás seguro?',
            text: "El producto será marcado como eliminado.",
            icon: 'warning',
            showCancelButton: true,
            confirmButtonColor: '#dc3545',
            confirmButtonText: 'Sí, eliminar'
        }).then(result => {
            if (result.isConfirmed) {
                showLoading(true);
                fetch(ENDPOINTS.delete(id), { method: 'DELETE' })
                    .then(response => response.json())
                    .then(data => {
                        if (data.success) {
                            showNotification(data.message, 'success');
                            localStorage.setItem('productoActualizado', Date.now());
                            dataTable.ajax.reload();
                        } else {
                            showNotification(data.message, 'danger');
                        }
                    })
                    .finally(() => showLoading(false));
            }
        });
    }

    function handleToggleStatus(e) {
        const id = $(this).data('id');
        showLoading(true);
        fetch(ENDPOINTS.toggleStatus(id), { method: 'POST' })
            .then(response => response.json())
            .then(data => {
                if (data.success) {
                    showNotification(data.message, 'success');
                    localStorage.setItem('productoActualizado', Date.now());
                    dataTable.ajax.reload();
                } else {
                    showNotification(data.message, 'danger');
                }
            })
            .finally(() => showLoading(false));
    }
    
    function handleDeleteImagen(e) {
        const imagenId = $(this).data('id');
        if (imagenId) {
            imagenesParaEliminar.push(imagenId);
            $(`#imagen-thumb-${imagenId}`).css('opacity', '0.4');
            $(this).remove();
        }
    }

    function openModalForNew() {
        clearForm();
        $('#modalTitle').text('Agregar Producto');
        productoModal.show();
    }

    function openModalForEdit(producto) {
        clearForm();
        $('#modalTitle').text('Editar Producto');
        $('#id').val(producto.id);
        $('#nombre').val(producto.nombre);
        $('#descripcion').val(producto.descripcion);
        $('#precio').val(producto.precio);
        $('#stock').val(producto.stock);
        $('#stockMinimo').val(producto.stockMinimo);
        $('#id_categoria').val(producto.categoria.id);
        
        renderizarGaleria(producto.imagenes);
        initSortable();
        productoModal.show();
    }

    function renderizarGaleria(imagenes) {
        const galeria = $('#galeria-imagenes');
        galeria.empty();
        if (imagenes && imagenes.length > 0) {
            imagenes.forEach(img => {
                const thumb = `
                    <div id="imagen-thumb-${img.id}" class="position-relative" data-id="${img.id}">
                        <img src="${img.url}" class="img-thumbnail" style="width: 100px; height: 100px; object-fit: cover;">
                        <button class="btn btn-danger btn-sm position-absolute top-0 end-0 btn-delete-img" data-id="${img.id}" title="Marcar para eliminar">
                            <i class="bi bi-x-lg"></i>
                        </button>
                    </div>
                `;
                galeria.append(thumb);
            });
            $('#galeria-container').show();
        } else {
            $('#galeria-container').hide();
        }
    }

    function initSortable() {
        const galeriaEl = document.getElementById('galeria-imagenes');
        if (sortableGaleria) {
            sortableGaleria.destroy();
        }
        sortableGaleria = new Sortable(galeriaEl, {
            animation: 150,
            onEnd: function (evt) {
                const ids = Array.from(galeriaEl.children).map(el => $(el).data('id'));
                updateImageOrder(ids);
            }
        });
    }

    async function updateImageOrder(ids) {
        showLoading(true);
        try {
            const response = await fetch(ENDPOINTS.updateImageOrder, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(ids)
            });
            const result = await response.json();
            if (result.success) {
                showNotification(result.message, 'success');
                localStorage.setItem('productoActualizado', Date.now());
                dataTable.ajax.reload(); // Recargar para reflejar el orden en la miniatura principal
            } else {
                throw new Error(result.message);
            }
        } catch (error) {
            showNotification(error.message, 'danger');
        } finally {
            showLoading(false);
        }
    }

    function clearForm() {
        $('#formProducto')[0].reset();
        $('#id').val('');
        
        if (sortableGaleria) {
            sortableGaleria.destroy();
            sortableGaleria = null;
        }

        $('#galeria-imagenes').empty();
        $('#galeria-container').hide();
        imagenesParaEliminar = [];

        $('#galeria-preview').empty();
        $('#galeria-preview-container').hide();
        newFiles = [];
    }

    function hideModal() {
        productoModal.hide();
        clearForm();
    }

    function showLoading(show) {
        let overlay = $('#loading-overlay');
        if (show) {
            if (overlay.length === 0) {
                overlay = $('<div id="loading-overlay" class="loading-overlay" style="display: flex;"><div class="spinner-border text-primary"></div></div>');
                $('body').append(overlay);
            }
            overlay.show();
        } else {
            overlay.hide();
        }
    }

    function showNotification(message, type = 'success') {
        const toastClass = type === 'danger' ? 'text-bg-danger' : (type === 'warning' ? 'text-bg-warning' : 'text-bg-success');
        const toast = $(`<div class="toast align-items-center ${toastClass} border-0" role="alert" aria-live="assertive" aria-atomic="true"><div class="d-flex"><div class="toast-body">${message}</div><button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"></button></div></div>`);
        $('#notification-container').append(toast);
        const bsToast = new bootstrap.Toast(toast[0], { delay: 5000 });
        bsToast.show();
    }
});
