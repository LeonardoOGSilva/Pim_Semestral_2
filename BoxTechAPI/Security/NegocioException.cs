namespace BoxTechAPI.Security;

// Erro de regra de negócio com código HTTP (400, 401, 404, 409...). Tratado pelo middleware de erros.
public class NegocioException : Exception
{
    public int StatusCode { get; }
    public NegocioException(string mensagem, int statusCode = 400) : base(mensagem) => StatusCode = statusCode;
}
