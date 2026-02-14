let originalOrders = [];
let displayedOrders = [];
let currentPage = 1;
const pageSize = 10;

function copyOrders() {
    const src = window.myAppData?.orders ?? [];
    originalOrders = src;
    displayedOrders = [...src];
}

async function handleCheckout(orderId) {
    try {
        const response = await fetch('/payment/getLinkForCheckout', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(orderId)
        });

        if (response.ok) {
            const checkoutUrl = await response.text();
            window.location.href = checkoutUrl; // Redirecting is usually better than an alert!
        } else {
            // Read the actual error message from the body
            const errorMessage = await response.text();
            alert("Server error: " + errorMessage);
        }
    } catch (error) {
        console.error("Network error:", error);
        alert("An error occurred. Check your connection.");
    }
}

function renderOrdersTable(ordersArray) {
    const tbody = document.getElementById("ordersTbody");
    const infoSpan = document.getElementById("paginationInfo");
    if (!tbody) return;

    tbody.innerHTML = "";
    const totalOrders = ordersArray.length;
    const startIndex = (currentPage - 1) * pageSize;
    const endIndex = Math.min(startIndex + pageSize, totalOrders);

    if (infoSpan) {
        infoSpan.textContent = totalOrders === 0 ? "No orders found." : `Showing ${startIndex + 1}-${endIndex} of ${totalOrders} orders`;
    }

    const paginatedItems = ordersArray.slice(startIndex, endIndex);

    paginatedItems.forEach(o => {
        const row = tbody.insertRow();

        // 1. Order ID
        row.insertCell().textContent = o.id ?? "";

        // 2. Wholesaler Name
        row.insertCell().textContent = o.wholesalerName ?? "";

        // 3. Date
        row.insertCell().textContent = o.orderDate ?? "";

        // 4. Status Badge
        const statusCell = row.insertCell();
        const badge = document.createElement("span");
        const statusClass = (o.status ?? "default").toLowerCase().replace(/\s+/g, '-');
        badge.className = `badge status-badge status-${statusClass}`;
        badge.textContent = o.status ?? "";
        statusCell.appendChild(badge);

        // 5. Payment Status (Clickable only if Pending)
        const paymentCell = row.insertCell();
        const paymentBtn = document.createElement("button");
        const currentPaymentStatus = (o.paymentStatus ?? "").trim();

        // Normalize status for comparison
        const isPending = currentPaymentStatus.toLowerCase() === "pending";

        if (isPending) {
            // ACTIVE STATE: Allow checkout
            paymentBtn.className = "btn btn-link p-0 text-decoration-none";
            paymentBtn.textContent = currentPaymentStatus;
            paymentBtn.onclick = () => handleCheckout(o.id);
        } else {
            // INACTIVE STATE: Just show text, no link/button behavior
            paymentBtn.className = "btn p-0 text-muted text-decoration-none cursor-default";
            paymentBtn.style.cursor = "default";
            paymentBtn.disabled = true;
            paymentBtn.textContent = currentPaymentStatus || "Paid"; // Fallback text
        }

        paymentCell.appendChild(paymentBtn);

        // 6. Price
        const price = o.totalPrice != null ? Number(o.totalPrice).toFixed(2) : "0.00";
        row.insertCell().textContent = `${price} USD`;

        // 7. Details Button
        const detailsCell = row.insertCell();
        detailsCell.className = "text-center";
        const detailsBtn = document.createElement("a");
        detailsBtn.className = "btn btn-sm btn-outline-primary";
        detailsBtn.href = `/offer/orders/${o.id}`;
        detailsBtn.textContent = "Details";
        detailsCell.appendChild(detailsBtn);

        // 8. Cancel Button (Logic: Only active if status is "Ordered")
        const cancelCell = row.insertCell();
        cancelCell.className = "text-center";

        const cancelForm = document.createElement("form");
        cancelForm.method = "POST";
        cancelForm.action = "/offer/orders/cancel";

        const hiddenInput = document.createElement("input");
        hiddenInput.type = "hidden";
        hiddenInput.name = "orderId";
        hiddenInput.value = o.id;

        const cancelBtn = document.createElement("button");
        cancelBtn.type = "submit";
        cancelBtn.textContent = "Cancel";

        // Check the status (case-insensitive to be safe)
        const currentStatus = (o.status ?? "").trim();

        if (currentStatus === "Ordered") {
            // ACTIVE STATE
            cancelBtn.className = "btn btn-sm btn-danger";

            cancelForm.onsubmit = function(e) {
                if (!confirm(`Are you sure you want to cancel order #${o.id}?`)) {
                    e.preventDefault();
                }
            };
        } else {
            // INACTIVE STATE
            cancelBtn.className = "btn btn-sm btn-secondary opacity-50";
            cancelBtn.disabled = true; // This makes it unclickable
            cancelBtn.title = "Only 'Ordered' items can be cancelled"; // Tooltip on hover

            // Prevent form submission just in case
            cancelForm.onsubmit = (e) => e.preventDefault();
        }

        cancelForm.appendChild(hiddenInput);
        cancelForm.appendChild(cancelBtn);
        cancelCell.appendChild(cancelForm);
    });

    renderPaginationControls(totalOrders);
}

