// pages.js
// Lógica específica de cada página da BoxTech (usa as funções de api.js).

const PAGINA = (location.pathname.split('/').pop() || 'index.html').toLowerCase();
const $ = (id) => document.getElementById(id);

// ================= Funções compartilhadas =================

function cartaoProduto(p, { favorito = false } = {}) {
    const esgotado = p.estoque <= 0;
    return `
    <div class="produto-card" data-id="${p.id}" style="cursor:pointer;">
        ${favorito ? `<button class="btn-remover-favorito" data-remover="${p.id}" title="Remover dos favoritos"><i class="fas fa-trash-alt"></i></button>` : ''}
        <img src="${esc(imagemProduto(p))}" alt="${esc(p.nome)}" class="img-produto">
        <div class="info-produto">
            <h3>${esc(p.nome)}</h3>
            <p class="preco">${brl(p.preco)}</p>
            <button class="btn-comprar" data-comprar="${p.id}" ${esgotado ? 'disabled' : ''}>${esgotado ? 'Esgotado' : 'Adicionar ao carrinho'}</button>
        </div>
    </div>`;
}

// Liga cliques de um grid de produtos: abrir detalhe, comprar e remover favorito.
function ligarGrid(grid, aoRemoverFavorito) {
    grid.addEventListener('click', async (e) => {
        const remover = e.target.closest('[data-remover]');
        if (remover) { e.stopPropagation(); aoRemoverFavorito && aoRemoverFavorito(Number(remover.dataset.remover)); return; }
        const comprar = e.target.closest('[data-comprar]');
        if (comprar) { e.stopPropagation(); await adicionarAoCarrinho(Number(comprar.dataset.comprar)); return; }
        const card = e.target.closest('.produto-card');
        if (card) location.href = `produto.html?id=${card.dataset.id}`;
    });
}

async function adicionarAoCarrinho(produtoId, quantidade = 1) {
    if (!estaLogado()) { exigirLogin(); return false; }
    try {
        await api('/carrinho', { metodo: 'POST', corpo: { produtoId, quantidade } });
        mostrarAviso('Produto adicionado ao carrinho.');
        atualizarContadorCarrinho();
        return true;
    } catch (e) { mostrarAviso(e.message, true); return false; }
}

// Usado pelos ícones de categoria da home.
async function irParaCategoria(nome) {
    try {
        const cats = await api('/categorias');
        const c = cats.find(x => x.nome === nome);
        if (c) location.href = `categoria.html?id=${c.id}`;
    } catch (e) { mostrarAviso(e.message, true); }
}

function statusVisual(status) {
    switch (status) {
        case 'Entregue': return ['status-delivered', 'fa-check-circle'];
        case 'Em Trânsito': return ['status-transit', 'fa-truck'];
        case 'Cancelado': return ['status-canceled', 'fa-times-circle'];
        case 'Pago': return ['status-paid', 'fa-credit-card'];
        default: return ['status-pending', 'fa-clock'];
    }
}

async function buscarCep(cep) {
    const limpo = cep.replace(/\D/g, '');
    if (limpo.length !== 8) return null;
    try {
        const d = await (await fetch(`https://viacep.com.br/ws/${limpo}/json/`)).json();
        return d.erro ? null : d;
    } catch { return null; }
}

// ================= Home =================
const HERO = [
    { titulo: 'Monte a máquina dos seus sonhos', cta: 'Comece sua montagem', categoria: 'Hardware' },
    { titulo: 'Periféricos de alta performance', cta: 'Ver periféricos', categoria: 'Periféricos' },
    { titulo: 'O seu setup gamer completo', cta: 'Ver games', categoria: 'Games' }
];

// Título e botão por cima do banner; trocam junto com o carrossel.
function iniciarHero() {
    const caixa = document.querySelector('.carousel-container');
    if (!caixa) return;
    const sobre = document.createElement('div');
    sobre.className = 'hero-overlay';
    sobre.innerHTML = '<h2></h2><button type="button" class="hero-cta"></button>';
    caixa.appendChild(sobre);
    const titulo = sobre.querySelector('h2'), botao = sobre.querySelector('.hero-cta');
    let atual = HERO[0];
    const mostrar = (i) => {
        atual = HERO[i % HERO.length];
        titulo.textContent = atual.titulo;
        botao.innerHTML = `${esc(atual.cta)} <i class="fas fa-chevron-right"></i>`;
    };
    botao.addEventListener('click', () => irParaCategoria(atual.categoria));
    document.addEventListener('carousel:change', (e) => mostrar(e.detail));
    mostrar(0);
}

