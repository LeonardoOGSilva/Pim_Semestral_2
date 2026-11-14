using System.ComponentModel.DataAnnotations;

namespace BoxTechAPI.DTOs;

// DTOs de autenticação: a API nunca expõe a entidade Usuario (que contém o hash da senha).
public class RegistroDto
{
    [Required, StringLength(60, MinimumLength = 2)]
    public string Nome { get; set; } = string.Empty;

    [Required, StringLength(100, MinimumLength = 2)]
    public string Sobrenome { get; set; } = string.Empty;

    [Required, RegularExpression(@"^\d{3}\.?\d{3}\.?\d{3}-?\d{2}$", ErrorMessage = "CPF inválido.")]
    public string CPF { get; set; } = string.Empty;

    [Required, EmailAddress, StringLength(150)]
    public string Email { get; set; } = string.Empty;

    // Mínimo de 8 caracteres, com letra e número.
    [Required, StringLength(72, MinimumLength = 8)]
    [RegularExpression(@"^(?=.*[A-Za-z])(?=.*\d).+$", ErrorMessage = "A senha deve ter letras e números.")]
    public string Senha { get; set; } = string.Empty;
}

public class LoginDto
{
    [Required, EmailAddress] public string Email { get; set; } = string.Empty;
    [Required] public string Senha { get; set; } = string.Empty;
}

public record AuthRespostaDto(string Token, DateTime ExpiraEm, int UsuarioId, string Nome, string Perfil);
public record UsuarioDto(int Id, string Nome, string Sobrenome, string CPF, string Email, string Perfil);
