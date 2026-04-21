const API_URL = '/api';
let authToken = localStorage.getItem('auth_token');
let currentMessages = [];
let currentCategory = 'primary';
let selectedMessageId = null;

// --- Slideshow Logic ---
let slideIndex = 0;
function showSlides() {
    try {
        let slides = document.getElementsByClassName("slide");
        if (slides.length === 0) return;
        for (let i = 0; i < slides.length; i++) {
            slides[i].classList.remove("active");
            slides[i].style.zIndex = "0";
        }
        slideIndex++;
        if (slideIndex > slides.length) { slideIndex = 1 }
        const activeSlide = slides[slideIndex - 1];
        activeSlide.classList.add("active");
        activeSlide.style.zIndex = "1";
        setTimeout(showSlides, 5000);
    } catch (e) {
        console.error("Diaporama Error:", e);
    }
}

// --- Landing Page Logic ---
function showLoginFromLanding() {
    const login = document.getElementById('login-page');
    if (login) {
        login.style.display = 'flex';
        login.style.opacity = '1';
        login.style.zIndex = '99999';
    }
}

function closeLoginForm() {
    const login = document.getElementById('login-page');
    if (login) login.style.display = 'none';
}

// Initialisation
document.addEventListener('DOMContentLoaded', () => {
    // Force start slideshow if on landing page
    if (!localStorage.getItem('auth_token')) {
        showSlides();
    } else {
        document.getElementById('landing-page').style.display = 'none';
        showApp();
        loadInbox();
    }
    
    // Manual binding just in case
    document.querySelector('.btn-connexion-top')?.addEventListener('click', showLoginFromLanding);
});

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
// Search Logic
document.getElementById('search-input')?.addEventListener('input', (e) => {
    const term = e.target.value.toLowerCase();
    if (!term) {
        renderMessageList(currentMessages);
        return;
    }
    const filtered = currentMessages.filter(m => 
        m.subject.toLowerCase().includes(term) || 
        m.sender.toLowerCase().includes(term) || 
        m.content.toLowerCase().includes(term)
    );
    renderMessageList(filtered);
});

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
    const user = localStorage.getItem('auth_user');
    const nameInput = document.getElementById('settings-display-name');
    if (nameInput) nameInput.value = localStorage.getItem(`display_name_${user}`) || '';
    
    const avatar = localStorage.getItem(`profile_img_${user}`);
    const preview = document.getElementById('settings-avatar-preview');
    if (preview) {
        if (avatar) {
            preview.innerHTML = `<img src="${avatar}" style="width: 100%; height: 100%; border-radius: 50%; object-fit: cover;">`;
        } else {
            preview.innerHTML = user ? user.charAt(0).toUpperCase() : 'U';
        }
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

// NOTE: showContacts() is defined above at line ~148. Duplicate removed.

function showSent() {
    hideAllSections();
    showSection('inbox-section'); // Re-use inbox view for list
    setActiveNav('nav-sent');
    currentFolder = 'sent';
    loadSent();
}

async function loadSent() {
    try {
        const res = await fetch(`${API_URL}/sent`, {
            headers: { 'Authorization': authToken }
        });
        if (res.ok) {
            currentMessages = await res.json();
            renderMessageList(currentMessages);
        }
    } catch (e) {
        console.error(e);
    }
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

// NOTE: showAdmin() is defined above at line ~108. Duplicate removed.

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

let currentFolder = 'inbox';
// --- Internationalization (i18n) handled in translations.js ---


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
    const langSelect = document.getElementById('language-select');
    if (langSelect) langSelect.value = currentLang;
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
            
            if (user === 'admin') {
                window.location.href = "/admin.html";
                return;
            }

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
    authToken = null;
    location.reload();
}

// --- Register Form ---
function showRegisterForm() {
    document.getElementById('login-form').style.display = 'none';
    document.getElementById('register-form').style.display = 'block';
    document.getElementById('login-error').style.display = 'none';
}

function showLoginForm() {
    document.getElementById('register-form').style.display = 'none';
    document.getElementById('login-form').style.display = 'block';
    document.getElementById('register-error').style.display = 'none';
}

const registerForm = document.getElementById('register-form');
if (registerForm) {
    registerForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        const username = document.getElementById('reg-username').value.trim();
        const password = document.getElementById('reg-password').value;
        const errorEl  = document.getElementById('register-error');
        const btn = e.target.querySelector('button');

        if (!username || username.length < 3) {
            errorEl.textContent = 'Le nom doit comporter au moins 3 caractères.';
            errorEl.style.display = 'block';
            return;
        }

        btn.textContent = 'Création...';
        btn.disabled = true;
        try {
            const res = await fetch(`${API_URL}/register`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ username, password })
            });
            const data = await res.json();
            if (res.ok && data.success) {
                showToast('✅ Compte créé ! Connectez-vous.', 'success');
                showLoginForm();
                document.getElementById('username').value = username;
            } else {
                errorEl.textContent = data.error || 'Erreur lors de la création.';
                errorEl.style.display = 'block';
            }
        } catch (err) {
            errorEl.textContent = 'Erreur de communication avec le serveur.';
            errorEl.style.display = 'block';
        } finally {
            btn.textContent = 'Créer le compte';
            btn.disabled = false;
        }
    });
}

