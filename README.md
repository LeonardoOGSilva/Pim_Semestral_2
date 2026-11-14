<<<<<<< HEAD
# BoxTech 🖥️🟣

> Plataforma de e-commerce de hardware, periféricos e produtos gamer, desenvolvida como Projeto Integrado Multidisciplinar (PIM IV) do curso de Análise e Desenvolvimento de Sistemas da UNIP: **Desenvolvimento e Implantação de uma Solução Tecnológica Corporativa Integrada**.

---

# 📋 Sobre o Projeto

A BoxTech é uma loja virtual especializada em hardware, periféricos e acessórios para gamers e profissionais de tecnologia. A solução integra **banco de dados SQL Server**, **API REST em camadas com autenticação e autorização**, **front-end web** e um **aplicativo mobile** que consome a mesma API.

O projeto é a evolução da VoxTech (PIM anterior), reestruturado para atender aos requisitos do PIM IV:

- Modelagem de dados (MER, modelo lógico e físico) com script completo
- Procedures, triggers, view e índices no banco
- API em camadas (Controllers, Services, Repositories, DTOs, Entities)
- Segurança: senhas com hash, JWT, perfis de acesso e validações
- Painel administrativo com relatórios
- Acessibilidade e responsividade

---

# 🚀 Funcionalidades

## 👤 Usuários e segurança

- Cadastro com validação de CPF (dígitos verificadores) e e-mail
- Login com **JWT** (expira em 120 minutos)
- Senhas armazenadas com **BCrypt** (nunca em texto puro)
- Perfis **Admin** e **Cliente**, com autorização por papel
- O cliente só acessa os próprios dados (carrinho, endereços, pedidos), identificados pelo token
- Persistência de sessão no navegador e página de perfil

## 🛍️ Catálogo

- Listagem de produtos com filtros por categoria e marca
- Busca por texto
- Página individual de produto, com galeria de imagens
- Categorias e marcas vindas do banco

## 🛒 Carrinho, favoritos e pedidos

- Carrinho persistido no banco, com contador de itens no cabeçalho
- Lista de favoritos
- Endereços de entrega
- Checkout com pagamento **simulado** (Cartão ou Pix), sem armazenar dados de cartão
- Cálculo de frete via **ViaCEP**, validado no servidor (frete grátis em hardware acima de R$ 1.000)
- Preço e frete sempre calculados no servidor, nunca confiados ao cliente
- Histórico de pedidos e cancelamento, com devolução automática do estoque

## 🧑‍💼 Painel administrativo

- CRUD de produtos
- Listagem de pedidos e alteração de status
- Relatório de vendas por período
- Relatório de produtos mais vendidos

## ♿ Acessibilidade

- Integração com VLibras
- Ajuste global do tamanho da fonte
- Leitor de tela em JavaScript
- Página de acessibilidade dedicada

## 📱 Responsividade

Desktop, tablet e smartphone.

---

# 🛠 Tecnologias

| Camada | Tecnologias |
|---|---|
| Front-end | HTML5, CSS3, JavaScript (Vanilla), Font Awesome |
| Back-end | ASP.NET Core Web API (.NET 10), C#, Entity Framework Core |
| Segurança | JWT Bearer, BCrypt.Net-Next, CORS, Rate Limiting, headers de segurança |
| Banco de dados | SQL Server |
| Documentação da API | Swagger (Swashbuckle) |
| APIs externas | ViaCEP, VLibras |

---

# 🏗 Arquitetura da API

```
BoxTechAPI/
├── Controllers/    # Endpoints REST (Auth, Produtos, Carrinho, Favoritos, Enderecos, Pedidos, Admin)
├── Services/       # Regras de negócio
├── Repositories/   # Acesso a dados (+ Unit of Work)
├── DTOs/           # Contratos de entrada e saída, com validação
├── Entities/       # Classes mapeadas para as tabelas
├── Data/           # AppDbContext e DbSeeder
├── Security/       # JWT, claims e exceções de negócio
└── Middleware/     # Tratamento global de erros
```

---

# 🗄 Banco de Dados

13 tabelas, entre elas: `Perfil`, `Usuario`, `Endereco`, `Categoria`, `Marca`, `Produto`, `ProdutoImagem`, `Carrinho`, `CarrinhoItem`, `Favorito`, `Pedido`, `PedidoItem` e `AuditoriaProduto`.

