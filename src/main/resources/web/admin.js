const API_URL = "/api";
const authToken = localStorage.getItem('auth_token');

// Check auth
if (!authToken) {
    window.location.href = "/";
}

async function loadData() {
    await loadUsers();
    await loadCluster();
}

async function loadUsers() {
    try {
        const res = await fetch(`${API_URL}/admin/users`, {
            headers: { 'Authorization': authToken }
        });
        if (res.status === 401 || res.status === 403) {
            alert("Session expirée ou droits insuffisants");
            localStorage.removeItem('auth_token');
            localStorage.removeItem('auth_user');
            window.location.href = "/";
            return;
        }
        const users = await res.json();
        renderUsers(users);
        updateCharts(users);
        
        // Update dashboard stats
        document.getElementById('total-users-val').textContent = users.length;
        const totalSize = users.reduce((acc, u) => acc + u.size, 0);
        document.getElementById('global-storage-val').textContent = (totalSize / (1024 * 1024)).toFixed(2) + " Mo";
    } catch (e) {
        console.error(e);
    }
}

function renderUsers(users) {
    const tbody = document.getElementById('users-table-body');
    tbody.innerHTML = '';
    
    users.forEach(user => {
        const usedPct = Math.min(((user.size / user.quota_limit) * 100), 100).toFixed(1);
        const usedMB = (user.size / (1024 * 1024)).toFixed(2);
        const limitMB = (user.quota_limit / (1024 * 1024)).toFixed(0);
        
        const avatarSrc = user.profile_image ? user.profile_image : `https://ui-avatars.com/api/?name=${user.username}&background=random`;
        
        const row = document.createElement('tr');
        row.innerHTML = `
            <td>
                <div style="display:flex; align-items:center; gap:12px;">
                    <img src="${avatarSrc}" style="width:36px; height:36px; border-radius:50%; object-fit: cover;">
                    <div style="display: flex; flex-direction: column;">
                        <span style="font-weight: 600;">${user.username}</span>
                        <span style="font-size: 11px; color: var(--text-secondary);">ID: #${Math.floor(Math.random()*1000)}</span>
                    </div>
                </div>
            </td>
            <td style="font-weight: 600;">${user.count} <span style="font-size: 11px; color: var(--text-secondary); font-weight: 400;">emails</span></td>
            <td>
                <div style="font-size:12px; margin-bottom:4px; display: flex; justify-content: space-between;">
                    <span>${usedMB} Mo</span>
                    <span style="color: var(--text-secondary);">${usedPct}%</span>
                </div>
                <div class="quota-bar">
                    <div class="quota-fill" style="width: ${usedPct}%; background: ${usedPct > 90 ? 'var(--danger)' : ''}"></div>
                </div>
            </td>
            <td style="color: var(--accent-blue); font-weight: 600;">${limitMB} Mo</td>
            <td>
                <div style="display: flex; gap: 8px;">
                    <button class="btn-action btn-quota" onclick="openQuotaModal('${user.username}', ${limitMB})" title="Modifier Quota">
                        <i class="fas fa-edit"></i>
                    </button>
                    <button class="btn-action btn-delete" onclick="deleteUser('${user.username}')" title="Supprimer">
                        <i class="fas fa-trash"></i>
                    </button>
                </div>
            </td>
        `;
        tbody.appendChild(row);
    });
}

async function loadCluster() {
    try {
        const res = await fetch(`${API_URL}/admin/cluster`, {
            headers: { 'Authorization': authToken }
        });
        const data = await res.json();
        renderNodes(data.nodes);
    } catch (e) {}
}

function renderNodes(nodes) {
    const containers = [document.getElementById('cluster-preview'), document.getElementById('nodes-detailed')];
    containers.forEach(container => {
        container.innerHTML = '';
        nodes.forEach(node => {
            const card = document.createElement('div');
            card.className = "node-card glass-panel";
            card.innerHTML = `
                <div class="node-header">
                    <span style="font-weight:600;">${node.name}</span>
                    <span class="status-badge ${node.status === 'Online' ? 'status-online' : 'status-offline'}">
                        ${node.status}
                    </span>
                </div>
                <div style="font-size:13px; color:var(--text-secondary);">
                    Latence: ${node.latency}<br>
                    Charge: ${Math.floor(Math.random() * 20)}%
                </div>
            `;
            container.appendChild(card);
        });
    });
}

let currentUserForQuota = '';

function openQuotaModal(username, currentLimit) {
    currentUserForQuota = username;
    document.getElementById('modal-username').textContent = username;
    document.getElementById('quota-input').value = currentLimit;
    document.getElementById('quota-modal').style.display = 'flex';
}

function closeModal() {
    document.getElementById('quota-modal').style.display = 'none';
}

