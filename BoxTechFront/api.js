// api.js
// Camada de comunicação do front-end com a BoxTech API: sessão (JWT), chamadas HTTP e utilitários.
// Carregado antes de script.js e pages.js.

// Usa o mesmo endereço pelo qual o site foi aberto (localhost no notebook, 192.168.x.x no celular).
const API_BASE = `http://${location.hostname || 'localhost'}:5132/api`;
const CHAVE_SESSAO = 'boxtech_sessao';

// ---------- Sessão (token JWT guardado no navegador) ----------
function obterSessao() {
    try {
        const bruto = localStorage.getItem(CHAVE_SESSAO) || sessionStorage.getItem(CHAVE_SESSAO);
        const s = JSON.parse(bruto || 'null');
        if (!s || !s.token) return null;
        if (new Date(s.expiraEm) <= new Date()) { encerrarSessao(); return null; }
        return s;
    } catch { return null; }
}

// lembrar = true: continua logado ao fechar o navegador; false: só até fechar a aba.
function salvarSessao(resposta, lembrar = true) {
    encerrarSessao();
    (lembrar ? localStorage : sessionStorage).setItem(CHAVE_SESSAO, JSON.stringify({
        token: resposta.token, expiraEm: resposta.expiraEm,
        usuarioId: resposta.usuarioId, nome: resposta.nome, perfil: resposta.perfil
    }));
}

function encerrarSessao() {
    localStorage.removeItem(CHAVE_SESSAO);
    sessionStorage.removeItem(CHAVE_SESSAO);
}
function estaLogado() { return obterSessao() !== null; }
function ehAdmin() { const s = obterSessao(); return !!s && s.perfil === 'Admin'; }

// Redireciona para o login quando a página exige autenticação.
function exigirLogin(admin = false) {
    const s = obterSessao();
    if (!s || (admin && s.perfil !== 'Admin')) {
        sessionStorage.setItem('boxtech_voltar', location.pathname.split('/').pop() + location.search);
        location.href = 'login.html';
        return false;
    }
    return true;
}

// ---------- Chamadas HTTP ----------
async function api(caminho, { metodo = 'GET', corpo = null } = {}) {
    const headers = { 'Accept': 'application/json' };
    const sessao = obterSessao();
    if (sessao) headers['Authorization'] = `Bearer ${sessao.token}`;
    if (corpo !== null) headers['Content-Type'] = 'application/json';

    let resposta;
    try {
        resposta = await fetch(`${API_BASE}${caminho}`, {
            method: metodo, headers, body: corpo !== null ? JSON.stringify(corpo) : null
        });
    } catch {
        throw new Error('Não foi possível conectar à API. Verifique se ela está rodando.');
    }

    if (resposta.status === 401 && sessao) { encerrarSessao(); }
    if (resposta.status === 204) return null;

    const texto = await resposta.text();
    let dados = null;
    try { dados = texto ? JSON.parse(texto) : null; } catch { /* resposta sem JSON */ }

    if (!resposta.ok) {
        let msg = (dados && dados.erro) || '';
        if (!msg && dados && dados.errors) msg = Object.values(dados.errors).flat().join(' ');
        if (!msg && resposta.status === 401) msg = 'Faça login para continuar.';
        if (!msg && resposta.status === 403) msg = 'Você não tem permissão para isso.';
        if (!msg && resposta.status === 429) msg = 'Muitas tentativas. Aguarde um minuto.';
        const erro = new Error(msg || `Erro ${resposta.status}`);
        erro.status = resposta.status;
        throw erro;
    }
    return dados;
}

