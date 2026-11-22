package com.example.voxtech;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONObject;

// Guarda o token JWT e os dados do usuário logado (equivale ao localStorage do site).
public class Sessao {
    private static SharedPreferences prefs() {
        return Config.contexto().getSharedPreferences("boxtech_sessao", Context.MODE_PRIVATE);
    }

    public static void salvar(JSONObject auth, String email) {
        prefs().edit()
                .putString("token", auth.optString("token"))
                .putString("nome", auth.optString("nome"))
                .putString("perfil", auth.optString("perfil"))
                .putString("email", email)
                .apply();
    }

    public static String token() { return prefs().getString("token", null); }
    public static boolean logado() { return token() != null; }
    public static String nome() { return prefs().getString("nome", ""); }
    public static String email() { return prefs().getString("email", ""); }
    public static String perfil() { return prefs().getString("perfil", ""); }

    public static void sair() { prefs().edit().clear().apply(); }
}
