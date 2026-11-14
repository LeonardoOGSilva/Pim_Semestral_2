using System.Security.Claims;
using System.Text;
using BoxTechAPI.Entities;
using Microsoft.Extensions.Options;
using Microsoft.IdentityModel.JsonWebTokens;
using Microsoft.IdentityModel.Tokens;

namespace BoxTechAPI.Security;

public interface ITokenService
{
    (string Token, DateTime ExpiraEm) Gerar(Usuario usuario);
}

// Gera o JWT assinado (HS256) com id, nome e perfil (role) do usuário.
public class TokenService : ITokenService
{
    private readonly JwtOptions _opt;
    public TokenService(IOptions<JwtOptions> opt) => _opt = opt.Value;

    public (string Token, DateTime ExpiraEm) Gerar(Usuario usuario)
    {
        var expira = DateTime.UtcNow.AddMinutes(_opt.ExpiraEmMinutos);
        var descriptor = new SecurityTokenDescriptor
        {
            Subject = new ClaimsIdentity(new[]
            {
                new Claim(ClaimTypes.NameIdentifier, usuario.Id.ToString()),
                new Claim(ClaimTypes.Name, usuario.Nome),
                new Claim(ClaimTypes.Role, usuario.Perfil.Nome)
            }),
            Issuer = _opt.Issuer,
            Audience = _opt.Audience,
            Expires = expira,
            SigningCredentials = new SigningCredentials(
                new SymmetricSecurityKey(Encoding.UTF8.GetBytes(_opt.Key)), SecurityAlgorithms.HmacSha256)
        };
        return (new JsonWebTokenHandler().CreateToken(descriptor), expira);
    }
}
