namespace BoxTechAPI.Entities;

// Chave composta (UsuarioId + ProdutoId): o mesmo produto não é favoritado duas vezes.
public class Favorito
{
    public int UsuarioId { get; set; }
    public int ProdutoId { get; set; }
    public Produto Produto { get; set; } = null!;
    public DateTime DataCriacao { get; set; } = DateTime.Now;
}
