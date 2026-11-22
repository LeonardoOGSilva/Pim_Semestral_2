package com.example.voxtech;

import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import org.json.JSONArray;
import org.json.JSONObject;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

// Histórico de pedidos (GET /api/pedidos) e cancelamento (POST /api/pedidos/{id}/cancelar).
public class TelaPedidos extends BaseLista {

    @Override
    protected String titulo() { return "Meus pedidos"; }

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
        Api.get("/pedidos", new Api.Retorno() {
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
            mensagem("Você ainda não fez nenhum pedido.", Ui.MUTED);
            return;
        }
        for (int i = 0; i < lista.length(); i++) container.addView(cartaoPedido(lista.optJSONObject(i)));
    }

    private LinearLayout cartaoPedido(JSONObject p) {
        final int id = p.optInt("id");
        final String status = p.optString("status");
        LinearLayout card = Ui.cartao(this);

        LinearLayout topo = new LinearLayout(this);
        topo.setGravity(Gravity.CENTER_VERTICAL);
        TextView titulo = Ui.texto(this, "Pedido #" + id, 16, Ui.BRANCO, true);
        titulo.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        topo.addView(titulo);
        int corStatus = status.equals("Cancelado") ? Ui.ERRO : status.equals("Entregue") ? Ui.OK : Ui.LILAS;
        TextView selo = Ui.texto(this, status, 11, corStatus, true);
        selo.setBackground(Ui.fundoComBorda(this, Ui.BG, 10, corStatus));
        selo.setPadding(Ui.dp(this, 10), Ui.dp(this, 4), Ui.dp(this, 10), Ui.dp(this, 4));
        topo.addView(selo);
        card.addView(topo);

        card.addView(Ui.texto(this, formatarData(p.optString("dataPedido")), 12, Ui.MUTED, false));

        JSONArray itens = p.optJSONArray("itens");
        LinearLayout lista = new LinearLayout(this);
        lista.setOrientation(LinearLayout.VERTICAL);
        Ui.margem(this, lista, 0, 10, 0, 0);
        if (itens != null) {
            for (int i = 0; i < itens.length(); i++) {
                JSONObject it = itens.optJSONObject(i);
                LinearLayout linha = new LinearLayout(this);
                TextView nome = Ui.texto(this, it.optInt("quantidade") + "x " + it.optString("nome"), 13, Ui.BRANCO, false);
                nome.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
                linha.addView(nome);
                linha.addView(Ui.texto(this, Ui.brl(it.optDouble("subtotal")), 13, Ui.MUTED, false));
                lista.addView(linha);
            }
        }
        card.addView(lista);

        String metodo = p.optString("metodoPagamento", "");
        String pag = metodo.isEmpty() || metodo.equals("null") ? "-" : metodo;
        if (pag.equals("Cartao")) pag = "Cartão";
        TextView detalhes = Ui.texto(this,
                "Frete: " + Ui.brl(p.optDouble("valorFrete")) + "  ·  Pagamento: " + pag
                        + (p.optString("statusPagamento", "").isEmpty() || p.optString("statusPagamento").equals("null")
                        ? "" : " (" + p.optString("statusPagamento") + ")"),
                12, Ui.MUTED, false);
        Ui.margem(this, detalhes, 0, 8, 0, 0);
        card.addView(detalhes);

        TextView total = Ui.texto(this, "Total: " + Ui.brl(p.optDouble("valorTotal")), 17, Ui.LILAS, true);
        Ui.margem(this, total, 0, 6, 0, 0);
        card.addView(total);

        if (status.equals("Aguardando Pagamento") || status.equals("Pago")) {
            TextView cancelar = Ui.botao(this, "Cancelar pedido", false);
            cancelar.setTextColor(Ui.ERRO);
            Ui.margem(this, cancelar, 0, 12, 0, 0);
            cancelar.setOnClickListener(v -> confirmarCancelamento(id));
            card.addView(cancelar);
        }
        return card;
    }

    private void confirmarCancelamento(int id) {
        new AlertDialog.Builder(this)
                .setTitle("Cancelar pedido #" + id + "?")
                .setMessage("Os produtos voltam para o estoque.")
                .setPositiveButton("Cancelar pedido", (d, w) ->
                        Api.post("/pedidos/" + id + "/cancelar", null, new Api.Retorno() {
                            @Override
                            public void ok(Object dados) {
                                if (!viva()) return;
                                aviso("Pedido cancelado.");
                                carregar();
                            }

                            @Override
                            public void erro(String mensagem, int status) {
                                if (viva()) tratarErro(mensagem, status);
                            }
                        }))
                .setNegativeButton("Voltar", null)
                .show();
    }

    private String formatarData(String iso) {
        try {
            return LocalDateTime.parse(iso).format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        } catch (Exception e) {
            return iso;
        }
    }
}
