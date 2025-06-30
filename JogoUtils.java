import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.FileNotFoundException;
import java.lang.reflect.Type;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import  java.util.List;

public class JogoUtils {
    // Método para salvar o progresso em um arquivo JSON
    public static void salvarProgresso(ProgressoDoJogo progresso, String caminhoArquivo) {
        Gson gson = new Gson();
        try (FileWriter writer = new FileWriter(caminhoArquivo)) {
            gson.toJson(progresso, writer);
        } catch (IOException e) {
            throw new RuntimeException("Erro ao salvar progresso.");
        }
    }

    // Método para carregar o progresso do arquivo JSON
    public static ProgressoDoJogo carregarProgresso(String caminhoArquivo) {
        Gson gson = new Gson();
        try (FileReader reader = new FileReader(caminhoArquivo)) {
            return gson.fromJson(reader, ProgressoDoJogo.class);
        } catch (IOException e) {
            throw new RuntimeException("Erro ao carregar progresso.");
        }
    }

    // Método para salvar a lista do ranking em ranking.json
    public static void salvarRanking(List<PontuacaoRanking> ranking, String caminhoArquivo){
        Gson gson = new Gson();
        try (FileWriter writer = new FileWriter((caminhoArquivo))){
            gson.toJson(ranking, writer);
        } catch (IOException e) {
            throw new RuntimeException("Erro ao salvar ranking.");
        }
    }

    // Método para carregar a lista do ranking de ranking.json
    public static List<PontuacaoRanking> carregarRanking(String caminhoArquivo){
        Gson gson = new Gson();
        Type tipoDaLista = new TypeToken<ArrayList<PontuacaoRanking>>() {}.getType();

        try (FileReader reader = new FileReader(caminhoArquivo)){
            List<PontuacaoRanking> ranking = gson.fromJson(reader, tipoDaLista);
            return ranking == null ? new ArrayList<>() : ranking;
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }
}