- **Colunas calculadas persistidas:** `Pedido.ValorTotal`, `PedidoItem.Subtotal`
- **Triggers:**
  - `trg_Produto_Auditoria`: registra alterações de produtos
  - `trg_Pedido_CancelamentoDevolveEstoque`: devolve o estoque ao cancelar um pedido
- **Procedures:** `sp_FinalizarPedido`, `sp_AtualizarStatusPedido`, `sp_RelatorioVendas`, `sp_ProdutosMaisVendidos`
- **View:** `vw_ProdutoCatalogo`
- **Índices** para as consultas mais frequentes

O script completo (`01_BoxTech_Banco.sql`) cria o banco, as tabelas, os objetos e os dados iniciais. O diagrama do MER acompanha o projeto.

---

# 🔗 Principais Endpoints

A documentação completa e testável está no Swagger. Rotas marcadas com 🔒 exigem token JWT.

| Recurso | Rotas |
|---|---|
| Autenticação | `POST /api/auth/registro`, `POST /api/auth/login`, 🔒 `GET /api/auth/eu` |
| Catálogo | `GET /api/produtos`, `GET /api/produtos/{id}`, `GET /api/categorias`, `GET /api/marcas` |
| Carrinho 🔒 | `GET/POST /api/carrinho`, `PUT/DELETE /api/carrinho/{itemId}` |
| Favoritos 🔒 | `/api/favoritos` |
| Endereços 🔒 | `/api/enderecos` |
| Pedidos 🔒 | `POST /api/pedidos` (checkout), `GET /api/pedidos`, `GET /api/pedidos/{id}`, cancelar pedido |
| Admin 🔒 (perfil Admin) | `/api/admin/produtos` (CRUD), `/api/admin/pedidos`, `/api/admin/pedidos/{id}/status`, `/api/admin/relatorios/vendas`, `/api/admin/relatorios/mais-vendidos` |

---

# ⚙️ Como Executar

## Pré-requisitos

- SQL Server (ou SQL Server Express) com o serviço em execução
- SQL Server Management Studio (SSMS)
- .NET SDK 10
- Editor com Live Server, ou Python 3, para servir o front

## 1. Banco de dados

1. Abra o SSMS e conecte-se à instância (por exemplo, `localhost\SQLEXPRESS`).
2. Execute o script `01_BoxTech_Banco.sql`.

## 2. API

Ajuste a string de conexão em `appsettings.json` se a sua instância for diferente e, em seguida:

```bash
cd BoxTechAPI
dotnet restore
dotnet run
```

Swagger: `http://localhost:5132/swagger`

Na primeira execução a API cria o usuário administrador com senha em hash.

## 3. Front-end

O front **deve ser servido por HTTP** (abrir o `index.html` direto, via `file://`, é bloqueado pelo CORS). Use o Live Server na porta 5500 ou:

```bash
cd BoxTechFront
python -m http.server 5500
```

Acesse `http://localhost:5500`.

> As origens liberadas ficam em `appsettings.json`, na chave `Cors:Origins`. Se usar outra porta, adicione-a ali.

## 🔑 Acesso inicial

| Perfil | E-mail | Senha |
|---|---|---|
| Administrador | `admin@boxtech.com` | `Admin@12345` |

Credenciais de **demonstração**: troque-as em `appsettings.json` (`Seed:AdminEmail` e `Seed:AdminSenha`) antes de qualquer uso real.

---

# 📱 Aplicativo Mobile

O app mobile consome a **mesma API e o mesmo banco** do site. Há um guia de integração com as rotas, o fluxo de autenticação por JWT e os formatos de dados.

---

# 🎓 Projeto Acadêmico

Projeto desenvolvido para a disciplina Projeto Integrado Multidisciplinar (PIM IV) do curso de Análise e Desenvolvimento de Sistemas da Universidade Paulista (UNIP).

---

# 👨‍💻 Desenvolvedores

- Leonardo Oliveira Gomes da Silva
- Gustavo Henrique
- Gustavo Pereira Celso
- Jandson Jose da Silva

---

# 📄 Licença

Projeto acadêmico desenvolvido exclusivamente para fins educacionais.
=======
# Pim_Semestral_2
>>>>>>> 378c48b7a519e7e550d5c84cbff0e1400fe3dd90
