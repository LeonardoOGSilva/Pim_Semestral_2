using BoxTechAPI.Entities;
using Microsoft.EntityFrameworkCore;

namespace BoxTechAPI.Data;

// Contexto do EF Core: mapeia as entidades para as tabelas criadas pelo script 01_BoxTech_Banco.sql.
// O banco é criado pelo script (database-first); não usamos migrations.
public class AppDbContext : DbContext
{
    public AppDbContext(DbContextOptions<AppDbContext> options) : base(options) { }

    public DbSet<Perfil> Perfis => Set<Perfil>();
    public DbSet<Usuario> Usuarios => Set<Usuario>();
    public DbSet<Endereco> Enderecos => Set<Endereco>();
    public DbSet<Categoria> Categorias => Set<Categoria>();
    public DbSet<Marca> Marcas => Set<Marca>();
    public DbSet<Produto> Produtos => Set<Produto>();
    public DbSet<ProdutoImagem> ProdutoImagens => Set<ProdutoImagem>();
    public DbSet<CarrinhoItem> CarrinhoItens => Set<CarrinhoItem>();
    public DbSet<Favorito> Favoritos => Set<Favorito>();
    public DbSet<Pedido> Pedidos => Set<Pedido>();
    public DbSet<PedidoItem> PedidoItens => Set<PedidoItem>();
    public DbSet<Pagamento> Pagamentos => Set<Pagamento>();

    protected override void ConfigureConventions(ModelConfigurationBuilder cb)
    {
        // Todos os decimais do banco são DECIMAL(10,2).
        cb.Properties<decimal>().HavePrecision(10, 2);
    }

    protected override void OnModelCreating(ModelBuilder mb)
    {
        mb.Entity<Perfil>().ToTable("Perfil");

        mb.Entity<Usuario>(e =>
        {
            e.ToTable("Usuario");
            e.Property(x => x.CPF).HasColumnType("varchar(14)");
            e.HasOne(x => x.Perfil).WithMany(p => p.Usuarios).HasForeignKey(x => x.PerfilId);
            e.HasMany(x => x.Enderecos).WithOne().HasForeignKey(x => x.UsuarioId);
        });

        mb.Entity<Endereco>(e =>
        {
            e.ToTable("Endereco");
            e.Property(x => x.Cep).HasColumnType("varchar(9)");
            e.Property(x => x.Estado).HasColumnType("char(2)");
        });

        mb.Entity<Categoria>().ToTable("Categoria");
        mb.Entity<Marca>().ToTable("Marca");

        // Produto tem trigger (auditoria): o EF precisa saber, senão o UPDATE falha.
        mb.Entity<Produto>(e =>
        {
            e.ToTable("Produto", t => t.HasTrigger("trg_Produto_Auditoria"));
            e.HasOne(x => x.Categoria).WithMany(c => c.Produtos).HasForeignKey(x => x.CategoriaId);
            e.HasOne(x => x.Marca).WithMany().HasForeignKey(x => x.MarcaId);
            e.HasMany(x => x.Imagens).WithOne().HasForeignKey(i => i.ProdutoId);
        });

        mb.Entity<ProdutoImagem>().ToTable("ProdutoImagem");

        mb.Entity<CarrinhoItem>(e =>
        {
            e.ToTable("CarrinhoItem");
            e.HasOne(x => x.Produto).WithMany().HasForeignKey(x => x.ProdutoId);
        });

        mb.Entity<Favorito>(e =>
        {
            e.ToTable("Favorito");
            e.HasKey(x => new { x.UsuarioId, x.ProdutoId });
            e.HasOne(x => x.Produto).WithMany().HasForeignKey(x => x.ProdutoId);
        });

        mb.Entity<Pedido>(e =>
        {
            e.ToTable("Pedido", t => t.HasTrigger("trg_Pedido_CancelamentoDevolveEstoque"));
            e.Property(x => x.ValorTotal).HasComputedColumnSql("[ValorProdutos] + [ValorFrete]", stored: true);
            e.HasOne(x => x.Usuario).WithMany().HasForeignKey(x => x.UsuarioId);
            e.HasOne(x => x.Endereco).WithMany().HasForeignKey(x => x.EnderecoId);
            e.HasMany(x => x.Itens).WithOne().HasForeignKey(i => i.PedidoId);
            e.HasOne(x => x.Pagamento).WithOne().HasForeignKey<Pagamento>(p => p.PedidoId);
        });

        mb.Entity<PedidoItem>(e =>
        {
            e.ToTable("PedidoItem");
            e.Property(x => x.Subtotal).HasComputedColumnSql("[Quantidade] * [PrecoUnitario]", stored: true);
            e.HasOne(x => x.Produto).WithMany().HasForeignKey(x => x.ProdutoId);
        });

        mb.Entity<Pagamento>().ToTable("Pagamento");
    }
}
