# App BoxTech (Android) – como rodar

## Antes de abrir o app (no notebook)
1. Banco: rodar `01_BoxTech_Banco.sql` no SSMS.
2. Iniciar tudo: dar dois cliques em `iniciar_boxtech.bat` (fica na pasta do projeto, ao lado de BoxTechAPI e BoxTechFront).
   Ele abre três janelas: API (porta 5132), site (5500) e imagens para o app (5501). Não feche as janelas.
   Manualmente, se preferir:
   - API (pasta BoxTechAPI): `dotnet run --urls "http://0.0.0.0:5132"`
   - Imagens (pasta BoxTechFront): `python -m http.server 5501`
   - Site (pasta BoxTechFront): `python -m http.server 5500`
3. Firewall do Windows: liberar as portas TCP 5132, 5500 e 5501 (rede Privada).
   Exemplo, no PowerShell como administrador:
   `New-NetFirewallRule -DisplayName "BoxTech" -Direction Inbound -Protocol TCP -LocalPort 5132,5500,5501 -Action Allow`
4. Se usar o Live Server do VS Code, feche-o ou troque a porta, para não ocupar a 5500/5501.

## Abrir o app
1. Android Studio > File > Open > selecione a pasta que contém `settings.gradle.kts` (BoxTechApp). Aguarde o Gradle sincronizar.
   O caminho do projeto não pode ter acentos nem estar no OneDrive (ex.: use `C:\Projetos\APP`).
2. Escolha o dispositivo e clique em Run.

## Servidor (IP)
- Emulador: não precisa mexer (usa 10.0.2.2).
- Celular físico: na tela de login toque em "Configurar servidor" e digite o IP do notebook
  (rode `ipconfig` e veja o IPv4, ex.: 192.168.0.15). Celular e notebook na mesma rede Wi-Fi.
- O IP muda quando troca de rede ou quando o celular rotea a internet: confira de novo e atualize no app.
- Teste rápido no navegador do celular: `http://IP-DO-NOTEBOOK:5501/Imagens/SSD1TB.png` deve mostrar a imagem.
  Se não abrir, é firewall, rede ou servidor de imagens parado.
- Alternativa por cabo USB: `adb reverse tcp:5132 tcp:5132` e `adb reverse tcp:5501 tcp:5501`, usando 127.0.0.1 no app.

## Contas para teste
- Admin: admin@boxtech.com / Admin@12345 (o app é de cliente; o painel admin fica no site)
- Ou crie uma conta na tela "Cadastre-se".

## O que o app faz (mesmas funções do site)
Login · Cadastro (CPF validado) · Catálogo com busca e categorias · Detalhe do produto ·
Favoritos · Carrinho (quantidade/remover) · Checkout (endereço com ViaCEP, Pix/Cartão simulado,
frete) · Meus pedidos (cancelar) · Sessão por token JWT.
Botão ↻ para atualizar o catálogo, o carrinho, os favoritos e os pedidos.
Visitante ("Entrar sem usuário") só navega no catálogo.

## Problemas comuns
- Produtos aparecem sem imagem: servidor 5501 parado, IP errado ou firewall. Rode o teste do navegador acima.
  Imagens muito pesadas atrasam; use `comprimir_imagens.py` na pasta BoxTechFront.
- SocketTimeoutException: API fora do ar, ou iniciada sem `--urls "http://0.0.0.0:5132"`, ou IP desatualizado.
- Gradle: "manifest doesn't exist": o Android Studio foi aberto na pasta errada; abra a que contém `settings.gradle.kts`.

## Arquivos principais (app/src/main/java/com/example/voxtech)
- Api.java: chamadas HTTP + token | Config.java: IP do servidor | Sessao.java: login salvo
- Tela*.java: uma classe por tela | BaseTela/BaseLista: base das telas | Ui.java / Img.java: visual e imagens
