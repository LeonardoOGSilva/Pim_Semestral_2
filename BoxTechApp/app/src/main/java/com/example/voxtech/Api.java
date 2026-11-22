package com.example.voxtech;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// Cliente HTTP da API BoxTech (mesmas rotas usadas pelo site). Roda em segundo plano
// e devolve o resultado na thread principal. Envia "Authorization: Bearer <token>" quando há login.
public class Api {

    public interface Retorno {
        // "dados" é um JSONObject, um JSONArray ou null (respostas 204).
        void ok(Object dados);

        void erro(String mensagem, int status);
    }

    private static final ExecutorService EXEC = Executors.newFixedThreadPool(4);
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    public static void get(String caminho, Retorno r) { chamar("GET", caminho, null, r); }

    public static void post(String caminho, JSONObject corpo, Retorno r) { chamar("POST", caminho, corpo, r); }

    public static void put(String caminho, JSONObject corpo, Retorno r) { chamar("PUT", caminho, corpo, r); }

    public static void delete(String caminho, Retorno r) { chamar("DELETE", caminho, null, r); }

    private static void chamar(String metodo, String caminho, JSONObject corpo, Retorno r) {
        EXEC.execute(() -> {
            int status;
            String texto;
            try {
                URL url = new URL(Config.baseApi() + caminho);
                HttpURLConnection c = (HttpURLConnection) url.openConnection();
                c.setConnectTimeout(15000);
                c.setReadTimeout(20000);
                c.setRequestMethod(metodo);
                c.setRequestProperty("Accept", "application/json");
                String token = Sessao.token();
                if (token != null) c.setRequestProperty("Authorization", "Bearer " + token);

                boolean temCorpo = metodo.equals("POST") || metodo.equals("PUT");
                if (temCorpo) {
                    byte[] bytes = (corpo != null ? corpo.toString() : "").getBytes(StandardCharsets.UTF_8);
                    c.setDoOutput(true);
                    c.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                    c.setFixedLengthStreamingMode(bytes.length);
                    try (OutputStream o = c.getOutputStream()) { o.write(bytes); }
                }

                status = c.getResponseCode();
                InputStream in = status >= 400 ? c.getErrorStream() : c.getInputStream();
                texto = ler(in);
                c.disconnect();
            } catch (IOException e) {
                entregarErro(r, "Não foi possível conectar à API em " + Config.baseApi() + "\n"
                        + "Motivo: " + e.getClass().getSimpleName()
                        + (e.getMessage() != null ? " - " + e.getMessage() : ""), 0);
                return;
            }

            Object dados = null;
            if (!texto.trim().isEmpty()) {
                try { dados = new JSONTokener(texto).nextValue(); } catch (JSONException ignorado) { }
            }

            if (status >= 200 && status < 300) {
                final Object resultado = dados;
                MAIN.post(() -> r.ok(resultado));
            } else {
                if (status == 401 && Sessao.logado()) Sessao.sair();
                entregarErro(r, mensagemDeErro(dados, status), status);
            }
        });
    }

    // Consulta de CEP (mesma API externa usada pelo site). Não usa o token da BoxTech.
    public static void viaCep(String cep, Retorno r) {
        EXEC.execute(() -> {
            try {
                URL url = new URL("https://viacep.com.br/ws/" + cep + "/json/");
                HttpURLConnection c = (HttpURLConnection) url.openConnection();
                c.setConnectTimeout(8000);
                c.setReadTimeout(10000);
                int status = c.getResponseCode();
                String texto = ler(status >= 400 ? c.getErrorStream() : c.getInputStream());
                c.disconnect();
                JSONObject o = new JSONObject(texto);
                if (status != 200 || o.optBoolean("erro", false)) {
                    entregarErro(r, "CEP não encontrado.", 404);
                } else {
                    MAIN.post(() -> r.ok(o));
                }
            } catch (IOException | JSONException e) {
                entregarErro(r, "Não foi possível consultar o CEP.", 0);
            }
        });
    }

    private static void entregarErro(Retorno r, String msg, int status) {
        MAIN.post(() -> r.erro(msg, status));
    }

    // Mesma lógica do site: campo "erro" ou o dicionário "errors" da validação.
    private static String mensagemDeErro(Object dados, int status) {
        if (dados instanceof JSONObject) {
            JSONObject o = (JSONObject) dados;
            String erro = o.optString("erro", "");
            if (!erro.isEmpty()) return erro;
            JSONObject erros = o.optJSONObject("errors");
            if (erros != null) {
                StringBuilder sb = new StringBuilder();
                Iterator<String> chaves = erros.keys();
                while (chaves.hasNext()) {
                    JSONArray lista = erros.optJSONArray(chaves.next());
                    if (lista == null) continue;
                    for (int i = 0; i < lista.length(); i++) {
                        if (sb.length() > 0) sb.append(' ');
                        sb.append(lista.optString(i));
                    }
                }
                if (sb.length() > 0) return sb.toString();
            }
        }
        switch (status) {
            case 401: return "Faça login para continuar.";
            case 403: return "Você não tem permissão para isso.";
            case 404: return "Não encontrado.";
            case 429: return "Muitas tentativas. Aguarde um minuto.";
            default: return "Erro " + status;
        }
    }

    private static String ler(InputStream in) throws IOException {
        if (in == null) return "";
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n;
        while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
        in.close();
        return out.toString("UTF-8");
    }
}
