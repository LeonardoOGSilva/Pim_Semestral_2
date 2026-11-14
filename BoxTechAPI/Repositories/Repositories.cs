using BoxTechAPI.Data;
using BoxTechAPI.DTOs;
using BoxTechAPI.Entities;
using Microsoft.Data.SqlClient;
using Microsoft.EntityFrameworkCore;

namespace BoxTechAPI.Repositories;

public class UsuarioRepository : IUsuarioRepository
{
    private readonly AppDbContext _db;
    public UsuarioRepository(AppDbContext db) => _db = db;

    public Task<Usuario?> ObterPorEmailAsync(string email) =>
        _db.Usuarios.Include(u => u.Perfil).FirstOrDefaultAsync(u => u.Email == email);

    public Task<Usuario?> ObterPorIdAsync(int id) =>
        _db.Usuarios.Include(u => u.Perfil).FirstOrDefaultAsync(u => u.Id == id);

    public Task<bool> EmailOuCpfExisteAsync(string email, string cpf) =>
        _db.Usuarios.AnyAsync(u => u.Email == email || u.CPF == cpf);

    public Task<Perfil?> ObterPerfilAsync(string nome) =>
        _db.Perfis.FirstOrDefaultAsync(p => p.Nome == nome);

    public async Task AdicionarAsync(Usuario usuario) => await _db.Usuarios.AddAsync(usuario);
}

public class ProdutoRepository : IProdutoRepository
{
    private readonly AppDbContext _db;
    public ProdutoRepository(AppDbContext db) => _db = db;

    public Task<List<Produto>> ListarAsync(int? categoriaId, string? busca, bool incluirInativos = false)
    {
        var q = _db.Produtos.AsNoTracking()
            .Include(p => p.Categoria).Include(p => p.Marca).Include(p => p.Imagens)
            .AsQueryable();

        if (!incluirInativos) q = q.Where(p => p.Ativo);
        if (categoriaId.HasValue) q = q.Where(p => p.CategoriaId == categoriaId.Value);
        if (!string.IsNullOrWhiteSpace(busca))
        {
            var termo = busca.Trim();
            q = q.Where(p => p.Nome.Contains(termo) || p.Descricao.Contains(termo));
        }
        return q.OrderBy(p => p.Id).ToListAsync();
    }

    public Task<Produto?> ObterAsync(int id, bool rastrear = false)
    {
        var q = _db.Produtos.Include(p => p.Categoria).Include(p => p.Marca).Include(p => p.Imagens).AsQueryable();
        if (!rastrear) q = q.AsNoTracking();
        return q.FirstOrDefaultAsync(p => p.Id == id);
    }

    public async Task AdicionarAsync(Produto produto) => await _db.Produtos.AddAsync(produto);

    public Task<List<Categoria>> ListarCategoriasAsync() =>
        _db.Categorias.AsNoTracking().Where(c => c.Ativa).OrderBy(c => c.Nome).ToListAsync();

    public Task<bool> CategoriaExisteAsync(int id) => _db.Categorias.AnyAsync(c => c.Id == id);
    public Task<bool> MarcaExisteAsync(int id) => _db.Marcas.AnyAsync(m => m.Id == id);
}

public class CarrinhoRepository : ICarrinhoRepository
{
    private readonly AppDbContext _db;
    public CarrinhoRepository(AppDbContext db) => _db = db;

    public Task<List<CarrinhoItem>> ListarAsync(int usuarioId) =>
        _db.CarrinhoItens.Include(c => c.Produto).Where(c => c.UsuarioId == usuarioId).OrderBy(c => c.Id).ToListAsync();

    public Task<CarrinhoItem?> ObterAsync(int usuarioId, int produtoId) =>
        _db.CarrinhoItens.FirstOrDefaultAsync(c => c.UsuarioId == usuarioId && c.ProdutoId == produtoId);

    // Filtra também pelo dono: ninguém altera o carrinho de outro usuário.
    public Task<CarrinhoItem?> ObterPorIdAsync(int id, int usuarioId) =>
        _db.CarrinhoItens.FirstOrDefaultAsync(c => c.Id == id && c.UsuarioId == usuarioId);

    public async Task AdicionarAsync(CarrinhoItem item) => await _db.CarrinhoItens.AddAsync(item);
    public void Remover(CarrinhoItem item) => _db.CarrinhoItens.Remove(item);
}

public class FavoritoRepository : IFavoritoRepository
{
    private readonly AppDbContext _db;
    public FavoritoRepository(AppDbContext db) => _db = db;

    public Task<List<Favorito>> ListarAsync(int usuarioId) =>
        _db.Favoritos.AsNoTracking().Include(f => f.Produto).Where(f => f.UsuarioId == usuarioId)
          .OrderByDescending(f => f.DataCriacao).ToListAsync();

    public Task<Favorito?> ObterAsync(int usuarioId, int produtoId) =>
        _db.Favoritos.FirstOrDefaultAsync(f => f.UsuarioId == usuarioId && f.ProdutoId == produtoId);

