package com.example.voxtech;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

// Base das telas internas (barra com voltar + título + área rolável + rodapé opcional).
public abstract class BaseLista extends BaseTela {

    protected LinearLayout container;
    protected LinearLayout rodape;

    protected abstract String titulo();

    protected abstract void iniciar();

    // Botão de atualizar: as telas que quiserem sobrescrevem os dois métodos abaixo.
    protected boolean podeAtualizar() { return false; }

    protected void atualizar() { }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tela_generica);
        container = findViewById(R.id.container);
        rodape = findViewById(R.id.rodape);
        ((TextView) findViewById(R.id.txtTituloTela)).setText(titulo());
        findViewById(R.id.btnVoltar).setOnClickListener(v -> finish());
        configurarBotaoAtualizar();
        iniciar();
    }

    private void configurarBotaoAtualizar() {
        View btn = findViewById(R.id.btnAtualizar);
        btn.setVisibility(podeAtualizar() ? View.VISIBLE : View.GONE);
        btn.setOnClickListener(v -> atualizar());
    }

    protected void mensagem(String texto, int cor) {
        container.removeAllViews();
        TextView t = Ui.texto(this, texto, 15, cor, false);
        t.setGravity(android.view.Gravity.CENTER);
        t.setPadding(0, Ui.dp(this, 60), 0, 0);
        container.addView(t);
    }
}
