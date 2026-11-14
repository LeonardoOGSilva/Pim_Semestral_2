using System.ComponentModel.DataAnnotations;

namespace BoxTechAPI.DTOs;

public record CategoriaDto(int Id, string Nome, string? Descricao);

public record ProdutoDto(
    int Id, string Nome, string Descricao, decimal Preco, int Estoque, string ImagemUrl,
    int CategoriaId, string Categoria, string? Marca, IReadOnlyList<string> Imagens);

// Usado pelo Admin para criar/editar produtos.
public class ProdutoSalvarDto
{
    [Required, StringLength(150, MinimumLength = 2)] public string Nome { get; set; } = string.Empty;
    [StringLength(4000)] public string Descricao { get; set; } = string.Empty;
    [Range(0, 1_000_000)] public decimal Preco { get; set; }
    [Range(0, 1_000_000)] public int Estoque { get; set; }
    [StringLength(300)] public string ImagemUrl { get; set; } = string.Empty;
    [Range(1, int.MaxValue)] public int CategoriaId { get; set; }
    public int? MarcaId { get; set; }
    public bool Ativo { get; set; } = true;
}
