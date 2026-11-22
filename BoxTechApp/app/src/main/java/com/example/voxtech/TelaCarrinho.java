package com.example.voxtech;

import android.content.Intent;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

// Carrinho salvo no banco (GET/PUT/DELETE /api/carrinho). O total vem calculado do servidor.
public class TelaCarrinho extends BaseLista {

    @Override
    protected String titulo() { return "Carrinho"; }

    @Override
    protected void iniciar() {
        if (!exigirLogin()) return;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Sessao.logado()) carregar();
    }

    @Override
    protected boolean podeAtualizar() { return true; }

    @Override
    protected void atualizar() { if (Sessao.logado()) carregar(); }

    private void carregar() {
        Api.get("/carrinho", new Api.Retorno() {
            @Override
            public void ok(Object dados) {
                if (!viva()) return;
                desenhar((JSONObject) dados);
            }

            @Override
            public void erro(String mensagem, int status) {
                if (!viva()) return;
                mensagem(mensagem, Ui.ERRO);
                if (status == 401) irParaLogin();
            }
        });
    }

    private void desenhar(JSONObject carrinho) {
        container.removeAllViews();
        JSONArray itens = carrinho.optJSONArray("itens");
        if (itens == null || itens.length() == 0) {
            rodape.setVisibility(View.GONE);
            mensagem("Seu carrinho está vazio no momento.", Ui.MUTED);
            return;
        }
        for (int i = 0; i < itens.length(); i++) container.addView(cartaoItem(itens.optJSONObject(i)));

        rodape.removeAllViews();
        rodape.setVisibility(View.VISIBLE);
        LinearLayout linhaTotal = new LinearLayout(this);
        linhaTotal.setGravity(Gravity.CENTER_VERTICAL);
        TextView lbl = Ui.texto(this, "Total", 15, Ui.MUTED, true);
        lbl.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        linhaTotal.addView(lbl);
        linhaTotal.addView(Ui.texto(this, Ui.brl(carrinho.optDouble("total")), 22, Ui.LILAS, true));
        rodape.addView(linhaTotal);
        rodape.addView(Ui.texto(this, "Frete calculado no checkout (grátis acima de R$ 1.000,00)", 11, Ui.MUTED, false));

        TextView finalizar = Ui.botao(this, "Finalizar compra", true);
        Ui.margem(this, finalizar, 0, 12, 0, 0);
        finalizar.setOnClickListener(v -> startActivity(new Intent(this, TelaCheckout.class)));
        rodape.addView(finalizar);
    }

    private LinearLayout cartaoItem(JSONObject item) {
        final int itemId = item.optInt("id");
        final int quantidade = item.optInt("quantidade");

        LinearLayout card = Ui.cartao(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);

        FrameLayout moldura = new FrameLayout(this);
        moldura.setBackground(Ui.fundo(this, Ui.BRANCO, 12));
        moldura.setLayoutParams(Ui.lp(Ui.dp(this, 64), Ui.dp(this, 64)));
        ImageView foto = new ImageView(this);
        foto.setScaleType(ImageView.ScaleType.FIT_CENTER);
        int pad = Ui.dp(this, 5);
        foto.setPadding(pad, pad, pad, pad);
        moldura.addView(foto, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        Img.carregar(foto, item.optString("imagemUrl"));
        card.addView(moldura);

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lpInfo = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lpInfo.setMargins(Ui.dp(this, 10), 0, Ui.dp(this, 6), 0);
        info.setLayoutParams(lpInfo);
        info.addView(Ui.texto(this, item.optString("nome"), 14, Ui.BRANCO, true));
        TextView preco = Ui.texto(this, Ui.brl(item.optDouble("subtotal")), 15, Ui.LILAS, true);
        preco.setPadding(0, Ui.dp(this, 4), 0, Ui.dp(this, 6));
        info.addView(preco);

        // Controle de quantidade: − n +
        LinearLayout qtd = new LinearLayout(this);
        qtd.setGravity(Gravity.CENTER_VERTICAL);
        qtd.setBackground(Ui.fundoComBorda(this, Ui.BG, 10, Ui.BORDA));
        qtd.setLayoutParams(Ui.lp(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        TextView menos = Ui.texto(this, "−", 18, quantidade > 1 ? Ui.LILAS : Ui.BORDA, true);
        TextView n = Ui.texto(this, String.valueOf(quantidade), 14, Ui.BRANCO, true);
        TextView mais = Ui.texto(this, "+", 18, Ui.LILAS, true);
        for (TextView t : new TextView[]{menos, n, mais}) {
            t.setGravity(Gravity.CENTER);
            t.setPadding(Ui.dp(this, 12), Ui.dp(this, 4), Ui.dp(this, 12), Ui.dp(this, 4));
            qtd.addView(t);
        }
        menos.setOnClickListener(v -> { if (quantidade > 1) atualizar(itemId, quantidade - 1); });
        mais.setOnClickListener(v -> atualizar(itemId, quantidade + 1));
        info.addView(qtd);
        card.addView(info);

        TextView lixeira = Ui.texto(this, "🗑", 20, Ui.LILAS, false);
        lixeira.setPadding(Ui.dp(this, 8), Ui.dp(this, 8), Ui.dp(this, 4), Ui.dp(this, 8));
        lixeira.setOnClickListener(v -> remover(itemId));
        card.addView(lixeira);
        return card;
    }

    private void atualizar(int itemId, int novaQuantidade) {
        JSONObject corpo = new JSONObject();
        try {
            corpo.put("quantidade", novaQuantidade);
        } catch (JSONException e) {
            return;
        }
        Api.put("/carrinho/" + itemId, corpo, recarregar());
    }

    private void remover(int itemId) {
        Api.delete("/carrinho/" + itemId, recarregar());
    }

    private Api.Retorno recarregar() {
        return new Api.Retorno() {
            @Override
            public void ok(Object dados) {
                if (viva()) carregar();
            }

            @Override
            public void erro(String mensagem, int status) {
                if (viva()) tratarErro(mensagem, status);
            }
        };
    }
}
