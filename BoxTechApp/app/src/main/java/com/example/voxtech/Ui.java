package com.example.voxtech;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.NumberFormat;
import java.util.Locale;

// Ajudantes para montar as telas em código mantendo o visual roxo do app.
public class Ui {
    public static final int BG = Color.parseColor("#09070F");
    public static final int CARD = Color.parseColor("#16121F");
    public static final int CAMPO = Color.parseColor("#1A1526");
    public static final int BORDA = Color.parseColor("#282133");
    public static final int ROXO = Color.parseColor("#7C3AED");
    public static final int LILAS = Color.parseColor("#A970FF");
    public static final int MUTED = Color.parseColor("#9D95AA");
    public static final int BRANCO = Color.WHITE;
    public static final int ERRO = Color.parseColor("#FF6B6B");
    public static final int OK = Color.parseColor("#4ADE80");

    public static int dp(Context c, int valor) {
        return Math.round(valor * c.getResources().getDisplayMetrics().density);
    }

    public static String brl(double valor) {
        return NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR")).format(valor);
    }

    public static GradientDrawable fundo(Context c, int cor, int raioDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(cor);
        g.setCornerRadius(dp(c, raioDp));
        return g;
    }

    public static GradientDrawable fundoComBorda(Context c, int cor, int raioDp, int corBorda) {
        GradientDrawable g = fundo(c, cor, raioDp);
        g.setStroke(dp(c, 1), corBorda);
        return g;
    }

    public static TextView texto(Context c, String t, int sp, int cor, boolean negrito) {
        TextView tv = new TextView(c);
        tv.setText(t);
        tv.setTextSize(sp);
        tv.setTextColor(cor);
        if (negrito) tv.setTypeface(null, Typeface.BOLD);
        return tv;
    }

    public static LinearLayout.LayoutParams lp(int w, int h) {
        return new LinearLayout.LayoutParams(w, h);
    }

    public static void margem(Context c, View v, int l, int t, int r, int b) {
        ViewGroup.LayoutParams p = v.getLayoutParams();
        if (p == null) p = lp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        if (p instanceof ViewGroup.MarginLayoutParams) {
            ((ViewGroup.MarginLayoutParams) p).setMargins(dp(c, l), dp(c, t), dp(c, r), dp(c, b));
        }
        v.setLayoutParams(p);
    }

    // Cartão vertical escuro e arredondado.
    public static LinearLayout cartao(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setBackground(fundo(c, CARD, 16));
        int p = dp(c, 14);
        l.setPadding(p, p, p, p);
        l.setLayoutParams(lp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        margem(c, l, 0, 0, 0, 12);
        return l;
    }

    // Botão feito com TextView para ter controle total do visual.
    public static TextView botao(Context c, String t, boolean primario) {
        TextView b = texto(c, t, 14, primario ? BRANCO : LILAS, true);
        b.setGravity(Gravity.CENTER);
        b.setBackground(primario ? fundo(c, ROXO, 12) : fundoComBorda(c, CAMPO, 12, BORDA));
        b.setPadding(dp(c, 16), dp(c, 13), dp(c, 16), dp(c, 13));
        b.setClickable(true);
        b.setFocusable(true);
        b.setLayoutParams(lp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return b;
    }

    public static void habilitar(TextView b, boolean ativo, String textoAtivo, String textoOcupado) {
        b.setEnabled(ativo);
        b.setAlpha(ativo ? 1f : 0.6f);
        b.setText(ativo ? textoAtivo : textoOcupado);
    }
}
