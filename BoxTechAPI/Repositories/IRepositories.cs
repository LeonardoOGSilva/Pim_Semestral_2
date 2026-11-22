using BoxTechAPI.DTOs;
using BoxTechAPI.Entities;

namespace BoxTechAPI.Repositories;

// Camada de acesso a dados: só conversa com o banco, sem regra de negócio.
public interface IUsuarioRepository
{
    Task<Usuario?> ObterPorEmailAsync(string email);
    Task<Usuario?> ObterPorIdAsync(int id);
    Task<bool> EmailOuCpfExisteAsync(string email, string cpf);
    Task<Perfil?> ObterPerfilAsync(string nome);
    Task AdicionarAsync(Usuario usuario);
}

public interface IProdutoRepository
{
    Task<List<Produto>> ListarAsync(int? categoriaId, string? busca, bool incluirInativos = false);
    Task<Produto?> ObterAsync(int id, bool rastrear = false);
    Task AdicionarAsync(Produto produto);
    Task<List<Categoria>> ListarCategoriasAsync();
    Task<List<Marca>> ListarMarcasAsync();
    Task<bool> CategoriaExisteAsync(int id);
    Task<bool> MarcaExisteAsync(int id);
    Task<bool> PossuiPedidosAsync(int id);
    Task ExcluirDefinitivoAsync(int id);
}

public interface ICarrinhoRepository
{
    Task<List<CarrinhoItem>> ListarAsync(int usuarioId);
    Task<CarrinhoItem?> ObterAsync(int usuarioId, int produtoId);
    Task<CarrinhoItem?> ObterPorIdAsync(int id, int usuarioId);
    Task AdicionarAsync(CarrinhoItem item);
    void Remover(CarrinhoItem item);
}

public interface IFavoritoRepository
{
    Task<List<Favorito>> ListarAsync(int usuarioId);
    Task<Favorito?> ObterAsync(int usuarioId, int produtoId);
    Task AdicionarAsync(Favorito favorito);
    void Remover(Favorito favorito);
}

public interface IEnderecoRepository
{
    Task<List<Endereco>> ListarAsync(int usuarioId);
    Task<Endereco?> ObterAsync(int id, int usuarioId);
    Task AdicionarAsync(Endereco endereco);
    void Remover(Endereco endereco);
    Task LimparPrincipalAsync(int usuarioId);
}

public interface IPedidoRepository
{
    Task<List<Pedido>> ListarDoUsuarioAsync(int usuarioId);
    Task<Pedido?> ObterDoUsuarioAsync(int id, int usuarioId);
    Task<List<Pedido>> ListarTodosAsync(string? status);
    Task<Pedido?> ObterSimplesAsync(int id);

    // Operações que executam as procedures do banco (regra transacional fica no SQL Server).
    Task<int> FinalizarAsync(int usuarioId, int enderecoId, string metodo, byte parcelas, decimal frete);
    Task AtualizarStatusAsync(int pedidoId, string novoStatus);
    Task<List<VendasCategoriaDto>> VendasPorCategoriaAsync(DateOnly inicio, DateOnly fim);
    Task<List<ProdutoMaisVendidoDto>> MaisVendidosAsync(int top);
}

// Unidade de persistência: confirma as alterações feitas pelos repositórios.
public interface IUnitOfWork
{
    Task<int> SalvarAsync();
}
