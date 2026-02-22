document.addEventListener('DOMContentLoaded', () => {
    const chatForm = document.getElementById('chat-form');
    const userInput = document.getElementById('user-input');
    const chatMessages = document.getElementById('chat-messages');
    const providerBtns = document.querySelectorAll('.provider-btn');
    
    let currentProvider = 'openai';

    // Provider selection handling
    providerBtns.forEach(btn => {
        btn.addEventListener('click', () => {
            providerBtns.forEach(b => b.classList.remove('active'));
            btn.classList.add('active');
            currentProvider = btn.dataset.provider;
            
            const providerNames = {
                openai: 'OpenAI',
                huggingface: 'Hugging Face',
                grok: 'Grok',
                sarvam: 'Sarvam AI'
            };
            addSystemMessage(`Switched to ${providerNames[currentProvider] || currentProvider}`);
        });
    });

    // Form submission handling
    chatForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        const message = userInput.value.trim();
        if (!message) return;

        // Clear input and add user message to UI
        userInput.value = '';
        addMessage(message, 'user');

        // Add loading indicator
        const loadingId = addLoadingIndicator();

        try {
            let endpoint = '/ai/generate';
            if (currentProvider === 'huggingface') endpoint = '/ai/hf';
            if (currentProvider === 'grok') endpoint = '/ai/grok';
            if (currentProvider === 'sarvam') endpoint = '/ai/sarvam';
            
            const response = await fetch(`${endpoint}?message=${encodeURIComponent(message)}`);
            const data = await response.json();

            removeLoadingIndicator(loadingId);

            if (data.error) {
                addMessage(`Error: ${data.error}`, 'ai error');
            } else {
                addMessage(data.generation || data.status || 'Received response', 'ai');
            }
        } catch (error) {
            removeLoadingIndicator(loadingId);
            addMessage(`Failed to connect to the server.`, 'ai error');
            console.error('Error:', error);
        }
    });

    function addMessage(text, type) {
        const messageDiv = document.createElement('div');
        messageDiv.className = `message ${type}-message`;
        messageDiv.innerHTML = `<div class="message-content">${text}</div>`;
        chatMessages.appendChild(messageDiv);
        scrollToBottom();
    }

    function addSystemMessage(text) {
        const div = document.createElement('div');
        div.className = 'message system-message';
        div.innerHTML = `<div class="message-content">${text}</div>`;
        chatMessages.appendChild(div);
        scrollToBottom();
    }

    function addLoadingIndicator() {
        const id = 'loading-' + Date.now();
        const div = document.createElement('div');
        div.id = id;
        div.className = 'message ai-message';
        div.innerHTML = `
            <div class="message-content typing-indicator">
                <div class="dot"></div>
                <div class="dot"></div>
                <div class="dot"></div>
            </div>
        `;
        chatMessages.appendChild(div);
        scrollToBottom();
        return id;
    }

    function removeLoadingIndicator(id) {
        const el = document.getElementById(id);
        if (el) el.remove();
    }

    function scrollToBottom() {
        chatMessages.scrollTop = chatMessages.scrollHeight;
    }
});
