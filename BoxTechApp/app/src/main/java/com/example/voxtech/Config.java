package com.example.voxtech;

import android.content.Context;
import android.content.SharedPreferences;

// Endereços do servidor (API e imagens). Podem ser trocados dentro do app, na tela de login
// ("Configurar servidor"), sem mexer no código. Aceitam:
//   - IP ou nome:   192.168.0.0            -> http://192.168.15.8:5132  (API) e :5500 (imagens)
//   - IP com porta: 192.168.0.0:5132
//   - Link público: https://algo.trycloudflare.com  (túnel, para usar com internet do celular)
public class Config {
    // Emulador do Android Studio enxerga o PC pelo IP especial 10.0.2.2.
    public static final String HOST_PADRAO = "10.0.2.2";
    public static final int PORTA_API = 5132;   // dotnet run
    public static final int PORTA_SITE = 5501;  // servidor do front (onde ficam as imagens)

    private static Context ctx;

    public static void init(Context c) { ctx = c.getApplicationContext(); }

    static Context contexto() { return ctx; }

    private static SharedPreferences prefs() {
        return ctx.getSharedPreferences("boxtech_config", Context.MODE_PRIVATE);
    }

    // ---------- Valores digitados (para mostrar no diálogo) ----------
    public static String entradaApi() { return prefs().getString("api", HOST_PADRAO); }

    public static String entradaImagens() { return prefs().getString("img", ""); }

    public static void definir(String api, String imagens) {
        prefs().edit()
                .putString("api", api == null ? "" : api.trim())
                .putString("img", imagens == null ? "" : imagens.trim())
                .apply();
    }

    // ---------- Endereços usados pelo app ----------
    public static String baseApi() {
        String origem = origem(entradaApi(), PORTA_API);
        if (origem.toLowerCase().endsWith("/api")) origem = origem.substring(0, origem.length() - 4);
        return origem + "/api";
    }

    // Se o campo de imagens ficar vazio, usa o mesmo servidor da API na porta 5500.
    public static String baseImagens() {
        String img = entradaImagens();
        if (img.isEmpty()) {
            return origem(hostDe(entradaApi()), PORTA_SITE) + "/";
        }
        return origem(img, PORTA_SITE) + "/";
    }

    // Só o IP/nome, sem esquema, porta ou caminho.
    private static String hostDe(String bruto) {
        String h = bruto == null ? "" : bruto.trim();
        h = h.replaceFirst("(?i)^https?://", "");
        int barra = h.indexOf('/');
        if (barra >= 0) h = h.substring(0, barra);
        int doisPontos = h.indexOf(':');
        if (doisPontos >= 0) h = h.substring(0, doisPontos);
        h = h.replaceAll("\\s", "");
        return h.isEmpty() ? HOST_PADRAO : h;
    }

    // Transforma a entrada em "http(s)://host[:porta]" sem barra no final.
    private static String origem(String entrada, int portaPadrao) {
        String e = entrada == null ? "" : entrada.trim().replaceAll("\\s", "");
        if (e.isEmpty()) e = HOST_PADRAO;
        while (e.endsWith("/")) e = e.substring(0, e.length() - 1);

        if (e.toLowerCase().startsWith("http://") || e.toLowerCase().startsWith("https://")) {
            return e; // link completo (ex.: túnel https): usa como está
        }
        // IP ou nome: acrescenta a porta padrão quando não foi informada.
        int barra = e.indexOf('/');
        String hostPorta = barra >= 0 ? e.substring(0, barra) : e;
        return "http://" + (hostPorta.contains(":") ? hostPorta : hostPorta + ":" + portaPadrao);
    }
}
