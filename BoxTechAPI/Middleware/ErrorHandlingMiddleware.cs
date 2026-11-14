using BoxTechAPI.Security;

namespace BoxTechAPI.Middleware;

// Converte exceções em respostas JSON padronizadas e evita vazar detalhes internos (stack trace).
public class ErrorHandlingMiddleware
{
    private readonly RequestDelegate _next;
    private readonly ILogger<ErrorHandlingMiddleware> _log;
    public ErrorHandlingMiddleware(RequestDelegate next, ILogger<ErrorHandlingMiddleware> log) { _next = next; _log = log; }

    public async Task InvokeAsync(HttpContext ctx)
    {
        try { await _next(ctx); }
        catch (NegocioException ex)
        {
            ctx.Response.StatusCode = ex.StatusCode;
            await ctx.Response.WriteAsJsonAsync(new { erro = ex.Message });
        }
        catch (Exception ex)
        {
            _log.LogError(ex, "Erro não tratado em {Path}", ctx.Request.Path);
            ctx.Response.StatusCode = 500;
            await ctx.Response.WriteAsJsonAsync(new { erro = "Erro interno. Tente novamente mais tarde." });
        }
    }
}