    public async Task AdicionarAsync(Favorito favorito) => await _db.Favoritos.AddAsync(favorito);
    public void Remover(Favorito favorito) => _db.Favoritos.Remove(favorito);
}

public class EnderecoRepository : IEnderecoRepository
{
    private readonly AppDbContext _db;
    public EnderecoRepository(AppDbContext db) => _db = db;

    public Task<List<Endereco>> ListarAsync(int usuarioId) =>
        _db.Enderecos.AsNoTracking().Where(e => e.UsuarioId == usuarioId)
          .OrderByDescending(e => e.Principal).ThenBy(e => e.Id).ToListAsync();

    public Task<Endereco?> ObterAsync(int id, int usuarioId) =>
        _db.Enderecos.FirstOrDefaultAsync(e => e.Id == id && e.UsuarioId == usuarioId);

    public async Task AdicionarAsync(Endereco endereco) => await _db.Enderecos.AddAsync(endereco);
    public void Remover(Endereco endereco) => _db.Enderecos.Remove(endereco);

    public Task LimparPrincipalAsync(int usuarioId) =>
        _db.Enderecos.Where(e => e.UsuarioId == usuarioId && e.Principal)
          .ExecuteUpdateAsync(s => s.SetProperty(e => e.Principal, false));
}

public class PedidoRepository : IPedidoRepository
{
    private readonly AppDbContext _db;
    public PedidoRepository(AppDbContext db) => _db = db;

    private IQueryable<Pedido> Base() => _db.Pedidos.AsNoTracking()
        .Include(p => p.Itens).ThenInclude(i => i.Produto)
        .Include(p => p.Pagamento);

    public Task<List<Pedido>> ListarDoUsuarioAsync(int usuarioId) =>
        Base().Where(p => p.UsuarioId == usuarioId).OrderByDescending(p => p.DataPedido).ToListAsync();

    public Task<Pedido?> ObterDoUsuarioAsync(int id, int usuarioId) =>
        Base().FirstOrDefaultAsync(p => p.Id == id && p.UsuarioId == usuarioId);

    public Task<List<Pedido>> ListarTodosAsync(string? status)
    {
        var q = _db.Pedidos.AsNoTracking().Include(p => p.Usuario).AsQueryable();
        if (!string.IsNullOrWhiteSpace(status)) q = q.Where(p => p.Status == status);
        return q.OrderByDescending(p => p.DataPedido).ToListAsync();
    }

    public Task<Pedido?> ObterSimplesAsync(int id) =>
        _db.Pedidos.AsNoTracking().FirstOrDefaultAsync(p => p.Id == id);

    // Chama sp_FinalizarPedido: valida, trava estoque, grava pedido/itens/pagamento e esvazia o carrinho.
    public async Task<int> FinalizarAsync(int usuarioId, int enderecoId, string metodo, byte parcelas, decimal frete)
    {
        var pedidoId = new SqlParameter("@PedidoId", System.Data.SqlDbType.Int) { Direction = System.Data.ParameterDirection.Output };
        await _db.Database.ExecuteSqlRawAsync(
            "EXEC dbo.sp_FinalizarPedido @UsuarioId, @EnderecoId, @MetodoPagamento, @Parcelas, @ValorFrete, @PedidoId OUTPUT",
            new SqlParameter("@UsuarioId", usuarioId),
            new SqlParameter("@EnderecoId", enderecoId),
            new SqlParameter("@MetodoPagamento", metodo),
            new SqlParameter("@Parcelas", (int)parcelas),
            new SqlParameter("@ValorFrete", System.Data.SqlDbType.Decimal) { Precision = 10, Scale = 2, Value = frete },
            pedidoId);
        return (int)pedidoId.Value;
    }

    public Task AtualizarStatusAsync(int pedidoId, string novoStatus) =>
        _db.Database.ExecuteSqlRawAsync("EXEC dbo.sp_AtualizarStatusPedido @PedidoId, @NovoStatus",
            new SqlParameter("@PedidoId", pedidoId), new SqlParameter("@NovoStatus", novoStatus));

    public Task<List<VendasCategoriaDto>> VendasPorCategoriaAsync(DateOnly inicio, DateOnly fim) =>
        _db.Database.SqlQuery<VendasCategoriaDto>(
            $"EXEC dbo.sp_RelatorioVendas {inicio.ToDateTime(TimeOnly.MinValue)}, {fim.ToDateTime(TimeOnly.MinValue)}").ToListAsync();

    public Task<List<ProdutoMaisVendidoDto>> MaisVendidosAsync(int top) =>
        _db.Database.SqlQuery<ProdutoMaisVendidoDto>($"EXEC dbo.sp_ProdutosMaisVendidos {top}").ToListAsync();
}

public class UnitOfWork : IUnitOfWork
{
    private readonly AppDbContext _db;
    public UnitOfWork(AppDbContext db) => _db = db;
    public Task<int> SalvarAsync() => _db.SaveChangesAsync();
}