function showApp() {
    const user = localStorage.getItem('auth_user') || 'U';
    
    if (user === 'admin') {
        window.location.href = "/admin.html";
        return;
    }

    document.getElementById('login-page').style.display = 'none';
    document.getElementById('landing-page').style.display = 'none';
    
    const avatar = localStorage.getItem(`profile_img_${user}`);
    const initialsEl = document.getElementById('user-initials');
    if (avatar) {
        initialsEl.innerHTML = `<img src="${avatar}" style="width: 100%; height: 100%; border-radius: 50%; object-fit: cover;">`;
    } else {
        initialsEl.textContent = user.charAt(0).toUpperCase();
    }

    startInboxPolling();
    initWebSockets();
    updateStorageUI();
}

function previewAvatar(input) {
    if (input.files && input.files[0]) {
        const reader = new FileReader();
        reader.onload = function(e) {
            const preview = document.getElementById('settings-avatar-preview');
            preview.innerHTML = `<img src="${e.target.result}" style="width: 100%; height: 100%; border-radius: 50%; object-fit: cover;">`;
            localStorage.setItem('temp_avatar', e.target.result);
        };
        reader.readAsDataURL(input.files[0]);
    }
}

function togglePasswordVisibility(id) {
    const input = document.getElementById(id);
    const icon = input.nextElementSibling;
    if (input.type === 'password') {
        input.type = 'text';
        icon.classList.replace('fa-eye', 'fa-eye-slash');
    } else {
        input.type = 'password';
        icon.classList.replace('fa-eye-slash', 'fa-eye');
    }
}

