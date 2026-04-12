const API_URL = '/api';
let authToken = localStorage.getItem('auth_token');
let currentMessages = [];
let selectedMessageId = null;

// Initialisation
if (authToken) {
    showApp();
    loadInbox();
}

// --- Dark Mode ---
function toggleDarkMode() {
    document.body.classList.toggle('dark-theme');
    const isDark = document.body.classList.contains('dark-theme');
    localStorage.setItem('dark_mode', isDark);
    updateThemeIcon(isDark);
}

function updateThemeIcon(isDark) {
    const btnIcon = document.querySelector('#dark-mode-btn i');
    if (btnIcon) {
        btnIcon.className = isDark ? 'fas fa-sun' : 'fas fa-moon';
    }
}

if (localStorage.getItem('dark_mode') === 'true') {
    document.body.classList.add('dark-theme');
    setTimeout(() => updateThemeIcon(true), 100);
}

// --- UI Feedback (Toasts) ---
function showToast(message, type = 'info') {
    const container = document.getElementById('toast-container');
    const toast = document.createElement('div');
    toast.className = `animate__animated animate__fadeInRight`;
    
    // Style compact et premium
    const colors = {
        success: '#1e8e3e',
        error: '#d93025',
        info: '#3c4043'
    };
    
    toast.style.cssText = `
        background: ${colors[type] || colors.info};
        color: white;
        padding: 12px 24px;
        border-radius: 8px;
        font-size: 14px;
        font-weight: 500;
        box-shadow: 0 4px 12px rgba(0,0,0,0.2);
        pointer-events: auto;
        min-width: 250px;
        text-align: left;
        border-left: 4px solid rgba(255,255,255,0.3);
    `;
    
    toast.textContent = message;
    container.appendChild(toast);
    
    setTimeout(() => {
        toast.classList.replace('animate__fadeInRight', 'animate__fadeOutRight');
        setTimeout(() => toast.remove(), 1000);
    }, 4000);
}

// --- Navigation Utilities ---
function hideAllSections() {
    const sections = ['inbox-section', 'dashboard-section', 'viewer-section', 'settings-section'];
    sections.forEach(id => {
        const el = document.getElementById(id);
        if (el) el.style.display = 'none';
    });
}

function showSection(id) {
    hideAllSections();
    const el = document.getElementById(id);
    el.style.display = 'flex';
    el.classList.remove('animate__animated', 'animate__fadeIn');
    void el.offsetWidth; // trigger reflow
    el.classList.add('animate__animated', 'animate__fadeIn');
}

function showInbox() {
    showSection('inbox-section');
    updateNavSelection(1);
    selectedMessageId = null;
    loadInbox();
}

function showDashboard() {
    showSection('dashboard-section');
    updateNavSelection(0);
    loadDashboardStats();
}

function showSettings() {
    showSection('settings-section');
    updateNavSelection(2);
}

function updateNavSelection(index) {
    document.querySelectorAll('.nav-link').forEach((l, i) => {
        if (i === index) l.classList.add('active');
        else l.classList.remove('active');
    });
}

function showViewer() {
    showSection('viewer-section');
}

