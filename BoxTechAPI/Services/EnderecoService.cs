using BoxTechAPI.DTOs;
using BoxTechAPI.Entities;
using BoxTechAPI.Repositories;
using BoxTechAPI.Security;
using Microsoft.EntityFrameworkCore;

namespace BoxTechAPI.Services;

public class EnderecoService : IEnderecoService
{
    private readonly IEnderecoRepository _enderecos;
    private readonly IUnitOfWork _uow;

    public EnderecoService(IEnderecoRepository enderecos, IUnitOfWork uow) { _enderecos = enderecos; _uow = uow; }

    public async Task<List<EnderecoDto>> ListarAsync(int usuarioId) =>
        (await _enderecos.ListarAsync(usuarioId)).Select(Mapear).ToList();

    public async Task<EnderecoDto> CriarAsync(int usuarioId, EnderecoSalvarDto dto)
    {
        var primeiro = (await _enderecos.ListarAsync(usuarioId)).Count == 0;
        var e = new Endereco { UsuarioId = usuarioId };
        Aplicar(e, dto);
        e.Principal = dto.Principal || primeiro; // o primeiro endereço é sempre o principal
        if (e.Principal) await _enderecos.LimparPrincipalAsync(usuarioId);
        await _enderecos.AdicionarAsync(e);
        await _uow.SalvarAsync();
        return Mapear(e);
    }

    public async Task<EnderecoDto> AtualizarAsync(int usuarioId, int id, EnderecoSalvarDto dto)
    {
        var e = await _enderecos.ObterAsync(id, usuarioId) ?? throw new NegocioException("Endereço não encontrado.", 404);
        if (dto.Principal && !e.Principal) await _enderecos.LimparPrincipalAsync(usuarioId);
        Aplicar(e, dto);
        e.Principal = dto.Principal || e.Principal;
        await _uow.SalvarAsync();
        return Mapear(e);
    }

    public async Task RemoverAsync(int usuarioId, int id)
    {
        var e = await _enderecos.ObterAsync(id, usuarioId) ?? throw new NegocioException("Endereço não encontrado.", 404);
        _enderecos.Remover(e);
        try { await _uow.SalvarAsync(); }
        catch (DbUpdateException) // FK: endereço já usado em algum pedido
        {
            throw new NegocioException("Este endereço está vinculado a um pedido e não pode ser removido.", 409);
        }
    }

    private static void Aplicar(Endereco e, EnderecoSalvarDto d)
    {
        e.Apelido = d.Apelido.Trim(); e.Cep = d.Cep.Trim(); e.Logradouro = d.Logradouro.Trim(); e.Numero = d.Numero.Trim();
        e.Complemento = string.IsNullOrWhiteSpace(d.Complemento) ? null : d.Complemento.Trim();
        e.Bairro = d.Bairro.Trim(); e.Cidade = d.Cidade.Trim(); e.Estado = d.Estado.Trim().ToUpperInvariant();
    }

    private static EnderecoDto Mapear(Endereco e) =>
        new(e.Id, e.Apelido, e.Cep, e.Logradouro, e.Numero, e.Complemento, e.Bairro, e.Cidade, e.Estado, e.Principal);
}
