package com.amongus.project.vista;

import javax.swing.JPanel;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.RadialGradientPaint;
import java.awt.Paint;
import java.awt.geom.Point2D;
import com.amongus.project.modelo.EstadoJuego;
import com.amongus.project.modelo.Jugador;
import com.amongus.project.modelo.Mapa;
import com.amongus.project.controlador.ManejadorEntrada;

/**
 * PanelJuego
 * ==========
 * Lienzo principal durante la partida.
 *
 * Cada instancia tiene su propio ManejadorEntrada y EstadoJuego,
 * por lo que múltiples ventanas abiertas simultáneamente (PruebaDirecta)
 * controlan a jugadores distintos de forma independiente.
 */
public class PanelJuego extends JPanel {

    private PantallaVotacion pantallaVotacion;

    // Instancia propia de ManejadorEntrada — NO estática, NO compartida
    private ManejadorEntrada manejadorEntrada;

    // Instancia propia de EstadoJuego — NO compartida entre ventanas
    private EstadoJuego estadoJuego;

    public PanelJuego(EstadoJuego estadoJuego) {
        this.estadoJuego = estadoJuego;

        setPreferredSize(new Dimension(800, 600));
        setBackground(Color.BLACK);
        setFocusable(true);

        // Cada ventana tiene su propio manejador → teclas independientes
        manejadorEntrada = new ManejadorEntrada(estadoJuego);
        addKeyListener(manejadorEntrada);

        // Ratón
        ManejadorEntrada.MouseHandler mouseHandler = manejadorEntrada.new MouseHandler();
        addMouseListener(mouseHandler);
        addMouseMotionListener(mouseHandler);

        this.pantallaVotacion = new PantallaVotacion(estadoJuego, manejadorEntrada);
    }

    /** BucleJuego lo usa para pasarle el manejador correcto a jugador.actualizar() */
    public ManejadorEntrada getManejadorEntrada() { return manejadorEntrada; }

    public PantallaVotacion getPantallaVotacion() { return pantallaVotacion; }

    public EstadoJuego getEstadoJuego() { return estadoJuego; }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        EstadoJuego.Fase fase = estadoJuego.getFaseActual();

        if (fase == EstadoJuego.Fase.VOTACION) {
            pantallaVotacion.render(g);
            return;
        }

        // --- CÁMARA ---
        int camX = 0, camY = 0;
        Jugador local = estadoJuego.getJugadorLocal();
        Mapa mapa     = estadoJuego.getMapa();

        if (local != null && mapa != null) {
            camX = local.getX() - (getWidth()  / 2);
            camY = local.getY() - (getHeight() / 2);
            if (camX < 0) camX = 0;
            if (camY < 0) camY = 0;
            if (camX > mapa.getAncho() - getWidth())  camX = mapa.getAncho() - getWidth();
            if (camY > mapa.getAlto()  - getHeight()) camY = mapa.getAlto()  - getHeight();
        }

        Graphics2D g2d = (Graphics2D) g;
        g2d.translate(-camX, -camY);

        // Capa 1: Mapa — le pasamos el flag de hitboxes de nuestra instancia del manejador
        if (mapa != null) mapa.render(g, manejadorEntrada.modoDesarrollador);

        // Capa 2: Jugadores
        for (Jugador j : estadoJuego.getJugadores()) {
            dibujarTripulante(g, j);
            g.setColor(Color.WHITE);
            g.drawString(j.getNombre(), j.getX(), j.getY() - 10);
        }
        if (local != null && !estadoJuego.getJugadores().contains(local)) {
            dibujarTripulante(g, local);
            g.setColor(Color.WHITE);
            g.drawString(local.getNombre(), local.getX(), local.getY() - 10);
        }

        // Capa 3: Niebla de guerra
        if (local != null && mapa != null) {
            dibujarCampoVisual(g2d, local, mapa.getAncho(), mapa.getAlto());
        }

        g2d.translate(camX, camY);