// --- Internationalization (i18n) ---
const translations = {
    fr: {
        "txt-brand-mail": "Mail",
        "txt-brand-mail-login": "Mail",
        "txt-new": "Nouveau",
        "txt-nav-dashboard": "Tableau de bord",
        "txt-nav-inbox": "Boîte de réception",
        "txt-nav-settings": "Paramètres",
        "txt-nav-fav": "Favoris",
        "txt-nav-sent": "Envoyés",
        "txt-nav-trash": "Corbeille",
        "txt-nav-logout": "Déconnexion",
        "txt-login-welcome": "Bienvenue",
        "txt-login-desc": "Connectez-vous à votre portail sécurisé",
        "txt-login-user": "Nom d'utilisateur",
        "txt-login-pass": "Mot de passe",
        "txt-btn-login": "Connexion",
        "txt-dash-title": "Aperçu du Compte",
        "txt-dash-activity": "Volume d'emails (7 derniers jours)",
        "txt-dash-storage": "Utilisation Stockage",
        "txt-dash-stats": "Statistiques Rapides",
        "txt-stat-total": "Total Emails",
        "txt-stat-unread": "Non lus",
        "txt-set-title": "Paramètres du Compte",
        "txt-set-name": "Nom d'affichage",
        "txt-set-lang": "Langue de l'interface",
        "txt-set-save": "Enregistrer les modifications",
        "txt-set-dark": "Bascule Mode Sombre",
        "txt-inbox-title": "Boîte de réception",
        "search-input": "Rechercher des emails ou des contacts..."
    },
    en: {
        "txt-brand-mail": "Mail",
        "txt-brand-mail-login": "Mail",
        "txt-new": "New Message",
        "txt-nav-dashboard": "Dashboard",
        "txt-nav-inbox": "Inbox",
        "txt-nav-settings": "Settings",
        "txt-nav-fav": "Favorites",
        "txt-nav-sent": "Sent",
        "txt-nav-trash": "Trash",
        "txt-nav-logout": "Logout",
        "txt-login-welcome": "Welcome Back",
        "txt-login-desc": "Log in to your secure portal",
        "txt-login-user": "Username",
        "txt-login-pass": "Password",
        "txt-btn-login": "Login",
        "txt-dash-title": "Account Overview",
        "txt-dash-activity": "Email Volume (Last 7 Days)",
        "txt-dash-storage": "Storage Usage",
        "txt-dash-stats": "Quick Stats",
        "txt-stat-total": "Total Emails",
        "txt-stat-unread": "Unread",
        "txt-set-title": "Account Settings",
        "txt-set-name": "Display Name",
        "txt-set-lang": "Interface Language",
        "txt-set-save": "Save Changes",
        "txt-set-dark": "Toggle Dark Mode",
        "txt-inbox-title": "Inbox",
        "search-input": "Search emails or contacts..."
    },
    ar: {
        "txt-brand-mail": "بريد",
        "txt-brand-mail-login": "بريد",
        "txt-new": "رسالة جديدة",
        "txt-nav-dashboard": "لوحة القيادة",
        "txt-nav-inbox": "صندوق الوارد",
        "txt-nav-settings": "الإعدادات",
        "txt-nav-fav": "المفضلة",
        "txt-nav-sent": "المرسل",
        "txt-nav-trash": "المحذوفات",
        "txt-nav-logout": "تسجيل الخروج",
        "txt-login-welcome": "مرحباً بك",
        "txt-login-desc": "سجل الدخول إلى بوابتك الآمنة",
        "txt-login-user": "اسم المستخدم",
        "txt-login-pass": "كلمة المرور",
        "txt-btn-login": "دخول",
        "txt-dash-title": "نظرة عامة على الحساب",
        "txt-dash-activity": "حجم الرسائل (آخر 7 أيام)",
        "txt-dash-storage": "استخدام المساحة",
        "txt-dash-stats": "إحصائيات سريعة",
        "txt-stat-total": "إجمالي الرسائل",
        "txt-stat-unread": "غير مقروءة",
        "txt-set-title": "إعدادات الحساب",
        "txt-set-name": "الاسم المستعار",
        "txt-set-lang": "لغة الواجهة",
        "txt-set-save": "حفظ التغييرات",
        "txt-set-dark": "تبديل الوضع الليلي",
        "txt-inbox-title": "صندوق الوارد",
        "search-input": "ابحث في الرسائل أو جهات الاتصال..."
    }
};

let currentLang = localStorage.getItem('app_lang') || 'fr';

