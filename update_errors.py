import re

with open('src/main/resources/static/learn.html', 'r', encoding='utf-8') as f:
    html = f.read()

# 1. Insert showBackendError helper
helper = """      function showBackendError(container, errData) {
        let msg = errData.error || errData.message || (typeof errData === 'string' ? errData : "An unknown error occurred");
        let uniqueId = 'trace-' + Math.random().toString(36).substring(2, 9);
        let traceHtml = errData.trace ? 
          `<div style="margin-top:10px;"><button onclick="document.getElementById('${uniqueId}').style.display = document.getElementById('${uniqueId}').style.display === 'none' ? 'block' : 'none';" style="background:none; border:none; color:#b91c1c; cursor:pointer; font-size:0.85rem; padding:0; text-decoration:underline;">Toggle Stack Trace</button><div id="${uniqueId}" style="margin-top:10px; font-size:0.75rem; color:#475569; background:#ffffff; padding:10px; border:1px solid #fecaca; border-radius:6px; display:none; white-space:pre-wrap; max-height:220px; overflow-y:auto; text-align:left; font-family:monospace;">${errData.trace}</div></div>` : '';
        
        container.innerHTML = `<div style="color:#b91c1c; background:#fef2f2; border:1px solid #fca5a5; padding:12px; border-radius:8px; margin-top:10px;">
            <div style="font-weight:600; margin-bottom:4px; font-size:1.05rem;">🛑 Error</div>
            <div style="font-size:0.95rem;">${msg}</div>
            ${traceHtml}
        </div>`;
      }

"""

if "function showBackendError" not in html:
    html = html.replace('async function fetchUser() {', helper + '      async function fetchUser() {')


# 2. Replace throwing logic
html = re.sub(r'if\s*\(\s*data\.error\s*\)\s*throw\s*new\s*Error\([^)]*\);', 'if (data.error) throw data;', html)


# 3. Replace catch blocks UI modifications
# e.g., resultsDiv.innerHTML = `<div style="color:red">Error: ${err.message}</div>`;
html = re.sub(
    r'resultsDiv\.innerHTML\s*=\s*`<div style="color:red">Error: \$\{err\.message\}</div>`;',
    'showBackendError(resultsDiv, err);',
    html
)

html = re.sub(
    r'container\.innerHTML\s*=\s*`<div style="color:red; text-align:center; padding:20px;">Error finding word: \$\{err\.message\}</div>`;',
    'showBackendError(container, err);',
    html
)

html = re.sub(
    r'content\.innerHTML\s*=\s*`<div style="color:red">Error generating quiz. Please try again.</div>`;',
    'showBackendError(content, err);',
    html
)

html = re.sub(
    r'resultsDiv\.innerHTML\s*=\s*`<div style="color:red">Error: \$\{errMsg\}</div>`;',
    'showBackendError(resultsDiv, data);',
    html
)

html = re.sub(
    r'resultsDiv\.innerHTML\s*=\s*`<div style="color:red">OCR Error: \$\{err\.message\}</div>`;',
    'showBackendError(resultsDiv, err);',
    html
)

html = re.sub(
    r'resultsDiv\.innerHTML\s*=\s*`<div style="color:red">Error checking grammar: \$\{err\.message\}</div>`;',
    'showBackendError(resultsDiv, err);',
    html
)

html = html.replace(
    'resultsDiv.innerHTML = `<div style="color:red">Error: ${err.message}. Please try again.</div>`;',
    'showBackendError(resultsDiv, err);'
)


# Specially for dictionary search throw
html = html.replace('throw new Error("Bad JSON response from AI");', 'throw { error: "Bad JSON response from AI" };')

with open('src/main/resources/static/learn.html', 'w', encoding='utf-8') as f:
    f.write(html)
