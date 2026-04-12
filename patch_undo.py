path = 'src/main/resources/web/app.js'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

marker = "document.getElementById('send-form').addEventListener('submit'"
if marker in content:
    start_idx = content.find(marker)
    # Find the closing });
    end_idx = content.find('});', start_idx) + 3
    
    new_code = r"""let pendingSendTimeout = null;
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
});"""
    content = content[:start_idx] + new_code + content[end_idx:]
    with open(path, 'w', encoding='utf-8') as f:
        f.write(content)
    print("Undo send patched")
else:
    print("Error: Could not find send-form listener")