async function iniciarIndex() {
    iniciarHero();
    const grid = $('produtos-grid');
    ligarGrid(grid);
    try {
        const produtos = await api('/produtos');
        grid.innerHTML = produtos.length ? produtos.map(p => cartaoProduto(p)).join('') : '<p>Nenhum produto disponível.</p>';
    } catch (e) { grid.innerHTML = `<p style="color:#e63946">${esc(e.message)}</p>`; }
}

// ================= Categoria =================
async function iniciarCategoria() {
    const params = new URLSearchParams(location.search);
    const id = params.get('id'), busca = params.get('busca');
    const consulta = new URLSearchParams();
    if (id) consulta.set('categoriaId', id);
    if (busca) consulta.set('busca', busca);
    const grid = $('produtos-grid');
    ligarGrid(grid);
    let todos = [];
    try {
        const [cats, produtos] = await Promise.all([api('/categorias'), api(`/produtos${consulta.toString() ? '?' + consulta : ''}`)]);
        todos = produtos;
        const cat = cats.find(c => String(c.id) === String(id));
        const titulo = busca ? `Resultados para "${busca}"` : (cat ? cat.nome : 'Todos os produtos');
        $('cat-titulo').textContent = titulo;
        document.title = `BoxTech - ${titulo}`;
        const marcas = [...new Set(produtos.map(p => p.marca).filter(Boolean))].sort();
        $('filtro-marcas').innerHTML = marcas.length
            ? marcas.map(m => `<label class="filter-item"><input type="checkbox" value="${esc(m)}"> ${esc(m)}</label>`).join('')
            : '<span style="color:var(--text-muted);font-size:.875rem">Sem marcas</span>';
    } catch (e) { grid.innerHTML = `<p style="color:#e63946">${esc(e.message)}</p>`; return; }

    const faixas = { 'ate100': [0, 100], '100a300': [100, 300], '300a500': [300, 500], 'acima500': [500, Infinity] };
    function aplicar() {
        const marcas = [...document.querySelectorAll('#filtro-marcas input:checked')].map(i => i.value);
        const fx = [...document.querySelectorAll('#filtro-precos input:checked')].map(i => faixas[i.value]);
        let lista = todos.filter(p => (!marcas.length || marcas.includes(p.marca)) &&
                                      (!fx.length || fx.some(([a, b]) => p.preco >= a && p.preco <= b)));
        const ord = $('ordenar').value;
        if (ord === 'menor-preco') lista.sort((a, b) => a.preco - b.preco);
        else if (ord === 'maior-preco') lista.sort((a, b) => b.preco - a.preco);
        $('cat-contagem').textContent = `Mostrando ${lista.length} de ${todos.length} produtos`;
        grid.innerHTML = lista.length ? lista.map(p => cartaoProduto(p)).join('') : '<p>Nenhum produto encontrado com esses filtros.</p>';
    }
    $('btn-aplicar').addEventListener('click', aplicar);
    $('ordenar').addEventListener('change', aplicar);
    aplicar();
}

