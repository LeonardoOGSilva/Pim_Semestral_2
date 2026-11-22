package com.example.voxtech;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

// Detalhe do produto (GET /api/produtos/{id}), com favoritar e adicionar ao carrinho.
public class TelaDetalheProdutos extends BaseLista {

    private int produtoId;
    private boolean favorito = false;
    private TextView btnFavorito;

    @Override
    protected String titulo() { return "Produto"; }

    @Override
    protected void iniciar() {
        produtoId = getIntent().getIntExtra("PRODUTO_ID", 0);
        mensagem("Carregando...", Ui.MUTED);

        Api.get("/produtos/" + produtoId, new Api.Retorno() {
            @Override
            public void ok(Object dados) {
                if (!viva()) return;
                desenhar((JSONObject) dados);
                if (Sessao.logado()) descobrirFavorito();
            }

            @Override
            public void erro(String mensagem, int status) {
                if (!viva()) return;
                mensagem(mensagem, Ui.ERRO);
            }
        });
    }

    private void desenhar(JSONObject p) {
        container.removeAllViews();
        final String nome = p.optString("nome");
        final double preco = p.optDouble("preco");
        final int estoque = p.optInt("estoque");

        // Foto principal em moldura branca.
        FrameLayout moldura = new FrameLayout(this);
        moldura.setBackground(Ui.fundo(this, Ui.BRANCO, 18));
        moldura.setLayoutParams(Ui.lp(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 260)));
        ImageView foto = new ImageView(this);
        foto.setScaleType(ImageView.ScaleType.FIT_CENTER);
        int pad = Ui.dp(this, 16);
        foto.setPadding(pad, pad, pad, pad);
        moldura.addView(foto, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        String principal = p.optString("imagemUrl");
        JSONArray extras = p.optJSONArray("imagens");
        if (principal.isEmpty() && extras != null && extras.length() > 0) principal = extras.optString(0);
        Img.carregar(foto, principal);
        container.addView(moldura);

        // Linha com categoria/marca e o coração de favorito.
        LinearLayout linha = new LinearLayout(this);
        linha.setGravity(Gravity.CENTER_VERTICAL);
        linha.setLayoutParams(Ui.lp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        Ui.margem(this, linha, 0, 16, 0, 0);
        String sub = p.optString("categoria");
        String marca = p.optString("marca", "");
        if (!marca.isEmpty() && !marca.equals("null")) sub += " · " + marca;
        TextView txtSub = Ui.texto(this, sub.toUpperCase(), 11, Ui.LILAS, true);
        txtSub.setLetterSpacing(0.1f);
        txtSub.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        linha.addView(txtSub);
        btnFavorito = Ui.texto(this, "♡", 26, Ui.LILAS, false);
        btnFavorito.setGravity(Gravity.CENTER);
        btnFavorito.setOnClickListener(v -> alternarFavorito());
        linha.addView(btnFavorito);
        container.addView(linha);

        TextView txtNome = Ui.texto(this, nome, 22, Ui.BRANCO, true);
        Ui.margem(this, txtNome, 0, 4, 0, 0);
        container.addView(txtNome);

        TextView txtPreco = Ui.texto(this, Ui.brl(preco), 26, Ui.LILAS, true);
        Ui.margem(this, txtPreco, 0, 10, 0, 0);
        container.addView(txtPreco);

        TextView txtParcelas = Ui.texto(this, "ou em até 10x de " + Ui.brl(preco / 10) + " sem juros", 13, Ui.MUTED, false);
        container.addView(txtParcelas);

        TextView txtEstoque = Ui.texto(this,
                estoque > 0 ? "Em estoque (" + estoque + " unidades)" : "Produto esgotado",
                13, estoque > 0 ? Ui.OK : Ui.ERRO, true);
        Ui.margem(this, txtEstoque, 0, 10, 0, 0);
        container.addView(txtEstoque);

        TextView tituloDesc = Ui.texto(this, "DESCRIÇÃO", 11, Ui.MUTED, true);
        tituloDesc.setLetterSpacing(0.1f);
        Ui.margem(this, tituloDesc, 0, 20, 0, 6);
        container.addView(tituloDesc);
        container.addView(Ui.texto(this, p.optString("descricao"), 14, Ui.BRANCO, false));

        // Rodapé fixo com as ações de compra.
        rodape.removeAllViews();
        rodape.setVisibility(android.view.View.VISIBLE);
        TextView btnAdd = Ui.botao(this, estoque > 0 ? "Adicionar ao carrinho" : "Esgotado", true);
        Ui.margem(this, btnAdd, 0, 0, 0, 8);
        TextView btnComprar = Ui.botao(this, "Comprar agora", false);
        if (estoque > 0) {
            btnAdd.setOnClickListener(v -> adicionar(btnAdd, false));
            btnComprar.setOnClickListener(v -> adicionar(btnComprar, true));
        } else {
            btnAdd.setEnabled(false);
            btnAdd.setAlpha(0.5f);
            btnComprar.setVisibility(android.view.View.GONE);
        }
        rodape.addView(btnAdd);
        rodape.addView(btnComprar);
    }

    private void adicionar(TextView botao, boolean irParaCarrinho) {
        if (!exigirLoginSemSair()) return;
        JSONObject corpo = new JSONObject();
        try {
            corpo.put("produtoId", produtoId);
            corpo.put("quantidade", 1);
        } catch (JSONException e) {
            return;
        }
        botao.setEnabled(false);
        Api.post("/carrinho", corpo, new Api.Retorno() {
            @Override
            public void ok(Object dados) {
                if (!viva()) return;
                botao.setEnabled(true);
                aviso("Produto adicionado ao carrinho.");
                if (irParaCarrinho) startActivity(new Intent(TelaDetalheProdutos.this, TelaCarrinho.class));
            }

            @Override
            public void erro(String mensagem, int status) {
                if (!viva()) return;
                botao.setEnabled(true);
                tratarErro(mensagem, status);
            }
        });
    }

    // Visitante vê o produto, mas não compra: só avisa, sem fechar a tela.
    private boolean exigirLoginSemSair() {
        if (Sessao.logado()) return true;
        aviso("Entre na sua conta para comprar.");
        return false;
    }

    private void descobrirFavorito() {
        Api.get("/favoritos", new Api.Retorno() {
            @Override
            public void ok(Object dados) {
                if (!viva()) return;
                JSONArray lista = (JSONArray) dados;
                for (int i = 0; i < lista.length(); i++) {
                    if (lista.optJSONObject(i).optInt("produtoId") == produtoId) favorito = true;
                }
                pintarFavorito();
            }

            @Override
            public void erro(String mensagem, int status) { }
        });
    }

    private void pintarFavorito() {
        if (btnFavorito == null) return;
        btnFavorito.setText(favorito ? "♥" : "♡");
    }

    private void alternarFavorito() {
        if (!exigirLoginSemSair()) return;
        final boolean quer = !favorito;
        Api.Retorno r = new Api.Retorno() {
            @Override
            public void ok(Object dados) {
                if (!viva()) return;
                favorito = quer;
                pintarFavorito();
                aviso(quer ? "Adicionado aos favoritos." : "Removido dos favoritos.");
            }

            @Override
            public void erro(String mensagem, int status) {
                if (viva()) tratarErro(mensagem, status);
            }
        };
        if (quer) Api.post("/favoritos/" + produtoId, null, r);
        else Api.delete("/favoritos/" + produtoId, r);
    }
}