function translateUI() {
    const langData = translations[currentLang];
    for (let id in langData) {
        const el = document.getElementById(id);
        if (el) {
            if (el.tagName === 'INPUT') el.placeholder = langData[id];
            else el.textContent = langData[id];
        }
    }
    // Handle RTL for Arabic
    document.body.dir = (currentLang === 'ar') ? 'rtl' : 'ltr';
    document.getElementById('language-select').value = currentLang;
}

function changeLanguage(lang) {
    currentLang = lang;
    localStorage.setItem('app_lang', lang);
    translateUI();
    showToast(currentLang === 'ar' ? 'تم تغيير اللغة' : (currentLang === 'en' ? 'Language updated' : 'Langue mise à jour'), 'success');
}

function saveSettings() {
    const name = document.getElementById('settings-display-name').value;
    localStorage.setItem('display_name', name);
    showToast(currentLang === 'ar' ? 'تم حفظ الإعدادات' : (currentLang === 'en' ? 'Settings saved' : 'Paramètres enregistrés'), 'success');
}

translateUI();

// --- Auth ---
document.getElementById('login-form').addEventListener('submit', async (e) => {
    e.preventDefault();
    const user = document.getElementById('username').value;
    const pass = document.getElementById('password').value;
    const errorEl = document.getElementById('login-error');
    const btn = e.target.querySelector('button');

    btn.textContent = 'Connexion encours...';
    btn.disabled = true;

    try {
        const response = await fetch(`${API_URL}/login`, {
            method: 'POST',
            body: JSON.stringify({ username: user, password: pass })
        });
        const data = await response.json();

        if (data.token) {
            authToken = data.token;
            localStorage.setItem('auth_token', authToken);
            localStorage.setItem('auth_user', user);
            showApp();
            loadInbox();
            showToast(`Bienvenue, ${user}`, 'success');
        } else {
            errorEl.textContent = data.error || 'Identifiants invalides';
            errorEl.style.display = 'block';
            showToast('Échec de la connexion', 'error');
        }
    } catch (err) {
        errorEl.textContent = 'Erreur de communication avec le serveur.';
        errorEl.style.display = 'block';
    } finally {
        btn.textContent = 'Connexion';
        btn.disabled = false;
    }
});

function logout() {
    localStorage.removeItem('auth_token');
    localStorage.removeItem('auth_user');
    location.reload();
}

function showApp() {
    document.getElementById('login-page').style.display = 'none';
    const user = localStorage.getItem('auth_user') || 'U';
    document.getElementById('user-initials').textContent = user.charAt(0).toUpperCase();
}

// --- Email Logic ---
// --- Search Logic ---
document.querySelector('.search-bar input').addEventListener('input', (e) => {
    const term = e.target.value.toLowerCase();
    const filtered = currentMessages.filter(msg => 
        msg.id.toString().includes(term) || 
        (msg.size && msg.size.toString().includes(term))
    );
    
    if (filtered.length === 0 && term !== "") {
        document.getElementById('messages-container').innerHTML = `
            <div class="empty-state animate__animated animate__fadeIn">
                <i class="fas fa-search"></i>
                <h3>Aucun résultat</h3>
                <p>Nous n'avons rien trouvé pour "${term}".</p>
            </div>
        `;
    } else {
        renderMessageList(filtered);
    }
});

async function loadInbox() {
    const container = document.getElementById('messages-container');
    
    try {
        container.innerHTML = Array(5).fill(0).map(() => `
            <div class="email-item skeleton-shimmer">
                <div class="skeleton" style="width:24px; height:24px; border-radius:4px"></div>
                <div class="skeleton" style="width:120px; height:16px; margin-left:10px"></div>
                <div class="skeleton" style="flex:1; height:16px; margin:0 20px"></div>
                <div class="skeleton" style="width:60px; height:16px"></div>
            </div>
        `).join('');
        
        const response = await fetch(`${API_URL}/inbox`, {
            headers: { 'Authorization': authToken }
        });

        if (response.status === 401) return logout();

        currentMessages = await response.json();
        renderMessageList();
    } catch (err) {
        container.innerHTML = `
            <div class="empty-state animate__animated animate__fadeIn">
                <i class="fas fa-exclamation-triangle" style="color:var(--error); opacity:1"></i>
                <h3>Erreur de synchronisation</h3>
                <p>Impossible de joindre les serveurs de messagerie.</p>
                <button class="btn-send" style="margin-top:20px; background:var(--bg-main); color:var(--text-main)" onclick="loadInbox()">Réessayer</button>
            </div>
        `;
    }
}

