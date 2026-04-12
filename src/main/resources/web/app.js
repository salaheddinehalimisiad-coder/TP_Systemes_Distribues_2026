const API_URL = '/api';
let authToken = localStorage.getItem('auth_token');
let currentMessages = [];
let currentCategory = 'primary';
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
    const sections = ['inbox-section', 'dashboard-section', 'viewer-section', 'settings-section', 'contacts-section', 'admin-section'];
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
    setActiveNav('nav-inbox');
    selectedMessageId = null;
    loadInbox();
    // Clear notification dot
    const dot = document.querySelector('#nav-inbox .nav-dot');
    if (dot) dot.remove();
}

function showDashboard() {
    showSection('dashboard-section');
    setActiveNav('nav-dashboard');
    loadDashboardStats();
}

function showSettings() {
    showSection('settings-section');
    setActiveNav('nav-settings');
}

function showAdmin() {
    showSection('admin-section');
    setActiveNav('nav-admin');
    showAdminTab('users');
}

function showAdminTab(tab) {
    document.getElementById('admin-users-tab').style.display = tab === 'users' ? 'block' : 'none';
    document.getElementById('admin-cluster-tab').style.display = tab === 'cluster' ? 'block' : 'none';
    
    if (tab === 'users') loadAdminUsers();
    else if (tab === 'cluster') loadClusterStatus();
}

async function loadClusterStatus() {
    const list = document.getElementById('cluster-node-list');
    list.innerHTML = '<div style="padding:20px;"><i class="fas fa-circle-notch fa-spin"></i> Audit du cluster en cours...</div>';
    try {
        const res = await fetch(`${API_URL}/admin/cluster`, { headers: { 'Authorization': authToken } });
        const data = await res.json();
        
        document.getElementById('current-node-name').textContent = data.currentNode;
        
        list.innerHTML = data.nodes.map(n => `
            <div style="background:var(--bg-white); border:1px solid var(--border-color); border-radius:12px; padding:20px; transition:var(--transition); box-shadow:0 4px 12px rgba(0,0,0,0.05);">
                <div style="display:flex; justify-content:space-between; margin-bottom:12px;">
                    <span style="font-weight:700;">${n.name}</span>
                    <span class="admin-badge ${n.status === 'Online' ? 'badge-active' : ''}" style="background:${n.status==='Online'?'#e6f4ea':'#fce8e6'}; color:${n.status==='Online'?'#1e8e3e':'#d93025'};">${n.status}</span>
                </div>
                <div style="font-size:12px; color:var(--text-secondary); display:flex; gap:16px;">
                    <span><i class="fas fa-tachometer-alt"></i> ${n.latency}</span>
                    <span><i class="fas fa-server"></i> REST API: 8080</span>
                </div>
            </div>
        `).join('');
    } catch (err) {
        list.innerHTML = '<div style="color:var(--error); padding:20px;">Échec de la communication avec le contrôleur de cluster.</div>';
    }
}

function showContacts() {
    showSection('contacts-section');
    setActiveNav('nav-fav');
    loadContacts();
}

function updateNavSelection(navId) {
    document.querySelectorAll('.nav-link').forEach(l => {
        if (l.id === navId || (l.getAttribute('onclick') && l.getAttribute('onclick').includes(navId))) {
             l.classList.add('active');
        } else {
             l.classList.remove('active');
        }
    });
}
// Specific nav update helper
function setActiveNav(id) {
    document.querySelectorAll('.nav-link').forEach(l => l.classList.remove('active'));
    const el = document.getElementById(id);
    if (el) el.classList.add('active');
}

function showViewer() {
    showSection('viewer-section');
}

function showContacts() {
    showSection('contacts-section');
    updateNavSelection(3);
    loadContacts();
}

function showSent() {
    // Sent emails not stored separately - show info toast
    showToast(currentLang === 'ar' ? 'صندوق المرسل سيتم دعمه قريباً' :
        (currentLang === 'en' ? 'Sent folder coming soon (IMAP)' : 'Dossier Envoyés : bientôt disponible (IMAP)'), 'info');
    showInbox();
}

