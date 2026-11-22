package com.example.voxtech;

import android.content.Intent;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

// Base das telas: toasts, checagem de tela viva, tratamento padrão de erros da API
// e ajuste automático para as barras do sistema (status e navegação) não cobrirem o conteúdo.
public abstract class BaseTela extends AppCompatActivity {

    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);
        ajustarBarrasDoSistema();
    }

    // Empurra o conteúdo para dentro da área livre: botões do rodapé não ficam atrás da barra
    // de navegação do Android, e o cabeçalho não fica sob o relógio/câmera. Também sobe com o teclado.
    private void ajustarBarrasDoSistema() {
        getWindow().getDecorView().setBackgroundColor(Ui.BG);
        View raiz = findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(raiz, (v, insets) -> {
            Insets b = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout()
                    | WindowInsetsCompat.Type.ime());
            v.setPadding(b.left, b.top, b.right, b.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
        ViewCompat.requestApplyInsets(raiz);
    }

    protected boolean viva() { return !isFinishing() && !isDestroyed(); }

    protected void aviso(String msg) { Toast.makeText(this, msg, Toast.LENGTH_SHORT).show(); }

    // Erro 401 (sessão expirada ou sem login): volta para o login.
    protected void tratarErro(String msg, int status) {
        if (!viva()) return;
        aviso(msg);
        if (status == 401) irParaLogin();
    }

    protected void irParaLogin() {
        Intent i = new Intent(this, MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
        finish();
    }

    // Telas que exigem conta. Retorna false (e abre o login) quando o usuário é visitante.
    protected boolean exigirLogin() {
        if (Sessao.logado()) return true;
        aviso("Entre na sua conta para continuar.");
        irParaLogin();
        return false;
    }
}