// ================= Produto =================
async function iniciarProduto() {
    const id = new URLSearchParams(location.search).get('id');
    if (!id) { location.href = 'index.html'; return; }
    let p;
    try { p = await api(`/produtos/${encodeURIComponent(id)}`); }
    catch (e) { $('produto-nome').textContent = e.message; return; }

    document.title = `BoxTech - ${p.nome}`;
    $('produto-nome').textContent = p.nome;
    $('produto-descricao').textContent = p.descricao;
    $('produto-preco').textContent = brl(p.preco);
    const parcela = document.querySelector('.installments');
    if (parcela) parcela.textContent = `ou ${brl(p.preco)} em até 10x de ${brl(p.preco / 10)} sem juros`;
    window.__subtotalFrete = p.preco;

    const principal = $('main-product-image');
    const fotos = p.imagens.length ? [p.imagemUrl, ...p.imagens].filter(Boolean) : [p.imagemUrl || IMAGEM_PADRAO];
    principal.src = fotos[0]; principal.alt = p.nome;
    const caixa = document.querySelector('.thumbnail-container');
    caixa.innerHTML = fotos.map((u, i) => `<img src="${esc(u)}" alt="Miniatura ${i + 1}" class="thumbnail ${i === 0 ? 'active' : ''}">`).join('');
    const miniaturas = () => [...caixa.querySelectorAll('.thumbnail')];
    let atual = 0;
    function mostrarFoto(i) {
        atual = (i + fotos.length) % fotos.length;
        principal.src = fotos[atual];
        miniaturas().forEach((m, k) => m.classList.toggle('active', k === atual));
    }
    caixa.addEventListener('click', (e) => {
        const t = e.target.closest('.thumbnail'); if (!t) return;
        mostrarFoto(miniaturas().indexOf(t));
    });
    const anterior = document.querySelector('.prev-arrow'), proxima = document.querySelector('.next-arrow');
    if (fotos.length > 1) {
        anterior.addEventListener('click', () => mostrarFoto(atual - 1));
        proxima.addEventListener('click', () => mostrarFoto(atual + 1));
    } else { anterior.style.display = 'none'; proxima.style.display = 'none'; }

    const compartilhar = document.querySelector('.share-icon');
    compartilhar.style.cursor = 'pointer';
    compartilhar.addEventListener('click', async () => {
        try {
            if (navigator.share) await navigator.share({ title: p.nome, url: location.href });
            else { await navigator.clipboard.writeText(location.href); mostrarAviso('Link do produto copiado.'); }
        } catch { /* compartilhamento cancelado */ }
    });

    const btnCarrinho = document.querySelector('.add-to-cart-btn');
    const btnComprar = document.querySelector('.buy-now-btn');
    if (p.estoque <= 0) {
        [btnCarrinho, btnComprar].forEach(b => { b.disabled = true; });
        btnComprar.textContent = 'Produto esgotado';
    }
    btnCarrinho.addEventListener('click', () => adicionarAoCarrinho(p.id));
    btnComprar.addEventListener('click', async () => { if (await adicionarAoCarrinho(p.id)) location.href = 'carrinho.html'; });

    // Favorito (coração)
    const coracao = document.querySelector('.fav-icon');
    let favorito = false;
    const pintar = () => { coracao.classList.toggle('fas', favorito); coracao.classList.toggle('far', !favorito); coracao.style.color = favorito ? 'var(--brand)' : ''; };
    if (estaLogado()) {
        try { favorito = (await api('/favoritos')).some(f => f.produtoId === p.id); } catch { /* ignora */ }
    }
    pintar();
    coracao.style.cursor = 'pointer';
    coracao.addEventListener('click', async () => {
        if (!estaLogado()) { exigirLogin(); return; }
        try {
            await api(`/favoritos/${p.id}`, { metodo: favorito ? 'DELETE' : 'POST' });
            favorito = !favorito; pintar();
            mostrarAviso(favorito ? 'Adicionado aos favoritos.' : 'Removido dos favoritos.');
        } catch (e) { mostrarAviso(e.message, true); }
    });
}

// ================= Carrinho =================
async function iniciarCarrinho() {
    if (!exigirLogin()) return;
    const area = $('cart-itens');

    async function desenhar(carrinho) {
        window.__subtotalFrete = carrinho.total;
        if (!carrinho.itens.length) {
            area.innerHTML = '<p style="padding:20px">Seu carrinho está vazio. <a href="index.html" style="color:var(--brand)">Continuar comprando</a></p>';
            $('cart-total').textContent = brl(0); $('btn-continuar').disabled = true; return;
        }
        $('btn-continuar').disabled = false;
        $('cart-total').textContent = brl(carrinho.total);
        area.innerHTML = carrinho.itens.map(i => `
        <div class="cart-item" data-item="${i.id}">
            <div class="cart-item-img"><img src="${esc(i.imagemUrl || IMAGEM_PADRAO)}" alt="${esc(i.nome)}"></div>
            <div class="cart-item-info">
                <h3>${esc(i.nome)}</h3>
                <div class="quantity-controls">
                    <button class="qty-btn" data-menos="${i.id}" data-qtd="${i.quantidade}" aria-label="Diminuir">-</button>
                    <input type="number" value="${i.quantidade}" readonly aria-label="Quantidade">
                    <button class="qty-btn" data-mais="${i.id}" data-qtd="${i.quantidade}" aria-label="Aumentar">+</button>
                </div>
            </div>
            <div class="cart-item-price-area">
                <button class="delete-item" data-remover="${i.id}" aria-label="Remover item"><i class="far fa-trash-alt"></i></button>
                <div class="price-tags">
                    <h2 class="price-main">${brl(i.subtotal)}</h2>
                    <p class="price-sub">${i.quantidade} x ${brl(i.preco)}</p>
                </div>
            </div>
        </div>`).join('');
    }

    async function acao(promessa) {
        try { await desenhar(await promessa); atualizarContadorCarrinho(); } catch (e) { mostrarAviso(e.message, true); }
    }

    area.addEventListener('click', (e) => {
        const menos = e.target.closest('[data-menos]'), mais = e.target.closest('[data-mais]'), rem = e.target.closest('[data-remover]');
        if (menos) {
            const q = Number(menos.dataset.qtd);
            if (q > 1) acao(api(`/carrinho/${menos.dataset.menos}`, { metodo: 'PUT', corpo: { quantidade: q - 1 } }));
        } else if (mais) {
            acao(api(`/carrinho/${mais.dataset.mais}`, { metodo: 'PUT', corpo: { quantidade: Number(mais.dataset.qtd) + 1 } }));
        } else if (rem) {
            acao(api(`/carrinho/${rem.dataset.remover}`, { metodo: 'DELETE' }));
        }
    });
    $('btn-continuar').addEventListener('click', () => { location.href = 'checkout.html'; });
    acao(api('/carrinho'));
}

