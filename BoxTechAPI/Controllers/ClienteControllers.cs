using BoxTechAPI.DTOs;
using BoxTechAPI.Security;
using BoxTechAPI.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace BoxTechAPI.Controllers;

// Todos os endpoints abaixo exigem login. O usuário é identificado pelo TOKEN (User.GetUserId()),
// então um cliente nunca consegue ver ou alterar dados de outro.

[ApiController, Authorize, Route("api/carrinho")]
public class CarrinhoController : ControllerBase
{
    private readonly ICarrinhoService _carrinho;
    public CarrinhoController(ICarrinhoService carrinho) => _carrinho = carrinho;

    [HttpGet]
    public async Task<ActionResult<CarrinhoDto>> Listar() => Ok(await _carrinho.ListarAsync(User.GetUserId()));

    [HttpPost]
    public async Task<ActionResult<CarrinhoDto>> Adicionar(CarrinhoAdicionarDto dto) =>
        Ok(await _carrinho.AdicionarAsync(User.GetUserId(), dto));

    [HttpPut("{itemId:int}")]
    public async Task<ActionResult<CarrinhoDto>> Atualizar(int itemId, CarrinhoAtualizarDto dto) =>
        Ok(await _carrinho.AtualizarQuantidadeAsync(User.GetUserId(), itemId, dto.Quantidade));

    [HttpDelete("{itemId:int}")]
    public async Task<ActionResult<CarrinhoDto>> Remover(int itemId) =>
        Ok(await _carrinho.RemoverAsync(User.GetUserId(), itemId));
}

[ApiController, Authorize, Route("api/favoritos")]
public class FavoritosController : ControllerBase
{
    private readonly IFavoritoService _favoritos;
    public FavoritosController(IFavoritoService favoritos) => _favoritos = favoritos;

    [HttpGet]
    public async Task<ActionResult<List<FavoritoDto>>> Listar() => Ok(await _favoritos.ListarAsync(User.GetUserId()));

    [HttpPost("{produtoId:int}")]
    public async Task<IActionResult> Adicionar(int produtoId)
    {
        await _favoritos.AdicionarAsync(User.GetUserId(), produtoId);
        return NoContent();
    }

    [HttpDelete("{produtoId:int}")]
    public async Task<IActionResult> Remover(int produtoId)
    {
        await _favoritos.RemoverAsync(User.GetUserId(), produtoId);
        return NoContent();
    }
}

[ApiController, Authorize, Route("api/enderecos")]
public class EnderecosController : ControllerBase
{
    private readonly IEnderecoService _enderecos;
    public EnderecosController(IEnderecoService enderecos) => _enderecos = enderecos;

    [HttpGet]
    public async Task<ActionResult<List<EnderecoDto>>> Listar() => Ok(await _enderecos.ListarAsync(User.GetUserId()));

    [HttpPost]
    public async Task<ActionResult<EnderecoDto>> Criar(EnderecoSalvarDto dto) =>
        Ok(await _enderecos.CriarAsync(User.GetUserId(), dto));

    [HttpPut("{id:int}")]
    public async Task<ActionResult<EnderecoDto>> Atualizar(int id, EnderecoSalvarDto dto) =>
        Ok(await _enderecos.AtualizarAsync(User.GetUserId(), id, dto));

    [HttpDelete("{id:int}")]
    public async Task<IActionResult> Remover(int id)
    {
        await _enderecos.RemoverAsync(User.GetUserId(), id);
        return NoContent();
    }
}

[ApiController, Authorize, Route("api/pedidos")]
public class PedidosController : ControllerBase
{
    private readonly IPedidoService _pedidos;
    public PedidosController(IPedidoService pedidos) => _pedidos = pedidos;

    // Checkout: transforma o carrinho em pedido (procedure sp_FinalizarPedido). Pagamento simulado.
    [HttpPost("checkout")]
    public async Task<ActionResult<PedidoDto>> Checkout(CheckoutDto dto) =>
        Ok(await _pedidos.FinalizarAsync(User.GetUserId(), dto));

    [HttpGet]
    public async Task<ActionResult<List<PedidoDto>>> Listar() => Ok(await _pedidos.ListarDoUsuarioAsync(User.GetUserId()));

    [HttpGet("{id:int}")]
    public async Task<ActionResult<PedidoDto>> Obter(int id) => Ok(await _pedidos.ObterDoUsuarioAsync(User.GetUserId(), id));

    [HttpPost("{id:int}/cancelar")]
    public async Task<IActionResult> Cancelar(int id)
    {
        await _pedidos.CancelarAsync(User.GetUserId(), id);
        return NoContent();
    }
}