// Envio de arquivo (multipart/form-data), ex.: foto do produto no painel admin.
async function apiEnvio(caminho, formData) {
    const headers = { 'Accept': 'application/json' };
    const sessao = obterSessao();
    if (sessao) headers['Authorization'] = `Bearer ${sessao.token}`;
    let resposta;
    try {
        resposta = await fetch(`${API_BASE}${caminho}`, { method: 'POST', headers, body: formData });
    } catch {
        throw new Error('Não foi possível conectar à API. Verifique se ela está rodando.');
    }
    const texto = await resposta.text();
    let dados = null;
    try { dados = texto ? JSON.parse(texto) : null; } catch { /* sem JSON */ }
    if (!resposta.ok) {
        let msg = (dados && dados.erro) || '';
        if (!msg && dados && dados.errors) msg = Object.values(dados.errors).flat().join(' ');
        if (!msg && resposta.status === 401) msg = 'Faça login para continuar.';
        if (!msg && resposta.status === 403) msg = 'Você não tem permissão para isso.';
        if (!msg && resposta.status === 413) msg = 'Arquivo grande demais.';
        throw new Error(msg || `Erro ${resposta.status}`);
    }
    return dados;
}

// ---------- Utilitários ----------
function brl(valor) {
    return Number(valor).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
}

// Evita injeção de HTML (XSS) ao exibir texto vindo da API.
function esc(texto) {
    return String(texto ?? '').replace(/[&<>"']/g, c =>
        ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
}

// Mesma regra de frete usada no servidor (PedidoService). O valor oficial é sempre o do servidor.
function calcularFrete(uf, subtotal) {
    if (subtotal >= 1000) return 0;
    switch ((uf || '').toUpperCase()) {
        case 'SP': return 15;
        case 'RJ': case 'MG': case 'ES': return 25;
        case 'PR': case 'SC': case 'RS': return 30;
        case 'DF': case 'GO': case 'MT': case 'MS': return 35;
        default: return 45;
    }
}

const IMAGEM_PADRAO = 'Imagens/Icon.jpeg';
function imagemProduto(p) { return (p && p.imagemUrl) ? p.imagemUrl : IMAGEM_PADRAO; }

function mostrarAviso(mensagem, erro = false) {
    let caixa = document.getElementById('boxtech-aviso');
    if (!caixa) {
        caixa = document.createElement('div');
        caixa.id = 'boxtech-aviso';
        caixa.setAttribute('role', 'status');
        caixa.style.cssText = 'position:fixed;top:20px;right:20px;z-index:10000;padding:14px 20px;border-radius:8px;' +
            'color:#fff;font-weight:600;box-shadow:0 5px 15px rgba(0,0,0,.5);max-width:340px;transition:opacity .3s;';
        document.body.appendChild(caixa);
    }
    caixa.style.background = erro ? '#c32b38' : '#2e7d32';
    caixa.textContent = mensagem;
    caixa.style.opacity = '1';
    clearTimeout(caixa._t);
    caixa._t = setTimeout(() => { caixa.style.opacity = '0'; }, 3500);
}

// Ajusta os ícones do cabeçalho conforme o login (perfil/entrar e atalho para o painel admin).
document.addEventListener('DOMContentLoaded', () => {
    const icones = document.querySelector('.header-icons');
    if (!icones) return;
    const sessao = obterSessao();
    const linkUsuario = icones.querySelector('a[href="login.html"]');
    if (linkUsuario && sessao) {
        linkUsuario.href = 'perfil.html';
        const i = linkUsuario.querySelector('i');
        if (i) i.title = `Perfil de ${sessao.nome}`;
    }
    if (sessao && sessao.perfil === 'Admin') {
        const a = document.createElement('a');
        a.href = 'admin.html'; a.style.color = 'inherit';
        a.innerHTML = '<i class="fas fa-tools" title="Administração"></i>';
        icones.prepend(a);
    }
});

// Contador de itens no ícone do carrinho (cabeçalho)
async function atualizarContadorCarrinho() {
    const selo = document.getElementById('cart-badge');
    if (!selo) return;
    if (!estaLogado()) { selo.hidden = true; return; }
    try {
        const carrinho = await api('/carrinho');
        const total = carrinho.itens.reduce((soma, i) => soma + i.quantidade, 0);
        selo.textContent = total > 99 ? '99+' : total;
        selo.hidden = total === 0;
    } catch { selo.hidden = true; }
}
document.addEventListener('DOMContentLoaded', atualizarContadorCarrinho);