function renderMessageList(messagesToRender = currentMessages) {
    const container = document.getElementById('messages-container');
    container.innerHTML = '';

    if (messagesToRender.length === 0) {
        container.innerHTML = `
            <div class="empty-state animate__animated animate__fadeIn">
                <i class="fas fa-envelope-open-text" style="font-size: 80px"></i>
                <h3>Votre boîte est vide</h3>
                <p>C'est le moment idéal pour démarrer une nouvelle conversation.</p>
            </div>
        `;
        return;
    }

    messagesToRender.forEach((msg, idx) => {
        const item = document.createElement('div');
        item.className = `email-item animate__animated animate__fadeInUp`;
        item.style.animationDelay = `${idx * 0.05}s`;
        
        item.innerHTML = `
            <div class="email-checkbox"><i class="far fa-square"></i></div>
            <div class="email-sender">Support Système #${msg.id}</div>
            <div class="email-summary">
                <span class="email-subject">Email Distribué #${msg.id}</span>
                <span style="color: var(--text-secondary)"> — Taille : ${msg.size} octets</span>
            </div>
            <div class="email-time">${new Date().toLocaleTimeString('fr-FR', {hour: '2-digit', minute:'2-digit'})}</div>
            <div class="email-actions">
                <div class="icon-btn" title="Archiver"><i class="fas fa-archive"></i></div>
                <div class="icon-btn" title="Supprimer" onclick="event.stopPropagation(); deleteMessageDirectly(${msg.id})">
                    <i class="far fa-trash-alt"></i>
                </div>
                <div class="icon-btn" title="Marquer comme lu"><i class="far fa-envelope-open"></i></div>
            </div>
        `;
        item.onclick = () => selectMessage(msg.id);
        container.appendChild(item);
    });
}

async function selectMessage(id) {
    selectedMessageId = id;
    showViewer();

    const bodyEl = document.getElementById('view-body');
    const subjectEl = document.getElementById('view-subject');
    const fromEl = document.getElementById('view-from');
    const avatarEl = document.getElementById('view-avatar');

    bodyEl.innerHTML = '<div style="padding:40px; text-align:center;"><i class="fas fa-circle-notch fa-spin fa-2x"></i></div>';
    subjectEl.textContent = 'Chargement...';

    try {
        const response = await fetch(`${API_URL}/messages/${id}`, {
            headers: { 'Authorization': authToken }
        });
        const data = await response.json();

        subjectEl.textContent = `Email de service #${id}`;
        fromEl.textContent = `Serveur de Messagerie #${id}`;
        avatarEl.textContent = id.toString().charAt(0).toUpperCase();
        bodyEl.innerHTML = `<div style="white-space: pre-wrap;">${data.body}</div>`;
    } catch (err) {
        bodyEl.textContent = 'Erreur lors de la récupération du contenu.';
    }
}

async function deleteCurrentMessage() {
    if (!selectedMessageId) return;
    if (!confirm('Souhaitez-vous supprimer définitivement ce message ?')) return;

    try {
        const response = await fetch(`${API_URL}/messages/${selectedMessageId}/delete`, {
            method: 'POST',
            headers: { 'Authorization': authToken }
        });
        
        if (response.ok) {
            showToast('Message supprimé', 'info');
            showInbox();
        } else {
            showToast('Impossible de supprimer le message', 'error');
        }
    } catch (err) {
        showToast('Erreur lors de la suppression', 'error');
    }
}