// Funkcja filtrowania dostosowana do hurtownika
function applyAllFilters() {
    const wholesalerVal = document.getElementById("filterWholesaler").value.toLowerCase();
    const idVal = document.getElementById("filterOrderId").value.toLowerCase();
    const dateFrom = document.getElementById("filterDateFrom").value;
    const dateTo = document.getElementById("filterDateTo").value;
    const statusVal = document.getElementById("filterStatus").value;

    displayedOrders = originalOrders.filter(o => {
        const matchesWholesaler = (o.wholesalerName ?? "").toLowerCase().includes(wholesalerVal);
        const matchesId = String(o.id).toLowerCase().includes(idVal);
        const matchesStatus = statusVal === "" || o.status === statusVal;

        let matchesDate = true;
        if (o.orderDate) {
            const orderDateStr = o.orderDate.toString().substring(0, 10);
            if (dateFrom && orderDateStr < dateFrom) matchesDate = false;
            if (dateTo && orderDateStr > dateTo) matchesDate = false;
        }
        return matchesWholesaler && matchesId && matchesStatus && matchesDate;
    });

    currentPage = 1;
    renderOrdersTable(displayedOrders);
}

// Reużywalne funkcje paginacji (takie same jak u Ciebie)
function renderPaginationControls(totalItems) {
    const paginationUl = document.getElementById("paginationControls");
    if (!paginationUl) return;
    paginationUl.innerHTML = "";
    const totalPages = Math.ceil(totalItems / pageSize);
    if (totalPages <= 1) return;

    createPageItem(paginationUl, "«", currentPage > 1, () => { currentPage--; renderOrdersTable(displayedOrders); });
    for (let i = 1; i <= totalPages; i++) {
        createPageItem(paginationUl, i, true, () => { currentPage = i; renderOrdersTable(displayedOrders); }, i === currentPage);
    }
    createPageItem(paginationUl, "»", currentPage < totalPages, () => { currentPage++; renderOrdersTable(displayedOrders); });
}

function createPageItem(container, text, enabled, onClick, isActive = false) {
    const li = document.createElement("li");
    li.className = `page-item ${enabled ? '' : 'disabled'} ${isActive ? 'active' : ''}`;
    const a = document.createElement("a");
    a.className = "page-link";
    a.href = "#";
    a.textContent = text;
    a.addEventListener("click", (e) => { e.preventDefault(); if (enabled) onClick(); });
    li.appendChild(a);
    container.appendChild(li);
}

document.addEventListener("DOMContentLoaded", () => {
    copyOrders();
    renderOrdersTable(displayedOrders);

    const filterIds = ["filterWholesaler", "filterOrderId", "filterDateFrom", "filterDateTo", "filterStatus"];
    filterIds.forEach(id => {
        const el = document.getElementById(id);
        if (el) el.addEventListener("input", applyAllFilters);
    });
});