// ================= Checkout =================
async function iniciarCheckout() {
    if (!exigirLogin()) return;
    let carrinho, enderecos = [], selecionado = null;
    try { [carrinho, enderecos] = await Promise.all([api('/carrinho'), api('/enderecos')]); }
    catch (e) { mostrarAviso(e.message, true); return; }
    if (!carrinho.itens.length) { location.href = 'carrinho.html'; return; }

    const lista = $('enderecos-lista'), form = $('form-endereco');

    function desenharResumo() {
        const end = enderecos.find(e => e.id === selecionado);
        const frete = end ? calcularFrete(end.estado, carrinho.total) : 0;
        $('resumo-itens').innerHTML =
            carrinho.itens.map(i => `<div class="summary-item"><span>${esc(i.nome)} (x${i.quantidade})</span><span>${brl(i.subtotal)}</span></div>`).join('') +
            `<div class="summary-item"><span>Frete ${end ? `(${esc(end.estado)})` : ''}</span><span>${end ? (frete === 0 ? 'Grátis' : brl(frete)) : '—'}</span></div>`;
        $('resumo-total').textContent = brl(carrinho.total + frete);
        const sel = $('select-parcelas'), total = carrinho.total + frete;
        sel.innerHTML = [1, 2, 3, 5, 10].map(n => `<option value="${n}">${n}x de ${brl(total / n)} sem juros</option>`).join('');
    }

    function desenharEnderecos() {
        if (!enderecos.length) { lista.innerHTML = '<p style="color:var(--text-muted)">Você ainda não tem endereços. Cadastre um abaixo.</p>'; form.style.display = 'block'; selecionado = null; }
        else {
            if (!enderecos.some(e => e.id === selecionado)) selecionado = (enderecos.find(e => e.principal) || enderecos[0]).id;
            lista.innerHTML = enderecos.map(e => `
            <label class="payment-method" style="display:block;margin-bottom:10px;text-align:left;">
                <input type="radio" name="endereco" value="${e.id}" ${e.id === selecionado ? 'checked' : ''}>
                <strong>${esc(e.apelido)}</strong>${e.principal ? ' (principal)' : ''}<br>
                <span style="color:var(--text-muted);font-size:.875rem">${esc(e.logradouro)}, ${esc(e.numero)}${e.complemento ? ' - ' + esc(e.complemento) : ''} — ${esc(e.bairro)}, ${esc(e.cidade)}/${esc(e.estado)} — CEP ${esc(e.cep)}</span>
            </label>`).join('') +
            '<button type="button" id="btn-novo-endereco" class="btn-comprar" style="width:auto;padding:8px 16px;">+ Novo endereço</button>';
        }
        desenharResumo();
    }

    lista.addEventListener('change', (e) => { if (e.target.name === 'endereco') { selecionado = Number(e.target.value); desenharResumo(); } });
    lista.addEventListener('click', (e) => { if (e.target.id === 'btn-novo-endereco') form.style.display = form.style.display === 'none' ? 'block' : 'none'; });

    $('end-cep').addEventListener('blur', async () => {
        const d = await buscarCep($('end-cep').value);
        if (!d) return;
        $('end-logradouro').value = d.logradouro || ''; $('end-bairro').value = d.bairro || '';
        $('end-cidade').value = d.localidade || ''; $('end-estado').value = d.uf || '';
    });

    $('btn-salvar-endereco').addEventListener('click', async () => {
        const corpo = {
            apelido: $('end-apelido').value.trim() || 'Casa', cep: $('end-cep').value.trim(),
            logradouro: $('end-logradouro').value.trim(), numero: $('end-numero').value.trim(),
            complemento: $('end-complemento').value.trim() || null, bairro: $('end-bairro').value.trim(),
            cidade: $('end-cidade').value.trim(), estado: $('end-estado').value.trim(), principal: false
        };
        try {
            const novo = await api('/enderecos', { metodo: 'POST', corpo });
            enderecos.push(novo); selecionado = novo.id;
            form.style.display = 'none'; form.querySelectorAll('input').forEach(i => { i.value = ''; });
            desenharEnderecos(); mostrarAviso('Endereço salvo.');
        } catch (e) { mostrarAviso(e.message, true); }
    });

    $('btn-finalizar').addEventListener('click', async () => {
        if (!selecionado) { mostrarAviso('Selecione ou cadastre um endereço de entrega.', true); return; }
        const metodo = document.querySelector('input[name="pagamento"]:checked').value === 'pix' ? 'Pix' : 'Cartao';
        const botao = $('btn-finalizar'); botao.disabled = true;
        try {
            // Pagamento simulado: apenas método e parcelas vão para a API (nenhum dado de cartão é enviado).
            const pedido = await api('/pedidos/checkout', { metodo: 'POST',
                corpo: { enderecoId: selecionado, metodo, parcelas: metodo === 'Pix' ? 1 : Number($('select-parcelas').value) } });
            alert(`Pedido #${pedido.id} realizado com sucesso!\nStatus: ${pedido.status}\nTotal: ${brl(pedido.valorTotal)}\nObrigado por comprar na BoxTech.`);
            location.href = 'perfil.html';
        } catch (e) { mostrarAviso(e.message, true); botao.disabled = false; }
    });

    desenharEnderecos();
}

