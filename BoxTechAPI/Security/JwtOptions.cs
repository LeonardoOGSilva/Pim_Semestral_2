namespace BoxTechAPI.Security;

public class JwtOptions
{
    public string Issuer { get; set; } = "BoxTechAPI";
    public string Audience { get; set; } = "BoxTechFront";
    public string Key { get; set; } = string.Empty;
    public int ExpiraEmMinutos { get; set; } = 120;
}
