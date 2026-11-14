# BoxTechAPI — como colocar no lugar do projeto antigo

1. Feche o Visual Studio / pare a API.
2. Na pasta do projeto atual (BoxTechAPI), APAGUE: `Controllers/`, `Models/`, `Data/AppDbContext.cs` antigo,
   `Program.cs` antigo, o `.csproj` antigo e o `appsettings.json` antigo.
   (Mantenha `wwwroot`/front se existir. Apague também as pastas `bin/` e `obj/`.)
3. Copie TUDO deste zip para a pasta do projeto (substituindo).
   Se o seu `.csproj` tiver outro nome, mantenha o seu nome e copie só o conteúdo de <ItemGroup>.
4. Rode o script `01_BoxTech_Banco.sql` no SSMS (se ainda não rodou).
5. No terminal, dentro da pasta do projeto:
       dotnet restore
       dotnet build
       dotnet run
6. Abra http://localhost:5132/swagger

## Contas criadas automaticamente
- Admin:  admin@boxtech.com / Admin@12345   (altere em appsettings.json -> Seed)
- Cliente demo: cliente.demo@boxtech.com / Cliente@123

## Teste no Swagger
1. POST /api/auth/login com a conta Admin -> copie o campo "token".
2. Botão "Authorize" (cadeado) -> cole só o token.
3. GET /api/admin/produtos, GET /api/admin/relatorios/mais-vendidos etc.
4. Como cliente: POST /api/carrinho {"produtoId":5,"quantidade":2} -> POST /api/pedidos/checkout
   {"enderecoId":1,"metodo":"Pix","parcelas":1}.

## Antes de entregar / produção
- Troque `Jwt:Key` por uma chave secreta sua (use `dotnet user-secrets` ou variável de ambiente).
- Ajuste `Cors:Origins` para a porta do seu front-end.
