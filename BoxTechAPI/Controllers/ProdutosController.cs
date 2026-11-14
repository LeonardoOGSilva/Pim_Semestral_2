using BoxTechAPI.DTOs;
using BoxTechAPI.Services;
using Microsoft.AspNetCore.Mvc;

namespace BoxTechAPI.Controllers;

// Catálogo público (somente leitura).
[ApiController]
[Route("api/produtos")]
public class ProdutosController : ControllerBase
{
    private readonly IProdutoService _produtos;
    public ProdutosController(IProdutoService produtos) => _produtos = produtos;

    // GET api/produtos?categoriaId=2&busca=ssd
    [HttpGet]
    public async Task<ActionResult<List<ProdutoDto>>> Listar([FromQuery] int? categoriaId, [FromQuery] string? busca) =>
        Ok(await _produtos.ListarAsync(categoriaId, busca));

    [HttpGet("{id:int}")]
    public async Task<ActionResult<ProdutoDto>> Obter(int id) => Ok(await _produtos.ObterAsync(id));
}

[ApiController]
[Route("api/categorias")]
public class CategoriasController : ControllerBase
{
    private readonly IProdutoService _produtos;
    public CategoriasController(IProdutoService produtos) => _produtos = produtos;

    [HttpGet]
    public async Task<ActionResult<List<CategoriaDto>>> Listar() => Ok(await _produtos.ListarCategoriasAsync());
}
