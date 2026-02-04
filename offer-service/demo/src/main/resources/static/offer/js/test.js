let originalOrders = [];
let displayedOrders = [];

function copyOrders() {
  const src = window.myAppData?.orders ?? [];
  originalOrders = src;                 // keep original reference
  displayedOrders = src.map(o => ({...o})); // copy you can modify
  return displayedOrders;
}

function renderOrdersTable(ordersArray) {
  const tbody = document.getElementById("ordersTbody");
  if (!tbody) return;

  tbody.innerHTML = "";

  ordersArray.forEach(o => {
    const row = tbody.insertRow();
    row.insertCell().textContent = o.id ?? "";
    row.insertCell().textContent = o.storeName ?? "";
    row.insertCell().textContent = o.orderDate ?? "";

    // --- UPDATED STATUS BADGE WITH DYNAMIC CLASS ---
    const statusCell = row.insertCell();
    const badge = document.createElement("span");

    // Create a CSS class based on status: e.g., "status-shipped"
    // .replace(/\s+/g, '-') turns "In progress" into "in-progress"
    const statusClass = (o.status ?? "default").toLowerCase().replace(/\s+/g, '-');

    // "badge" is for Bootstrap shape, "status-badge" is your base,
    // and "status-xxx" is your specific color style
    badge.className = `badge status-badge status-${statusClass}`;
    badge.textContent = o.status ?? "";
    statusCell.appendChild(badge);
    // -----------------------------------------------

    row.insertCell().textContent = o.paymentStatus ?? "";

    const price = o.totalPrice != null ? Number(o.totalPrice).toFixed(2) : "0.00";
    row.insertCell().textContent = `${price} USD`;

    const detailsCell = row.insertCell();
    const a = document.createElement("a");
    a.className = "btn btn-sm btn-outline-primary";
    a.href = `/offer/orders/${o.id}`;
    a.textContent = "Details";
    detailsCell.appendChild(a);

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
}

function filterByStoreName(query) {
  const q = (query ?? "").trim().toLowerCase();

  // if empty -> show all
  if (q.length === 0) return originalOrders.map(o => ({...o}));

  // contains match: "St" matches "Best Store", "Storehouse", etc.
  return originalOrders
    .filter(o => (o.storeName ?? "").toLowerCase().startsWith(q))
    .map(o => ({...o}));
}



// New function to filter by Order ID
function filterByOrderId(query) {
  const q = (query ?? "").trim();

  // If empty, show all (or rather, start from original list)
  if (q.length === 0) return originalOrders.map(o => ({...o}));

  // Match the ID (converting to string to ensure comparison works)
  return originalOrders
    .filter(o => String(o.id).toLowerCase().startsWith(q.toLowerCase()))
    .map(o => ({...o}));
}

function applyAllFilters() {
    const storeVal = document.getElementById("filterStore").value.toLowerCase();
    const idVal = document.getElementById("filterOrderId").value.toLowerCase();
    const dateFrom = document.getElementById("filterDateFrom").value;
    const dateTo = document.getElementById("filterDateTo").value;
    const statusVal = document.getElementById("filterStatus").value; // Get dropdown value

    displayedOrders = originalOrders.filter(o => {
        // 1. Store Filter
        const matchesStore = (o.storeName ?? "").toLowerCase().includes(storeVal);

        // 2. ID Filter
        const matchesId = String(o.id).toLowerCase().includes(idVal);

        // 3. Status Filter
        // If statusVal is empty (All Statuses), it matches everything.
        const matchesStatus = statusVal === "" || o.status === statusVal;

        // 4. Date Filter
        let matchesDate = true;
        if (o.orderDate) {
            const orderDateStr = o.orderDate.substring(0, 10);
            if (dateFrom && orderDateStr < dateFrom) matchesDate = false;
            if (dateTo && orderDateStr > dateTo) matchesDate = false;
        }

        // Only return true if ALL conditions are met
        return matchesStore && matchesId && matchesStatus && matchesDate;
    });

    renderOrdersTable(displayedOrders);
}

document.addEventListener("DOMContentLoaded", () => {
    copyOrders();
    renderOrdersTable(displayedOrders);

    // List of all filter element IDs, now including filterStatus
    const filterIds = [
        "filterStore",
        "filterOrderId",
        "filterDateFrom",
        "filterDateTo",
        "filterStatus"
    ];

    filterIds.forEach(id => {
        const element = document.getElementById(id);
        if (element) {
            // "input" covers text boxes and date pickers
            // "change" is specific to selects, but "input" usually works for both in modern browsers
            element.addEventListener("input", applyAllFilters);
        }
    });
});