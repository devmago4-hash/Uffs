import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GradientPaint;
import java.awt.Color;
import java.awt.Font;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Dimension;
import java.awt.RenderingHints;
import java.awt.Rectangle;
import java.awt.BasicStroke;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class SimPanel3D extends JPanel {
    private static final int W = 900, H = 600;

    private Camera camera;
    private Tornado tornado;
    private House3D house;
    private RigidBox3D carro;
    private RigidBox3D lixeira;
    private Pole3D poste;
    private CloudSystem clouds;
    private Precipitation precip;
    private final List<Face3D> chaoFaces;
    private final List<Face3D> montanhas;
    private final List<Face3D> arbustos;

    // "Shader": iluminacao difusa (Lambert) por face + neblina de distancia
    private static final Vector3 DIRECAO_LUZ = new Vector3(0.4, 0.85, 0.3).normalized();
    private static final double LUZ_AMBIENTE = 0.35;
    private static final double LUZ_DIFUSA = 0.65;
    private static final double NEBLINA_INICIO = 55;
    private static final double NEBLINA_FIM = 160;

    private boolean pausado = true;
    private final Set<Integer> keysDown = new HashSet<>();
    private final javax.swing.Timer timer;

    // Controles touch (o teclado do Termux:X11 nem sempre chega ate o AWT)
    private boolean tqSobe, tqDesce;
    private boolean arrastandoOlhar = false;
    private int ultimoArrasteX, ultimoArrasteY;

    // Manche virtual (joystick) de movimento - drag dentro do circulo controla frente/tras/lados
    private boolean joystickAtivo = false;
    private double joyKnobX = 0, joyKnobY = 0; // deslocamento visual do manche em pixels
    private double joyForward = 0, joyStrafe = 0; // -1..1

    // Relampago: flash de brilho aleatorio nas fases mais severas
    private double flashIntensity = 0;
    private double tempoProximoRaio = 5.0;
    private final Random rngRaios = new Random();

    public Runnable onStateChanged;

    private static final double MOVE_SPEED = 18;
    private static final double ROT_SPEED = 1.6;
    private static final double TOUCH_LOOK_SENSIBILIDADE = 0.006;

    public SimPanel3D() {
        setPreferredSize(new Dimension(W, H));
        setBackground(new Color(20, 25, 35));
        setFocusable(true);

        chaoFaces = construirChao();
        montanhas = construirMontanhas();
        arbustos = construirArbustos();
        reset(Tornado.EFScale.EF2);

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int k = e.getKeyCode();
                if (k == KeyEvent.VK_SPACE && !keysDown.contains(k)) {
                    pausado = !pausado;
                }
                keysDown.add(k);
            }

            @Override
            public void keyReleased(KeyEvent e) {
                keysDown.remove(e.getKeyCode());
            }
        });

        MouseAdapter toque = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
                int x = e.getX(), y = e.getY();

                if (dentro(retPausa(), x, y)) {
                    pausado = !pausado;
                    return;
                }
                if (dentro(retSobe(), x, y)) { tqSobe = true; return; }
                if (dentro(retDesce(), x, y)) { tqDesce = true; return; }

                Point centro = joystickCentro();
                double dist = Math.hypot(x - centro.x, y - centro.y);
                if (dist <= joystickRaioBase() * 1.4) {
                    joystickAtivo = true;
                    atualizarJoystick(x, y);
                    return;
                }

                // fora de qualquer controle: comeca a "olhar" arrastando o dedo
                arrastandoOlhar = true;
                ultimoArrasteX = x;
                ultimoArrasteY = y;
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (joystickAtivo) {
                    atualizarJoystick(e.getX(), e.getY());
                    return;
                }
                if (!arrastandoOlhar) return;
                int dx = e.getX() - ultimoArrasteX;
                int dy = e.getY() - ultimoArrasteY;
                camera.rotate(dx * TOUCH_LOOK_SENSIBILIDADE, -dy * TOUCH_LOOK_SENSIBILIDADE);
                ultimoArrasteX = e.getX();
                ultimoArrasteY = e.getY();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                tqSobe = tqDesce = false;
                arrastandoOlhar = false;
                joystickAtivo = false;
                joyKnobX = 0;
                joyKnobY = 0;
                joyForward = 0;
                joyStrafe = 0;
            }
        };
        addMouseListener(toque);
        addMouseMotionListener(toque);

        timer = new javax.swing.Timer(16, e -> tick());
        timer.start();
    }

    private void atualizarJoystick(int x, int y) {
        Point centro = joystickCentro();
        double dx = x - centro.x;
        double dy = y - centro.y;
        double raioMax = joystickRaioMax();
        double dist = Math.hypot(dx, dy);
        if (dist > raioMax && dist > 1e-6) {
            dx = dx / dist * raioMax;
            dy = dy / dist * raioMax;
        }
        joyKnobX = dx;
        joyKnobY = dy;
        joyForward = -dy / raioMax;
        joyStrafe = dx / raioMax;
    }

    private boolean dentro(Rectangle r, int x, int y) {
        return r.contains(x, y);
    }

    // Escala geral da UI de acordo com o tamanho real da tela (deixa botoes/manche/HUD
    // proporcionais tanto num celular pequeno quanto numa janela grande do Termux:X11)
    private double escalaUI() {
        double m = Math.min(getLarguraAtual(), getAlturaAtual());
        return Math.max(0.75, Math.min(1.5, m / 640.0));
    }

    private int btnSize() { return (int) Math.round(46 * escalaUI()); }
    private int gap() { return (int) Math.round(8 * escalaUI()); }
    private int margemUI() { return (int) Math.round(20 * escalaUI()); }
    private int joystickRaioBase() { return (int) Math.round(52 * escalaUI()); }
    private int joystickRaioMax() { return (int) Math.round(36 * escalaUI()); }

    private Point joystickCentro() {
        int r = joystickRaioBase();
        return new Point(margemUI() + r, getAlturaAtual() - margemUI() - r);
    }

    private Rectangle retSobe() {
        int b = btnSize();
        return new Rectangle(getLarguraAtual() - b - margemUI(), getAlturaAtual() - 2 * (b + gap()) - margemUI(), b, b);
    }

    private Rectangle retDesce() {
        int b = btnSize();
        return new Rectangle(getLarguraAtual() - b - margemUI(), getAlturaAtual() - (b + gap()) - margemUI(), b, b);
    }

    private Rectangle retPausa() {
        double escala = escalaUI();
        int w = (int) Math.round(118 * escala), h = (int) Math.round(42 * escala);
        return new Rectangle(getLarguraAtual() - w - 14, 14, w, h);
    }

    private int getLarguraAtual() { return getWidth() <= 0 ? W : getWidth(); }
    private int getAlturaAtual() { return getHeight() <= 0 ? H : getHeight(); }

    public void reset(Tornado.EFScale escala) {
        tornado = new Tornado(escala, new Vector2(-40, -25));
        house = new House3D(new Vector3(15, 0, -5), 10, 8, 3, 2);
        carro = new RigidBox3D(new Vector3(10, 0, -9), 1.8, 1.4, 4.2, 1200, 1.1,
                new Color(180, 30, 30), new Color(140, 20, 20));
        lixeira = new RigidBox3D(new Vector3(21, 0, 4), 0.5, 0.7, 0.5, 8, 1.2,
                new Color(90, 90, 95), new Color(70, 70, 75));
        poste = new Pole3D(new Vector3(4, 0, -2), 6.5);
        clouds = new CloudSystem(new Vector3(tornado.center.x, 45, tornado.center.y));
        precip = new Precipitation(new Vector3(tornado.center.x, 0, tornado.center.y), 45);
        camera = new Camera(new Vector3(-10, 8, -45), 0.3, -0.1);
        pausado = true;
        flashIntensity = 0;
        tempoProximoRaio = 5.0;
        repaint();
    }

    public void iniciar() {
        tornado.setPhase(TornadoPhase.INSTABILIDADE);
        pausado = false;
    }

    public void pausarRetomar() {
        pausado = !pausado;
    }

    public Tornado getTornado() {
        return tornado;
    }

    private void tick() {
        double dt = 0.016;
        atualizarCamera(dt);

        if (!pausado) {
            tornado.update(dt);
            house.aplicarVento(tornado, dt);
            carro.update(tornado, dt);
            lixeira.update(tornado, dt);
            poste.update(tornado, dt);
            clouds.update(dt, new Vector3(tornado.center.x, 45, tornado.center.y));
            precip.update(tornado, dt, tornado.phase, tornado.scale);
            atualizarRelampago(dt);
        }

        if (flashIntensity > 0) flashIntensity = Math.max(0, flashIntensity - dt * 2.2);

        repaint();
        if (onStateChanged != null) onStateChanged.run();
    }

    /** Dispara flashes de relampago aleatorios durante as fases mais severas do tornado. */
    private void atualizarRelampago(double dt) {
        boolean fasePropiciaRaio = tornado.phase == TornadoPhase.WALL_CLOUD
                || tornado.phase == TornadoPhase.FUNIL
                || tornado.phase == TornadoPhase.TOQUE_NO_SOLO
                || tornado.phase == TornadoPhase.MADURO
                || tornado.phase == TornadoPhase.ENFRAQUECENDO;
        if (!fasePropiciaRaio) {
            tempoProximoRaio = 2.5;
            return;
        }
        tempoProximoRaio -= dt;
        if (tempoProximoRaio <= 0) {
            flashIntensity = 0.5 + rngRaios.nextDouble() * 0.5;
            tempoProximoRaio = 2.0 + rngRaios.nextDouble() * 5.0;
        }
    }

    private void atualizarCamera(double dt) {
        if (keysDown.contains(KeyEvent.VK_W)) camera.moveForward(MOVE_SPEED * dt);
        if (keysDown.contains(KeyEvent.VK_S)) camera.moveForward(-MOVE_SPEED * dt);
        if (keysDown.contains(KeyEvent.VK_A)) camera.moveRight(-MOVE_SPEED * dt);
        if (keysDown.contains(KeyEvent.VK_D)) camera.moveRight(MOVE_SPEED * dt);
        if (keysDown.contains(KeyEvent.VK_Q) || tqDesce) camera.moveUp(-MOVE_SPEED * dt);
        if (keysDown.contains(KeyEvent.VK_E) || tqSobe) camera.moveUp(MOVE_SPEED * dt);
        if (keysDown.contains(KeyEvent.VK_LEFT)) camera.rotate(-ROT_SPEED * dt, 0);
        if (keysDown.contains(KeyEvent.VK_RIGHT)) camera.rotate(ROT_SPEED * dt, 0);
        if (keysDown.contains(KeyEvent.VK_UP)) camera.rotate(0, ROT_SPEED * dt);
        if (keysDown.contains(KeyEvent.VK_DOWN)) camera.rotate(0, -ROT_SPEED * dt);

        if (Math.abs(joyForward) > 0.001) camera.moveForward(MOVE_SPEED * dt * joyForward);
        if (Math.abs(joyStrafe) > 0.001) camera.moveRight(MOVE_SPEED * dt * joyStrafe);
    }

    private List<Face3D> construirChao() {
        List<Face3D> out = new ArrayList<>();
        int n = 10;
        double tile = 14;
        double half = n * tile / 2;
        Random rng = new Random(42);
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                double x0 = -half + i * tile, z0 = -half + j * tile;
                double x1 = x0 + tile, z1 = z0 + tile;
                boolean par = (i + j) % 2 == 0;
                int base = par ? 0 : -10;
                int variacao = rng.nextInt(14) - 7;
                Color cor = new Color(
                        clampCor(78 + base + variacao),
                        clampCor(106 + base + variacao),
                        clampCor(58 + base + variacao));
                out.add(new Face3D(new Vector3[]{
                        new Vector3(x0, 0, z0), new Vector3(x1, 0, z0), new Vector3(x1, 0, z1), new Vector3(x0, 0, z1)
                }, cor));
            }
        }
        return out;
    }

    private int clampCor(int v) {
        return Math.max(0, Math.min(255, v));
    }

    /** Anel de "montanhas" baixas no horizonte - so para dar profundidade a paisagem, bem distante. */
    private List<Face3D> construirMontanhas() {
        List<Face3D> out = new ArrayList<>();
        int n = 18;
        double raio = 150;
        Random rng = new Random(7);
        Color corMontanha = new Color(60, 75, 85);
        for (int i = 0; i < n; i++) {
            double ang0 = 2 * Math.PI * i / n;
            double ang1 = 2 * Math.PI * (i + 1) / n;
            double pico = 18 + rng.nextDouble() * 22;
            Vector3 p0 = new Vector3(Math.cos(ang0) * raio, 0, Math.sin(ang0) * raio);
            Vector3 p1 = new Vector3(Math.cos(ang1) * raio, 0, Math.sin(ang1) * raio);
            Vector3 topo = new Vector3(Math.cos((ang0 + ang1) / 2) * raio * 0.94, pico, Math.sin((ang0 + ang1) / 2) * raio * 0.94);
            out.add(new Face3D(new Vector3[]{p0, p1, topo}, corMontanha));
        }
        return out;
    }

    /** Arbustos espalhados pelo terreno, so decoracao estatica. */
    private List<Face3D> construirArbustos() {
        List<Face3D> out = new ArrayList<>();
        Random rng = new Random(99);
        Color corArbusto = new Color(45, 80, 40);
        for (int i = 0; i < 14; i++) {
            double x = -55 + rng.nextDouble() * 110;
            double z = -55 + rng.nextDouble() * 110;
            if (x > 5 && x < 35 && z > -20 && z < 15) continue; // longe da casa/carro/poste
            double s = 0.6 + rng.nextDouble() * 0.5;
            Vector3 pos = new Vector3(x, 0, z);
            Vector3[] local = new Vector3[]{
                    new Vector3(-s / 2, 0, -s / 2), new Vector3(s / 2, 0, -s / 2),
                    new Vector3(s / 2, 0, s / 2), new Vector3(-s / 2, 0, s / 2),
                    new Vector3(-s / 2, s, -s / 2), new Vector3(s / 2, s, -s / 2),
                    new Vector3(s / 2, s, s / 2), new Vector3(-s / 2, s, s / 2)
            };
            Vector3[] w = new Vector3[8];
            for (int k = 0; k < 8; k++) w[k] = local[k].add(pos);
            out.add(new Face3D(new Vector3[]{w[4], w[5], w[6], w[7]}, corArbusto));
            out.add(new Face3D(new Vector3[]{w[0], w[1], w[5], w[4]}, corArbusto));
            out.add(new Face3D(new Vector3[]{w[2], w[3], w[7], w[6]}, corArbusto));
            out.add(new Face3D(new Vector3[]{w[3], w[0], w[4], w[7]}, corArbusto));
            out.add(new Face3D(new Vector3[]{w[1], w[2], w[6], w[5]}, corArbusto));
        }
        return out;
    }

    private List<Face3D> funnelFaces() {
        List<Face3D> out = new ArrayList<>();
        if (tornado.funnelWidthFactor <= 0.001) return out;

        double baseRadius = 8 * tornado.funnelWidthFactor;
        double topRadius = 3 * tornado.funnelWidthFactor;
        double topY = 3 + 35 * Math.min(1.0, tornado.funnelWidthFactor + 0.3);
        int niveis = 14;
        int lados = 10;

        for (int i = 0; i < niveis; i++) {
            double t = i / (double) (niveis - 1);
            double y = topY * t;
            double radius = baseRadius + (topRadius - baseRadius) * t;
            double wobble = Math.sin(tornado.rotationAngle * 2 + t * 8) * (1.5 * tornado.funnelWidthFactor);
            double cx = tornado.center.x + wobble;
            double cz = tornado.center.y;

            Vector3[] verts = new Vector3[lados];
            for (int k = 0; k < lados; k++) {
                double ang = 2 * Math.PI * k / lados;
                verts[k] = new Vector3(cx + Math.cos(ang) * radius, y, cz + Math.sin(ang) * radius);
            }
            int alpha = (int) (140 * (1 - t * 0.5));
            out.add(new Face3D(verts, new Color(55, 55, 60, Math.max(25, Math.min(255, alpha)))));
        }
        return out;
    }

    private int fasePeso(TornadoPhase p) {
        switch (p) {
            case CALMARIA: return 0;
            case INSTABILIDADE: return 15;
            case WALL_CLOUD: return 30;
            case FUNIL: return 40;
            case TOQUE_NO_SOLO: return 55;
            case MADURO: return 65;
            case ENFRAQUECENDO: return 45;
            case ROPE_OUT: return 25;
            default: return 5;
        }
    }

    private Color corFase(TornadoPhase p) {
        switch (p) {
            case CALMARIA: return new Color(120, 190, 255);
            case INSTABILIDADE: return new Color(255, 220, 120);
            case WALL_CLOUD: return new Color(255, 175, 90);
            case FUNIL: return new Color(255, 140, 70);
            case TOQUE_NO_SOLO: return new Color(255, 90, 70);
            case MADURO: return new Color(225, 40, 40);
            case ENFRAQUECENDO: return new Color(255, 150, 90);
            case ROPE_OUT: return new Color(255, 200, 130);
            default: return new Color(160, 200, 255);
        }
    }

    private String nomeFase(TornadoPhase p) {
        switch (p) {
            case CALMARIA: return "Calmaria";
            case INSTABILIDADE: return "Instabilidade";
            case WALL_CLOUD: return "Wall Cloud";
            case FUNIL: return "Funil";
            case TOQUE_NO_SOLO: return "Tornado em solo";
            case MADURO: return "Tornado maduro";
            case ENFRAQUECENDO: return "Enfraquecendo";
            case ROPE_OUT: return "Rope-out";
            case DISSIPADO: return "Dissipado";
            default: return p.name();
        }
    }

    private static class RenderItem {
        Polygon poly;
        Color color;
        double depth;

        RenderItem(Polygon poly, Color color, double depth) {
            this.poly = poly;
            this.color = color;
            this.depth = depth;
        }
    }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int dark = fasePeso(tornado.phase);
        Color corCeuTopo = new Color(Math.max(8, 55 - dark), Math.max(15, 80 - dark), Math.max(25, 130 - dark));
        Color corCeuHorizonte = new Color(Math.max(20, 130 - dark), Math.max(30, 150 - dark), Math.max(35, 165 - dark));
        g.setPaint(new GradientPaint(0, 0, corCeuTopo, 0, getHeight() * 0.65f, corCeuHorizonte));
        g.fillRect(0, 0, getWidth(), getHeight());
        g.setPaint(null);

        Color corCeu = corCeuHorizonte;

        List<Face3D> cena = new ArrayList<>();
        cena.addAll(montanhas);
        cena.addAll(chaoFaces);
        cena.addAll(arbustos);
        cena.addAll(house.render());
        cena.addAll(carro.render());
        cena.addAll(lixeira.render());
        cena.addAll(poste.render());
        cena.addAll(clouds.render());
        cena.addAll(funnelFaces());

        int W2 = getLarguraAtual();
        int H2 = getAlturaAtual();
        double focal = Math.min(W2, H2) * 0.9;

        List<RenderItem> itens = new ArrayList<>();
        for (Face3D f : cena) {
            Vector3[] cam = new Vector3[f.vertices.length];
            boolean valido = true;
            double somaZ = 0;
            for (int i = 0; i < f.vertices.length; i++) {
                Vector3 c = camera.toCameraSpace(f.vertices[i]);
                if (c.z < 0.3) {
                    valido = false;
                    break;
                }
                cam[i] = c;
                somaZ += c.z;
            }
            if (!valido) continue;

            int[] xs = new int[cam.length];
            int[] ys = new int[cam.length];
            for (int i = 0; i < cam.length; i++) {
                xs[i] = (int) (W2 / 2.0 + (cam[i].x / cam[i].z) * focal);
                ys[i] = (int) (H2 / 2.0 - (cam[i].y / cam[i].z) * focal);
            }
            double depth = somaZ / cam.length;
            Color corSombreada = aplicarShading(f);
            Color corComNeblina = aplicarNeblina(corSombreada, depth, corCeu);
            itens.add(new RenderItem(new Polygon(xs, ys, cam.length), corComNeblina, depth));
        }

        itens.sort((a, b) -> Double.compare(b.depth, a.depth));
        for (RenderItem it : itens) {
            g.setColor(it.color);
            g.fillPolygon(it.poly);
        }

        desenharChuvaEGranizo(g, W2, H2, focal);
        desenharRelampago(g, W2, H2);

        desenharHUD(g);
        desenharControlesTouch(g);
    }

    /** Painel de status: fase atual (com cor de severidade), vento e dano na casa, com barras de progresso. */
    private void desenharHUD(Graphics2D g) {
        double escala = escalaUI();
        int pad = (int) Math.round(13 * escala);
        int largura = (int) Math.round(232 * escala);
        int altura = (int) Math.round(94 * escala);
        int x = 14, y = 14;

        g.setColor(new Color(8, 10, 16, 165));
        g.fillRoundRect(x, y, largura, altura, 16, 16);
        g.setColor(new Color(255, 255, 255, 40));
        g.setStroke(new BasicStroke(1f));
        g.drawRoundRect(x, y, largura, altura, 16, 16);

        int dotR = (int) Math.round(10 * escala);
        int tx = x + pad, ty = y + pad;
        g.setColor(corFase(tornado.phase));
        g.fillOval(tx, ty, dotR, dotR);

        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, (int) Math.round(14 * escala)));
        g.drawString(nomeFase(tornado.phase), tx + dotR + 8, ty + dotR);

        g.setFont(new Font("SansSerif", Font.PLAIN, (int) Math.round(11 * escala)));
        g.setColor(new Color(200, 205, 215));
        g.drawString("Escala " + tornado.scale.name(), tx + dotR + 8, ty + dotR + (int) Math.round(15 * escala));

        double kmh = tornado.maxTangentialSpeed * 3.6;
        int larguraBarra = largura - 2 * pad;
        int alturaBarra = (int) Math.round(8 * escala);

        int y1 = y + pad + dotR + (int) Math.round(20 * escala);
        desenharGauge(g, x + pad, y1, larguraBarra, alturaBarra, kmh, 500,
                String.format("Vento maximo: %.0f km/h", kmh), escala);

        double dano = house.percentualDestruido();
        int y2 = y1 + (int) Math.round(28 * escala);
        desenharGauge(g, x + pad, y2, larguraBarra, alturaBarra, dano, 100,
                String.format("Casa: %.0f%% destruida", dano), escala);
    }

    /** Barra de progresso colorida (verde -> amarelo -> vermelho) com um rotulo acima. */
    private void desenharGauge(Graphics2D g, int x, int y, int w, int h, double valor, double max, String label, double escala) {
        g.setFont(new Font("SansSerif", Font.PLAIN, (int) Math.round(11 * escala)));
        g.setColor(new Color(225, 228, 235));
        g.drawString(label, x, y);

        int barY = y + (int) Math.round(4 * escala);
        double t = Math.max(0, Math.min(1, valor / max));

        g.setColor(new Color(255, 255, 255, 35));
        g.fillRoundRect(x, barY, w, h, h, h);
        if (t > 0.01) {
            g.setColor(corGauge(t));
            g.fillRoundRect(x, barY, Math.max(h, (int) (w * t)), h, h, h);
        }
    }

    private Color corGauge(double t) {
        if (t < 0.5) {
            return interpolarCor(new Color(70, 200, 110), new Color(255, 210, 70), t / 0.5);
        }
        return interpolarCor(new Color(255, 210, 70), new Color(230, 55, 50), (t - 0.5) / 0.5);
    }

    private Color interpolarCor(Color a, Color b, double t) {
        t = Math.max(0, Math.min(1, t));
        int r = (int) (a.getRed() + (b.getRed() - a.getRed()) * t);
        int gg = (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t);
        int bl = (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * t);
        return new Color(r, gg, bl);
    }

    private void desenharBotao(Graphics2D g, Rectangle r, String label, boolean pressionado, double escala) {
        g.setColor(pressionado ? new Color(255, 255, 255, 120) : new Color(255, 255, 255, 55));
        g.fillRoundRect(r.x, r.y, r.width, r.height, 14, 14);
        g.setColor(new Color(255, 255, 255, 170));
        g.setStroke(new BasicStroke(1.5f));
        g.drawRoundRect(r.x, r.y, r.width, r.height, 14, 14);
        g.setFont(new Font("SansSerif", Font.BOLD, (int) Math.round(20 * escala)));
        int tw = g.getFontMetrics().stringWidth(label);
        g.drawString(label, r.x + (r.width - tw) / 2, r.y + r.height / 2 + (int) Math.round(7 * escala));
    }

    /** Manche virtual: circulo base fixo + "bolinha" que acompanha o arrasto do dedo (drag), controla frente/tras/lados. */
    private void desenharJoystick(Graphics2D g, double escala) {
        Point centro = joystickCentro();
        int raioBase = joystickRaioBase();

        g.setColor(new Color(255, 255, 255, joystickAtivo ? 60 : 35));
        g.fillOval(centro.x - raioBase, centro.y - raioBase, raioBase * 2, raioBase * 2);
        g.setColor(new Color(255, 255, 255, 120));
        g.setStroke(new BasicStroke(2f));
        g.drawOval(centro.x - raioBase, centro.y - raioBase, raioBase * 2, raioBase * 2);

        int raioKnob = (int) Math.round(22 * escala);
        int kx = centro.x + (int) joyKnobX;
        int ky = centro.y + (int) joyKnobY;
        g.setColor(joystickAtivo ? new Color(255, 255, 255, 215) : new Color(255, 255, 255, 150));
        g.fillOval(kx - raioKnob, ky - raioKnob, raioKnob * 2, raioKnob * 2);
        g.setColor(new Color(15, 15, 20, 180));
        g.drawOval(kx - raioKnob, ky - raioKnob, raioKnob * 2, raioKnob * 2);
    }

    private void desenharBotaoPausa(Graphics2D g, double escala) {
        Rectangle rp = retPausa();
        Color destaque = pausado ? new Color(70, 200, 110) : new Color(230, 90, 60);
        g.setColor(new Color(destaque.getRed(), destaque.getGreen(), destaque.getBlue(), 70));
        g.fillRoundRect(rp.x, rp.y, rp.width, rp.height, 14, 14);
        g.setColor(new Color(destaque.getRed(), destaque.getGreen(), destaque.getBlue(), 215));
        g.setStroke(new BasicStroke(1.8f));
        g.drawRoundRect(rp.x, rp.y, rp.width, rp.height, 14, 14);
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, (int) Math.round(14 * escala)));
        String txt = pausado ? "\u25B6 Retomar" : "\u275A\u275A Pausar";
        int tw = g.getFontMetrics().stringWidth(txt);
        g.drawString(txt, rp.x + (rp.width - tw) / 2, rp.y + rp.height / 2 + (int) Math.round(5 * escala));
    }

    private void desenharControlesTouch(Graphics2D g) {
        double escala = escalaUI();

        desenharJoystick(g, escala);
        desenharBotao(g, retSobe(), "+", tqSobe, escala);
        desenharBotao(g, retDesce(), "\u2212", tqDesce, escala);
        desenharBotaoPausa(g, escala);

        g.setFont(new Font("SansSerif", Font.PLAIN, (int) Math.round(12 * escala)));
        g.setColor(new Color(255, 255, 255, 190));
        g.drawString("Arraste fora do manche para olhar ao redor", 14, getAlturaAtual() - (int) Math.round(12 * escala));
    }

    /**
     * "Shader" simples de iluminacao difusa (Lambert): calcula a normal da face
     * (produto vetorial de duas arestas) e usa o produto escalar com a direcao
     * da luz para modular o brilho. ambiente + difusa*max(0, N.L).
     */
    private Color aplicarShading(Face3D f) {
        if (f.vertices.length < 3) return f.color;
        Vector3 v0 = f.vertices[0], v1 = f.vertices[1], v2 = f.vertices[2];
        Vector3 normal = v1.sub(v0).cross(v2.sub(v0));
        double comprimento = normal.length();
        if (comprimento < 1e-9) return f.color;
        normal = normal.scale(1.0 / comprimento);

        double intensidade = Math.max(0, normal.dot(DIRECAO_LUZ));
        double brilho = Math.min(1.2, LUZ_AMBIENTE + LUZ_DIFUSA * intensidade);

        Color c = f.color;
        int r = (int) Math.min(255, c.getRed() * brilho);
        int g = (int) Math.min(255, c.getGreen() * brilho);
        int b = (int) Math.min(255, c.getBlue() * brilho);
        return new Color(r, g, b, c.getAlpha());
    }

    /** Neblina de distancia: mistura linear entre a cor da face e a cor do ceu conforme a profundidade. */
    private Color aplicarNeblina(Color c, double depth, Color ceu) {
        double t = (depth - NEBLINA_INICIO) / (NEBLINA_FIM - NEBLINA_INICIO);
        t = Math.max(0, Math.min(1, t));
        if (t <= 0) return c;
        int r = (int) (c.getRed() * (1 - t) + ceu.getRed() * t);
        int g = (int) (c.getGreen() * (1 - t) + ceu.getGreen() * t);
        int b = (int) (c.getBlue() * (1 - t) + ceu.getBlue() * t);
        return new Color(r, g, b, c.getAlpha());
    }

    /** Overlay branco translucido para simular o brilho de um relampago iluminando a cena inteira. */
    private void desenharRelampago(Graphics2D g, int W2, int H2) {
        if (flashIntensity <= 0.01) return;
        int alpha = (int) Math.min(255, flashIntensity * 190);
        g.setColor(new Color(255, 255, 255, alpha));
        g.fillRect(0, 0, W2, H2);
    }

    private void desenharChuvaEGranizo(Graphics2D g, int W2, int H2, double focal) {
        g.setColor(new Color(180, 190, 210, 160));
        for (Precipitation.RainDrop d : precip.chuva) {
            Vector3 c1 = camera.toCameraSpace(d.pos);
            Vector3 c2 = camera.toCameraSpace(d.pos.add(new Vector3(0, 1.4, 0)));
            if (c1.z < 0.3 || c2.z < 0.3) continue;
            int x1 = (int) (W2 / 2.0 + (c1.x / c1.z) * focal), y1 = (int) (H2 / 2.0 - (c1.y / c1.z) * focal);
            int x2 = (int) (W2 / 2.0 + (c2.x / c2.z) * focal), y2 = (int) (H2 / 2.0 - (c2.y / c2.z) * focal);
            g.drawLine(x1, y1, x2, y2);
        }

        g.setColor(new Color(230, 230, 240, 220));
        for (Precipitation.HailStone h : precip.granizo) {
            Vector3 c = camera.toCameraSpace(h.pos);
            if (c.z < 0.3) continue;
            int x = (int) (W2 / 2.0 + (c.x / c.z) * focal), y = (int) (H2 / 2.0 - (c.y / c.z) * focal);
            int r = Math.max(2, (int) (h.raio * focal / c.z));
            g.fillOval(x - r / 2, y - r / 2, r, r);
        }
    }
}
