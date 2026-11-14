using BoxTechAPI.DTOs;
using BoxTechAPI.Entities;
using BoxTechAPI.Repositories;
using BoxTechAPI.Security;

namespace BoxTechAPI.Services;

public class CarrinhoService : ICarrinhoService
{
    private readonly ICarrinhoRepository _carrinho;
    private readonly IProdutoRepository _produtos;
    private readonly IUnitOfWork _uow;

    public CarrinhoService(ICarrinhoRepository carrinho, IProdutoRepository produtos, IUnitOfWork uow)
    {
        _carrinho = carrinho; _produtos = produtos; _uow = uow;
    }

    public async Task<CarrinhoDto> ListarAsync(int usuarioId)
    {
        var itens = (await _carrinho.ListarAsync(usuarioId))
            .Select(c => new CarrinhoItemDto(c.Id, c.ProdutoId, c.Produto.Nome, c.Produto.Preco,
                                             c.Produto.ImagemUrl, c.Quantidade, c.Produto.Preco * c.Quantidade))
            .ToList();
        return new CarrinhoDto(itens, itens.Sum(i => i.Subtotal));
    }

    public async Task<CarrinhoDto> AdicionarAsync(int usuarioId, CarrinhoAdicionarDto dto)
    {
        var produto = await _produtos.ObterAsync(dto.ProdutoId);
        if (produto is null || !produto.Ativo) throw new NegocioException("Produto não encontrado.", 404);

        var existente = await _carrinho.ObterAsync(usuarioId, dto.ProdutoId);
        var novaQtd = (existente?.Quantidade ?? 0) + dto.Quantidade;
        if (novaQtd > produto.Estoque) throw new NegocioException($"Estoque insuficiente. Disponível: {produto.Estoque}.", 409);

        if (existente is null)
            await _carrinho.AdicionarAsync(new CarrinhoItem { UsuarioId = usuarioId, ProdutoId = dto.ProdutoId, Quantidade = dto.Quantidade });
        else
            existente.Quantidade = novaQtd;

        await _uow.SalvarAsync();
        return await ListarAsync(usuarioId);
    }

    public async Task<CarrinhoDto> AtualizarQuantidadeAsync(int usuarioId, int itemId, int quantidade)
    {
        var item = await _carrinho.ObterPorIdAsync(itemId, usuarioId) ?? throw new NegocioException("Item não encontrado.", 404);
        var produto = await _produtos.ObterAsync(item.ProdutoId);
        if (produto is null || quantidade > produto.Estoque) throw new NegocioException("Estoque insuficiente.", 409);

        item.Quantidade = quantidade;
        await _uow.SalvarAsync();
        return await ListarAsync(usuarioId);
    }

    public async Task<CarrinhoDto> RemoverAsync(int usuarioId, int itemId)
    {
        var item = await _carrinho.ObterPorIdAsync(itemId, usuarioId) ?? throw new NegocioException("Item não encontrado.", 404);
        _carrinho.Remover(item);
        await _uow.SalvarAsync();
        return await ListarAsync(usuarioId);
    }
}