async function loadContacts() {
    const list = document.getElementById('contacts-list');
    list.innerHTML = '<div style="padding:20px; color:var(--text-secondary);"><i class="fas fa-circle-notch fa-spin"></i> Chargement...</div>';
    try {
        const res = await fetch(`${API_URL}/contacts`, { headers: { 'Authorization': authToken } });
        if (res.status === 401) return logout();
        const contacts = await res.json();
        list.innerHTML = contacts.map(c => `
            <div style="background:var(--bg-main); border:1px solid var(--border-color); border-radius:12px; padding:20px; display:flex; align-items:center; gap:14px; cursor:pointer; transition:var(--transition);" 
                 onmouseover="this.style.borderColor='var(--primary)'" onmouseout="this.style.borderColor='var(--border-color)'"
                 onclick="composeToContact('${c.email}')">
                <div style="width:48px;height:48px;background:linear-gradient(135deg,#1a73e8,#9333ea);border-radius:50%;display:flex;align-items:center;justify-content:center;color:white;font-weight:700;font-size:18px;flex-shrink:0;">${c.initials}</div>
                <div>
                    <div style="font-weight:600;">${c.username}</div>
                    <div style="font-size:12px;color:var(--text-secondary);">${c.email}</div>
                </div>
                <div style="margin-left:auto;"><i class="fas fa-paper-plane" style="color:var(--primary);"></i></div>
            </div>
        `).join('');
        if (contacts.length === 0) {
            list.innerHTML = '<p style="color:var(--text-secondary); padding:20px;">Aucun contact trouvé.</p>';
        }
    } catch (err) {
        list.innerHTML = '<p style="color:var(--error); padding:20px;">Erreur de chargement des contacts.</p>';
    }
}

function composeToContact(email) {
    openCompose();
    document.getElementById('send-to').value = email;
    document.getElementById('send-subject').focus();
}

function showAdmin() {
    showSection('admin-section');
    updateNavSelection(4); // Or appropriate index
    loadAdminUsers();
}

async function loadAdminUsers() {
    const list = document.getElementById('admin-user-list');
    list.innerHTML = '<tr><td colspan="4" style="padding:20px; text-align:center;"><i class="fas fa-circle-notch fa-spin"></i> Chargement...</td></tr>';
    try {
        const res = await fetch(`${API_URL}/admin/users`, { headers: { 'Authorization': authToken } });
        if (res.status === 401) return logout();
        if (res.status === 403) {
            showToast('Accès refusé', 'error');
            return showDashboard();
        }
        const users = await res.json();
        list.innerHTML = users.map(u => `
            <tr style="border-bottom:1px solid var(--border-color);">
                <td style="padding:16px; font-weight:600;">${u.username}</td>
                <td style="padding:16px;">${u.mail_count || u.count}</td>
                <td style="padding:16px;">${( (u.storage_used || u.size) / 1024).toFixed(2)} KB</td>
                <td style="padding:16px;">
                    <button class="icon-btn" style="color:var(--error);" title="Supprimer (Bientôt)"><i class="fas fa-trash-alt"></i></button>
                    <button class="icon-btn" style="color:var(--primary);" title="Stats"><i class="fas fa-chart-line"></i></button>
                </td>
            </tr>
        `).join('');
    } catch (err) {
        list.innerHTML = '<tr><td colspan="4" style="padding:20px; text-align:center; color:var(--error);">Erreur de chargement.</td></tr>';
    }
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
        "txt-nav-admin": "Administration",
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
        "search-input": "Rechercher des emails ou des contacts...",
        "txt-view-to": "à",
        "txt-view-me": "moi",
        "txt-compose-title": "Nouveau message",
        "send-to": "À :",
        "send-subject": "Objet",
        "send-content": "Rédiger votre message...",
        "txt-btn-send": "Envoyer le message",
        "txt-email-service": "Email de service",
        "txt-mail-server": "Serveur de Messagerie",
        "txt-delete-confirm": "Souhaitez-vous supprimer ce message ?",
        "txt-system-support": "Support Système",
        "txt-msg-dist": "Email Distribué",
        "txt-size": "Taille",
        "txt-bytes": "octets",
        "txt-storage-of": "sur",
        "txt-storage-used": "utilisés",
        "txt-empty-title": "Votre boîte est vide",
        "txt-empty-desc": "C'est le moment idéal pour démarrer une nouvelle conversation.",
        "msg-deleted": "Message supprimé",
        "msg-delete-fail": "Impossible de supprimer le message",
        "msg-send-success": "Message envoyé !",
        "msg-send-fail": "Échec de l'envoi SMTP"
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
        "search-input": "Search emails or contacts...",
        "txt-view-to": "to",
        "txt-view-me": "me",
        "txt-compose-title": "New Message",
        "send-to": "To:",
        "send-subject": "Subject",
        "send-content": "Type your message here...",
        "txt-btn-send": "Send Message",
        "txt-email-service": "Service Email",
        "txt-mail-server": "Mail Server",
        "txt-delete-confirm": "Do you want to delete this message?",
        "txt-system-support": "System Support",
        "txt-msg-dist": "Distributed Email",
        "txt-size": "Size",
        "txt-bytes": "bytes",
        "txt-storage-of": "of",
        "txt-storage-used": "used",
        "txt-empty-title": "Your inbox is empty",
        "txt-empty-desc": "Perfect time to start a new conversation.",
        "msg-deleted": "Message deleted",
        "msg-delete-fail": "Could not delete",
        "msg-send-success": "Message sent!",
        "msg-send-fail": "SMTP send failed"
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
        "search-input": "ابحث في الرسائل أو جهات الاتصال...",
        "txt-view-to": "إلى",
        "txt-view-me": "أنا",
        "txt-compose-title": "رسالة جديدة",
        "send-to": "إلى:",
        "send-subject": "الموضوع",
        "send-content": "اكتب رسالتك هنا...",
        "txt-btn-send": "إرسال الرسالة",
        "txt-email-service": "بريد الخدمة",
        "txt-mail-server": "خادم البريد",
        "txt-delete-confirm": "هل تريد حذف هذه الرسالة؟",
        "txt-system-support": "دعم النظام",
        "txt-msg-dist": "رسالة موزعة",
        "txt-size": "الحجم",
        "txt-bytes": "بايت",
        "txt-storage-of": "من",
        "txt-storage-used": "مستخدمة",
        "txt-empty-title": "صندوق الوارد فارغ",
        "txt-empty-desc": "الوقت مثالي لبدء محادثة جديدة.",
        "msg-deleted": "تم حذف الرسالة",
        "msg-delete-fail": "تعذر الحذف",
        "msg-send-success": "تم الإرسال!",
        "msg-send-fail": "فشل إرسال SMTP"
    }
};

