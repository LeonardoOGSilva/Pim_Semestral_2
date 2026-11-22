package com.example.voxtech;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

// Catálogo: lista produtos da API, com busca e filtro por categoria (GET /api/produtos, /api/categorias).
public class TelaProdutos extends BaseTela {

    private LinearLayout containerProdutos, containerCategorias;
    private TextView badge;
    private EditText edtBusca;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable buscaPendente;

    private int categoriaSelecionada = 0; // 0 = todas
    private int versaoBusca = 0;          // descarta respostas antigas se o usuário digitar de novo

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tela_produtos);

        containerProdutos = findViewById(R.id.containerProdutos);
        containerCategorias = findViewById(R.id.containerCategorias);
        badge = findViewById(R.id.badgeCarrinho);
        edtBusca = findViewById(R.id.edtBusca);

        TextView txtNome = findViewById(R.id.txtNomeUsuarioHeader);
        TextView txtEmail = findViewById(R.id.txtEmailUsuarioHeader);
        TextView btnSair = findViewById(R.id.btnSairLoja);

        if (Sessao.logado()) {
            txtNome.setText("Olá, " + Sessao.nome());
            txtEmail.setText(Sessao.email());
            btnSair.setText("Sair");
        } else {
            txtNome.setText("Visitante");
            txtEmail.setText("Entre para comprar");
            btnSair.setText("Entrar");
        }

        // Sair (ou Entrar, quando visitante) volta para a tela de login.
        btnSair.setOnClickListener(v -> {
            Sessao.sair();
            irParaLogin();
        });

        findViewById(R.id.btnIrCarrinhoTopo).setOnClickListener(v -> abrirSeLogado(TelaCarrinho.class));
        findViewById(R.id.btnFavoritos).setOnClickListener(v -> abrirSeLogado(TelaFavoritos.class));
        findViewById(R.id.btnPedidos).setOnClickListener(v -> abrirSeLogado(TelaPedidos.class));

        edtBusca.setOnEditorActionListener((v, acao, ev) -> {
            if (acao == EditorInfo.IME_ACTION_SEARCH) {
                carregarProdutos();
                return true;
            }
            return false;
        });
        edtBusca.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }

            @Override
            public void afterTextChanged(Editable s) {
                // Espera o usuário parar de digitar por meio segundo antes de buscar.
                if (buscaPendente != null) handler.removeCallbacks(buscaPendente);
                buscaPendente = TelaProdutos.this::carregarProdutos;
                handler.postDelayed(buscaPendente, 500);
            }
        });

        findViewById(R.id.btnAtualizar).setOnClickListener(v -> {
            carregarCategorias();
            carregarProdutos();
        });

        carregarCategorias();
        carregarProdutos();
    }

    @Override
    protected void onResume() {
        super.onResume();
        atualizarBadge();
    }

    private void abrirSeLogado(Class<?> tela) {
        if (!Sessao.logado()) {
            aviso("Entre na sua conta para usar este recurso.");
            return;
        }
        startActivity(new Intent(this, tela));
    }

    // Contador de itens no ícone do carrinho.
    private void atualizarBadge() {
        if (!Sessao.logado()) {
            badge.setVisibility(View.GONE);
            return;
        }
        Api.get("/carrinho", new Api.Retorno() {
            @Override
            public void ok(Object dados) {
                if (!viva()) return;
                JSONArray itens = ((JSONObject) dados).optJSONArray("itens");
                int total = 0;
                if (itens != null) {
                    for (int i = 0; i < itens.length(); i++) total += itens.optJSONObject(i).optInt("quantidade");
                }
                badge.setText(total > 99 ? "99+" : String.valueOf(total));
                badge.setVisibility(total > 0 ? View.VISIBLE : View.GONE);
            }

            @Override
            public void erro(String mensagem, int status) {
                if (viva()) badge.setVisibility(View.GONE);
            }
        });
    }

    // ---------- Categorias ----------
    private void carregarCategorias() {
        Api.get("/categorias", new Api.Retorno() {
            @Override
            public void ok(Object dados) {
                if (!viva()) return;
                containerCategorias.removeAllViews();
                adicionarChip("Todas", 0);
                JSONArray lista = (JSONArray) dados;
                for (int i = 0; i < lista.length(); i++) {
                    JSONObject c = lista.optJSONObject(i);
                    adicionarChip(c.optString("nome"), c.optInt("id"));
                }
            }

            @Override
            public void erro(String mensagem, int status) { /* o erro aparece na lista de produtos */ }
        });
    }

    private void adicionarChip(String nome, int id) {
        TextView chip = Ui.texto(this, nome, 13, Ui.BRANCO, true);
        chip.setTag(id);
        chip.setGravity(Gravity.CENTER);
        chip.setPadding(Ui.dp(this, 16), Ui.dp(this, 8), Ui.dp(this, 16), Ui.dp(this, 8));
        LinearLayout.LayoutParams lp = Ui.lp(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, Ui.dp(this, 8), 0);
        chip.setLayoutParams(lp);
        chip.setOnClickListener(v -> {
            categoriaSelecionada = id;
            pintarChips();
            carregarProdutos();
        });
        containerCategorias.addView(chip);
        pintarChips();
    }

    private void pintarChips() {
        for (int i = 0; i < containerCategorias.getChildCount(); i++) {
            TextView chip = (TextView) containerCategorias.getChildAt(i);
            boolean ativo = ((Integer) chip.getTag()) == categoriaSelecionada;
            chip.setBackground(ativo ? Ui.fundo(this, Ui.ROXO, 20) : Ui.fundoComBorda(this, Ui.CAMPO, 20, Ui.BORDA));
            chip.setTextColor(ativo ? Ui.BRANCO : Ui.MUTED);
        }
    }

    // ---------- Produtos ----------
    private void carregarProdutos() {
        final int minhaVersao = ++versaoBusca;
        String caminho = "/produtos";
        String busca = edtBusca.getText().toString().trim();
        StringBuilder q = new StringBuilder();
        if (categoriaSelecionada > 0) q.append("categoriaId=").append(categoriaSelecionada);
        if (!busca.isEmpty()) {
            if (q.length() > 0) q.append('&');
            q.append("busca=").append(java.net.URLEncoder.encode(busca));
        }
        if (q.length() > 0) caminho += "?" + q;

        containerProdutos.removeAllViews();
        containerProdutos.addView(Ui.texto(this, "Carregando produtos...", 14, Ui.MUTED, false));

        Api.get(caminho, new Api.Retorno() {
            @Override
            public void ok(Object dados) {
                if (!viva() || minhaVersao != versaoBusca) return;
                desenharProdutos((JSONArray) dados);
            }

            @Override
            public void erro(String mensagem, int status) {
                if (!viva() || minhaVersao != versaoBusca) return;
                containerProdutos.removeAllViews();
                TextView t = Ui.texto(TelaProdutos.this, mensagem, 14, Ui.ERRO, false);
                containerProdutos.addView(t);
                TextView tentar = Ui.botao(TelaProdutos.this, "Tentar novamente", false);
                Ui.margem(TelaProdutos.this, tentar, 0, 16, 0, 0);
                tentar.setOnClickListener(v -> carregarProdutos());
                containerProdutos.addView(tentar);
            }
        });
    }

    private void desenharProdutos(JSONArray lista) {
        containerProdutos.removeAllViews();
        if (lista.length() == 0) {
            containerProdutos.addView(Ui.texto(this, "Nenhum produto encontrado.", 14, Ui.MUTED, false));
            return;
        }
        // Duas colunas: cada linha tem dois cartões de mesmo tamanho.
        for (int i = 0; i < lista.length(); i += 2) {
            LinearLayout linha = new LinearLayout(this);
            linha.setOrientation(LinearLayout.HORIZONTAL);
            linha.setLayoutParams(Ui.lp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            Ui.margem(this, linha, 0, 0, 0, 12);
            linha.addView(cartaoProduto(lista.optJSONObject(i), i % 2 == 0));
            if (i + 1 < lista.length()) linha.addView(cartaoProduto(lista.optJSONObject(i + 1), false));
            else linha.addView(new View(this), peso());
            containerProdutos.addView(linha);
        }
    }

    private LinearLayout.LayoutParams peso() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
        lp.setMargins(Ui.dp(this, 6), 0, 0, 0);
        return lp;
    }

    private View cartaoProduto(JSONObject p, boolean primeiraColuna) {
        final int id = p.optInt("id");
        final String nome = p.optString("nome");
        final boolean esgotado = p.optInt("estoque") <= 0;

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(Ui.fundo(this, Ui.CARD, 16));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
        lp.setMargins(primeiraColuna ? 0 : Ui.dp(this, 6), 0, primeiraColuna ? Ui.dp(this, 6) : 0, 0);
        card.setLayoutParams(lp);
        card.setClickable(true);
        card.setFocusable(true);
        card.setOnClickListener(v -> abrirDetalhe(id));

        // Foto sobre fundo branco (as fotos do catálogo têm fundo claro).
        FrameLayout moldura = new FrameLayout(this);
        moldura.setBackground(Ui.fundo(this, Ui.BRANCO, 16));
        moldura.setLayoutParams(Ui.lp(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 130)));
        ImageView foto = new ImageView(this);
        foto.setScaleType(ImageView.ScaleType.FIT_CENTER);
        int pad = Ui.dp(this, 10);
        foto.setPadding(pad, pad, pad, pad);
        moldura.addView(foto, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        Img.carregar(foto, p.optString("imagemUrl"));
        card.addView(moldura);

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setPadding(Ui.dp(this, 12), Ui.dp(this, 10), Ui.dp(this, 12), Ui.dp(this, 12));
        card.addView(info);

        TextView txtNome = Ui.texto(this, nome, 13, Ui.BRANCO, true);
        txtNome.setMinLines(2);
        txtNome.setMaxLines(2);
        txtNome.setEllipsize(android.text.TextUtils.TruncateAt.END);
        info.addView(txtNome);

        TextView txtPreco = Ui.texto(this, Ui.brl(p.optDouble("preco")), 16, Ui.LILAS, true);
        txtPreco.setPadding(0, Ui.dp(this, 4), 0, Ui.dp(this, 8));
        info.addView(txtPreco);

        TextView btn = Ui.botao(this, esgotado ? "Esgotado" : "ADICIONAR", true);
        btn.setTextSize(11);
        btn.setPadding(Ui.dp(this, 6), Ui.dp(this, 10), Ui.dp(this, 6), Ui.dp(this, 10));
        if (esgotado) {
            btn.setEnabled(false);
            btn.setAlpha(0.5f);
        } else {
            btn.setOnClickListener(v -> adicionarAoCarrinho(id, btn));
        }
        info.addView(btn);
        return card;
    }

    private void abrirDetalhe(int id) {
        Intent i = new Intent(this, TelaDetalheProdutos.class);
        i.putExtra("PRODUTO_ID", id);
        startActivity(i);
    }

    private void adicionarAoCarrinho(int produtoId, TextView btn) {
        if (!Sessao.logado()) {
            aviso("Entre na sua conta para adicionar ao carrinho.");
            return;
        }
        JSONObject corpo = new JSONObject();
        try {
            corpo.put("produtoId", produtoId);
            corpo.put("quantidade", 1);
        } catch (JSONException e) {
            return;
        }
        btn.setEnabled(false);
        Api.post("/carrinho", corpo, new Api.Retorno() {
            @Override
            public void ok(Object dados) {
                if (!viva()) return;
                btn.setEnabled(true);
                aviso("Produto adicionado ao carrinho.");
                atualizarBadge();
            }

            @Override
            public void erro(String mensagem, int status) {
                if (!viva()) return;
                btn.setEnabled(true);
                tratarErro(mensagem, status);
            }
        });
    }
}
