using BoxTechAPI.DTOs;
using BoxTechAPI.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace BoxTechAPI.Controllers;

// Área administrativa: somente usuários com o perfil "Admin" (role dentro do JWT).
[ApiController]
[Authorize(Roles = "Admin")]
[Route("api/admin")]
public class AdminController : ControllerBase
{
    private readonly IProdutoService _produtos;
    private readonly IPedidoService _pedidos;

    public AdminController(IProdutoService produtos, IPedidoService pedidos)
    {
        _produtos = produtos; _pedidos = pedidos;
    }

    // ---- Produtos (CRUD) ----
    [HttpGet("produtos")]
    public async Task<ActionResult<List<ProdutoDto>>> ListarProdutos() => Ok(await _produtos.ListarAdminAsync());

    [HttpPost("produtos")]
    public async Task<ActionResult<ProdutoDto>> CriarProduto(ProdutoSalvarDto dto)
    {
        var criado = await _produtos.CriarAsync(dto);
        return CreatedAtAction(nameof(ListarProdutos), new { id = criado.Id }, criado);
    }

    [HttpPut("produtos/{id:int}")]
    public async Task<ActionResult<ProdutoDto>> AtualizarProduto(int id, ProdutoSalvarDto dto) =>
        Ok(await _produtos.AtualizarAsync(id, dto));

    [HttpDelete("produtos/{id:int}")]
    public async Task<IActionResult> DesativarProduto(int id)
    {
        await _produtos.DesativarAsync(id);
        return NoContent();
    }

    // ---- Pedidos ----
    [HttpGet("pedidos")]
    public async Task<ActionResult<List<PedidoResumoAdminDto>>> ListarPedidos([FromQuery] string? status) =>
        Ok(await _pedidos.ListarTodosAsync(status));

    [HttpPut("pedidos/{id:int}/status")]
    public async Task<IActionResult> AtualizarStatus(int id, AtualizarStatusDto dto)
    {
        await _pedidos.AtualizarStatusAsync(id, dto.Status);
        return NoContent();
    }

    // ---- Relatórios (procedures do banco) ----
    [HttpGet("relatorios/vendas")]
    public async Task<ActionResult<List<VendasCategoriaDto>>> Vendas([FromQuery] DateOnly inicio, [FromQuery] DateOnly fim) =>
        Ok(await _pedidos.RelatorioVendasAsync(inicio, fim));

    [HttpGet("relatorios/mais-vendidos")]
    public async Task<ActionResult<List<ProdutoMaisVendidoDto>>> MaisVendidos([FromQuery] int top = 10) =>
        Ok(await _pedidos.MaisVendidosAsync(top));
}
