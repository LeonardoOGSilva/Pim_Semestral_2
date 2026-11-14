using System.Security.Claims;

namespace BoxTechAPI.Security;

public static class ClaimsExtensions
{
    // O id do usuário SEMPRE vem do token, nunca da URL ou do corpo da requisição.
    public static int GetUserId(this ClaimsPrincipal user) =>
        int.TryParse(user.FindFirstValue(ClaimTypes.NameIdentifier), out var id)
            ? id : throw new NegocioException("Token inválido.", 401);
}
