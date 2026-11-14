namespace BoxTechAPI.Entities;

public class ProdutoImagem
{
    public int Id { get; set; }
    public int ProdutoId { get; set; }
    public string Url { get; set; } = string.Empty;
    public byte Ordem { get; set; } = 1;
}