// ================= Favoritos =================
async function iniciarFavoritos() {
    if (!exigirLogin()) return;
    const grid = $('produtos-grid');
    async function carregar() {
        try {
            const favs = await api('/favoritos');
            grid.innerHTML = favs.length
                ? favs.map(f => cartaoProduto({ id: f.produtoId, nome: f.nome, preco: f.preco, imagemUrl: f.imagemUrl, estoque: 1 }, { favorito: true })).join('')
                : '<p>Você ainda não favoritou nenhum produto.</p>';
        } catch (e) { grid.innerHTML = `<p style="color:#e63946">${esc(e.message)}</p>`; }
    }
    ligarGrid(grid, async (id) => {
        try { await api(`/favoritos/${id}`, { metodo: 'DELETE' }); await carregar(); } catch (e) { mostrarAviso(e.message, true); }
    });
    carregar();
}

// ================= Login / Cadastro =================
function iniciarLogin() {
    const entrar = async () => {
        try {
            const r = await api('/auth/login', { metodo: 'POST', corpo: { email: $('email').value.trim(), senha: $('senha').value } });
            salvarSessao(r, $('lembrar').checked);
            const voltar = sessionStorage.getItem('boxtech_voltar'); sessionStorage.removeItem('boxtech_voltar');
            location.href = r.perfil === 'Admin' ? 'admin.html' : (voltar || 'perfil.html');
        } catch (e) { mostrarAviso(e.message, true); }
    };
    $('btn-login').addEventListener('click', entrar);
    $('link-esqueceu').addEventListener('click', (e) => {
        e.preventDefault();
        mostrarAviso('A recuperação de senha ainda não está disponível nesta versão. Procure o suporte da loja.');
    });
    $('senha').addEventListener('keydown', (e) => { if (e.key === 'Enter') entrar(); });
}

function iniciarCadastro() {
    $('cadastro-form').addEventListener('submit', async (e) => {
        e.preventDefault();
        if ($('senha').value !== $('confirmarSenha').value) { mostrarAviso('As senhas não coincidem.', true); return; }
        try {
            const r = await api('/auth/registro', { metodo: 'POST', corpo: {
                nome: $('nome').value.trim(), sobrenome: $('sobrenome').value.trim(), cpf: $('cpf').value.trim(),
                email: $('email').value.trim(), senha: $('senha').value } });
            salvarSessao(r);
            mostrarAviso('Cadastro realizado com sucesso!');
            setTimeout(() => { location.href = 'index.html'; }, 800);
        } catch (err) { mostrarAviso(err.message, true); }
    });
}

