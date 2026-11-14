namespace BoxTechAPI.Entities;

// Usuário do sistema. A senha nunca é guardada em texto puro: apenas o hash (BCrypt).
public class Usuario
{
    public int Id { get; set; }
    public int PerfilId { get; set; }
    public Perfil Perfil { get; set; } = null!;
    public string Nome { get; set; } = string.Empty;
    public string Sobrenome { get; set; } = string.Empty;
    public string CPF { get; set; } = string.Empty;
    public string Email { get; set; } = string.Empty;
    public string SenhaHash { get; set; } = string.Empty;
    public bool Ativo { get; set; } = true;
    public DateTime DataCadastro { get; set; } = DateTime.Now;
    public ICollection<Endereco> Enderecos { get; set; } = new List<Endereco>();
}
