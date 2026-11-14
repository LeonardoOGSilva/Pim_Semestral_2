namespace BoxTechAPI.Entities;

public class CarrinhoItem
{
    public int Id { get; set; }
    public int UsuarioId { get; set; }
    public int ProdutoId { get; set; }
    public Produto Produto { get; set; } = null!;
    public int Quantidade { get; set; } = 1;
}
