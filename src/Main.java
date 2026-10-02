import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JComboBox;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.BorderFactory;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class Main {

    // Paleta do tema escuro da janela (o canvas 3D tem seu proprio ceu dinamico)
    private static final Color BG_JANELA = new Color(16, 18, 24);
    private static final Color BG_PAINEL = new Color(22, 25, 33);
    private static final Color BORDA = new Color(45, 50, 60);
    private static final Color TEXTO_PRIMARIO = new Color(235, 237, 242);
    private static final Color TEXTO_SECUNDARIO = new Color(150, 158, 172);
    private static final Color ACENTO = new Color(90, 160, 255);
    private static final Color ACENTO_ALERTA = new Color(230, 90, 60);

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::criarUI);
    }

    private static void criarUI() {
        JFrame frame = new JFrame("Simulador de Tornado 3D");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.getContentPane().setBackground(BG_JANELA);

        SimPanel3D simPanel = new SimPanel3D();
        frame.add(simPanel, BorderLayout.CENTER);

        // ---- Cabecalho: titulo do app ----
        JPanel topo = new JPanel(new BorderLayout());
        topo.setBackground(BG_PAINEL);
        topo.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, BORDA),
                BorderFactory.createEmptyBorder(10, 16, 10, 16)));

        JLabel titulo = new JLabel("\uD83C\uDF2A  Simulador de Tornado 3D");
        titulo.setFont(new Font("SansSerif", Font.BOLD, 18));
        titulo.setForeground(TEXTO_PRIMARIO);
        topo.add(titulo, BorderLayout.WEST);

        JLabel subtitulo = new JLabel("Java + Termux X11");
        subtitulo.setFont(new Font("SansSerif", Font.PLAIN, 12));
        subtitulo.setForeground(TEXTO_SECUNDARIO);
        topo.add(subtitulo, BorderLayout.EAST);

        frame.add(topo, BorderLayout.NORTH);

        // ---- Barra de controles ----
        JPanel controller = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        controller.setBackground(BG_PAINEL);

        JLabel lblIntensidade = new JLabel("Intensidade (EF):");
        estilizarLabel(lblIntensidade, TEXTO_PRIMARIO, 13, false);

        JComboBox<Tornado.EFScale> escalaBox = new JComboBox<>(Tornado.EFScale.values());
        escalaBox.setSelectedItem(Tornado.EFScale.EF2);
        estilizarCombo(escalaBox);

        JButton btnIniciar = criarBotao("\u25B6  Iniciar", ACENTO);
        JButton btnPausar = criarBotao("\u23EF  Pausar / Retomar", null);
        JButton btnReiniciar = criarBotao("\u21BB  Reiniciar", null);

        controller.add(lblIntensidade);
        controller.add(escalaBox);
        controller.add(btnIniciar);
        controller.add(btnPausar);
        controller.add(btnReiniciar);

        // ---- Rodape: fase atual + instrucoes ----
        JLabel lblFase = new JLabel("Fase: Calmaria");
        estilizarLabel(lblFase, ACENTO, 13, true);
        lblFase.setBorder(BorderFactory.createEmptyBorder(0, 12, 6, 0));

        JLabel lblInstrucoes = new JLabel(
                "<html>Arraste a tela para olhar ao redor. Use o manche no canto inferior "
                        + "esquerdo para se mover, os botoes +/- para subir/descer, "
                        + "e o botao no canto superior direito para pausar.</html>");
        estilizarLabel(lblInstrucoes, TEXTO_SECUNDARIO, 12, false);
        lblInstrucoes.setBorder(BorderFactory.createEmptyBorder(0, 12, 10, 12));

        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new javax.swing.BoxLayout(infoPanel, javax.swing.BoxLayout.Y_AXIS));
        infoPanel.setBackground(BG_PAINEL);
        infoPanel.add(lblFase);
        infoPanel.add(lblInstrucoes);

        JPanel south = new JPanel(new BorderLayout());
        south.setBackground(BG_PAINEL);
        south.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, BORDA));
        south.add(controller, BorderLayout.NORTH);
        south.add(infoPanel, BorderLayout.SOUTH);
        frame.add(south, BorderLayout.SOUTH);

        btnIniciar.addActionListener(e -> {
            Tornado.EFScale escolhida = (Tornado.EFScale) escalaBox.getSelectedItem();
            simPanel.reset(escolhida);
            simPanel.iniciar();
            simPanel.requestFocusInWindow();
        });

        btnPausar.addActionListener(e -> {
            simPanel.pausarRetomar();
            simPanel.requestFocusInWindow();
        });

        btnReiniciar.addActionListener(e -> {
            Tornado.EFScale escolhida = (Tornado.EFScale) escalaBox.getSelectedItem();
            simPanel.reset(escolhida);
            simPanel.requestFocusInWindow();
        });

        simPanel.onStateChanged = () -> lblFase.setText("Fase: " + formatarFase(simPanel.getTornado().phase));

        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        simPanel.requestFocusInWindow();
    }

    private static String formatarFase(TornadoPhase p) {
        String nome = p.name().replace('_', ' ').toLowerCase();
        return Character.toUpperCase(nome.charAt(0)) + nome.substring(1);
    }

    private static void estilizarLabel(JLabel lbl, Color cor, int tamanho, boolean negrito) {
        lbl.setForeground(cor);
        lbl.setFont(new Font("SansSerif", negrito ? Font.BOLD : Font.PLAIN, tamanho));
    }

    private static void estilizarCombo(JComboBox<?> combo) {
        combo.setBackground(new Color(30, 34, 43));
        combo.setForeground(TEXTO_PRIMARIO);
        combo.setFont(new Font("SansSerif", Font.PLAIN, 13));
        combo.setFocusable(false);
        combo.setBorder(BorderFactory.createLineBorder(BORDA, 1));
    }

    /** Botao "flat" com destaque de cor opcional (usado no botao primario "Iniciar"). */
    private static JButton criarBotao(String texto, Color corDestaque) {
        Color base = corDestaque != null ? corDestaque : new Color(38, 42, 52);
        Color hover = corDestaque != null ? corDestaque.brighter() : new Color(48, 53, 65);

        JButton btn = new JButton(texto);
        btn.setFont(new Font("SansSerif", Font.BOLD, 13));
        btn.setForeground(TEXTO_PRIMARIO);
        btn.setBackground(base);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setOpaque(true);
        btn.setBorderPainted(false);

        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) { btn.setBackground(hover); }

            @Override
            public void mouseExited(MouseEvent e) { btn.setBackground(base); }
        });

        return btn;
    }
}
