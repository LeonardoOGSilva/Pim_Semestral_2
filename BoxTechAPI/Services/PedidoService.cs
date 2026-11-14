using BoxTechAPI.DTOs;
using BoxTechAPI.Entities;
using BoxTechAPI.Repositories;
using BoxTechAPI.Security;
using Microsoft.Data.SqlClient;

namespace BoxTechAPI.Services;

public class PedidoService : IPedidoService
{
    private static readonly string[] StatusValidos =
        { "Aguardando Pagamento", "Pago", "Em Trânsito", "Entregue", "Cancelado" };

    private readonly IPedidoRepository _pedidos;
    private readonly ICarrinhoRepository _carrinho;
    private readonly IEnderecoRepository _enderecos;

    public PedidoService(IPedidoRepository pedidos, ICarrinhoRepository carrinho, IEnderecoRepository enderecos)
    {
        _pedidos = pedidos; _carrinho = carrinho; _enderecos = enderecos;
    }

    public async Task<PedidoDto> FinalizarAsync(int usuarioId, CheckoutDto dto)
    {
        var endereco = await _enderecos.ObterAsync(dto.EnderecoId, usuarioId)
                       ?? throw new NegocioException("Endereço não encontrado.", 404);

        var itens = await _carrinho.ListarAsync(usuarioId);
        var subtotal = itens.Sum(i => i.Produto.Preco * i.Quantidade);
        var frete = CalcularFrete(endereco.Estado, subtotal); // o frete é calculado no servidor, nunca vem do cliente

        var parcelas = dto.Metodo == "Pix" ? (byte)1 : dto.Parcelas;
        try
        {
            var id = await _pedidos.FinalizarAsync(usuarioId, endereco.Id, dto.Metodo, parcelas, frete);
            return await ObterDoUsuarioAsync(usuarioId, id);
        }
        catch (SqlException ex) when (ex.Number is >= 50000 and < 51000)
        {
            // Erros de negócio lançados pela procedure (THROW 5000x): carrinho vazio, sem estoque etc.
            throw new NegocioException(ex.Message, 409);
        }
    }

    public async Task<List<PedidoDto>> ListarDoUsuarioAsync(int usuarioId) =>
        (await _pedidos.ListarDoUsuarioAsync(usuarioId)).Select(Mapear).ToList();

    public async Task<PedidoDto> ObterDoUsuarioAsync(int usuarioId, int id) =>
        Mapear(await _pedidos.ObterDoUsuarioAsync(id, usuarioId) ?? throw new NegocioException("Pedido não encontrado.", 404));

    public async Task CancelarAsync(int usuarioId, int id)
    {
        _ = await _pedidos.ObterDoUsuarioAsync(id, usuarioId) ?? throw new NegocioException("Pedido não encontrado.", 404);
        await AtualizarComTratamentoAsync(id, "Cancelado"); // a trigger devolve o estoque
    }

    public async Task<List<PedidoResumoAdminDto>> ListarTodosAsync(string? status) =>
        (await _pedidos.ListarTodosAsync(status))
            .Select(p => new PedidoResumoAdminDto(p.Id, p.DataPedido, $"{p.Usuario.Nome} {p.Usuario.Sobrenome}",
                                                   p.Usuario.Email, p.Status, p.ValorTotal)).ToList();

    public async Task AtualizarStatusAsync(int id, string novoStatus)
    {
        if (!StatusValidos.Contains(novoStatus)) throw new NegocioException("Status inválido.");
        _ = await _pedidos.ObterSimplesAsync(id) ?? throw new NegocioException("Pedido não encontrado.", 404);
        await AtualizarComTratamentoAsync(id, novoStatus);
    }

    public Task<List<VendasCategoriaDto>> RelatorioVendasAsync(DateOnly inicio, DateOnly fim)
    {
        if (fim < inicio) throw new NegocioException("A data final deve ser maior ou igual à inicial.");
        return _pedidos.VendasPorCategoriaAsync(inicio, fim);
    }

    public Task<List<ProdutoMaisVendidoDto>> MaisVendidosAsync(int top) =>
        _pedidos.MaisVendidosAsync(Math.Clamp(top, 1, 50));

    private async Task AtualizarComTratamentoAsync(int id, string status)
    {
        try { await _pedidos.AtualizarStatusAsync(id, status); }
        catch (SqlException ex) when (ex.Number is >= 50000 and < 51000)
        {
            throw new NegocioException(ex.Message, 409);
        }
    }

    // Frete simulado por região; grátis acima de R$ 1.000,00.
    private static decimal CalcularFrete(string uf, decimal subtotal)
    {
        if (subtotal >= 1000m) return 0m;
        return uf.ToUpperInvariant() switch
        {
            "SP" => 15m,
            "RJ" or "MG" or "ES" => 25m,
            "PR" or "SC" or "RS" => 30m,
            "DF" or "GO" or "MT" or "MS" => 35m,
            _ => 45m
        };
    }

    private static PedidoDto Mapear(Pedido p) => new(
        p.Id, p.DataPedido, p.Status, p.ValorProdutos, p.ValorFrete, p.ValorTotal,
        p.Pagamento?.Metodo, p.Pagamento?.Status, p.Pagamento?.Parcelas,
        p.Itens.Select(i => new PedidoItemDto(i.ProdutoId, i.Produto.Nome, i.Quantidade, i.PrecoUnitario, i.Subtotal)).ToList());
}
