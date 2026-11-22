package com.example.voxtech;

import android.os.Bundle;
import android.util.Patterns;
import android.widget.EditText;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

// Cadastro de cliente: POST /api/auth/registro (a API já devolve o token e entra logado).
public class TelaCadastro extends BaseTela {

    private EditText edtNome, edtSobrenome, edtCpf, edtEmail, edtSenha, edtConfirmar;
    private TextView btnCadastrar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cadastro);

        edtNome = findViewById(R.id.edtNome);
        edtSobrenome = findViewById(R.id.edtSobrenome);
        edtCpf = findViewById(R.id.edtCpf);
        edtEmail = findViewById(R.id.edtEmail);
        edtSenha = findViewById(R.id.edtSenha);
        edtConfirmar = findViewById(R.id.edtConfirmar);
        btnCadastrar = findViewById(R.id.btnCadastrar);

        btnCadastrar.setOnClickListener(v -> cadastrar());
        findViewById(R.id.txtJaTenhoConta).setOnClickListener(v -> finish());
    }

    private void cadastrar() {
        String nome = edtNome.getText().toString().trim();
        String sobrenome = edtSobrenome.getText().toString().trim();
        String cpf = edtCpf.getText().toString().trim();
        String email = edtEmail.getText().toString().trim();
        String senha = edtSenha.getText().toString();
        String confirmar = edtConfirmar.getText().toString();

        if (nome.length() < 2 || sobrenome.length() < 2) { aviso("Informe nome e sobrenome."); return; }
        if (!cpfValido(cpf)) { aviso("CPF inválido."); return; }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) { aviso("E-mail inválido."); return; }
        if (senha.length() < 8 || !senha.matches(".*[A-Za-z].*") || !senha.matches(".*\\d.*")) {
            aviso("A senha deve ter no mínimo 8 caracteres, com letras e números.");
            return;
        }
        if (!senha.equals(confirmar)) { aviso("As senhas não conferem."); return; }

        JSONObject corpo = new JSONObject();
        try {
            corpo.put("nome", nome);
            corpo.put("sobrenome", sobrenome);
            corpo.put("cpf", cpf);
            corpo.put("email", email);
            corpo.put("senha", senha);
        } catch (JSONException e) {
            return;
        }

        Ui.habilitar(btnCadastrar, false, "Criar conta", "Criando...");
        Api.post("/auth/registro", corpo, new Api.Retorno() {
            @Override
            public void ok(Object dados) {
                if (!viva()) return;
                Sessao.salvar((JSONObject) dados, email);
                aviso("Conta criada! Bem-vindo(a).");
                startActivity(new android.content.Intent(TelaCadastro.this, TelaProdutos.class)
                        .addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
                                | android.content.Intent.FLAG_ACTIVITY_NEW_TASK));
                finish();
            }

            @Override
            public void erro(String mensagem, int status) {
                if (!viva()) return;
                Ui.habilitar(btnCadastrar, true, "Criar conta", "");
                aviso(mensagem);
            }
        });
    }

    // Mesma validação por dígitos verificadores usada no site e na API.
    static boolean cpfValido(String texto) {
        String cpf = texto.replaceAll("\\D", "");
        if (cpf.length() != 11 || cpf.matches("(\\d)\\1{10}")) return false;
        for (int t = 9; t < 11; t++) {
            int soma = 0;
            for (int i = 0; i < t; i++) soma += (cpf.charAt(i) - '0') * (t + 1 - i);
            int digito = (soma * 10) % 11;
            if (digito == 10) digito = 0;
            if (digito != cpf.charAt(t) - '0') return false;
        }
        return true;
    }
}
