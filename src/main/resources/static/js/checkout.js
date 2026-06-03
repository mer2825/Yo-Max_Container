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
    const continueToPaymentBtn = document.getElementById('continueToPaymentBtn');
    const backToStep1Btn = document.getElementById('backToStep1Btn');
    const cartItems = document.getElementById('cartItems');
    const subtotalAmount = document.getElementById('subtotalAmount');
    const totalAmount = document.getElementById('totalAmount');
    const paymentAmount = document.getElementById('paymentAmount');
    const ultraCompactSummary = document.getElementById('ultraCompactSummary');

    let uploadedFile = null;
    let cartData = [];
    let currentStep = 1;

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

        // Generar resumen ultra-compacto
        generateUltraCompactSummary(total);
    }

    // Generar resumen ultra-compacto
    function generateUltraCompactSummary(total) {
        if (cartData.length === 0) return;

        let summaryText = '';
        if (cartData.length === 1) {
            const item = cartData[0];
            const nombre = item.nombre || item.name;
            const cantidad = item.cantidad || item.quantity;
            summaryText = `${nombre} x${cantidad}`;
        } else {
            const firstItem = cartData[0];
            const nombre = firstItem.nombre || firstItem.name;
            const cantidad = firstItem.cantidad || firstItem.quantity;
            const remainingCount = cartData.length - 1;
            summaryText = `${nombre} x${cantidad} + ${remainingCount} producto${remainingCount > 1 ? 's' : ''} más`;
        }

        summaryText += ` · Total: S/ ${total.toFixed(2)}`;
        ultraCompactSummary.textContent = summaryText;
    }

    // Navegar entre pantallas
    function goToStep(step) {
        currentStep = step;

        // Ocultar todas las pantallas
        document.querySelectorAll('.checkout-screen').forEach(screen => {
            screen.classList.remove('active');
        });

        // Mostrar la pantalla correspondiente
        if (step === 1) {
            document.getElementById('screen1').classList.add('active');
            updateProgressBar(2);
        } else if (step === 2) {
            document.getElementById('screen2').classList.add('active');
            updateProgressBar(3);
        } else if (step === 3) {
            document.getElementById('screen3').classList.add('active');
            updateProgressBar(4);
        }
    }

    // Actualizar barra de progreso
    function updateProgressBar(activeStep) {
        document.querySelectorAll('.step').forEach((stepEl, index) => {
            const stepNumber = index + 1;
            stepEl.classList.remove('completed', 'active');

            if (stepNumber < activeStep) {
                stepEl.classList.add('completed');
            } else if (stepNumber === activeStep) {
                stepEl.classList.add('active');
            }
        });
    }

    // Validar paso 1
    function validateStep1() {
        const nombreCliente = document.getElementById('nombreCliente').value.trim();
        const dniCliente = document.getElementById('dniCliente').value.trim();
        const telefonoCliente = document.getElementById('telefonoCliente').value.trim();
        const dniError = document.getElementById('dni-error');

        const dniRegex = /^[0-9]{8}$/;
        const telefonoRegex = /^[0-9]{9}$/;

        // Ocultar mensaje de error DNI
        if (dniError) {
            dniError.style.display = 'none';
        }

        if (!nombreCliente) {
            alert('Por favor, ingresa tu nombre completo.');
            return false;
        }

        if (!dniCliente || !dniRegex.test(dniCliente)) {
            if (dniError) {
                dniError.style.display = 'block';
            }
            return false;
        }

        if (!telefonoCliente || !telefonoRegex.test(telefonoCliente)) {
            alert('Por favor, ingresa un teléfono válido de 9 dígitos.');
            return false;
        }

        return true;
    }

    // Validar DNI en tiempo real
    document.getElementById('dniCliente').addEventListener('input', function() {
        const dniCliente = this.value.trim();
        const dniError = document.getElementById('dni-error');
        const dniRegex = /^[0-9]{8}$/;

        // Solo permitir números
        this.value = this.value.replace(/[^0-9]/g, '');

        if (dniError) {
            if (dniCliente.length > 0 && !dniRegex.test(dniCliente)) {
                dniError.style.display = 'block';
            } else {
                dniError.style.display = 'none';
            }
        }
    });

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

    // Validar formulario paso 2 (solo archivo)
    function validateForm() {
        const isFormValid = uploadedFile;
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

    // Event listener para botón "Continuar al pago"
    continueToPaymentBtn.addEventListener('click', () => {
        if (validateStep1()) {
            goToStep(2);
        }
    });

    // Event listener para botón "Volver"
    backToStep1Btn.addEventListener('click', () => {
        goToStep(1);
    });

    // Enviar pedido
    confirmBtn.addEventListener('click', async () => {
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

            // Mostrar pantalla de confirmación
            document.getElementById('confirmationOrderNumber').textContent = pedidoResult.pedidoId;
            document.getElementById('confirmationPhone').textContent = telefonoCliente;
            goToStep(3);

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

    // Verificar configuración Yape y mostrar alerta para admin si está vacía
    function checkYapeConfig() {
        const yapeConfigAlert = document.getElementById('yapeConfigAlert');
        if (!yapeConfigAlert) return;

        // Verificar si el usuario es administrador
        const mainElement = document.querySelector('.checkout-container');
        const isAdmin = mainElement && mainElement.getAttribute('data-is-admin') === 'true';

        if (!isAdmin) return; // Solo mostrar alerta para administradores

        // Verificar si los campos Yape están vacíos en el HTML
        const yapeNumberElement = document.querySelector('.yape-number');
        const yapeTitularElement = document.querySelector('.payment-section .fs-4');

        if (yapeNumberElement && yapeTitularElement) {
            const yapeNumber = yapeNumberElement.textContent.trim();
            const yapeTitular = yapeTitularElement.textContent.trim();

            // Si están vacíos o son "---", mostrar alerta
            if ((yapeNumber === '---' || yapeNumber === '') && (yapeTitular === '---' || yapeTitular === '')) {
                yapeConfigAlert.classList.remove('d-none');
            }
        }
    }

    // Inicializar
    loadCart();
    updateProgressBar(2); // Inicializar barra de progreso en paso 2 (Mis datos)
    checkYapeConfig(); // Verificar configuración Yape
});
