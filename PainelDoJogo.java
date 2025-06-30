import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import javax.imageio.ImageIO;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.Timer;

public class PainelDoJogo extends JPanel implements Runnable, KeyListener {
        private enum GameState {TITLE, TUTORIAL, PLAYING, GAME_OVER, RANKING}
        private GameState gameState = GameState.TITLE;
        private Lixo lixo;
        private List<Projetil> projeteis = new ArrayList<>();
        private List<Lixeira> lixeiras = new ArrayList<>();
        private int pontuacao;
        private int vidas;
        private String msg = "";
        private boolean rodando = false;
        private Thread threadDoJogo;
        private Clip bgm;
        private Image bg;
        private Image gifGameOver;
        private Image titleImage;
        private Image tutorialImage;
        private ArrayList<String> urlsAleatoria = new ArrayList<>();
        private ArrayList<String> tiposAleatorio = new ArrayList<>();
        private List<PontuacaoRanking> ranking;
        private JTextField campoNome;
        private JTextField campoClasse;
        private boolean exibindoRanking = false;
        private boolean exibirFps = false;
        private int framesAtual = 0;
        private int fpsExibido = 0;
        private long proximoFps = 0;
        private int alpha = 255;

        public PainelDoJogo(){
            pontuacao = 0;
            vidas = 5;

            //Embaralhando os tipos que ja estao preenchidos na classe Lixeira
            for(String tipo : embaralharTipos(Lixeira.tipos)) {
                tiposAleatorio.add(tipo);
            }

            //Preenchendo a arrayList das urls com a ordem correta para o inicio do jogo
            urlsAleatoria = compararTipoComUrl(tiposAleatorio);

            //Instanciando lista para trabalhar com os valores de posicao armazenados na class Lixeira
            List<Integer> posicaoX = new ArrayList<>(Lixeira.valoresXY.keySet());

            lixo = new Lixo(375, 550);
            for(int i = 0; i < 5; i++) {
                Integer key = posicaoX.get(i); // Pega a chave na posição i
                Integer posicaoY = Lixeira.valoresXY.get(key); // Obtém o valor associado à chave
                lixeiras.add(new Lixeira(key, posicaoY, tiposAleatorio.get(i), urlsAleatoria.get(i))); //Criando as Lixeiras e adicionando à lista
            }
            addKeyListener(this);
            setFocusable(true);
            try{
                bg = ImageIO.read(getClass().getResource("/imgs/Gol.jpg"));
                gifGameOver = Toolkit.getDefaultToolkit().getImage(getClass().getResource("/imgs/gameover2.gif"));
                titleImage = ImageIO.read(getClass().getResource("/imgs/title.png"));
                tutorialImage = ImageIO.read(getClass().getResource("/imgs/tutorial.png"));
            } catch (Exception e) {
                e.printStackTrace();
            }

            this.setLayout(null);

            campoNome = new JTextField("");
            campoNome.setBounds(390,320,300,40);
            campoNome.setVisible(false);
            this.add(campoNome);

            campoClasse = new JTextField("");
            campoClasse.setBounds(390,370,300,40);
            campoClasse.setVisible(false);
            this.add(campoClasse);

            campoClasse.addActionListener(e -> processarEntradaRanking());
            ranking = JogoUtils.carregarRanking("ranking.json");

            iniciar();
        }

        //Funcao que fara com que os tipos de lixeiras fique aleatorios na hora de iniciar o jogo
        private ArrayList<String> embaralharTipos(ArrayList<String> tipos) {
            Collections.shuffle(tipos);
            return tipos;
        }

        //Funcao que compara as urls das imagens com os tipos ja embaralhados, para que possa adicionar as imagens de acordo com o tipo
        private ArrayList<String> compararTipoComUrl(ArrayList<String> tipos) {
            ArrayList<String> urls = new ArrayList<>();

            for(String tipo : tipos) {
                for(String url : Lixeira.urlImages) {
                    if(url.toUpperCase().contains(tipo.toUpperCase())) {
                        urls.add(url);
                    }
                }
            }
            return urls;
        }

        public synchronized void iniciar() {
            rodando = true;
            threadDoJogo = new Thread(this);
            threadDoJogo.start();
        }

