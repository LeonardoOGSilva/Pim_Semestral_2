using BoxTechAPI.DTOs;
using BoxTechAPI.Entities;
using BoxTechAPI.Repositories;
using BoxTechAPI.Security;

namespace BoxTechAPI.Services;

public class AuthService : IAuthService
{
    private readonly IUsuarioRepository _usuarios;
    private readonly IUnitOfWork _uow;
    private readonly ITokenService _tokens;

    public AuthService(IUsuarioRepository usuarios, IUnitOfWork uow, ITokenService tokens)
    {
        _usuarios = usuarios; _uow = uow; _tokens = tokens;
    }

    public async Task<AuthRespostaDto> RegistrarAsync(RegistroDto dto)
    {
        var cpf = FormatarCpf(dto.CPF);
        if (cpf == null) throw new NegocioException("CPF inválido.");
        var email = dto.Email.Trim().ToLowerInvariant();

        if (await _usuarios.EmailOuCpfExisteAsync(email, cpf))
            throw new NegocioException("E-mail ou CPF já cadastrado.", 409);

        var perfil = await _usuarios.ObterPerfilAsync("Cliente")
                     ?? throw new NegocioException("Perfil 'Cliente' não encontrado. Execute o script do banco.", 500);

        var usuario = new Usuario
        {
            PerfilId = perfil.Id, Perfil = perfil,
            Nome = dto.Nome.Trim(), Sobrenome = dto.Sobrenome.Trim(),
            CPF = cpf, Email = email,
            SenhaHash = BCrypt.Net.BCrypt.HashPassword(dto.Senha, 11) // hash com salt; nunca guardamos a senha
        };
        await _usuarios.AdicionarAsync(usuario);
        await _uow.SalvarAsync();
        return Resposta(usuario);
    }

    public async Task<AuthRespostaDto> LoginAsync(LoginDto dto)
    {
        var usuario = await _usuarios.ObterPorEmailAsync(dto.Email.Trim().ToLowerInvariant());

        // Mensagem única para e-mail inexistente ou senha errada (não revela qual dos dois falhou).
        if (usuario is null || !usuario.Ativo || !SenhaConfere(dto.Senha, usuario.SenhaHash))
            throw new NegocioException("E-mail ou senha inválidos.", 401);

        return Resposta(usuario);
    }

    public async Task<UsuarioDto> ObterUsuarioAsync(int id)
    {
        var u = await _usuarios.ObterPorIdAsync(id) ?? throw new NegocioException("Usuário não encontrado.", 404);
        return new UsuarioDto(u.Id, u.Nome, u.Sobrenome, u.CPF, u.Email, u.Perfil.Nome);
    }

    private AuthRespostaDto Resposta(Usuario u)
    {
        var (token, expira) = _tokens.Gerar(u);
        return new AuthRespostaDto(token, expira, u.Id, u.Nome, u.Perfil.Nome);
    }

    private static bool SenhaConfere(string senha, string hash)
    {
        try { return BCrypt.Net.BCrypt.Verify(senha, hash); }
        catch { return false; } // hash inválido (ex.: usuário de demonstração sem hash real)
    }

    // Valida os dígitos verificadores e devolve o CPF no formato 000.000.000-00.
    public static string? FormatarCpf(string entrada)
    {
        var d = new string(entrada.Where(char.IsDigit).ToArray());
        if (d.Length != 11 || d.Distinct().Count() == 1) return null;
        for (var t = 9; t <= 10; t++)
        {
            var soma = 0;
            for (var i = 0; i < t; i++) soma += (d[i] - '0') * (t + 1 - i);
            var dv = soma * 10 % 11 % 10;
            if (dv != d[t] - '0') return null;
        }
        return $"{d[..3]}.{d[3..6]}.{d[6..9]}-{d[9..]}";
    }
}