async function applyQuota() {
    const limitMB = parseInt(document.getElementById('quota-input').value);
    if (!limitMB || limitMB <= 0) return;
    
    const limitBytes = limitMB * 1024 * 1024;
    
    try {
        const res = await fetch(`${API_URL}/admin/users/${currentUserForQuota}/quota`, {
            method: 'POST',
            headers: { 
                'Authorization': authToken,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ limit: limitBytes })
        });
        if (res.ok) {
            closeModal();
            loadUsers();
        }
    } catch (e) {}
}

async function deleteUser(username) {
    if (!confirm(`Etes-vous sûr de vouloir supprimer l'utilisateur ${username} ?`)) return;
    try {
        const res = await fetch(`${API_URL}/admin/users/${username}/delete`, {
            method: 'POST',
            headers: { 'Authorization': authToken }
        });
        if (res.ok) {
            loadUsers();
        }
    } catch (e) {}
}

async function sendBroadcast() {
    const subject = document.getElementById('broadcast-subject').value;
    const content = document.getElementById('broadcast-content').value;
    
    if (!subject || !content) {
        alert("Veuillez remplir tous les champs");
        return;
    }
    
    try {
        const res = await fetch(`${API_URL}/admin/broadcast`, {
            method: 'POST',
            headers: { 
                'Authorization': authToken,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ subject, content })
        });
        
        if (res.ok) {
            alert("Message diffusé avec succès !");
            document.getElementById('broadcast-subject').value = '';
            document.getElementById('broadcast-content').value = '';
            switchTab('dashboard');
        } else {
            alert("Erreur lors de la diffusion");
        }
    } catch (e) {
        console.error(e);
    }
}

function switchTab(tabId) {
    document.querySelectorAll('section').forEach(s => s.style.display = 'none');
    document.getElementById(`tab-${tabId}`).style.display = 'block';
    
    document.querySelectorAll('.nav-item').forEach(i => i.classList.remove('active'));
    // Find the clicked item
    const clickedItem = Array.from(document.querySelectorAll('.nav-item')).find(i => i.getAttribute('onclick')?.includes(`'${tabId}'`));
    if (clickedItem) clickedItem.classList.add('active');
}

// Search Logic
document.querySelector('.search-box input')?.addEventListener('input', (e) => {
    const term = e.target.value.toLowerCase();
    const rows = document.querySelectorAll('#users-table-body tr');
    rows.forEach(row => {
        const username = row.querySelector('span').textContent.toLowerCase();
        row.style.display = username.includes(term) ? '' : 'none';
    });
});

function logout() {
    localStorage.removeItem('auth_token');
    localStorage.removeItem('auth_user');
    window.location.href = "/";
}

// --- Analytics & Charts ---
let storageChart = null;
let trafficChart = null;
let activityChart = null;

function initCharts() {
    const ctxStorage = document.getElementById('storageChart').getContext('2d');
    storageChart = new Chart(ctxStorage, {
        type: 'bar',
        data: { labels: [], datasets: [{ label: 'Stockage (Mo)', data: [], backgroundColor: '#38bdf8' }] },
        options: { responsive: true, maintainAspectRatio: false, plugins: { legend: { display: false } } }
    });

    const ctxTraffic = document.getElementById('trafficChart').getContext('2d');
    trafficChart = new Chart(ctxTraffic, {
        type: 'doughnut',
        data: { labels: ['Node 1', 'Node 2', 'Node 3'], datasets: [{ data: [30, 45, 25], backgroundColor: ['#38bdf8', '#818cf8', '#fbbf24'] }] },
        options: { responsive: true, maintainAspectRatio: false, cutout: '70%' }
    });

    const ctxActivity = document.getElementById('activityChart').getContext('2d');
    activityChart = new Chart(ctxActivity, {
        type: 'line',
        data: { 
            labels: ['Lun', 'Mar', 'Mer', 'Jeu', 'Ven', 'Sam', 'Dim'], 
            datasets: [{ 
                label: 'Emails traités', 
                data: [120, 190, 300, 250, 400, 150, 80], 
                borderColor: '#818cf8', 
                tension: 0.4, 
                fill: true, 
                backgroundColor: 'rgba(129, 140, 248, 0.1)' 
            }] 
        },
        options: { responsive: true, maintainAspectRatio: false }
    });
}

function updateCharts(users) {
    if (!storageChart) return;
    
    // Top 5 storage users
    const topUsers = [...users].sort((a, b) => b.size - a.size).slice(0, 5);
    storageChart.data.labels = topUsers.map(u => u.username);
    storageChart.data.datasets[0].data = topUsers.map(u => (u.size / (1024 * 1024)).toFixed(2));
    storageChart.update();

    // Randomize traffic for effect
    trafficChart.data.datasets[0].data = trafficChart.data.datasets[0].data.map(v => Math.max(10, v + (Math.random() * 10 - 5)));
    trafficChart.update();
}

// Initial Load
initCharts();
loadData();
setInterval(loadCluster, 5000);
setInterval(() => { if (storageChart) updateCharts([]); }, 3000); // Pulse effect
