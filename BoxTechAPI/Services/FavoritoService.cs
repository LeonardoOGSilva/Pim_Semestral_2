using BoxTechAPI.DTOs;
using BoxTechAPI.Entities;
using BoxTechAPI.Repositories;
using BoxTechAPI.Security;

namespace BoxTechAPI.Services;

public class FavoritoService : IFavoritoService
{
    private readonly IFavoritoRepository _favoritos;
    private readonly IProdutoRepository _produtos;
    private readonly IUnitOfWork _uow;

    public FavoritoService(IFavoritoRepository favoritos, IProdutoRepository produtos, IUnitOfWork uow)
    {
        _favoritos = favoritos; _produtos = produtos; _uow = uow;
    }

    public async Task<List<FavoritoDto>> ListarAsync(int usuarioId) =>
        (await _favoritos.ListarAsync(usuarioId))
            .Select(f => new FavoritoDto(f.ProdutoId, f.Produto.Nome, f.Produto.Preco, f.Produto.ImagemUrl)).ToList();

    public async Task AdicionarAsync(int usuarioId, int produtoId)
    {
        var produto = await _produtos.ObterAsync(produtoId);
        if (produto is null || !produto.Ativo) throw new NegocioException("Produto não encontrado.", 404);
        if (await _favoritos.ObterAsync(usuarioId, produtoId) is not null) return; // já favoritado: idempotente
        await _favoritos.AdicionarAsync(new Favorito { UsuarioId = usuarioId, ProdutoId = produtoId });
        await _uow.SalvarAsync();
    }

    public async Task RemoverAsync(int usuarioId, int produtoId)
    {
        var fav = await _favoritos.ObterAsync(usuarioId, produtoId);
        if (fav is null) return;
        _favoritos.Remover(fav);
        await _uow.SalvarAsync();
    }
}
