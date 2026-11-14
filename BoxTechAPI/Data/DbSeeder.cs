using BoxTechAPI.Entities;
using Microsoft.EntityFrameworkCore;

namespace BoxTechAPI.Data;

// Executado na inicialização: cria o Admin real (com hash) e ajusta o cliente de demonstração.
public static class DbSeeder
{
    public static async Task SeedAsync(IServiceProvider services, IConfiguration config)
    {
        using var scope = services.CreateScope();
        var db = scope.ServiceProvider.GetRequiredService<AppDbContext>();

        var perfilAdmin = await db.Perfis.FirstOrDefaultAsync(p => p.Nome == "Admin");
        if (perfilAdmin is null) return; // banco ainda não foi criado pelo script

        var email = (config["Seed:AdminEmail"] ?? "admin@boxtech.com").ToLowerInvariant();
        var senha = config["Seed:AdminSenha"] ?? "Admin@12345";

        if (!await db.Usuarios.AnyAsync(u => u.Email == email))
        {
            db.Usuarios.Add(new Usuario
            {
                PerfilId = perfilAdmin.Id, Nome = "Administrador", Sobrenome = "BoxTech",
                CPF = "999.999.999-99", Email = email,
                SenhaHash = BCrypt.Net.BCrypt.HashPassword(senha, 11)
            });
        }

        // O script SQL cria o cliente demo com um hash fictício; aqui ele recebe um hash real.
        var demo = await db.Usuarios.FirstOrDefaultAsync(u => u.Email == "cliente.demo@boxtech.com");
        if (demo is not null && demo.SenhaHash.StartsWith("SUBSTITUIR"))
            demo.SenhaHash = BCrypt.Net.BCrypt.HashPassword("Cliente@123", 11);

        await db.SaveChangesAsync();
    }
}
