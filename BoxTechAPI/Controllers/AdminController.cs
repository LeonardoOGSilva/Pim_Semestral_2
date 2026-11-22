using BoxTechAPI.DTOs;
using BoxTechAPI.Security;
using BoxTechAPI.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace BoxTechAPI.Controllers;

// Área administrativa: somente usuários com o perfil "Admin" (role dentro do JWT).
[ApiController]
[Authorize(Roles = "Admin")]
[Route("api/admin")]
public class AdminController : ControllerBase
{
    private readonly IProdutoService _produtos;
    private readonly IPedidoService _pedidos;
    private readonly IConfiguration _config;
    private readonly IWebHostEnvironment _env;

    public AdminController(IProdutoService produtos, IPedidoService pedidos, IConfiguration config, IWebHostEnvironment env)
    {
        _produtos = produtos; _pedidos = pedidos; _config = config; _env = env;
    }

    // ---- Produtos (CRUD) ----
    [HttpGet("produtos")]
    public async Task<ActionResult<List<ProdutoDto>>> ListarProdutos() => Ok(await _produtos.ListarAdminAsync());

    [HttpPost("produtos")]
    public async Task<ActionResult<ProdutoDto>> CriarProduto(ProdutoSalvarDto dto)
    {
        var criado = await _produtos.CriarAsync(dto);
        return CreatedAtAction(nameof(ListarProdutos), new { id = criado.Id }, criado);
    }

    [HttpPut("produtos/{id:int}")]
    public async Task<ActionResult<ProdutoDto>> AtualizarProduto(int id, ProdutoSalvarDto dto) =>
        Ok(await _produtos.AtualizarAsync(id, dto));

    [HttpDelete("produtos/{id:int}")]
    public async Task<IActionResult> DesativarProduto(int id)
    {
        await _produtos.DesativarAsync(id);
        return NoContent();
    }

    [HttpDelete("produtos/{id:int}/definitivo")]
    public async Task<IActionResult> ExcluirProduto(int id)
    {
        await _produtos.ExcluirAsync(id);
        return NoContent();
    }

    // ---- Upload da foto do produto: o arquivo escolhido é copiado para a pasta Imagens do site ----
    private const long TamanhoMaximoImagem = 8 * 1024 * 1024;

    [HttpPost("produtos/imagem")]
    [Consumes("multipart/form-data")]
    [RequestSizeLimit(TamanhoMaximoImagem + 1024 * 1024)]
    public async Task<IActionResult> EnviarImagem([FromForm] IFormFile arquivo)
    {
        if (arquivo is null || arquivo.Length == 0) throw new NegocioException("Selecione um arquivo de imagem.");
        if (arquivo.Length > TamanhoMaximoImagem) throw new NegocioException("A imagem deve ter no máximo 8 MB.");

        // Confere o conteúdo real do arquivo (assinatura), e não só a extensão informada.
        var cab = new byte[12];
        int lidos;
        await using (var leitura = arquivo.OpenReadStream())
            lidos = await leitura.ReadAsync(cab, 0, cab.Length);

        string? extensao = null;
        if (lidos >= 4 && cab[0] == 0x89 && cab[1] == 0x50 && cab[2] == 0x4E && cab[3] == 0x47) extensao = ".png";
        else if (lidos >= 3 && cab[0] == 0xFF && cab[1] == 0xD8 && cab[2] == 0xFF) extensao = ".jpg";
        else if (lidos >= 12 && cab[0] == 'R' && cab[1] == 'I' && cab[2] == 'F' && cab[3] == 'F'
                 && cab[8] == 'W' && cab[9] == 'E' && cab[10] == 'B' && cab[11] == 'P') extensao = ".webp";
        if (extensao is null) throw new NegocioException("Formato não permitido. Envie uma imagem PNG, JPG ou WEBP.");

        // Nome seguro e único (ninguém escolhe o caminho de gravação).
        var baseNome = new string(Path.GetFileNameWithoutExtension(arquivo.FileName)
            .ToLowerInvariant().Select(c => char.IsAsciiLetterOrDigit(c) ? c : '-').ToArray()).Trim('-');
        if (baseNome.Length > 40) baseNome = baseNome[..40];
        if (baseNome.Length == 0) baseNome = "produto";
        var sufixo = Guid.NewGuid().ToString("N")[..8];
        var nomeFinal = $"{baseNome}-{sufixo}{extensao}";

        // Pasta Imagens do site (appsettings: Imagens:Pasta, relativa à pasta da API).
        var pastaConfig = _config["Imagens:Pasta"] ?? "../BoxTechFront/Imagens";
        var pasta = Path.GetFullPath(Path.Combine(_env.ContentRootPath, pastaConfig));
        Directory.CreateDirectory(pasta);

        await using (var destino = System.IO.File.Create(Path.Combine(pasta, nomeFinal)))
            await arquivo.CopyToAsync(destino);

        return Ok(new { caminho = $"Imagens/{nomeFinal}" });
    }

    // ---- Pedidos ----
    [HttpGet("pedidos")]
    public async Task<ActionResult<List<PedidoResumoAdminDto>>> ListarPedidos([FromQuery] string? status) =>
        Ok(await _pedidos.ListarTodosAsync(status));

    [HttpPut("pedidos/{id:int}/status")]
    public async Task<IActionResult> AtualizarStatus(int id, AtualizarStatusDto dto)
    {
        await _pedidos.AtualizarStatusAsync(id, dto.Status);
        return NoContent();
    }

    // ---- Relatórios (procedures do banco) ----
    [HttpGet("relatorios/vendas")]
    public async Task<ActionResult<List<VendasCategoriaDto>>> Vendas([FromQuery] DateOnly inicio, [FromQuery] DateOnly fim) =>
        Ok(await _pedidos.RelatorioVendasAsync(inicio, fim));

    [HttpGet("relatorios/mais-vendidos")]
    public async Task<ActionResult<List<ProdutoMaisVendidoDto>>> MaisVendidos([FromQuery] int top = 10) =>
        Ok(await _pedidos.MaisVendidosAsync(top));
}