// ================= Perfil =================
async function iniciarPerfil() {
    if (!exigirLogin()) return;
    const sessao = obterSessao();
    $('nome-usuario').textContent = `Olá, ${sessao.nome}!`;
    const titulo = $('perfil-titulo'), conteudo = $('perfil-conteudo');

    const abas = {
        async pedidos() {
            titulo.textContent = 'Meus Pedidos';
            const pedidos = await api('/pedidos');
            conteudo.innerHTML = pedidos.length ? pedidos.map(p => {
                const [cls, icone] = statusVisual(p.status);
                const podeCancelar = p.status === 'Aguardando Pagamento' || p.status === 'Pago';
                return `
                <div class="order-card">
                    <div class="order-header">
                        <div><span class="order-number">Pedido #${p.id}</span>
                        <span class="order-date">Realizado em: ${new Date(p.dataPedido).toLocaleDateString('pt-BR')}</span></div>
                        <div class="order-status ${cls}"><i class="fas ${icone}"></i> ${esc(p.status)}</div>
                    </div>
                    <div class="order-body">
                        <div class="order-details">
                            ${p.itens.map(i => `<p>${i.quantidade}x ${esc(i.nome)} — ${brl(i.subtotal)}</p>`).join('')}
                            <p>Frete: ${brl(p.valorFrete)} | Pagamento: ${esc(p.metodoPagamento || '-')} (${esc(p.statusPagamento || '-')})</p>
                            <p class="order-price">Total: ${brl(p.valorTotal)}</p>
                        </div>
                        ${podeCancelar ? `<button class="btn-comprar" data-cancelar="${p.id}" style="width:auto;padding:10px 20px;">Cancelar pedido</button>` : ''}
                    </div>
                </div>`;
            }).join('') : '<p>Você ainda não fez pedidos.</p>';
        },
        async dados() {
            titulo.textContent = 'Meus Dados';
            const u = await api('/auth/eu');
            conteudo.innerHTML = `<div class="order-card"><div class="order-body"><div class="order-details">
                <p><strong>Nome:</strong> ${esc(u.nome)} ${esc(u.sobrenome)}</p>
                <p><strong>CPF:</strong> ${esc(u.cpf)}</p>
                <p><strong>E-mail:</strong> ${esc(u.email)}</p>
                <p><strong>Perfil:</strong> ${esc(u.perfil)}</p></div></div></div>`;
        },
        async enderecos() {
            titulo.textContent = 'Endereços';
            const lista = await api('/enderecos');
            conteudo.innerHTML = (lista.length ? lista.map(e => `
                <div class="order-card"><div class="order-body"><div class="order-details">
                    <h4>${esc(e.apelido)}${e.principal ? ' (principal)' : ''}</h4>
                    <p>${esc(e.logradouro)}, ${esc(e.numero)}${e.complemento ? ' - ' + esc(e.complemento) : ''}</p>
                    <p>${esc(e.bairro)} — ${esc(e.cidade)}/${esc(e.estado)} — CEP ${esc(e.cep)}</p></div>
                    <button class="btn-comprar" data-rem-end="${e.id}" style="width:auto;padding:10px 20px;">Remover</button>
                </div></div>`).join('') : '<p>Nenhum endereço cadastrado. Você pode cadastrar durante o checkout.</p>');
        },
        async seguranca() {
            titulo.textContent = 'Segurança';
            conteudo.innerHTML = `<div class="order-card"><div class="order-body"><div class="order-details">
                <p>Sua senha é armazenada com hash (BCrypt) e nunca em texto puro.</p>
                <p>Sessão válida até: ${new Date(sessao.expiraEm).toLocaleString('pt-BR')}</p></div></div></div>`;
        }
    };

    async function abrir(nome) {
        document.querySelectorAll('.profile-link[data-aba]').forEach(a => a.classList.toggle('active', a.dataset.aba === nome));
        try { await abas[nome](); } catch (e) { conteudo.innerHTML = `<p style="color:#e63946">${esc(e.message)}</p>`; }
    }

    document.querySelectorAll('.profile-link[data-aba]').forEach(a => a.addEventListener('click', (e) => { e.preventDefault(); abrir(a.dataset.aba); }));
    $('btn-sair').addEventListener('click', () => encerrarSessao());
    conteudo.addEventListener('click', async (e) => {
        const c = e.target.closest('[data-cancelar]'), r = e.target.closest('[data-rem-end]');
        try {
            if (c && confirm('Cancelar este pedido?')) { await api(`/pedidos/${c.dataset.cancelar}/cancelar`, { metodo: 'POST' }); mostrarAviso('Pedido cancelado.'); abrir('pedidos'); }
            if (r && confirm('Remover este endereço?')) { await api(`/enderecos/${r.dataset.remEnd}`, { metodo: 'DELETE' }); abrir('enderecos'); }
        } catch (err) { mostrarAviso(err.message, true); }
    });
    abrir('pedidos');
}

