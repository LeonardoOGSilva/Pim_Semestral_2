namespace BoxTechAPI.Entities;

// Pagamento simulado: dados de cartão NUNCA são armazenados.
public class Pagamento
{
    public int Id { get; set; }
    public int PedidoId { get; set; }
    public string Metodo { get; set; } = string.Empty;
    public string Status { get; set; } = string.Empty;
    public byte Parcelas { get; set; }
    public decimal Valor { get; set; }
    public DateTime? DataPagamento { get; set; }
}
