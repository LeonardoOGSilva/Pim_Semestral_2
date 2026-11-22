package com.example.voxtech;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import org.json.JSONException;
import org.json.JSONObject;

// Tela de login: autentica na API (POST /api/auth/login) e guarda o token JWT.
public class MainActivity extends BaseTela {

    private EditText campoEmail, campoSenha;
    private Button botaoEntrar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Já está logado: vai direto para a loja.
        if (Sessao.logado()) {
            abrirLoja();
            return;
        }

        setContentView(R.layout.activity_main);

        campoEmail = findViewById(R.id.editTextTextEmailAddress);
        campoSenha = findViewById(R.id.editTextTextPassword);
        botaoEntrar = findViewById(R.id.button);
        TextView txtSemUsuario = findViewById(R.id.textView2);
        TextView txtCriarConta = findViewById(R.id.txtCriarConta);
        TextView txtServidor = findViewById(R.id.txtServidor);

        botaoEntrar.setOnClickListener(v -> entrar());

        // Visitante pode ver o catálogo; carrinho, favoritos e pedidos exigem conta.
        txtSemUsuario.setOnClickListener(v -> abrirLoja());
        txtCriarConta.setOnClickListener(v -> startActivity(new Intent(this, TelaCadastro.class)));
        txtServidor.setOnClickListener(v -> configurarServidor());
    }

    private void entrar() {
        String email = campoEmail.getText().toString().trim();
        String senha = campoSenha.getText().toString();

        if (email.isEmpty() || senha.isEmpty()) {
            aviso("Digite seu e-mail e senha!");
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            aviso("Digite um e-mail válido (ex: usuario@email.com)");
            return;
        }

        JSONObject corpo = new JSONObject();
        try {
            corpo.put("email", email);
            corpo.put("senha", senha);
        } catch (JSONException e) {
            return;
        }

        botaoEntrar.setEnabled(false);
        botaoEntrar.setText("Entrando...");
        Api.post("/auth/login", corpo, new Api.Retorno() {
            @Override
            public void ok(Object dados) {
                if (!viva()) return;
                Sessao.salvar((JSONObject) dados, email);
                abrirLoja();
            }

            @Override
            public void erro(String mensagem, int status) {
                if (!viva()) return;
                botaoEntrar.setEnabled(true);
                botaoEntrar.setText("Entrar");
                aviso(status == 401 ? "E-mail ou senha incorretos." : mensagem);
            }
        });
    }

    private void abrirLoja() {
        startActivity(new Intent(this, TelaProdutos.class));
        finish();
    }

    // Permite apontar o app para o IP do notebook ou para um link público (túnel).
    private void configurarServidor() {
        int p = Ui.dp(this, 20);
        LinearLayout caixa = new LinearLayout(this);
        caixa.setOrientation(LinearLayout.VERTICAL);
        caixa.setPadding(p, p / 2, p, 0);

        TextView lblApi = new TextView(this);
        lblApi.setText("Endereço da API");
        EditText campoApi = new EditText(this);
        campoApi.setText(Config.entradaApi());
        campoApi.setHint("192.168.0.0  ou  https://xxxx.trycloudflare.com");
        campoApi.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        campoApi.setSingleLine(true);

        TextView lblImg = new TextView(this);
        lblImg.setText("Endereço das imagens (opcional)");
        lblImg.setPadding(0, Ui.dp(this, 12), 0, 0);
        EditText campoImg = new EditText(this);
        campoImg.setText(Config.entradaImagens());
        campoImg.setHint("vazio = mesmo IP, porta 5500");
        campoImg.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        campoImg.setSingleLine(true);

        caixa.addView(lblApi);
        caixa.addView(campoApi);
        caixa.addView(lblImg);
        caixa.addView(campoImg);

        new AlertDialog.Builder(this)
                .setTitle("Servidor")
                .setMessage("Wi-Fi: IP do notebook (ex.: 192.168.0.0).\n"
                        + "Internet do celular: link público do túnel (https://...).\n"
                        + "Emulador: 10.0.2.2")
                .setView(caixa)
                .setPositiveButton("Salvar", (d, w) -> {
                    Config.definir(campoApi.getText().toString(), campoImg.getText().toString());
                    aviso("API: " + Config.baseApi());
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
}
