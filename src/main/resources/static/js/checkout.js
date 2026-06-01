// Checkout - Sistema de Pedidos Web con Yape

console.log('checkout.js loaded');

document.addEventListener('DOMContentLoaded', function() {
    console.log('checkout.js DOMContentLoaded fired');
    const uploadArea = document.getElementById('uploadArea');
    const selectFileBtn = document.getElementById('selectFileBtn');
    const voucherFile = document.getElementById('voucherFile');
    const previewContainer = document.getElementById('previewContainer');
    const previewImage = document.getElementById('previewImage');
    const removeFileBtn = document.getElementById('removeFileBtn');
    const fileError = document.getElementById('fileError');
    const checkoutForm = document.getElementById('checkoutForm');
    const confirmBtn = document.getElementById('confirmBtn');
    const cartItems = document.getElementById('cartItems');
    const subtotalAmount = document.getElementById('subtotalAmount');
    const totalAmount = document.getElementById('totalAmount');
    const paymentAmount = document.getElementById('paymentAmount');

    let uploadedFile = null;
    let cartData = [];

    // Cargar carrito desde localStorage
    function loadCart() {
        const cart = localStorage.getItem('cart');
        console.log('Cart from localStorage:', cart);
        if (cart) {
            cartData = JSON.parse(cart);
            console.log('Parsed cart data:', cartData);
            renderCart();
        } else {
            // Si no hay carrito, redirigir al catálogo
            console.log('No cart found, redirecting to /');
            window.location.href = '/';
        }
        
        // Cargar datos del cliente del localStorage (si vienen del catálogo)
        loadClienteData();
    }
    
    // Cargar datos del cliente del localStorage
    function loadClienteData() {
        const clienteData = localStorage.getItem('clienteData');
        if (clienteData) {
            const data = JSON.parse(clienteData);
            const nombreCliente = document.getElementById('nombreCliente');
            const dniCliente = document.getElementById('dniCliente');
            
            if (nombreCliente && data.nombre) {
                nombreCliente.value = data.nombre;
            }
            if (dniCliente && data.dni) {
                dniCliente.value = data.dni;
            }
            
            // Limpiar localStorage después de usar los datos
            localStorage.removeItem('clienteData');
        }
    }

    // Renderizar carrito
    function renderCart() {
        console.log('renderCart called, cartData.length:', cartData.length);
        if (cartData.length === 0) {
            console.log('Cart is empty, redirecting to /');
            window.location.href = '/';
            return;
        }

        let subtotal = 0;
        cartItems.innerHTML = '';

        cartData.forEach(item => {
            console.log('Processing item:', item);
            // Compatibilidad con nombres de propiedades del catálogo (price, quantity, name)
            const precio = item.precio || item.price;
            const cantidad = item.cantidad || item.quantity;
            const nombre = item.nombre || item.name;
            const itemTotal = precio * cantidad;
            subtotal += itemTotal;

            const cartItem = document.createElement('div');
            cartItem.className = 'cart-item';
            cartItem.innerHTML = `
                <div>
                    <strong>${nombre}</strong>
                    <small class="text-muted d-block">Cantidad: ${cantidad} x S/ ${precio.toFixed(2)}</small>
                </div>
                <span class="fw-bold">S/ ${itemTotal.toFixed(2)}</span>
            `;
            cartItems.appendChild(cartItem);
        });

        const total = subtotal;
        subtotalAmount.textContent = `S/ ${subtotal.toFixed(2)}`;
        totalAmount.textContent = `S/ ${total.toFixed(2)}`;
        paymentAmount.textContent = `S/ ${total.toFixed(2)}`;
    }

    // Validar archivo
    function validateFile(file) {
        const validTypes = ['image/jpeg', 'image/png', 'image/webp'];
        const maxSize = 5 * 1024 * 1024; // 5MB

        if (!validTypes.includes(file.type)) {
            return 'El archivo debe ser JPG, PNG o WEBP';
        }

        if (file.size > maxSize) {
            return 'El archivo no debe superar 5MB';
        }

        return null;
    }

    // Mostrar preview del archivo
    function showPreview(file) {
        const reader = new FileReader();
        reader.onload = function(e) {
            previewImage.src = e.target.result;
            previewContainer.style.display = 'block';
            uploadArea.classList.add('has-file');
        };
        reader.readAsDataURL(file);
    }

    // Limpiar archivo
    function clearFile() {
        uploadedFile = null;
        voucherFile.value = '';
        previewContainer.style.display = 'none';
        uploadArea.classList.remove('has-file');
        fileError.style.display = 'none';
        validateForm();
    }

    // Validar formulario
    function validateForm() {
        const nombreCliente = document.getElementById('nombreCliente').value.trim();
        const dniCliente = document.getElementById('dniCliente').value.trim();
        const telefonoCliente = document.getElementById('telefonoCliente').value.trim();

        const isFormValid = nombreCliente && 
                           dniCliente && 
                           telefonoCliente && 
                           uploadedFile;

        confirmBtn.disabled = !isFormValid;
    }

    // Event listeners para subida de archivo
    uploadArea.addEventListener('click', () => voucherFile.click());

    uploadArea.addEventListener('dragover', (e) => {
        e.preventDefault();
        uploadArea.classList.add('dragover');
    });

    uploadArea.addEventListener('dragleave', () => {
        uploadArea.classList.remove('dragover');
    });

    uploadArea.addEventListener('drop', (e) => {
        e.preventDefault();
        uploadArea.classList.remove('dragover');

        const files = e.dataTransfer.files;
        if (files.length > 0) {
            handleFile(files[0]);
        }
    });

    selectFileBtn.addEventListener('click', (e) => {
        e.stopPropagation();
        voucherFile.click();
    });

    voucherFile.addEventListener('change', (e) => {
        if (e.target.files.length > 0) {
            handleFile(e.target.files[0]);
        }
    });

    removeFileBtn.addEventListener('click', clearFile);

    function handleFile(file) {
        const error = validateFile(file);
        if (error) {
            fileError.textContent = error;
            fileError.style.display = 'block';
            return;
        }

        fileError.style.display = 'none';
        uploadedFile = file;
        showPreview(file);
        validateForm();
    }

    // Event listeners para validación de formulario
    document.getElementById('nombreCliente').addEventListener('input', validateForm);
    document.getElementById('dniCliente').addEventListener('input', validateForm);
    document.getElementById('telefonoCliente').addEventListener('input', validateForm);

    // Enviar formulario
    checkoutForm.addEventListener('submit', async (e) => {
        e.preventDefault();

        if (!uploadedFile) {
            fileError.textContent = 'Debes subir el comprobante de pago';
            fileError.style.display = 'block';
            return;
        }

        confirmBtn.disabled = true;
        confirmBtn.innerHTML = '<i class="bi bi-hourglass-split"></i> Procesando...';

        try {
            // Subir voucher
            const formData = new FormData();
            formData.append('file', uploadedFile);

            const uploadResponse = await fetch('/api/upload', {
                method: 'POST',
                body: formData
            });

            if (!uploadResponse.ok) {
                throw new Error('Error al subir el comprobante');
            }

            const uploadData = await uploadResponse.json();
            const voucherPath = uploadData.filePath;

            // Crear pedido
            const clienteId = document.getElementById('clienteId').value;
            const nombreCliente = document.getElementById('nombreCliente').value.trim();
            const dniCliente = document.getElementById('dniCliente').value.trim();
            const telefonoCliente = document.getElementById('telefonoCliente').value.trim();
            const notaCliente = document.getElementById('notaCliente').value.trim();

            const pedidoData = {
                cliente: clienteId ? { id: parseInt(clienteId) } : null,
                nombreCliente: nombreCliente,
                dniCliente: dniCliente,
                telefonoCliente: telefonoCliente,
                metodoPago: 'Yape',
                voucherImagen: voucherPath,
                items: cartData.map(item => ({
                    producto: { id: item.id },
                    cantidad: item.cantidad || item.quantity,
                    precioUnitario: item.precio || item.price
                }))
            };

            const pedidoResponse = await fetch('/pedidos_web/api/crear', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(pedidoData)
            });

            if (!pedidoResponse.ok) {
                const errorText = await pedidoResponse.text();
                console.error('Error al crear el pedido:', errorText);
                throw new Error('Error al crear el pedido');
            }

            const pedidoResult = await pedidoResponse.json();

            // Limpiar carrito
            localStorage.removeItem('cart');
            updateCartCount();

            // Mostrar modal de confirmación
            document.getElementById('orderNumber').textContent = pedidoResult.pedidoId;
            const modal = new bootstrap.Modal(document.getElementById('confirmationModal'));
            modal.show();

        } catch (error) {
            console.error('Error:', error);
            alert('Hubo un error al procesar tu pedido. Por favor, intenta nuevamente.');
            confirmBtn.disabled = false;
            confirmBtn.innerHTML = '<i class="bi bi-check-circle"></i> Confirmar Pedido';
        }
    });

    // Actualizar contador del carrito
    function updateCartCount() {
        const cart = localStorage.getItem('cart');
        const count = cart ? JSON.parse(cart).length : 0;
        const cartCountElement = document.getElementById('cart-count');
        if (cartCountElement) {
            cartCountElement.textContent = count;
        }
    }

    // Inicializar
    loadCart();
    validateForm();
});
