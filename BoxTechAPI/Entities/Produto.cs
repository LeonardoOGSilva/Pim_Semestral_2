namespace BoxTechAPI.Entities;

public class Produto
{
    public int Id { get; set; }
    public int CategoriaId { get; set; }
    public Categoria Categoria { get; set; } = null!;
    public int? MarcaId { get; set; }
    public Marca? Marca { get; set; }
    public string Nome { get; set; } = string.Empty;
    public string Descricao { get; set; } = string.Empty;
    public decimal Preco { get; set; }
    public int Estoque { get; set; }
    public string ImagemUrl { get; set; } = string.Empty;
    public bool Ativo { get; set; } = true;
    public DateTime DataCadastro { get; set; } = DateTime.Now;
    public ICollection<ProdutoImagem> Imagens { get; set; } = new List<ProdutoImagem>();
}
