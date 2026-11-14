using System.ComponentModel.DataAnnotations;

namespace BoxTechAPI.DTOs;

public record CarrinhoItemDto(int Id, int ProdutoId, string Nome, decimal Preco, string ImagemUrl, int Quantidade, decimal Subtotal);
public record CarrinhoDto(IReadOnlyList<CarrinhoItemDto> Itens, decimal Total);

public class CarrinhoAdicionarDto
{
    [Range(1, int.MaxValue)] public int ProdutoId { get; set; }
    [Range(1, 99)] public int Quantidade { get; set; } = 1;
}

public class CarrinhoAtualizarDto
{
    [Range(1, 99)] public int Quantidade { get; set; }
}

public record EnderecoDto(int Id, string Apelido, string Cep, string Logradouro, string Numero,
                          string? Complemento, string Bairro, string Cidade, string Estado, bool Principal);

public class EnderecoSalvarDto
{
    [Required, StringLength(40)] public string Apelido { get; set; } = "Casa";
    [Required, RegularExpression(@"^\d{5}-?\d{3}$", ErrorMessage = "CEP inválido.")] public string Cep { get; set; } = string.Empty;
    [Required, StringLength(150)] public string Logradouro { get; set; } = string.Empty;
    [Required, StringLength(10)] public string Numero { get; set; } = string.Empty;
    [StringLength(60)] public string? Complemento { get; set; }
    [Required, StringLength(80)] public string Bairro { get; set; } = string.Empty;
    [Required, StringLength(80)] public string Cidade { get; set; } = string.Empty;
    [Required, RegularExpression(@"^[A-Za-z]{2}$", ErrorMessage = "UF inválida.")] public string Estado { get; set; } = string.Empty;
    public bool Principal { get; set; }
}

public class CheckoutDto
{
    [Range(1, int.MaxValue)] public int EnderecoId { get; set; }

    // "Cartao" ou "Pix". Nenhum dado de cartão trafega ou é guardado (pagamento simulado).
    [Required, RegularExpression("^(Cartao|Pix)$", ErrorMessage = "Método de pagamento inválido.")]
    public string Metodo { get; set; } = string.Empty;

    [Range(1, 12)] public byte Parcelas { get; set; } = 1;
}

public record PedidoItemDto(int ProdutoId, string Nome, int Quantidade, decimal PrecoUnitario, decimal Subtotal);

public record PedidoDto(int Id, DateTime DataPedido, string Status, decimal ValorProdutos, decimal ValorFrete,
                        decimal ValorTotal, string? MetodoPagamento, string? StatusPagamento, byte? Parcelas,
                        IReadOnlyList<PedidoItemDto> Itens);

public record PedidoResumoAdminDto(int Id, DateTime DataPedido, string Cliente, string Email, string Status, decimal ValorTotal);

public class AtualizarStatusDto
{
    [Required] public string Status { get; set; } = string.Empty;
}

// Classes (e não records) porque recebem o resultado direto das procedures via SqlQuery.
public class VendasCategoriaDto
{
    public string Categoria { get; set; } = string.Empty;
    public int Pedidos { get; set; }
    public int UnidadesVendidas { get; set; }
    public decimal Faturamento { get; set; }
}
public class ProdutoMaisVendidoDto
{
    public int Id { get; set; }
    public string Nome { get; set; } = string.Empty;
    public int UnidadesVendidas { get; set; }
    public decimal Faturamento { get; set; }
}

public record FavoritoDto(int ProdutoId, string Nome, decimal Preco, string ImagemUrl);
