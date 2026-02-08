document.addEventListener('DOMContentLoaded', function() {

    /**
     * Orchestrates the cart update process.
     * Communicates with the backend and handles the response or potential errors.
     * * @param {number} cartId - ID of the specific wholesaler's basket.
     * @param {number} offerId - ID of the specific product offer being updated.
     * @param {number} quantity - The target quantity (0 triggers server-side removal).
     */
    async function updateCart(cartId, offerId, quantity) {
        const params = new URLSearchParams();
        params.append('cartId', cartId);
        params.append('offerId', offerId);
        params.append('quantity', quantity);

        try {
            const response = await fetch('/offer/cart/update', {
                method: 'POST',
                headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                body: params
            });

            // Handle non-200 HTTP statuses (e.g., 400 Bad Request from validation failures)
            if (!response.ok) {
                // Extract the custom error message (e.getMessage()) sent by the Spring controller
                const errorMsg = await response.text();
                throw new Error(errorMsg || "Server failed to update quantity.");
            }

            // On success, parse the updated List<CartInformationDTO> and refresh the DOM
            const cartList = await response.json();
            renderUI(cartList);

        } catch (error) {
            console.error('Cart Update Error:', error);
            // Display the specific backend validation message to the user
            alert(error.message);
        }
    }

    /**
     * Synchronizes the DOM with the latest cart state provided by the server.
     * Handles dynamic removal of items, baskets, and updates totals.
     * * @param {Array} cartList - Array of CartInformationDTO objects.
     */
    function renderUI(cartList) {
        let grandTotal = 0;
        const activeIds = cartList.map(c => c.cartId);

        // 1. CLEANUP: Remove entire wholesaler <details> blocks if they no longer exist in the data
        document.querySelectorAll('details.basket').forEach(el => {
            const id = parseInt(el.querySelector('input')?.dataset.cartId);
            if (id && !activeIds.includes(id)) el.remove();
        });

        // 2. DATA SYNC: Iterate through active carts to update items and sub-totals
        cartList.forEach(cart => {
            grandTotal += cart.cartTotal;

            // Locate the specific basket container for this wholesaler
            const basket = document.querySelector(`input[data-cart-id="${cart.cartId}"]`)?.closest('details');

            if (basket) {
                // Update the basket header summary (item count and sub-total)
                const meta = basket.querySelector('.basket-meta');
                if (meta) meta.innerHTML = `(${cart.items.length} items, $<span>${cart.cartTotal.toFixed(2)}</span>)`;

                // Remove item rows (<tr>) that are no longer present in the updated cart data
                const currentOfferIds = cart.items.map(it => it.offerId.toString());
                basket.querySelectorAll('tbody tr').forEach(row => {
                    const rowId = row.querySelector('input')?.dataset.offerId;
                    if (rowId && !currentOfferIds.includes(rowId)) row.remove();
                });

                // Update pricing and quantities for items still in the cart
                cart.items.forEach(item => {
                    const row = basket.querySelector(`tr:has([data-offer-id="${item.offerId}"])`);
                    if (row) {
                        // Update the line total cell (calculated as price * quantity)
                        row.cells[3].innerText = item.lineTotal.toFixed(2);

                        // Sync the input value (useful if server-side logic modified the quantity)
                        const input = row.querySelector('.cart-qty-input');
                        if (input) input.value = item.quantity;
                    }
                });
            }
        });

        // 3. GLOBAL TOTALS: Update the grand total and the active basket count
        const totalDisplay = document.querySelector('.baskets-summary b');
        if (totalDisplay) totalDisplay.innerText = grandTotal.toFixed(2) + " ZŁ";

        const titleCount = document.querySelector('.baskets-title span');
        if (titleCount) titleCount.innerText = cartList.length;

        // 4. EMPTY STATE: If no carts remain, replace the view with an empty cart message
        if (cartList.length === 0) {
            const container = document.querySelector('.baskets');
            if (container) {
                container.innerHTML = '<div class="baskets-title">Baskets (0 active)</div><p>Your cart is empty.</p>';
            }
        }
    }

    // --- Interaction Listeners ---

    // Handle 'Remove' button clicks
    document.querySelectorAll('.remove-item-btn').forEach(btn => {
        btn.addEventListener('click', function() {
            if (confirm("Remove this item?")) {
                updateCart(this.dataset.cartId, this.dataset.offerId, 0);
            }
        });
    });

    // Handle quantity changes in input fields
    document.querySelectorAll('.cart-qty-input').forEach(input => {
        input.addEventListener('change', function() {
            const val = parseInt(this.value);

            // Client-side validation to prevent negative numbers or text
            if (isNaN(val) || val < 1) {
                alert("Quantity must be at least 1.");
                this.value = 1;
                return;
            }

            updateCart(this.dataset.cartId, this.dataset.offerId, val);
        });
    });
});