        if (estadoJuego.getJugadores().isEmpty() && local == null) {
            g.setColor(Color.WHITE);
            g.drawString("No hay jugadores. Inicia partida.", 300, 300);
        }
    }

    // Caché para la niebla de guerra — evita recrear el gradiente 60 veces/segundo
    private RadialGradientPaint gradCache;
    private float gradCacheX, gradCacheY, gradCacheRadio;

    private void dibujarCampoVisual(Graphics2D g2d, Jugador local, int anchoMapa, int altoMapa) {
        float radio;
        if (local.isImpostor()) {
            radio = 350.0f;
        } else {
            radio = estadoJuego.areLucesSaboteadas() ? 50.0f : 180.0f;
        }
        if (radio <= 0) radio = 1.0f;

        float cx = local.getX() + 15;
        float cy = local.getY() + 20;

        // Solo recrear el gradiente si cambió la posición o el radio
        if (gradCache == null || cx != gradCacheX || cy != gradCacheY || radio != gradCacheRadio) {
            gradCache = new RadialGradientPaint(
                new Point2D.Float(cx, cy), radio,
                new float[]{0.0f, 0.7f, 1.0f},
                new Color[]{
                    new Color(0, 0, 0,   0),
                    new Color(0, 0, 0, 120),
                    new Color(0, 0, 0, 255)
                }
            );
            gradCacheX = cx;
            gradCacheY = cy;
            gradCacheRadio = radio;
        }

        Paint original = g2d.getPaint();
        g2d.setPaint(gradCache);
        // Pintar un área lo suficientemente grande para cubrir la ventana
        // sin importar si el jugador está en el borde del mapa
        int rectX = (int)(cx - getWidth());
        int rectY = (int)(cy - getHeight());
        g2d.fillRect(rectX, rectY, getWidth() * 2, getHeight() * 2);
        g2d.setPaint(original);
    }

    private void dibujarTripulante(Graphics g, Jugador j) {
        int x = j.getX(), y = j.getY();
        int w = 30, h = 40;
        int dir = j.getDireccion();
        if (dir == 0) dir = 1;

        // Paralizado
        if (!j.isVivo()) {
            g.setColor(j.getColor().darker().darker());
            g.fillRoundRect(x, y + 25, w + 15, h / 2, 10, 10);
            g.setColor(Color.WHITE);
            g.fillOval(x + 15, y + 20, 10, 10);
            g.setColor(Color.RED);
            g.drawString("PARALIZADO", x - 10, y - 5);
            return;
        }

        // Vivo
        g.setColor(j.getColor());
        
        // --- ANIMACIÓN DE ASESINATO ---
        if (j.isAnimandoAsesinato()) {
            // Interpolar posición hacia la víctima
            int progreso = j.getFrameAsesinato();
            int totalFrames = 30;
            float ratio = (float) progreso / totalFrames;
            x = (int) (j.getX() + (j.getXVictima() - j.getX()) * ratio);
            y = (int) (j.getY() + (j.getYVictima() - j.getY()) * ratio);
            
            // Dibujar efecto de sangre/ataque intermitente
            if (progreso % 4 < 2) {
                g.setColor(Color.RED);
                g.fillOval(j.getXVictima() - 10, j.getYVictima() - 10, 50, 50);
                g.setColor(Color.WHITE);
                g.drawLine(j.getXVictima(), j.getYVictima(), j.getXVictima() + 30, j.getYVictima() + 40);
                g.drawLine(j.getXVictima() + 30, j.getYVictima(), j.getXVictima(), j.getYVictima() + 40);
            }
            g.setColor(j.getColor()); // Restaurar color para dibujar al impostor moviéndose
        }

        if (dir == 1) g.fillRect(x - 5,    y + 10, 10, 25);
        else          g.fillRect(x + w - 5, y + 10, 10, 25);
        g.fillRoundRect(x, y, w, h, 15, 15);
        g.fillRect(x,         y + h - 5, 10, 15);
        g.fillRect(x + w - 10, y + h - 5, 10, 15);

        g.setColor(new Color(150, 200, 220));
        if (dir == 1) g.fillRoundRect(x + 15, y + 10, 18, 12, 5, 5);
        else          g.fillRoundRect(x - 3,  y + 10, 18, 12, 5, 5);

        // Hitboxes (modo desarrollador)
        if (manejadorEntrada.modoDesarrollador) {
            g.setColor(Color.GREEN);
            g.drawRect(j.getHitbox().x, j.getHitbox().y,
                       j.getHitbox().width, j.getHitbox().height);
        }

        // Indicador HUD para el impostor local
        Jugador local = estadoJuego.getJugadorLocal();
        if (j == local && j.isImpostor()) {
            g.setColor(Color.RED);
            g.drawString(j.getNombre(), x, y - 10);
            g.setColor(Color.ORANGE);
            g.drawString("[Q] Paralizar | [E] Alcantarilla", x - 30, y + 65);
        }
    }
}