async function deleteMessageDirectly(id) {
    if (!confirm(`Supprimer le message #${id} ?`)) return;
    try {
        const response = await fetch(`${API_URL}/messages/${id}/delete`, {
            method: 'POST',
            headers: { 'Authorization': authToken }
        });
        if (response.ok) {
            showToast('Message supprimé', 'info');
            loadInbox();
        }
    } catch (err) {
        showToast('Erreur lors de la suppression rapide', 'error');
    }
}

// --- Compose ---
function openCompose() {
    document.getElementById('compose-overlay').style.display = 'flex';
}

function closeCompose() {
    document.getElementById('compose-overlay').style.display = 'none';
}

document.getElementById('send-form').addEventListener('submit', async (e) => {
    e.preventDefault();
    const btn = e.target.querySelector('.btn-send');
    const originalText = btn.textContent;
    btn.disabled = true;
    btn.textContent = 'Envoi en cours...';

    const payload = {
        to: document.getElementById('send-to').value,
        subject: document.getElementById('send-subject').value,
        content: document.getElementById('send-content').value
    };

    try {
        const response = await fetch(`${API_URL}/send`, {
            method: 'POST',
            headers: { 
                'Authorization': authToken,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(payload)
        });

        if (response.ok) {
            closeCompose();
            e.target.reset();
            showToast('Message envoyé !', 'success');
        } else {
            showToast('Échec de l\'envoi SMTP', 'error');
        }
    } catch (err) {
        showToast('Erreur lors de l\'envoi', 'error');
    } finally {
        btn.disabled = false;
        btn.textContent = originalText;
    }
});

// --- Dashboard Stats & Charts ---
let activityChart = null;
let storageChart = null;

async function loadDashboardStats() {
    try {
        const response = await fetch(`${API_URL}/stats`, {
            headers: { 'Authorization': authToken }
        });
        const stats = await response.json();

        // Update Text Stats
        document.getElementById('stat-total-emails').textContent = stats.totalEmails;
        document.getElementById('stat-unread-emails').textContent = stats.unreadEmails;
        
        const sizeMB = (stats.totalSize / (1024 * 1024)).toFixed(2);
        const limitMB = (stats.storageLimit / (1024 * 1024)).toFixed(0);
        document.getElementById('storageText').textContent = `${sizeMB} Mo sur ${limitMB} Mo utilisés`;

        initCharts(stats);
    } catch (err) {
        console.error('Erreur Stats:', err);
    }
}

function initCharts(stats) {
    const ctxActivity = document.getElementById('activityChart').getContext('2d');
    const ctxStorage = document.getElementById('storageChart').getContext('2d');

    if (activityChart) activityChart.destroy();
    if (storageChart) storageChart.destroy();

    activityChart = new Chart(ctxActivity, {
        type: 'line',
        data: {
            labels: ['Lun', 'Mar', 'Mer', 'Jeu', 'Ven', 'Sam', 'Dim'],
            datasets: [{
                label: 'Emails',
                data: stats.weeklyActivity || [0,0,0,0,0,0,0],
                borderColor: '#1a73e8',
                backgroundColor: 'rgba(26, 115, 232, 0.1)',
                fill: true,
                tension: 0.4
            }]
        },
        options: {
            plugins: { legend: { display: false } },
            scales: { y: { beginAtZero: true } }
        }
    });

    storageChart = new Chart(ctxStorage, {
        type: 'doughnut',
        data: {
            labels: ['Utilisé', 'Libre'],
            datasets: [{
                data: [stats.totalSize, stats.storageLimit - stats.totalSize],
                backgroundColor: ['#1a73e8', '#e8f0fe'],
                borderWidth: 0
            }]
        },
        options: {
            cutout: '80%',
            plugins: { legend: { display: false } }
        }
    });
}

