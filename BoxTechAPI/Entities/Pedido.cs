namespace BoxTechAPI.Entities;

// Pedidos são criados pela procedure sp_FinalizarPedido (transação no banco).
public class Pedido
{
    public int Id { get; set; }
    public int UsuarioId { get; set; }
    public Usuario Usuario { get; set; } = null!;
    public int EnderecoId { get; set; }
    public Endereco Endereco { get; set; } = null!;
    public DateTime DataPedido { get; set; }
    public string Status { get; set; } = string.Empty;
    public decimal ValorProdutos { get; set; }
    public decimal ValorFrete { get; set; }
    public decimal ValorTotal { get; private set; } // coluna calculada no banco
    public ICollection<PedidoItem> Itens { get; set; } = new List<PedidoItem>();
    public Pagamento? Pagamento { get; set; }
}
