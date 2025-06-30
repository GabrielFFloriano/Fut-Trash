public class PontuacaoRanking implements Comparable<PontuacaoRanking> {
    private String nome;
    private String classe;
    private int pontuacao;

    public PontuacaoRanking(){}

    public PontuacaoRanking(String nome, String classe, int pontuacao){
        this.nome = nome;
        this.classe = classe;
        this.pontuacao = pontuacao;
    }

    public int getPontuacao() {
        return pontuacao;
    }

    public void setPontuacao(int pontuacao) {
        this.pontuacao = pontuacao;
    }

    public String getClasse() {
        return classe;
    }

    public void setClasse(String classe) {
        this.classe = classe;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    @Override
    public int compareTo(PontuacaoRanking outra){
        // Ordena pontuação descrescentemente
        return Integer.compare(outra.pontuacao, this.pontuacao);
    }
}
