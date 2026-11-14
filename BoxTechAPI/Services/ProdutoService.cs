using BoxTechAPI.DTOs;
using BoxTechAPI.Entities;
using BoxTechAPI.Repositories;
using BoxTechAPI.Security;

namespace BoxTechAPI.Services;

public class ProdutoService : IProdutoService
{
    private readonly IProdutoRepository _produtos;
    private readonly IUnitOfWork _uow;

    public ProdutoService(IProdutoRepository produtos, IUnitOfWork uow) { _produtos = produtos; _uow = uow; }

    public async Task<List<ProdutoDto>> ListarAsync(int? categoriaId, string? busca) =>
        (await _produtos.ListarAsync(categoriaId, busca)).Select(Mapear).ToList();

    public async Task<ProdutoDto> ObterAsync(int id)
    {
        var p = await _produtos.ObterAsync(id);
        if (p is null || !p.Ativo) throw new NegocioException("Produto não encontrado.", 404);
        return Mapear(p);
    }

    public async Task<List<CategoriaDto>> ListarCategoriasAsync() =>
        (await _produtos.ListarCategoriasAsync()).Select(c => new CategoriaDto(c.Id, c.Nome, c.Descricao)).ToList();

    public async Task<List<ProdutoDto>> ListarAdminAsync() =>
        (await _produtos.ListarAsync(null, null, incluirInativos: true)).Select(Mapear).ToList();

    public async Task<ProdutoDto> CriarAsync(ProdutoSalvarDto dto)
    {
        await ValidarReferenciasAsync(dto);
        var p = new Produto();
        Aplicar(p, dto);
        await _produtos.AdicionarAsync(p);
        await _uow.SalvarAsync();
        return Mapear((await _produtos.ObterAsync(p.Id))!);
    }

    public async Task<ProdutoDto> AtualizarAsync(int id, ProdutoSalvarDto dto)
    {
        await ValidarReferenciasAsync(dto);
        var p = await _produtos.ObterAsync(id, rastrear: true) ?? throw new NegocioException("Produto não encontrado.", 404);
        Aplicar(p, dto); // se preço/estoque mudarem, a trigger grava a auditoria
        await _uow.SalvarAsync();
        return Mapear((await _produtos.ObterAsync(id))!);
    }

    // Exclusão lógica: preserva o histórico de pedidos que já usaram o produto.
    public async Task DesativarAsync(int id)
    {
        var p = await _produtos.ObterAsync(id, rastrear: true) ?? throw new NegocioException("Produto não encontrado.", 404);
        p.Ativo = false;
        await _uow.SalvarAsync();
    }

    private async Task ValidarReferenciasAsync(ProdutoSalvarDto dto)
    {
        if (!await _produtos.CategoriaExisteAsync(dto.CategoriaId)) throw new NegocioException("Categoria inexistente.");
        if (dto.MarcaId.HasValue && !await _produtos.MarcaExisteAsync(dto.MarcaId.Value)) throw new NegocioException("Marca inexistente.");
    }

    private static void Aplicar(Produto p, ProdutoSalvarDto d)
    {
        p.Nome = d.Nome.Trim(); p.Descricao = d.Descricao ?? string.Empty; p.Preco = d.Preco; p.Estoque = d.Estoque;
        p.ImagemUrl = d.ImagemUrl ?? string.Empty; p.CategoriaId = d.CategoriaId; p.MarcaId = d.MarcaId; p.Ativo = d.Ativo;
    }

    private static ProdutoDto Mapear(Produto p) => new(
        p.Id, p.Nome, p.Descricao, p.Preco, p.Estoque, p.ImagemUrl, p.CategoriaId, p.Categoria.Nome,
        p.Marca?.Nome, p.Imagens.OrderBy(i => i.Ordem).Select(i => i.Url).ToList());
}