let currentLang = localStorage.getItem('app_lang') || 'fr';

function translateUI() {
    const langData = translations[currentLang];
    for (let id in langData) {
        const el = document.getElementById(id);
        if (el) {
            if (el.tagName === 'INPUT' || el.tagName === 'TEXTAREA') el.placeholder = langData[id];
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
    
    // Admin specific UI
    if (user === 'admin') {
        const navAdmin = document.getElementById('nav-admin');
        if (navAdmin) navAdmin.style.display = 'flex';
    }

    startInboxPolling();
    initWebSockets();
}

let wsSocket = null;
function initWebSockets() {
    if (wsSocket) wsSocket.close();
    
    const protocol = location.protocol === 'https:' ? 'wss:' : 'ws:';
    wsSocket = new WebSocket(`${protocol}//${location.host}/ws`);
    
    wsSocket.onopen = () => {
        wsSocket.send(JSON.stringify({ type: 'auth', token: authToken }));
    };
    
    wsSocket.onmessage = (event) => {
        const data = JSON.parse(event.data);
        if (data.type === 'new_mail') {
            showToast(`${translations[currentLang]['txt-new']} : ${data.subject}`, 'success');
            // Refresh inbox if currently viewing it
            if (document.getElementById('inbox-section').style.display !== 'none') {
                loadInbox();
            } else {
                // Add a notification dot to inbox nav
                const inboxLink = document.getElementById('nav-inbox');
                if (inboxLink) {
                    let dot = inboxLink.querySelector('.nav-dot');
                    if (!dot) {
                        dot = document.createElement('span');
                        dot.className = 'nav-dot';
                        dot.style.cssText = 'width:8px; height:8px; background:#1a73e8; border-radius:50%; margin-left:8px;';
                        inboxLink.appendChild(dot);
                    }
                }
            }
            // Update badge immediately
            pollInboxCount();
            
            // Notification sonore discrète
            new Audio('https://assets.mixkit.co/active_storage/sfx/2358/2358-preview.mp3').play().catch(() => {});
        }
    };
    
    wsSocket.onclose = () => {
        setTimeout(initWebSockets, 5000); // Reconnect
    };
}

let _pollInterval = null;
function startInboxPolling() {
    if (_pollInterval) return;
    pollInboxCount();
    _pollInterval = setInterval(pollInboxCount, 30000);
}

async function pollInboxCount() {
    if (!authToken) return;
    try {
        const res = await fetch(API_URL + '/inbox/count', { headers: { 'Authorization': authToken } });
        if (res.status === 401) return;
        const data = await res.json();
        const badge = document.getElementById('inbox-badge');
        if (badge) {
            if (data.count > 0) {
                badge.textContent = data.count;
                badge.style.display = 'inline-block';
            } else {
                badge.style.display = 'none';
            }
        }
    } catch (_) {}
}

// --- Email Logic ---
// --- Search Logic ---
document.querySelector('.search-bar input').addEventListener('input', (e) => {
    const term = e.target.value.toLowerCase();
    const filtered = currentMessages.filter(msg => 
        (msg.id && msg.id.toString().includes(term)) || 
        (msg.subject && msg.subject.toLowerCase().includes(term)) ||
        (msg.from && msg.from.toLowerCase().includes(term)) ||
        (msg.date && msg.date.toLowerCase().includes(term))
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
    // Si on change de catégorie, on ne recharge pas forcément tout, mais ici on le fait pour synchro
    if (!currentMessages.length) {
        container.innerHTML = Array(5).fill(0).map(() => `
            <div class="email-item skeleton-shimmer">
                <div class="skeleton" style="width:24px; height:24px; border-radius:4px"></div>
                <div class="skeleton" style="width:120px; height:16px; margin-left:10px"></div>
                <div class="skeleton" style="flex:1; height:16px; margin:0 20px"></div>
                <div class="skeleton" style="width:60px; height:16px"></div>
            </div>
        `).join('');
    }
    
    try {
        const response = await fetch(`${API_URL}/inbox`, {
            headers: { 'Authorization': authToken }
        });

        if (response.status === 401) return logout();

        currentMessages = await response.json();
        renderMessageList();
    } catch (err) {
        // ... (error handling remains similar)
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

function switchCategory(cat) {
    currentCategory = cat;
    document.querySelectorAll('.category-tab').forEach(t => t.classList.remove('active'));
    document.getElementById(`cat-${cat}`).classList.add('active');
    renderMessageList();
}

function getCategory(msg) {
    const from = (msg.from || "").toLowerCase();
    if (from.match(/facebook|linkedin|twitter|instagram|social|network/)) return 'social';
    if (from.match(/noreply|marketing|newsletter|amazon|google|promo|offer/)) return 'promotions';
    return 'primary';
}

function renderMessageList(messagesToRender = null) {
    const container = document.getElementById('messages-container');
    container.innerHTML = '';

    let list = messagesToRender || currentMessages;
    
    // Filter by category if no custom list (search) is provided
    if (!messagesToRender) {
        list = list.filter(m => getCategory(m) === currentCategory);
    }

    if (list.length === 0) {
        const lang = translations[currentLang];
        const emptyIcons = {
            'primary': 'fa-envelope-open-text',
            'social': 'fa-users',
            'promotions': 'fa-tag'
        };
        container.innerHTML = `
            <div class="empty-state animate__animated animate__fadeIn" style="padding:100px 20px;">
                <div style="background:var(--bg-main); width:120px; height:120px; border-radius:50%; display:flex; align-items:center; justify-content:center; margin:0 auto 24px;">
                    <i class="fas ${emptyIcons[currentCategory] || 'fa-folder-open'}" style="font-size: 48px; color:var(--text-secondary); opacity:0.5;"></i>
                </div>
                <h3>${lang["txt-empty-title"]}</h3>
                <p style="max-width:300px; margin:10px auto;">${lang["txt-empty-desc"]}</p>
            </div>
        `;
        return;
    }

    list.forEach((msg, idx) => {
        const lang = translations[currentLang];
        const item = document.createElement('div');
        item.className = `email-item animate__animated animate__fadeInUp`;
        item.style.animationDelay = `${idx * 0.05}s`;
        
                const senderName = msg.from || `#${msg.id}`;
        const senderInitial = senderName.charAt(0).toUpperCase();
        const displayDate = msg.date ? formatEmailDate(msg.date) : new Date().toLocaleTimeString('fr-FR', {hour: '2-digit', minute:'2-digit'});
        item.innerHTML = `
            <div class="email-avatar-mini" style="width:36px;height:36px;background:linear-gradient(135deg,#1a73e8,#9333ea);border-radius:50%;display:flex;align-items:center;justify-content:center;color:white;font-weight:700;font-size:14px;flex-shrink:0;">${senderInitial}</div>
            <div class="email-sender" style="font-weight:600;">${senderName}</div>
            <div class="email-summary">
                <span class="email-subject">${msg.subject || '(sans objet)'}</span>
            </div>
            <div class="email-time">${displayDate}</div>
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
        
        if (response.status === 401) return logout();
        
        const data = await response.json();
        const lang = translations[currentLang];

                subjectEl.textContent = data.subject || `(sans objet) #${id}`;
        fromEl.textContent = data.from || `Expéditeur #${id}`;
        avatarEl.textContent = (data.from || id.toString()).charAt(0).toUpperCase();
        bodyEl.innerHTML = `<div style="white-space: pre-wrap;">${data.body}</div>`;
        // Hide any previous AI summary box
        document.getElementById('ai-summary-box').style.display = 'none';
    } catch (err) {
        bodyEl.textContent = currentLang === 'en' ? 'Error retrieving content.' : (currentLang === 'ar' ? 'خطأ في استرداد المحتوى' : 'Erreur lors de la récupération du contenu.');
    }
}

async function summarizeCurrentEmail() {
    if (!selectedMessageId) return;
    const btn = document.getElementById('btn-ai-summarize');
    const box = document.getElementById('ai-summary-box');
    const textEl = document.getElementById('ai-summary-text');

    btn.classList.add('loading');
    btn.innerHTML = '<i class="fas fa-circle-notch fa-spin"></i> Analyse...';
    box.style.display = 'block';
    textEl.textContent = 'Analyse IA en cours...';

    try {
        const bodyText = document.getElementById('view-body').innerText || '';
        const response = await fetch(`${API_URL}/ai/summarize`, {
            method: 'POST',
            headers: { 'Authorization': authToken, 'Content-Type': 'application/json' },
            body: JSON.stringify({ text: bodyText })
        });
        const data = await response.json();
        textEl.textContent = data.summary || 'Résumé indisponible.';
        showToast('✨ Résumé IA généré !', 'success');
    } catch (err) {
        textEl.textContent = 'Erreur lors de la connexion au service IA.';
        showToast('Erreur résumé IA', 'error');
    } finally {
        btn.classList.remove('loading');
        btn.innerHTML = '<i class="fas fa-magic"></i> Résumé IA';
    }
}

function replyToCurrentMessage() {
    if (!selectedMessageId) return;
    openCompose();
    const subject = document.getElementById('view-subject').textContent;
    document.getElementById('send-subject').value = `Re: ${subject}`;
    document.getElementById('send-to').focus();
}

async function deleteCurrentMessage() {
    if (!selectedMessageId) return;
    const lang = translations[currentLang];
    if (!confirm(lang["txt-delete-confirm"])) return;

    try {
        const response = await fetch(`${API_URL}/messages/${selectedMessageId}/delete`, {
            method: 'POST',
            headers: { 'Authorization': authToken }
        });
        
        if (response.status === 401) return logout();

        if (response.ok) {
            showToast(currentLang === 'en' ? 'Message deleted' : (currentLang === 'ar' ? 'تم حذف الرسالة' : 'Message supprimé'), 'info');
            showInbox();
        } else {
            showToast(currentLang === 'en' ? 'Could not delete' : (currentLang === 'ar' ? 'تعذر الحذف' : 'Impossible de supprimer le message'), 'error');
        }
    } catch (err) {
        showToast(currentLang === 'en' ? 'Error during deletion' : (currentLang === 'ar' ? 'خطأ أثناء الحذف' : 'Erreur lors de la suppression'), 'error');
    }
}

async function deleteMessageDirectly(id) {
    const lang = translations[currentLang];
    if (!confirm(`${lang["txt-delete-confirm"]} (#${id})`)) return;
    try {
        const response = await fetch(`${API_URL}/messages/${id}/delete`, {
            method: 'POST',
            headers: { 'Authorization': authToken }
        });
        
        if (response.status === 401) return logout();

        if (response.ok) {
            showToast(lang["msg-deleted"], 'info');
            loadInbox();
        } else {
            showToast(lang["msg-delete-fail"], 'error');
        }
    } catch (err) {
        showToast(lang["msg-delete-fail"], 'error');
    }
}

// --- Compose ---
let emailQuill;
document.addEventListener('DOMContentLoaded', () => {
    emailQuill = new Quill('#quill-editor', {
        theme: 'snow',
        placeholder: 'Rédiger votre message...',
        modules: {
            toolbar: [
                ['bold', 'italic', 'underline', 'strike'],
                ['blockquote', 'code-block'],
                [{ 'header': 1 }, { 'header': 2 }],
                [{ 'list': 'ordered'}, { 'list': 'bullet' }],
                [{ 'color': [] }, { 'background': [] }],
                [{ 'align': [] }],
                ['link', 'image'],
                ['clean']
            ]
        }
    });
});

function openCompose() {
    document.getElementById('compose-overlay').style.display = 'flex';
    if(emailQuill) emailQuill.setContents([]);
}

function closeCompose() {
    document.getElementById('compose-overlay').style.display = 'none';
}

let pendingSendTimeout = null;
document.getElementById('send-form').addEventListener('submit', (e) => {
    e.preventDefault();
    const payload = {
        to: document.getElementById('send-to').value,
        subject: document.getElementById('send-subject').value,
        content: emailQuill ? emailQuill.root.innerHTML : ""
    };

    closeCompose();
    const lang = translations[currentLang];
    const undoMsg = currentLang === 'en' ? 'Sending in 5s...' : (currentLang === 'ar' ? 'جاري الإرسال خلال 5 ثوانٍ...' : 'Envoi dans 5s...');
    showToast(undoMsg, 'info');
    
    const undoBtn = document.createElement('button');
    undoBtn.id = 'undo-send-btn';
    undoBtn.innerHTML = `<i class="fas fa-undo"></i> ${currentLang === 'en' ? 'Undo' : (currentLang === 'ar' ? 'تراجع' : 'Annuler')}`;
    undoBtn.style.cssText = 'position:fixed; bottom:30px; left:50%; transform:translateX(-50%); z-index:9999; background:#d93025; color:white; border:none; padding:12px 24px; border-radius:30px; font-weight:700; cursor:pointer; box-shadow:0 4px 15px rgba(0,0,0,0.3);';
    document.body.appendChild(undoBtn);

    const performSend = async () => {
        if (undoBtn) undoBtn.remove();
        try {
            const response = await fetch(`${API_URL}/send`, {
                method: 'POST',
                headers: { 'Authorization': authToken, 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });
            if (response.ok) {
                showToast(lang["msg-send-success"], 'success');
                document.getElementById('send-form').reset();
            } else {
                showToast(lang["msg-send-fail"], 'error');
            }
        } catch (err) {
            showToast('Erreur serveur SMTP', 'error');
        }
    };

    pendingSendTimeout = setTimeout(performSend, 5000);

    undoBtn.onclick = () => {
        clearTimeout(pendingSendTimeout);
        undoBtn.remove();
        showToast(currentLang === 'en' ? 'Sending cancelled' : 'Envoi annulé', 'info');
        openCompose();
    };
});

        if (response.status === 401) return logout();

        if (response.ok) {
            const lang = translations[currentLang];
            closeCompose();
            e.target.reset();
            showToast(lang["msg-send-success"], 'success');
        } else {
            const lang = translations[currentLang];
            showToast(lang["msg-send-fail"], 'error');
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
        
        if (response.status === 401) return logout();

        const stats = await response.json();
        const lang = translations[currentLang];

        // Update Text Stats
        document.getElementById('stat-total-emails').textContent = stats.totalEmails;
        document.getElementById('stat-unread-emails').textContent = stats.unreadEmails;
        
        const sizeMB = (stats.totalSize / (1024 * 1024)).toFixed(2);
        const limitMB = (stats.storageLimit / (1024 * 1024)).toFixed(0);
        document.getElementById('storageText').textContent = `${sizeMB} Mo ${lang["txt-storage-of"]} ${limitMB} Mo ${lang["txt-storage-used"]}`;

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

// --- PWA Service Worker Registration ---
if ('serviceWorker' in navigator) {
  window.addEventListener('load', () => {
    navigator.serviceWorker.register('./sw.js').then((registration) => {
      console.log('ServiceWorker registration successful with scope: ', registration.scope);
    }, (err) => {
      console.log('ServiceWorker registration failed: ', err);
    });
  });
}
 

function formatEmailDate(dateStr) {
    try {
        const d = new Date(dateStr);
        const now = new Date();
        if (d.toDateString() === now.toDateString())
            return d.toLocaleTimeString('fr-FR', {hour: '2-digit', minute: '2-digit'});
        if (now - d < 7 * 86400000)
            return d.toLocaleDateString('fr-FR', {weekday: 'short', hour: '2-digit', minute: '2-digit'});
        return d.toLocaleDateString('fr-FR', {day: '2-digit', month: 'short'});
    } catch(_) { return dateStr || ''; }
}
