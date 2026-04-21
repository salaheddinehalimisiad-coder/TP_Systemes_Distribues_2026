// --- Keyboard Shortcuts (Power User Mode) ---
document.addEventListener('keydown', (e) => {
    const tag = document.activeElement.tagName;
    if (tag === 'INPUT' || tag === 'TEXTAREA' || document.activeElement.classList.contains('ql-editor')) return;
    if (document.getElementById('login-page').style.display !== 'none' || 
        (document.getElementById('landing-page') && document.getElementById('landing-page').style.display !== 'none')) return;

    switch(e.key) {
        case 'c': case 'C':
            openCompose(); break;
        case 'i': case 'I':
            showInbox(); break;
        case 'd': case 'D':
            showDashboard(); break;
        case 'Escape':
            closeCompose();
            if (document.getElementById('viewer-section').style.display !== 'none') showInbox();
            break;
        case 'r': case 'R':
            replyToCurrentMessage(); break;
        case 's': case 'S':
            summarizeCurrentEmail(); break;
        case '?':
            toggleShortcutBanner(); break;
    }
});

let shortcutBannerVisible = false;
function toggleShortcutBanner() {
    const modal = document.getElementById('keyboard-shortcuts-modal');
    shortcutBannerVisible = !shortcutBannerVisible;
    modal.classList.toggle('visible', shortcutBannerVisible);
    if (shortcutBannerVisible) {
        setTimeout(() => {
            shortcutBannerVisible = false;
            modal.classList.remove('visible');
        }, 5000);
    }
}

document.addEventListener('DOMContentLoaded', () => {
    document.body.insertAdjacentHTML('beforeend', `
    <div id="keyboard-shortcuts-modal">
        <span class="shortcut-key"><kbd>C</kbd> Composer</span>
        <span class="shortcut-key"><kbd>I</kbd> Inbox</span>
        <span class="shortcut-key"><kbd>D</kbd> Dashboard</span>
        <span class="shortcut-key"><kbd>R</kbd> R&eacute;pondre</span>
        <span class="shortcut-key"><kbd>S</kbd> R&eacute;sum&eacute; IA</span>
        <span class="shortcut-key"><kbd>Esc</kbd> Fermer</span>
        <span class="shortcut-key"><kbd>?</kbd> Aide</span>
    </div>`);
});