        private void tocarBgm(String caminho) {
            try {
                URL soundURL = getClass().getResource(caminho);
                AudioInputStream audioStream = AudioSystem.getAudioInputStream(soundURL);
                bgm = AudioSystem.getClip();
                bgm.open(audioStream);
                FloatControl gainControl = (FloatControl) bgm.getControl(FloatControl.Type.MASTER_GAIN);
                gainControl.setValue(-10.0f); // Ajusta o volume
                bgm.loop(Clip.LOOP_CONTINUOUSLY);
            } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
                e.printStackTrace();
                System.out.println("Erro ao reproduzir som: " + e.getMessage());
            }
        }

        private void tocarEfeitoSonoro(String caminho) {
            try {
                URL soundURL = getClass().getResource(caminho);
                AudioInputStream audioStream = AudioSystem.getAudioInputStream(soundURL);
                Clip clip = AudioSystem.getClip();
                clip.open(audioStream);

                FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                gainControl.setValue(-10.0f);

                clip.start(); // Apenas inicia, sem loop
            } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
                e.printStackTrace();
                System.out.println("Erro ao reproduzir som: " + e.getMessage());
            }
        }

        private void pararBgm(){
            if (bgm != null && bgm.isRunning()){
                bgm.stop();
                bgm.close();
            }
        }

        @Override
        public void run() {           
            boolean mostrarTutorial = false;
            if (gameState == GameState.TITLE) {
                mostrarTutorial = true;
                tocarEfeitoSonoro("/sons/startup.wav");
            }
            // Aguarda o término do jingle para passar para o tutorial
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            
            // Exibe a tela de tutorial
            if (mostrarTutorial) {
                gameState = GameState.TUTORIAL;
                mostrarTutorial = false;
                repaint();
            }

            proximoFps = System.nanoTime() + 1_000_000_000; // 1 bilhão de nanossegundos = 1 segundo

            while(rodando){
                if (gameState == GameState.PLAYING) {
                    atualizar();
                }
                framesAtual++;
                if (System.nanoTime() > proximoFps){
                    fpsExibido = framesAtual;
                    framesAtual = 0;
                    proximoFps = System.nanoTime() + 1_000_000_000;
                }

                repaint();
                try {
                    Thread.sleep(16); // aproximadamente 60 fps
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        private void atualizar() {
            if (gameState == GameState.PLAYING) {
                lixo.mover(getWidth(), pontuacao);
     
                // Movimentação dos projéteis
                Iterator<Projetil> iteradorProjeteis = projeteis.iterator();
                while(iteradorProjeteis.hasNext()) {
                     Projetil proj = iteradorProjeteis.next();
                     proj.mover();
                     if (proj.foraDaTela()) {
                         iteradorProjeteis.remove(); //Remove Projéteis que sairam da tela
                         vidas  = (vidas==0) ? vidas = 0 : vidas-1;
                         tocarEfeitoSonoro("/sons/errou.wav");
                         verificarGameOver();
     
                         try {
                             lixo.randomizarLixo();
                         } catch (IOException e) {
                             e.printStackTrace();
                         }
     
                         lixo.setAtivo(true);
                     }
                }  
     
                for(Lixeira lixeira : lixeiras) {
                     lixeira.mover();
                }  
     
                for(Lixeira lixeira : lixeiras) {
                     lixeira.mover();
                }    
     
                List<Projetil> projeteisARemover = new ArrayList<>();
                boolean lixoDeveReaparecer = false;
     
                for (Lixeira lixeira : lixeiras) {
                 for (Projetil proj : projeteis) {
                     if (lixeira.getLimites().intersects(proj.getLimite())) {
                         // Colisão detectada
                         if (lixeira.getTipo().equalsIgnoreCase(proj.getTipo())){
                             pontuacao+=100;
                             //Adicionando sonzinho quando acerta
                             try{
                                 URL soundURL = getClass().getResource("/sons/acertou.wav");
                                 AudioInputStream audioStream = AudioSystem.getAudioInputStream(soundURL);
                                 Clip clip = AudioSystem.getClip();
                                 clip.open(audioStream);
                                 clip.start();
                             } catch(UnsupportedAudioFileException | IOException | LineUnavailableException e){
                                 e.printStackTrace();
                                 System.out.println("Erro ao reproduzir som: " + e.getMessage());
                             }                          
                         } else{
                             //Adicionando sonzinho quando erra
                             try{
                                vidas  = (vidas==0) ? vidas = 0 : vidas-1;
                                verificarGameOver();
                                 URL soundURL = getClass().getResource("/sons/errou.wav");
                                 AudioInputStream audioStream = AudioSystem.getAudioInputStream(soundURL);
                                 Clip clip = AudioSystem.getClip();
                                 clip.open(audioStream);
                                 clip.start();
                             } catch(UnsupportedAudioFileException | IOException | LineUnavailableException e){
                                 e.printStackTrace();
                                 System.out.println("Erro ao reproduzir som: " + e.getMessage());
                             }                
                         }
                             projeteisARemover.add(proj); 
                             lixoDeveReaparecer = true;                      
                             lixoDeveReaparecer = true;                      
                         }
                     }
                 }
     
                 projeteis.removeAll(projeteisARemover);
     
                 if (lixoDeveReaparecer) {
                     try {
                         lixo.randomizarLixo();
                     } catch (IOException e) {
                         e.printStackTrace();
                     }
                     lixo.setAtivo(true);
                 }
            }
        }

        @Override
        protected void paintComponent(Graphics g){
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;

            switch (gameState) {
                case TITLE:
                    g.drawImage(titleImage, 0, 0, getWidth(), getHeight(), this);
                    break;
    
                case TUTORIAL:
                    g.drawImage(tutorialImage, 0, 0, getWidth(), getHeight(), this);
                    break;
    
                case PLAYING:
                    g.drawImage(bg, 0, 0, getWidth(), getHeight(), null);
                    g.setFont(new Font("Arial", Font.BOLD, 20));;

                    if (exibirFps) {
                        g.setFont(new Font("Arial", Font.BOLD, 16));
                        g.setColor(Color.RED);
                        g.drawString(fpsExibido+" FPS", 10, 20); // Desenha no canto superior esquerdo
                    }

                    if (alpha > 0) {
                        g2d.setColor(Color.RED);
                        g2d.drawString(msg, 30, 50);
                    }
                    g.setColor(Color.BLACK);
                    g.drawString("Pontos: "+pontuacao, 30, 650);
                    g.drawString("Vidas: "+vidas, 30, 670);
                    
                    // desenhar objs aqui
                    if (lixo.isAtivo()) {
                        try {
                            lixo.desenhar(g);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                
                    for (Projetil proj : new ArrayList<>(projeteis)) {
                        proj.desenhar(g);
                    }
                    for(Lixeira lixeira : new ArrayList<>(lixeiras)) {
                        lixeira.desenhar(g);
                    }
                    break;
    
                case GAME_OVER:
                    if (gifGameOver != null) {
                        g.drawImage(gifGameOver, 0, 0, getWidth(), getHeight(), this);
                    }
                    g.setFont(new Font("Arial", Font.BOLD, 40));
                    g.setColor(Color.RED);
                    String textoPontuacao = "Sua pontuação: " + pontuacao;
                    int textoPontuacaoLargura = g.getFontMetrics().stringWidth(textoPontuacao);
                    g.drawString(textoPontuacao, (getWidth() - textoPontuacaoLargura) / 2, getHeight() / 2);
                    String textoContinuar = "[ENTER] para continuar.";
                    int textoContinuarLargura = g.getFontMetrics().stringWidth(textoContinuar);
                    g.drawString(textoContinuar, (getWidth() - textoContinuarLargura) / 2, getHeight() / 3);
                break;

                case RANKING:
                    g.setColor(Color.BLACK);
                    g.fillRect(0, 0, getWidth(), getHeight());

                    FontMetrics fm;
                    int larguraTexto;

                    if (!exibindoRanking) {
                        g.setFont(new Font("Arial", Font.BOLD, 50));
                        g.setColor(Color.YELLOW);
                        String textoTitulo = "FIM DE JOGO";
                        fm = g.getFontMetrics();
                        larguraTexto = fm.stringWidth(textoTitulo);
                        g.drawString(textoTitulo, (getWidth() - larguraTexto) / 2, getHeight() / 4);

                        g.setFont(new Font("Arial", Font.PLAIN, 25));
                        g.setColor(Color.WHITE);
                        String textoPontuacaoRanking = "Sua pontuação: " + pontuacao;
                        fm = g.getFontMetrics();
                        larguraTexto = fm.stringWidth(textoPontuacaoRanking);
                        g.drawString(textoPontuacaoRanking, (getWidth() - larguraTexto) / 2, getHeight() / 2 - 80);

                        String textoInstrucao = "Digite seu nome e turma.";
                        fm = g.getFontMetrics();
                        larguraTexto = fm.stringWidth(textoInstrucao);
                        g.drawString(textoInstrucao, (getWidth() - larguraTexto) / 2, getHeight() / 2 - 50);

                        // Dica de confirmação
                        g.setFont(new Font("Arial", Font.ITALIC, 20));
                        g.setColor(Color.GRAY);
                        String textoConfirmar = "[ENTER] no campo da turma para confirmar";
                        fm = g.getFontMetrics();
                        larguraTexto = fm.stringWidth(textoConfirmar);
                        g.drawString(textoConfirmar, (getWidth() - larguraTexto) / 2, getHeight() / 2 + 130);


                    } else {
                        g.setFont(new Font("Arial", Font.BOLD, 40));
                        g.setColor(Color.YELLOW);
                        String textoRankingTitulo = "TOP 10 DO RANKING";
                        fm = g.getFontMetrics();
                        larguraTexto = fm.stringWidth(textoRankingTitulo);
                        g.drawString(textoRankingTitulo, (getWidth() - larguraTexto) / 2, getHeight() / 8);

                        g.setFont(new Font("Monospaced", Font.BOLD, 20));
                        g.setColor(Color.WHITE);

                        int yPos = getHeight() / 4;
                        int xPos = getWidth() / 5;

                        int tamMax = Math.min(10, ranking.size());
                        for (int i = 0; i < tamMax; i++) {
                            PontuacaoRanking r = ranking.get(i);
                            String texto = String.format("%2d. %-20s (%-10s) - %d pts",
                                    i + 1,
                                    r.getNome(),
                                    r.getClasse(),
                                    r.getPontuacao());
                            g.drawString(texto, xPos, yPos);
                            yPos += 30;
                        }

                        g.setFont(new Font("Arial", Font.BOLD, 25));
                        g.setColor(Color.GREEN);
                        String textoReiniciar = "Pressione [R] para reiniciar o jogo.";
                        String textoSair = "Pressione [Esc] para sair do jogo.";
                        fm = g.getFontMetrics();
                        larguraTexto = fm.stringWidth(textoReiniciar);
                        g.drawString(textoReiniciar, (getWidth() - larguraTexto) / 2, getHeight() - 100);
                        g.drawString(textoSair, (getWidth() - larguraTexto) / 2, getHeight() - 70);
                    }
                    break;
            }
        }

        @Override
        public void keyTyped(KeyEvent e) {
        }

        @Override
        public void keyPressed(KeyEvent e) {
            if(e.getKeyCode() == KeyEvent.VK_SPACE && gameState==GameState.PLAYING) {
                if (projeteis.isEmpty()){
                    projeteis.add(new Projetil(lixo.getX(),lixo.getY(), lixo.getTipo()));
                    lixo.setAtivo(false);
                }
            }
            if(e.getKeyCode() == KeyEvent.VK_S && gameState==GameState.PLAYING) {
                salvarProgresso();
                desvanecer();
            }
            if(e.getKeyCode() == KeyEvent.VK_L && gameState==GameState.PLAYING) {
                carregarProgresso();                
                desvanecer();
            }
            if(e.getKeyCode() == KeyEvent.VK_F1 && gameState==GameState.PLAYING) {
                exibirFps = !exibirFps;
            }
        }

        @Override
        public void keyReleased(KeyEvent e) {
            if (gameState == GameState.TUTORIAL && e.getKeyCode() == KeyEvent.VK_ENTER) {
                gameState = GameState.PLAYING;
                tocarBgm("/sons/bgm.wav");
            }
            if (gameState == GameState.GAME_OVER && e.getKeyCode() == KeyEvent.VK_ENTER) {
                iniciarEntradaRanking();
            }
            if (gameState == GameState.RANKING && exibindoRanking && e.getKeyCode() == KeyEvent.VK_R) {
                reiniciarJogo();
            }
            if (gameState == GameState.RANKING && exibindoRanking && e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                System.exit(0);
            }
        }

        public void salvarProgresso(){
            ProgressoDoJogo progresso = new ProgressoDoJogo(lixo.getX(), lixo.getY(), pontuacao, vidas, lixo.getVelocidade(), lixo.getTipo());
            for(Lixeira lixeira : lixeiras){
                progresso.addPosicoesLixeiras(new int[]{lixeira.getX(), lixeira.getY()});
            }
            JogoUtils.salvarProgresso(progresso, "save.json");
            msg = "Progresso salvo com sucesso!";
        }

        public void carregarProgresso(){
            ProgressoDoJogo progresso = JogoUtils.carregarProgresso("save.json");
            if (progresso != null){
                projeteis.clear();
                lixo.setX(progresso.getPosicaoLixoX());
                lixo.setY(progresso.getPosicaoLixoY());
                lixo.setVelocidade(progresso.getVelocidade());
                if (lixo.getVelocidade()>0) lixo.setDirecao(1); else lixo.setDirecao(-1);
                lixo.setTipo(progresso.getTipo());
                try {
                    lixo.setImagem(lixo.carregarImagemTipo(lixo.getTipo()));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                lixo.setAtivo(true);
                pontuacao = progresso.getPontuacao();
                vidas = progresso.getVidas();
               // lixeiras.clear();
                
                for (int i = 0; i < lixeiras.size(); i++){
                    int[] posicao = progresso.getPosicoesLixeiras().get(i);
                    lixeiras.get(i).setX(posicao[0]);
                    lixeiras.get(i).setY(posicao[1]);   
                }

                msg = "Progresso carregado com sucesso!";
            }
            else msg = "Erro ao carregar o progresso.";
        }

        public void desvanecer(){
            alpha = 255;
            // Timer para criar o efeito de fade-out (executa a cada 50ms)
            Timer timer = new Timer(50, e -> {
                alpha -= 5; // Reduz a opacidade gradualmente
                if (alpha <= 0) {
                    ((Timer) e.getSource()).stop(); // Para o timer quando a string desaparecer
                }
                repaint(); // Atualiza o painel para aplicar o efeito
            });
            timer.start();
        }
    
        private void verificarGameOver() {
            if (vidas <= 0) {
                Random rand = new Random();
                String imgGameOver;
                int chance = rand.nextInt(100);
                if (chance < 1) { // 1% de chance (apenas o número 0)
                    imgGameOver = "/imgs/gameover.gif";
                } else if (chance < 10) { // 9% de chance (números de 1 a 9)
                    imgGameOver = "/imgs/gameover0.gif";
                } else if (chance < 40) { // 30% de chance (números de 10 a 39)
                    imgGameOver = "/imgs/gameover1.gif";
                } else { // 60% de chance (números de 40 a 99)
                    imgGameOver = "/imgs/gameover2.gif";
                }

                try {
                   gifGameOver = Toolkit.getDefaultToolkit().getImage(getClass().getResource(imgGameOver));
                } catch (RuntimeException e) {
                    System.err.println("Falha ao carregar a imagem sorteada: " + imgGameOver);
                }

                gameState = GameState.GAME_OVER;
            }
        }

        private void reiniciarJogo() {
            pontuacao = 0;
            vidas = 5;
            msg = "";
            exibindoRanking = false;
            projeteis.clear();

            tiposAleatorio.clear();
            tiposAleatorio.addAll(embaralharTipos(Lixeira.tipos));
            urlsAleatoria = compararTipoComUrl(tiposAleatorio);

            List<Integer> posicaoX = new ArrayList<>(Lixeira.valoresXY.keySet());
            for(int i = 0; i < lixeiras.size(); i++) {
                Integer key = posicaoX.get(i);
                Integer posicaoY = Lixeira.valoresXY.get(key);
                lixeiras.get(i).setX(key);
                lixeiras.get(i).setY(posicaoY);
                lixeiras.get(i).setVelocidade(2);

                try {
                    lixeiras.get(i).setImagem(ImageIO.read(getClass().getResource(urlsAleatoria.get(i))));
                    lixeiras.get(i).setTipo(tiposAleatorio.get(i));
                } catch (IOException e) {
                    throw new RuntimeException("Erro ao carregar imagem: "+e.getMessage());
                }
            }

            lixo = new Lixo(375, 550);
            lixo.setAtivo(true);

            pararBgm();
            gameState = GameState.TUTORIAL;
        }

        private void iniciarEntradaRanking() {
            gameState = GameState.RANKING;
            campoNome.setVisible(true);
            campoClasse.setVisible(true);

            campoNome.requestFocusInWindow();
        }

        private void processarEntradaRanking(){
            String nome = campoNome.getText();
            String classe = campoClasse.getText();

            ranking.add(new PontuacaoRanking(nome,classe, this.pontuacao));
            Collections.sort(ranking);
            JogoUtils.salvarRanking(ranking, "ranking.json");

            campoNome.setVisible(false);
            campoClasse.setVisible(false);
            exibindoRanking = true;
            repaint();
        }
}
