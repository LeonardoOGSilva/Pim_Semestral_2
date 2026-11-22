package com.example.voxtech;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.LruCache;
import android.widget.ImageView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// Carrega as fotos dos produtos (pasta Imagens do site) com cache em memória e em disco:
// depois de baixada uma vez, a foto aparece mesmo se a conexão estiver lenta ou cair.
public class Img {
    private static final ExecutorService EXEC = Executors.newFixedThreadPool(3);
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final LruCache<String, Bitmap> CACHE =
            new LruCache<String, Bitmap>((int) (Runtime.getRuntime().maxMemory() / 8)) {
                @Override
                protected int sizeOf(String chave, Bitmap b) { return b.getByteCount(); }
            };

    private static String resolver(String caminho) {
        if (caminho == null || caminho.trim().isEmpty()) return null;
        String c = caminho.trim().replace(" ", "%20");
        if (c.startsWith("http://") || c.startsWith("https://")) return c;
        while (c.startsWith("/")) c = c.substring(1);
        return Config.baseImagens() + c;
    }

    public static void carregar(ImageView iv, String caminho) {
        iv.setImageResource(R.drawable.ic_placeholder);
        final String url = resolver(caminho);
        iv.setTag(url);
        if (url == null) return;

        Bitmap emMemoria = CACHE.get(url);
        if (emMemoria != null) {
            iv.setImageBitmap(emMemoria);
            return;
        }

        EXEC.execute(() -> {
            // 1) disco  2) rede (com 3 tentativas)
            byte[] dados = lerDoDisco(url);
            if (dados == null) {
                for (int tentativa = 1; tentativa <= 3 && dados == null; tentativa++) {
                    dados = baixar(url);
                }
                if (dados != null) gravarNoDisco(url, dados);
            }
            if (dados == null) return;

            Bitmap bmp = decodificar(dados, url);
            if (bmp == null) return;
            CACHE.put(url, bmp);
            MAIN.post(() -> {
                // A view pode ter sido reaproveitada para outro produto enquanto baixava.
                if (url.equals(iv.getTag())) iv.setImageBitmap(bmp);
            });
        });
    }

    // ---------- Disco ----------
    private static File arquivoDoCache(String url) {
        File pasta = new File(Config.contexto().getCacheDir(), "imagens");
        if (!pasta.exists()) pasta.mkdirs();
        return new File(pasta, Integer.toHexString(url.hashCode()) + ".img");
    }

    private static byte[] lerDoDisco(String url) {
        try {
            File f = arquivoDoCache(url);
            if (!f.exists() || f.length() == 0) return null;
            try (InputStream in = new FileInputStream(f)) {
                return lerTudo(in);
            }
        } catch (Throwable e) {
            return null;
        }
    }

    private static void gravarNoDisco(String url, byte[] dados) {
        try (FileOutputStream out = new FileOutputStream(arquivoDoCache(url))) {
            out.write(dados);
        } catch (Throwable e) {
            Log.w("BoxTechImg", "Não foi possível gravar o cache de " + url + " -> " + e);
        }
    }

    // ---------- Rede ----------
    private static byte[] baixar(String endereco) {
        try {
            HttpURLConnection c = (HttpURLConnection) new URL(endereco).openConnection();
            c.setConnectTimeout(15000);
            c.setReadTimeout(60000);
            int codigo = c.getResponseCode();
            if (codigo != 200) {
                Log.w("BoxTechImg", "HTTP " + codigo + " ao baixar " + endereco);
                return null;
            }
            byte[] dados;
            try (InputStream in = c.getInputStream()) {
                dados = lerTudo(in);
            }
            c.disconnect();
            return dados;
        } catch (Throwable e) {
            Log.w("BoxTechImg", "Falha ao baixar " + endereco + " -> " + e);
            return null;
        }
    }

    private static byte[] lerTudo(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
        return out.toByteArray();
    }

    // Reduz fotos grandes para não estourar a memória do celular.
    private static Bitmap decodificar(byte[] dados, String url) {
        try {
            BitmapFactory.Options medidas = new BitmapFactory.Options();
            medidas.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(dados, 0, dados.length, medidas);
            int amostra = 1;
            while (medidas.outWidth / amostra > 900) amostra *= 2;
            BitmapFactory.Options opcoes = new BitmapFactory.Options();
            opcoes.inSampleSize = amostra;
            Bitmap resultado = BitmapFactory.decodeByteArray(dados, 0, dados.length, opcoes);
            if (resultado == null) Log.w("BoxTechImg", "Não foi possível decodificar " + url);
            return resultado;
        } catch (Throwable e) {
            Log.w("BoxTechImg", "Erro ao decodificar " + url + " -> " + e);
            return null;
        }
    }
}
