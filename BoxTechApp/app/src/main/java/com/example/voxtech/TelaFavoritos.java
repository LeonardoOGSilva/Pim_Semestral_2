package com.example.voxtech;

import android.content.Intent;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

// Lista de favoritos do cliente (GET/DELETE /api/favoritos).
public class TelaFavoritos extends BaseLista {

    @Override
    protected String titulo() { return "Favoritos"; }

    @Override
    protected void iniciar() {
        if (!exigirLogin()) return;
        carregar();
    }

    @Override
    protected boolean podeAtualizar() { return true; }

    @Override
    protected void atualizar() { if (Sessao.logado()) carregar(); }

    private void carregar() {
        mensagem("Carregando...", Ui.MUTED);
        Api.get("/favoritos", new Api.Retorno() {
            @Override
            public void ok(Object dados) {
                if (!viva()) return;
                desenhar((JSONArray) dados);
            }

            @Override
            public void erro(String mensagem, int status) {
                if (!viva()) return;
                mensagem(mensagem, Ui.ERRO);
                if (status == 401) irParaLogin();
            }
        });
    }

    private void desenhar(JSONArray lista) {
        container.removeAllViews();
        if (lista.length() == 0) {
            mensagem("Você ainda não tem favoritos.", Ui.MUTED);
            return;
        }
        for (int i = 0; i < lista.length(); i++) container.addView(linha(lista.optJSONObject(i)));
    }

    private LinearLayout linha(JSONObject f) {
        final int produtoId = f.optInt("produtoId");
        LinearLayout card = Ui.cartao(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setClickable(true);
        card.setOnClickListener(v -> {
            Intent i = new Intent(this, TelaDetalheProdutos.class);
            i.putExtra("PRODUTO_ID", produtoId);
            startActivity(i);
        });

        FrameLayout moldura = new FrameLayout(this);
        moldura.setBackground(Ui.fundo(this, Ui.BRANCO, 12));
        moldura.setLayoutParams(Ui.lp(Ui.dp(this, 76), Ui.dp(this, 76)));
        ImageView foto = new ImageView(this);
        foto.setScaleType(ImageView.ScaleType.FIT_CENTER);
        int pad = Ui.dp(this, 6);
        foto.setPadding(pad, pad, pad, pad);
        moldura.addView(foto, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        Img.carregar(foto, f.optString("imagemUrl"));
        card.addView(moldura);

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lpInfo = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lpInfo.setMargins(Ui.dp(this, 12), 0, Ui.dp(this, 8), 0);
        info.setLayoutParams(lpInfo);
        info.addView(Ui.texto(this, f.optString("nome"), 14, Ui.BRANCO, true));
        TextView preco = Ui.texto(this, Ui.brl(f.optDouble("preco")), 15, Ui.LILAS, true);
        preco.setPadding(0, Ui.dp(this, 4), 0, 0);
        info.addView(preco);
        card.addView(info);

        TextView remover = Ui.texto(this, "♥", 24, Ui.LILAS, false);
        remover.setPadding(Ui.dp(this, 10), Ui.dp(this, 6), Ui.dp(this, 6), Ui.dp(this, 6));
        remover.setOnClickListener(v -> removerFavorito(produtoId));
        card.addView(remover);
        return card;
    }

    private void removerFavorito(int produtoId) {
        Api.delete("/favoritos/" + produtoId, new Api.Retorno() {
            @Override
            public void ok(Object dados) {
                if (!viva()) return;
                aviso("Removido dos favoritos.");
                carregar();
            }

            @Override
            public void erro(String mensagem, int status) {
                if (viva()) tratarErro(mensagem, status);
            }
        });
    }
}