// ================= Administração =================
async function iniciarAdmin() {
    if (!exigirLogin(true)) return;
    const painel = $('admin-conteudo');
    // Categorias e marcas carregam separadas: se uma falhar, a outra continua funcionando.
    const [categorias, marcas] = await Promise.all([
        api('/categorias').catch(e => { mostrarAviso('Categorias: ' + e.message, true); return []; }),
        api('/marcas').catch(e => { mostrarAviso('Marcas: ' + e.message, true); return []; })
    ]);

    const opcoes = (lista, sel, vazio) => (vazio ? `<option value="">${vazio}</option>` : '') +
        lista.map(x => `<option value="${x.id}" ${x.id === sel ? 'selected' : ''}>${esc(x.nome)}</option>`).join('');

    const telas = {
        async produtos() {
            const lista = await api('/admin/produtos');
            painel.innerHTML = `
            <div class="checkout-section">
                <h3><i class="fas fa-box"></i> <span id="form-prod-titulo">Novo produto</span></h3>
                <input type="hidden" id="prod-id">
                <div class="form-row">
                    <div class="form-group" style="flex:2"><label>Nome</label><input id="prod-nome" maxlength="150"></div>
                    <div class="form-group"><label>Preço (R$)</label><input id="prod-preco" type="number" min="0" step="0.01"></div>
                    <div class="form-group"><label>Estoque</label><input id="prod-estoque" type="number" min="0"></div>
                </div>
                <div class="form-row">
                    <div class="form-group"><label>Categoria</label><select id="prod-categoria" class="admin-select">${opcoes(categorias, null)}</select></div>
                    <div class="form-group"><label>Marca</label><select id="prod-marca" class="admin-select">${opcoes(marcas, null, '— sem marca —')}</select></div>
                    <div class="form-group" style="flex:2"><label>Imagem (caminho)</label><input id="prod-imagem" placeholder="Imagens/Produto.png"></div>
                </div>
                <div class="form-group"><label>Descrição</label><input id="prod-descricao" maxlength="4000"></div>
                <button class="btn-primary" id="prod-salvar" style="width:auto;padding:10px 24px">Salvar</button>
                <button class="btn-comprar" id="prod-cancelar" style="width:auto;padding:10px 24px;display:none">Cancelar edição</button>
            </div>
            <div class="admin-tabela-wrap"><table class="admin-tabela">
                <thead><tr><th>ID</th><th>Produto</th><th>Categoria</th><th>Preço</th><th>Estoque</th><th>Situação</th><th></th></tr></thead>
                <tbody>${lista.map(p => `<tr>
                    <td>${p.id}</td><td>${esc(p.nome)}</td><td>${esc(p.categoria)}</td><td>${brl(p.preco)}</td><td>${p.estoque}</td>
                    <td>${p.ativo ? 'Ativo' : 'Inativo'}</td>
                    <td><button class="admin-btn" data-editar='${esc(JSON.stringify(p))}'>Editar</button>
                        ${p.ativo ? `<button class="admin-btn perigo" data-desativar="${p.id}">Desativar</button>` : ''}</td></tr>`).join('')}</tbody>
            </table></div>`;

            const limpar = () => { ['prod-id', 'prod-nome', 'prod-preco', 'prod-estoque', 'prod-imagem', 'prod-descricao'].forEach(i => { $(i).value = ''; });
                $('form-prod-titulo').textContent = 'Novo produto'; $('prod-cancelar').style.display = 'none'; };
            $('prod-cancelar').addEventListener('click', limpar);
            $('prod-salvar').addEventListener('click', async () => {
                const id = $('prod-id').value;
                const corpo = { nome: $('prod-nome').value.trim(), descricao: $('prod-descricao').value, preco: Number($('prod-preco').value),
                    estoque: Number($('prod-estoque').value), imagemUrl: $('prod-imagem').value.trim(),
                    categoriaId: Number($('prod-categoria').value), marcaId: $('prod-marca').value ? Number($('prod-marca').value) : null, ativo: true };
                try {
                    await api(id ? `/admin/produtos/${id}` : '/admin/produtos', { metodo: id ? 'PUT' : 'POST', corpo });
                    mostrarAviso('Produto salvo.'); abrirTela('produtos');
                } catch (e) { mostrarAviso(e.message, true); }
            });
            painel.querySelector('tbody').addEventListener('click', async (e) => {
                const ed = e.target.closest('[data-editar]'), de = e.target.closest('[data-desativar]');
                if (ed) {
                    const p = JSON.parse(ed.dataset.editar);
                    $('prod-id').value = p.id; $('prod-nome').value = p.nome; $('prod-preco').value = p.preco; $('prod-estoque').value = p.estoque;
                    $('prod-imagem').value = p.imagemUrl; $('prod-descricao').value = p.descricao;
                    $('prod-categoria').value = p.categoriaId; $('prod-marca').value = p.marcaId ?? '';
                    $('form-prod-titulo').textContent = `Editar produto #${p.id}`; $('prod-cancelar').style.display = 'inline-block';
                    window.scrollTo({ top: 0, behavior: 'smooth' });
                }
                if (de && confirm('Desativar este produto? Ele sai da loja, mas o histórico de pedidos é mantido.')) {
                    try { await api(`/admin/produtos/${de.dataset.desativar}`, { metodo: 'DELETE' }); abrirTela('produtos'); } catch (err) { mostrarAviso(err.message, true); }
                }
            });
        },
        async pedidos() {
            const lista = await api('/admin/pedidos');
            const status = ['Aguardando Pagamento', 'Pago', 'Em Trânsito', 'Entregue', 'Cancelado'];
            painel.innerHTML = `<div class="admin-tabela-wrap"><table class="admin-tabela">
                <thead><tr><th>Pedido</th><th>Data</th><th>Cliente</th><th>Total</th><th>Status</th></tr></thead>
                <tbody>${lista.map(p => `<tr><td>#${p.id}</td><td>${new Date(p.dataPedido).toLocaleDateString('pt-BR')}</td>
                    <td>${esc(p.cliente)}<br><small>${esc(p.email)}</small></td><td>${brl(p.valorTotal)}</td>
                    <td><select class="admin-select" data-status="${p.id}" data-atual="${esc(p.status)}">
                        ${status.map(s => `<option ${s === p.status ? 'selected' : ''}>${s}</option>`).join('')}</select></td></tr>`).join('')}</tbody>
            </table></div>${lista.length ? '' : '<p>Nenhum pedido.</p>'}`;
            painel.onchange = async (e) => {
                const s = e.target.closest('[data-status]'); if (!s) return;
                try { await api(`/admin/pedidos/${s.dataset.status}/status`, { metodo: 'PUT', corpo: { status: s.value } }); mostrarAviso('Status atualizado.'); s.dataset.atual = s.value; }
                catch (err) { mostrarAviso(err.message, true); s.value = s.dataset.atual; }
            };
        },
        async relatorios() {
            const hoje = new Date().toISOString().slice(0, 10), inicio = new Date(Date.now() - 30 * 864e5).toISOString().slice(0, 10);
            painel.innerHTML = `<div class="checkout-section"><h3><i class="fas fa-chart-bar"></i> Vendas por categoria</h3>
                <div class="form-row"><div class="form-group"><label>De</label><input type="date" id="rel-ini" value="${inicio}"></div>
                <div class="form-group"><label>Até</label><input type="date" id="rel-fim" value="${hoje}"></div></div>
                <button class="btn-primary" id="rel-gerar" style="width:auto;padding:10px 24px">Gerar</button>
                <div id="rel-vendas" style="margin-top:15px"></div></div>
                <div class="checkout-section"><h3><i class="fas fa-trophy"></i> Produtos mais vendidos</h3><div id="rel-top"></div></div>`;
            const tabela = (cab, linhas) => linhas.length
                ? `<div class="admin-tabela-wrap"><table class="admin-tabela"><thead><tr>${cab.map(c => `<th>${c}</th>`).join('')}</tr></thead><tbody>${linhas.join('')}</tbody></table></div>`
                : '<p>Sem dados no período.</p>';
            const gerar = async () => {
                try {
                    const v = await api(`/admin/relatorios/vendas?inicio=${$('rel-ini').value}&fim=${$('rel-fim').value}`);
                    $('rel-vendas').innerHTML = tabela(['Categoria', 'Pedidos', 'Unidades', 'Faturamento'],
                        v.map(r => `<tr><td>${esc(r.categoria)}</td><td>${r.pedidos}</td><td>${r.unidadesVendidas}</td><td>${brl(r.faturamento)}</td></tr>`));
                } catch (e) { mostrarAviso(e.message, true); }
            };
            $('rel-gerar').addEventListener('click', gerar); gerar();
            const top = await api('/admin/relatorios/mais-vendidos?top=10');
            $('rel-top').innerHTML = tabela(['#', 'Produto', 'Unidades', 'Faturamento'],
                top.map((r, i) => `<tr><td>${i + 1}</td><td>${esc(r.nome)}</td><td>${r.unidadesVendidas}</td><td>${brl(r.faturamento)}</td></tr>`));
        }
    };

    async function abrirTela(nome) {
        document.querySelectorAll('.profile-link[data-aba]').forEach(a => a.classList.toggle('active', a.dataset.aba === nome));
        try { await telas[nome](); } catch (e) { painel.innerHTML = `<p style="color:#e63946">${esc(e.message)}</p>`; }
    }
    document.querySelectorAll('.profile-link[data-aba]').forEach(a => a.addEventListener('click', (e) => { e.preventDefault(); abrirTela(a.dataset.aba); }));
    $('btn-sair').addEventListener('click', () => encerrarSessao());
    abrirTela('produtos');
}

// ================= Inicialização por página =================
document.addEventListener('DOMContentLoaded', () => {
    const rotas = {
        'index.html': iniciarIndex, '': iniciarIndex, 'categoria.html': iniciarCategoria, 'produto.html': iniciarProduto,
        'carrinho.html': iniciarCarrinho, 'checkout.html': iniciarCheckout, 'favoritos.html': iniciarFavoritos,
        'login.html': iniciarLogin, 'cadastro.html': iniciarCadastro, 'perfil.html': iniciarPerfil, 'admin.html': iniciarAdmin
    };
    const fn = rotas[PAGINA];
    if (fn) fn();
});
