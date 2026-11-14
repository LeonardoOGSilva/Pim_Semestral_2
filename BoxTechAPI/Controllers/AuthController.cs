using BoxTechAPI.DTOs;
using BoxTechAPI.Security;
using BoxTechAPI.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.RateLimiting;

namespace BoxTechAPI.Controllers;

// Cadastro e login. O limite de requisições por minuto reduz ataques de força bruta.
[ApiController]
[Route("api/auth")]
[EnableRateLimiting("auth")]
public class AuthController : ControllerBase
{
    private readonly IAuthService _auth;
    public AuthController(IAuthService auth) => _auth = auth;

    [HttpPost("registro")]
    public async Task<ActionResult<AuthRespostaDto>> Registrar(RegistroDto dto) => Ok(await _auth.RegistrarAsync(dto));

    [HttpPost("login")]
    public async Task<ActionResult<AuthRespostaDto>> Login(LoginDto dto) => Ok(await _auth.LoginAsync(dto));

    [Authorize]
    [HttpGet("eu")]
    public async Task<ActionResult<UsuarioDto>> Eu() => Ok(await _auth.ObterUsuarioAsync(User.GetUserId()));
}