async function saveSettings() {
    const displayName = document.getElementById('settings-display-name').value;
    const lang = document.getElementById('language-select').value;
    const profileImage = localStorage.getItem('temp_avatar') || document.getElementById('settings-avatar-preview').style.backgroundImage.replace(/url\(['"](.+)['"]\)/, '$1');
    
    // Sync with server
    try {
        await fetch(`${API_URL}/user/profile`, {
            method: 'POST',
            headers: { 
                'Authorization': authToken,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ displayName, profileImage })
        });
        localStorage.removeItem('temp_avatar');
    } catch (e) {
        console.error("Erreur sync profil:", e);
    }

    if (displayName) {
        document.getElementById('user-initials').textContent = displayName.charAt(0).toUpperCase();
    }
    
    changeLanguage(lang);
    showToast(currentLang === 'en' ? 'Settings saved' : 'Paramètres enregistrés', 'success');
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
    if (msg.category && msg.category !== 'primary') return msg.category;
    
    // Heuristic fallback
    const from = (msg.from || "").toLowerCase();
    const subject = (msg.subject || "").toLowerCase();
    if (from.match(/facebook|linkedin|twitter|instagram|social|network|github/)) return 'social';
    if (from.match(/noreply|marketing|newsletter|amazon|google|promo|offer/) || subject.match(/offre|promotion|remise/)) return 'promotions';
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
        container.innerHTML = `
            <div class="empty-state animate__animated animate__fadeIn">
                <i class="fas fa-inbox"></i>
                <h3>Votre boîte est vide</h3>
                <p>Parfait pour commencer de nouvelles conversations.</p>
            </div>
        `;
        return;
    }

    list.forEach((msg, idx) => {
        const item = document.createElement('div');
        item.className = `email-item animate__animated animate__fadeInUp`;
        item.style.animationDelay = `${idx * 0.03}s`;
        if (msg.unread) item.classList.add('unread');
        
        const senderName = msg.from || `Expéditeur Inconnu`;
        const displayDate = msg.date ? formatEmailDate(msg.date) : "...";
        
        item.innerHTML = `
            <div class="email-checkbox-wrapper" onclick="event.stopPropagation()">
                <input type="checkbox" class="email-checkbox">
            </div>
            <div class="email-star-wrapper ${msg.starred ? 'starred' : ''}" onclick="event.stopPropagation(); toggleStarred(${msg.dbId}, this)">
                <i class="${msg.starred ? 'fas' : 'far'} fa-star"></i>
            </div>
            <div class="email-sender" style="width: 180px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;">${senderName}</div>
            <div class="email-summary" style="flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;">
                <span class="email-subject">${msg.subject}</span>
                <span style="color: var(--text-secondary); margin-left: 8px; opacity:0.6; font-size:12px;">[${msg.category || 'primary'}]</span>
            </div>
            <div class="email-time" style="width: 100px; text-align: right; font-size: 12px; color: var(--text-secondary);">${displayDate}</div>
            <div class="email-actions">
                <div class="icon-btn" title="Archiver"><i class="fas fa-archive"></i></div>
                <div class="icon-btn" title="Supprimer" onclick="event.stopPropagation(); deleteMessageDirectly(${msg.id})">
                    <i class="far fa-trash-alt"></i>
                </div>
                <div class="icon-btn" title="Favoris" onclick="event.stopPropagation(); toggleStarred(${msg.dbId}, this)">
                    <i class="far fa-star"></i>
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
    // Pre-fill subject
    const subject = document.getElementById('view-subject').textContent;
    document.getElementById('send-subject').value = subject.startsWith('Re: ') ? subject : `Re: ${subject}`;
    // Pre-fill To: with the sender's address
    const fromText = document.getElementById('view-from').textContent;
    document.getElementById('send-to').value = fromText;
    // Focus on the editor
    if (emailQuill) emailQuill.focus();
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

// (Orphaned code removed — was causing a fatal SyntaxError)

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

        // Update Global Profile UI
        if (stats.profileImage) {
            const avatarDivs = [document.getElementById('user-initials'), document.getElementById('settings-avatar-preview')];
            avatarDivs.forEach(div => {
                if (div) {
                    div.style.backgroundImage = `url(${stats.profileImage})`;
                    div.style.backgroundSize = 'cover';
                    div.style.backgroundPosition = 'center';
                    div.textContent = '';
                }
            });
        }
        if (stats.displayName) {
            const nameInput = document.getElementById('settings-display-name');
            if (nameInput) nameInput.value = stats.displayName;
            const userInitials = document.getElementById('user-initials');
            if (userInitials && !stats.profileImage) {
                userInitials.textContent = stats.displayName.charAt(0).toUpperCase();
            }
        }

        // Update Text Stats
        document.getElementById('stat-total-emails').textContent = stats.totalEmails;
        document.getElementById('stat-unread-emails').textContent = stats.unreadEmails;
        
        const sizeMB = (stats.totalSize / (1024 * 1024)).toFixed(2);
        const limitMB = (stats.storageLimit / (1024 * 1024)).toFixed(0);
        document.getElementById('storageText').textContent = `${sizeMB} Mo ${lang["txt-storage-of"]} ${limitMB} Mo ${lang["txt-storage-used"]}`;

        // Sidebar Storage Update
        const pct = Math.min((stats.totalSize / stats.storageLimit) * 100, 100);
        const sideFill = document.getElementById('side-storage-fill');
        if (sideFill) {
            sideFill.style.width = `${pct}%`;
            sideFill.style.background = pct > 90 ? 'var(--error)' : 'var(--primary)';
        }

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

// --- Gestionnaire de Tâches ---
let mailTasks = JSON.parse(localStorage.getItem('mail_tasks')) || [];

function toggleTasksPanel() {
    const panel = document.getElementById('tasks-panel');
    panel.classList.toggle('open');
    if (panel.classList.contains('open')) renderTasks();
}

function renderTasks() {
    const list = document.getElementById('tasks-list');
    if (mailTasks.length === 0) {
        list.innerHTML = '<div style="text-align:center; color:var(--text-secondary); margin-top:40px;"><i class="fas fa-clipboard-list" style="font-size:48px; opacity:0.3; margin-bottom:12px; display:block;"></i><span style="font-size:14px;">Aucune tâche pour le moment.</span></div>';
        return;
    }
    
    list.innerHTML = mailTasks.map(t => `
        <div class="task-item ${t.completed ? 'completed' : ''}">
            <input type="checkbox" ${t.completed ? 'checked' : ''} onclick="toggleTaskStatus(${t.id})">
            <span style="flex:1; font-size:14px; color:var(--text-main); line-height:1.4;">${t.text}</span>
            <i class="fas fa-trash-alt" style="color:var(--error); cursor:pointer; font-size:14px; opacity:0.6; transition:opacity 0.2s;" onmouseover="this.style.opacity=1" onmouseout="this.style.opacity=0.6" onclick="deleteTask(${t.id})"></i>
        </div>
    `).join('');
}

function addTaskFromInput() {
    const input = document.getElementById('new-task-input');
    const text = input.value.trim();
    if (text) {
        addTask(text);
        input.value = '';
    }
}

function addTask(text) {
    mailTasks.unshift({ id: Date.now(), text: text, completed: false });
    saveTasks();
    renderTasks();
}

function toggleTaskStatus(id) {
    const task = mailTasks.find(t => t.id === id);
    if (task) {
        task.completed = !task.completed;
        saveTasks();
        renderTasks();
    }
}

function deleteTask(id) {
    mailTasks = mailTasks.filter(t => t.id !== id);
    saveTasks();
    renderTasks();
}

function saveTasks() {
    localStorage.setItem('mail_tasks', JSON.stringify(mailTasks));
}

function turnEmailIntoTask() {
    const subject = document.getElementById('view-subject').textContent;
    if (subject) {
        addTask(`Traiter l'email : "${subject}"`);
        const panel = document.getElementById('tasks-panel');
        if (!panel.classList.contains('open')) {
            toggleTasksPanel();
        }
        showToast('Tâche créée !', 'success');
    }
}
