using BoxTechAPI.DTOs;

namespace BoxTechAPI.Services;

// Camada de serviços: regras de negócio. Controllers só chamam serviços; serviços só chamam repositórios.
public interface IAuthService
{
    Task<AuthRespostaDto> RegistrarAsync(RegistroDto dto);
    Task<AuthRespostaDto> LoginAsync(LoginDto dto);
    Task<UsuarioDto> ObterUsuarioAsync(int id);
}

public interface IProdutoService
{
    Task<List<ProdutoDto>> ListarAsync(int? categoriaId, string? busca);
    Task<ProdutoDto> ObterAsync(int id);
    Task<List<CategoriaDto>> ListarCategoriasAsync();
    Task<List<MarcaDto>> ListarMarcasAsync();
    // Administração
    Task<List<ProdutoDto>> ListarAdminAsync();
    Task<ProdutoDto> CriarAsync(ProdutoSalvarDto dto);
    Task<ProdutoDto> AtualizarAsync(int id, ProdutoSalvarDto dto);
    Task DesativarAsync(int id);
    Task ExcluirAsync(int id);
}

public interface ICarrinhoService
{
    Task<CarrinhoDto> ListarAsync(int usuarioId);
    Task<CarrinhoDto> AdicionarAsync(int usuarioId, CarrinhoAdicionarDto dto);
    Task<CarrinhoDto> AtualizarQuantidadeAsync(int usuarioId, int itemId, int quantidade);
    Task<CarrinhoDto> RemoverAsync(int usuarioId, int itemId);
}

public interface IFavoritoService
{
    Task<List<FavoritoDto>> ListarAsync(int usuarioId);
    Task AdicionarAsync(int usuarioId, int produtoId);
    Task RemoverAsync(int usuarioId, int produtoId);
}

public interface IEnderecoService
{
    Task<List<EnderecoDto>> ListarAsync(int usuarioId);
    Task<EnderecoDto> CriarAsync(int usuarioId, EnderecoSalvarDto dto);
    Task<EnderecoDto> AtualizarAsync(int usuarioId, int id, EnderecoSalvarDto dto);
    Task RemoverAsync(int usuarioId, int id);
}

public interface IPedidoService
{
    Task<PedidoDto> FinalizarAsync(int usuarioId, CheckoutDto dto);
    Task<List<PedidoDto>> ListarDoUsuarioAsync(int usuarioId);
    Task<PedidoDto> ObterDoUsuarioAsync(int usuarioId, int id);
    Task CancelarAsync(int usuarioId, int id);
    // Administração
    Task<List<PedidoResumoAdminDto>> ListarTodosAsync(string? status);
    Task AtualizarStatusAsync(int id, string novoStatus);
    Task<List<VendasCategoriaDto>> RelatorioVendasAsync(DateOnly inicio, DateOnly fim);
    Task<List<ProdutoMaisVendidoDto>> MaisVendidosAsync(int top);
}
