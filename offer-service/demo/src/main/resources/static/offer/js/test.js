let originalOrders = [];
let displayedOrders = [];
let currentPage = 1;
const pageSize = 10; // Change this to 5, 20, etc.

function copyOrders() {
    const src = window.myAppData?.orders ?? [];
    originalOrders = src;
    displayedOrders = [...src];
    return displayedOrders;
}

function renderOrdersTable(ordersArray) {
    const tbody = document.getElementById("ordersTbody");
    const infoSpan = document.getElementById("paginationInfo");
    if (!tbody) return;

    // 1. Clear the existing table rows
    tbody.innerHTML = "";

    // 2. Pagination Math
    const totalOrders = ordersArray.length;
    const startIndex = (currentPage - 1) * pageSize;
    const endIndex = Math.min(startIndex + pageSize, totalOrders);

    // 3. Update the "Showing X-Y of Z" label
    if (infoSpan) {
        if (totalOrders === 0) {
            infoSpan.textContent = "No orders found.";
        } else {
            // (startIndex + 1) because users count from 1, not 0
            infoSpan.textContent = `Showing ${startIndex + 1}-${endIndex} of ${totalOrders} orders`;
        }
    }

    // 4. Get only the items for the current page
    const paginatedItems = ordersArray.slice(startIndex, endIndex);

    // 5. Build the rows
    paginatedItems.forEach(o => {
        const row = tbody.insertRow();

        // Order ID, Store, and Date
        row.insertCell().textContent = o.id ?? "";
        row.insertCell().textContent = o.storeName ?? "";
        row.insertCell().textContent = o.orderDate ?? "";

        // Status Badge
        const statusCell = row.insertCell();
        const badge = document.createElement("span");
        const statusClass = (o.status ?? "default").toLowerCase().replace(/\s+/g, '-');
        badge.className = `badge status-badge status-${statusClass}`;
        badge.textContent = o.status ?? "";
        statusCell.appendChild(badge);

        // Payment Status
        row.insertCell().textContent = o.paymentStatus ?? "";

        // Price formatting
        const price = o.totalPrice != null ? Number(o.totalPrice).toFixed(2) : "0.00";
        row.insertCell().textContent = `${price} USD`;

        // Details Button
        const detailsCell = row.insertCell();
        const a = document.createElement("a");
        a.className = "btn btn-sm btn-outline-primary";
        a.href = `/offer/orders/${o.id}`;
        a.textContent = "Details";
        detailsCell.appendChild(a);

        // Status Update Form
        const changeCell = row.insertCell();
        changeCell.innerHTML = `
            <form action="/offer/wholesaler/orders/status" method="post" class="d-flex gap-2">
                <input type="hidden" name="orderId" value="${o.id}" />
                <select name="status" class="form-select form-select-sm">
                    <option value="Ordered" ${o.status === 'Ordered' ? 'selected' : ''}>Ordered</option>
                    <option value="In progress" ${o.status === 'In progress' ? 'selected' : ''}>In progress</option>
                    <option value="Shipped" ${o.status === 'Shipped' ? 'selected' : ''}>Shipped</option>
                    <option value="Delivered" ${o.status === 'Delivered' ? 'selected' : ''}>Delivered</option>
                </select>
                <button type="submit" class="btn btn-sm btn-success">Update</button>
            </form>
        `;
    });

    // 6. Refresh the navigation buttons at the bottom
    renderPaginationControls(totalOrders);
}

function renderPaginationControls(totalItems) {
    const paginationUl = document.getElementById("paginationControls");
    if (!paginationUl) return;

    paginationUl.innerHTML = "";
    const totalPages = Math.ceil(totalItems / pageSize);

    if (totalPages <= 1) return; // Hide if only one page

    // Previous Button
    createPageItem(paginationUl, "«", currentPage > 1, () => {
        currentPage--;
        renderOrdersTable(displayedOrders);
    });

    // Page Numbers
    for (let i = 1; i <= totalPages; i++) {
        const isActive = (i === currentPage);
        createPageItem(paginationUl, i, true, () => {
            currentPage = i;
            renderOrdersTable(displayedOrders);
        }, isActive);
    }

    // Next Button
    createPageItem(paginationUl, "»", currentPage < totalPages, () => {
        currentPage++;
        renderOrdersTable(displayedOrders);
    });
}

function createPageItem(container, text, enabled, onClick, isActive = false) {
    const li = document.createElement("li");
    li.className = `page-item ${enabled ? '' : 'disabled'} ${isActive ? 'active' : ''}`;

    const a = document.createElement("a");
    a.className = "page-link";
    a.href = "#";
    a.textContent = text;
    a.addEventListener("click", (e) => {
        e.preventDefault();
        if (enabled) onClick();
    });

    li.appendChild(a);
    container.appendChild(li);
}

function applyAllFilters() {
    const storeVal = document.getElementById("filterStore").value.toLowerCase();
    const idVal = document.getElementById("filterOrderId").value.toLowerCase();
    const dateFrom = document.getElementById("filterDateFrom").value;
    const dateTo = document.getElementById("filterDateTo").value;
    const statusVal = document.getElementById("filterStatus").value;

    displayedOrders = originalOrders.filter(o => {
        const matchesStore = (o.storeName ?? "").toLowerCase().includes(storeVal);
        const matchesId = String(o.id).toLowerCase().includes(idVal);
        const matchesStatus = statusVal === "" || o.status === statusVal;

        let matchesDate = true;
        if (o.orderDate) {
            const orderDateStr = o.orderDate.substring(0, 10);
            if (dateFrom && orderDateStr < dateFrom) matchesDate = false;
            if (dateTo && orderDateStr > dateTo) matchesDate = false;
        }
        return matchesStore && matchesId && matchesStatus && matchesDate;
    });

    // Reset to page 1 whenever filters change
    currentPage = 1;
    renderOrdersTable(displayedOrders);
}

// Initial Load
document.addEventListener("DOMContentLoaded", () => {
    copyOrders();
    renderOrdersTable(displayedOrders);

    const filterIds = ["filterStore", "filterOrderId", "filterDateFrom", "filterDateTo", "filterStatus"];
    filterIds.forEach(id => {
        const el = document.getElementById(id);
        if (el) el.addEventListener("input", applyAllFilters);
    });
});