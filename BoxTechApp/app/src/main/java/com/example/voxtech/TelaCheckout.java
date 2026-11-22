package com.example.voxtech;

import android.content.Intent;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

// Checkout: escolhe/cadastra endereço (com ViaCEP), forma de pagamento (simulada) e finaliza
// (POST /api/pedidos/checkout). Preço e frete oficiais são sempre os calculados pelo servidor.
public class TelaCheckout extends BaseLista {

    private static final int[] OPCOES_PARCELAS = {1, 2, 3, 5, 10};

    private LinearLayout secEnderecos, formEndereco, secPagamento, secResumo;
    private EditText fApelido, fCep, fLogradouro, fNumero, fComplemento, fBairro, fCidade, fUf;
    private TextView btnFinalizar;

    private JSONArray enderecos = new JSONArray();
    private JSONObject carrinho;
    private int enderecoSelecionado = 0;
    private String metodo = "Pix";
    private int parcelas = 1;

    @Override
    protected String titulo() { return "Finalizar compra"; }

    @Override
    protected void iniciar() {
        if (!exigirLogin()) return;

        secEnderecos = secao("ENDEREÇO DE ENTREGA");
        formEndereco = criarFormulario();
        secPagamento = secao("PAGAMENTO (SIMULADO)");
        secResumo = secao("RESUMO DO PEDIDO");

        btnFinalizar = Ui.botao(this, "Finalizar pedido", true);
        rodape.setVisibility(View.VISIBLE);
        rodape.addView(btnFinalizar);
        btnFinalizar.setOnClickListener(v -> finalizar());

        carregarTudo();
    }

    // ---------- Carregamento ----------
    private void carregarTudo() {
        Api.get("/carrinho", new Api.Retorno() {
            @Override
            public void ok(Object dados) {
                if (!viva()) return;
                carrinho = (JSONObject) dados;
                JSONArray itens = carrinho.optJSONArray("itens");
                if (itens == null || itens.length() == 0) {
                    aviso("Seu carrinho está vazio.");
                    finish();
                    return;
                }
                carregarEnderecos(0);
            }

            @Override
            public void erro(String mensagem, int status) {
                if (viva()) tratarErro(mensagem, status);
            }
        });
    }

    private void carregarEnderecos(int selecionarId) {
        Api.get("/enderecos", new Api.Retorno() {
            @Override
            public void ok(Object dados) {
                if (!viva()) return;
                enderecos = (JSONArray) dados;
                enderecoSelecionado = selecionarId;
                // Sem seleção explícita: usa o principal, ou o primeiro da lista.
                if (enderecoSelecionado == 0 && enderecos.length() > 0) {
                    enderecoSelecionado = enderecos.optJSONObject(0).optInt("id");
                    for (int i = 0; i < enderecos.length(); i++) {
                        if (enderecos.optJSONObject(i).optBoolean("principal"))
                            enderecoSelecionado = enderecos.optJSONObject(i).optInt("id");
                    }
                }
                desenharEnderecos();
                desenharPagamento();
                desenharResumo();
                formEndereco.setVisibility(enderecos.length() == 0 ? View.VISIBLE : View.GONE);
            }

            @Override
            public void erro(String mensagem, int status) {
                if (viva()) tratarErro(mensagem, status);
            }
        });
    }

    // ---------- Seções ----------
    private LinearLayout secao(String titulo) {
        TextView t = Ui.texto(this, titulo, 11, Ui.MUTED, true);
        t.setLetterSpacing(0.1f);
        Ui.margem(this, t, 0, 8, 0, 8);
        container.addView(t);
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setLayoutParams(Ui.lp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        container.addView(l);
        return l;
    }

    private void desenharEnderecos() {
        secEnderecos.removeAllViews();
        for (int i = 0; i < enderecos.length(); i++) {
            JSONObject e = enderecos.optJSONObject(i);
            final int id = e.optInt("id");
            boolean ativo = id == enderecoSelecionado;

            LinearLayout card = Ui.cartao(this);
            card.setBackground(Ui.fundoComBorda(this, Ui.CARD, 16, ativo ? Ui.ROXO : Ui.BORDA));
            card.setClickable(true);
            card.addView(Ui.texto(this, (ativo ? "● " : "○ ") + e.optString("apelido"), 14, ativo ? Ui.LILAS : Ui.BRANCO, true));
            String compl = e.optString("complemento", "");
            if (compl.equals("null")) compl = "";
            String linha = e.optString("logradouro") + ", " + e.optString("numero") + (compl.isEmpty() ? "" : " - " + compl);
            card.addView(Ui.texto(this, linha, 13, Ui.MUTED, false));
            card.addView(Ui.texto(this, e.optString("bairro") + " · " + e.optString("cidade") + "/" + e.optString("estado")
                    + " · CEP " + e.optString("cep"), 12, Ui.MUTED, false));
            card.setOnClickListener(v -> {
                enderecoSelecionado = id;
                desenharEnderecos();
                desenharResumo();
            });
            secEnderecos.addView(card);
        }

        TextView novo = Ui.botao(this, formEndereco != null && formEndereco.getVisibility() == View.VISIBLE
                ? "Fechar formulário" : "+ Novo endereço", false);
        novo.setOnClickListener(v -> {
            boolean abrir = formEndereco.getVisibility() != View.VISIBLE;
            formEndereco.setVisibility(abrir ? View.VISIBLE : View.GONE);
            desenharEnderecos();
        });
        secEnderecos.addView(novo);
    }

    private LinearLayout criarFormulario() {
        LinearLayout f = Ui.cartao(this);
        Ui.margem(this, f, 0, 12, 0, 12);
        f.setVisibility(View.GONE);

        fApelido = campo(f, "Apelido (ex.: Casa)", InputType.TYPE_CLASS_TEXT);
        fApelido.setText("Casa");
        fCep = campo(f, "CEP (somente números)", InputType.TYPE_CLASS_NUMBER);
        fLogradouro = campo(f, "Rua / Avenida", InputType.TYPE_CLASS_TEXT);
        fNumero = campo(f, "Número", InputType.TYPE_CLASS_TEXT);
        fComplemento = campo(f, "Complemento (opcional)", InputType.TYPE_CLASS_TEXT);
        fBairro = campo(f, "Bairro", InputType.TYPE_CLASS_TEXT);
        fCidade = campo(f, "Cidade", InputType.TYPE_CLASS_TEXT);
        fUf = campo(f, "UF (ex.: SP)", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);

        // Ao completar 8 dígitos, o ViaCEP preenche o restante.
        fCep.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }

            @Override
            public void afterTextChanged(Editable s) {
                String limpo = s.toString().replaceAll("\\D", "");
                if (limpo.length() == 8) buscarCep(limpo);
            }
        });

        TextView salvar = Ui.botao(this, "Salvar endereço", true);
        Ui.margem(this, salvar, 0, 8, 0, 0);
        salvar.setOnClickListener(v -> salvarEndereco(salvar));
        f.addView(salvar);
        container.addView(f, container.indexOfChild(secEnderecos) + 1);
        return f;
    }

    private EditText campo(LinearLayout pai, String dica, int tipo) {
        EditText e = new EditText(this);
        e.setHint(dica);
        e.setInputType(tipo);
        e.setSingleLine(true);
        e.setTextColor(Ui.BRANCO);
        e.setHintTextColor(android.graphics.Color.parseColor("#5E566E"));
        e.setTextSize(14);
        e.setBackground(Ui.fundo(this, Ui.CAMPO, 10));
        e.setPadding(Ui.dp(this, 12), Ui.dp(this, 12), Ui.dp(this, 12), Ui.dp(this, 12));
        e.setLayoutParams(Ui.lp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        Ui.margem(this, e, 0, 0, 0, 8);
        pai.addView(e);
        return e;
    }

    private void buscarCep(String cep) {
        Api.viaCep(cep, new Api.Retorno() {
            @Override
            public void ok(Object dados) {
                if (!viva()) return;
                JSONObject d = (JSONObject) dados;
                fLogradouro.setText(d.optString("logradouro"));
                fBairro.setText(d.optString("bairro"));
                fCidade.setText(d.optString("localidade"));
                fUf.setText(d.optString("uf"));
                fNumero.requestFocus();
            }

            @Override
            public void erro(String mensagem, int status) {
                if (viva()) aviso(mensagem);
            }
        });
    }

    private void salvarEndereco(TextView botao) {
        String cep = fCep.getText().toString().replaceAll("\\D", "");
        String uf = fUf.getText().toString().trim().toUpperCase();
        if (fApelido.getText().toString().trim().isEmpty() || cep.length() != 8
                || fLogradouro.getText().toString().trim().isEmpty()
                || fNumero.getText().toString().trim().isEmpty()
                || fBairro.getText().toString().trim().isEmpty()
                || fCidade.getText().toString().trim().isEmpty() || uf.length() != 2) {
            aviso("Preencha CEP, rua, número, bairro, cidade e UF.");
            return;
        }
        JSONObject corpo = new JSONObject();
        try {
            corpo.put("apelido", fApelido.getText().toString().trim());
            corpo.put("cep", cep);
            corpo.put("logradouro", fLogradouro.getText().toString().trim());
            corpo.put("numero", fNumero.getText().toString().trim());
            String compl = fComplemento.getText().toString().trim();
            corpo.put("complemento", compl.isEmpty() ? JSONObject.NULL : compl);
            corpo.put("bairro", fBairro.getText().toString().trim());
            corpo.put("cidade", fCidade.getText().toString().trim());
            corpo.put("estado", uf);
            corpo.put("principal", enderecos.length() == 0);
        } catch (JSONException e) {
            return;
        }
        Ui.habilitar(botao, false, "Salvar endereço", "Salvando...");
        Api.post("/enderecos", corpo, new Api.Retorno() {
            @Override
            public void ok(Object dados) {
                if (!viva()) return;
                Ui.habilitar(botao, true, "Salvar endereço", "");
                int novoId = dados instanceof JSONObject ? ((JSONObject) dados).optInt("id") : 0;
                for (EditText e : new EditText[]{fCep, fLogradouro, fNumero, fComplemento, fBairro, fCidade, fUf}) e.setText("");
                formEndereco.setVisibility(View.GONE);
                aviso("Endereço salvo.");
                carregarEnderecos(novoId);
            }

            @Override
            public void erro(String mensagem, int status) {
                if (!viva()) return;
                Ui.habilitar(botao, true, "Salvar endereço", "");
                tratarErro(mensagem, status);
            }
        });
    }

    // ---------- Pagamento ----------
    private void desenharPagamento() {
        secPagamento.removeAllViews();
        LinearLayout linha = new LinearLayout(this);
        linha.setLayoutParams(Ui.lp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        linha.addView(opcaoMetodo("Pix", "Pix"));
        linha.addView(opcaoMetodo("Cartão de crédito", "Cartao"));
        secPagamento.addView(linha);

        if (metodo.equals("Cartao")) {
            double total = totalComFrete();
            TextView lbl = Ui.texto(this, "PARCELAS", 11, Ui.MUTED, true);
            Ui.margem(this, lbl, 0, 14, 0, 6);
            secPagamento.addView(lbl);
            for (int n : OPCOES_PARCELAS) {
                boolean ativo = n == parcelas;
                TextView op = Ui.texto(this, (ativo ? "● " : "○ ") + n + "x de " + Ui.brl(total / n) + " sem juros",
                        14, ativo ? Ui.LILAS : Ui.BRANCO, ativo);
                op.setPadding(Ui.dp(this, 4), Ui.dp(this, 8), 0, Ui.dp(this, 8));
                op.setOnClickListener(v -> {
                    parcelas = n;
                    desenharPagamento();
                });
                secPagamento.addView(op);
            }
        }
        TextView nota = Ui.texto(this, "Pagamento simulado: nenhum dado de cartão é coletado ou armazenado.", 11, Ui.MUTED, false);
        Ui.margem(this, nota, 0, 10, 0, 0);
        secPagamento.addView(nota);
    }

    private TextView opcaoMetodo(String rotulo, String valor) {
        boolean ativo = metodo.equals(valor);
        TextView t = Ui.texto(this, rotulo, 14, ativo ? Ui.BRANCO : Ui.MUTED, true);
        t.setGravity(Gravity.CENTER);
        t.setBackground(ativo ? Ui.fundo(this, Ui.ROXO, 12) : Ui.fundoComBorda(this, Ui.CAMPO, 12, Ui.BORDA));
        t.setPadding(Ui.dp(this, 10), Ui.dp(this, 13), Ui.dp(this, 10), Ui.dp(this, 13));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lp.setMargins(valor.equals("Pix") ? 0 : Ui.dp(this, 4), 0, valor.equals("Pix") ? Ui.dp(this, 4) : 0, 0);
        t.setLayoutParams(lp);
        t.setOnClickListener(v -> {
            metodo = valor;
            if (valor.equals("Pix")) parcelas = 1;
            desenharPagamento();
        });
        return t;
    }

    // ---------- Resumo ----------
    // Mesma regra de frete do servidor (PedidoService). O valor oficial é o do servidor.
    private double calcularFrete(String uf, double subtotal) {
        if (subtotal >= 1000) return 0;
        switch (uf.toUpperCase()) {
            case "SP": return 15;
            case "RJ": case "MG": case "ES": return 25;
            case "PR": case "SC": case "RS": return 30;
            case "DF": case "GO": case "MT": case "MS": return 35;
            default: return 45;
        }
    }

    private JSONObject enderecoAtual() {
        for (int i = 0; i < enderecos.length(); i++) {
            if (enderecos.optJSONObject(i).optInt("id") == enderecoSelecionado) return enderecos.optJSONObject(i);
        }
        return null;
    }

    private double frete() {
        JSONObject e = enderecoAtual();
        return e == null ? 0 : calcularFrete(e.optString("estado"), carrinho.optDouble("total"));
    }

    private double totalComFrete() { return carrinho.optDouble("total") + frete(); }

    private void desenharResumo() {
        secResumo.removeAllViews();
        LinearLayout card = Ui.cartao(this);
        JSONArray itens = carrinho.optJSONArray("itens");
        for (int i = 0; i < itens.length(); i++) {
            JSONObject it = itens.optJSONObject(i);
            card.addView(linhaValor(it.optString("nome") + " (x" + it.optInt("quantidade") + ")",
                    Ui.brl(it.optDouble("subtotal")), false));
        }
        JSONObject e = enderecoAtual();
        double f = frete();
        card.addView(linhaValor("Frete" + (e != null ? " (" + e.optString("estado") + ")" : ""),
                e == null ? "—" : (f == 0 ? "Grátis" : Ui.brl(f)), false));
        card.addView(linhaValor("Total", Ui.brl(totalComFrete()), true));
        secResumo.addView(card);
        desenharPagamento();
    }

    private LinearLayout linhaValor(String esquerda, String direita, boolean destaque) {
        LinearLayout l = new LinearLayout(this);
        l.setLayoutParams(Ui.lp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        l.setPadding(0, Ui.dp(this, 3), 0, Ui.dp(this, 3));
        TextView a = Ui.texto(this, esquerda, destaque ? 16 : 13, destaque ? Ui.BRANCO : Ui.MUTED, destaque);
        a.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        l.addView(a);
        l.addView(Ui.texto(this, direita, destaque ? 18 : 13, destaque ? Ui.LILAS : Ui.BRANCO, destaque));
        return l;
    }

    // ---------- Finalizar ----------
    private void finalizar() {
        if (enderecoAtual() == null) {
            aviso("Cadastre ou selecione um endereço de entrega.");
            return;
        }
        JSONObject corpo = new JSONObject();
        try {
            corpo.put("enderecoId", enderecoSelecionado);
            corpo.put("metodo", metodo);
            corpo.put("parcelas", metodo.equals("Pix") ? 1 : parcelas);
        } catch (JSONException e) {
            return;
        }
        Ui.habilitar(btnFinalizar, false, "Finalizar pedido", "Processando...");
        Api.post("/pedidos/checkout", corpo, new Api.Retorno() {
            @Override
            public void ok(Object dados) {
                if (!viva()) return;
                JSONObject pedido = (JSONObject) dados;
                new AlertDialog.Builder(TelaCheckout.this)
                        .setTitle("Pedido realizado!")
                        .setMessage("Pedido #" + pedido.optInt("id") + "\nTotal: " + Ui.brl(pedido.optDouble("valorTotal")))
                        .setCancelable(false)
                        .setPositiveButton("Ver meus pedidos", (d, w) -> {
                            startActivity(new Intent(TelaCheckout.this, TelaPedidos.class));
                            finish();
                        })
                        .show();
            }

            @Override
            public void erro(String mensagem, int status) {
                if (!viva()) return;
                Ui.habilitar(btnFinalizar, true, "Finalizar pedido", "");
                tratarErro(mensagem, status);
            }
        });
    }
}
