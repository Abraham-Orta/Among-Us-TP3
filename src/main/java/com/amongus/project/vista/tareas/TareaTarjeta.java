package com.amongus.project.vista.tareas;
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author pancho
 */
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;

//por hacer aumentar dificultad 
public class TareaTarjeta extends JPanel implements MouseListener, MouseMotionListener {

    private final int ANCHO = 800, ALTO = 600;
    
    // Posiciones y dimensiones
    private Rectangle tarjetaRect;
    private final Point POS_INICIAL = new Point(50, 300);
    private final int READER_Y = 150;
    
    // Lógica del juego
    private long tiempoInicio;
    private boolean arrastrando = false;
    private boolean completada = false;
    
    // Textos en Español
    private String mensaje = "PASE TARJETA";
    private Color colorMensaje = new Color(255, 160, 0); // Naranja pantalla
    
    // Animación de retorno
    private Timer timerRetorno;
    private double animX, animY;

    // Estado de luces
    private boolean luzRoja = true;
    private boolean luzVerde = false;

    public TareaTarjeta() {
        setPreferredSize(new Dimension(ANCHO, ALTO));
        setBackground(new Color(30, 33, 36)); // Gris espacial oscuro
        
        tarjetaRect = new Rectangle(POS_INICIAL.x, POS_INICIAL.y, 240, 150);
        animX = POS_INICIAL.x;
        animY = POS_INICIAL.y;

        addMouseListener(this);
        addMouseMotionListener(this);
        
        timerRetorno = new Timer(15, e -> animarRetorno());
    }

    private void animarRetorno() {
        double dx = POS_INICIAL.x - animX;
        double dy = POS_INICIAL.y - animY;
        
        animX += dx * 0.2;
        animY += dy * 0.2;
        
        tarjetaRect.setLocation((int)animX, (int)animY);
        
        if (Math.abs(dx) < 1 && Math.abs(dy) < 1) {
            tarjetaRect.setLocation(POS_INICIAL);
            timerRetorno.stop();
        }
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        dibujarLector(g2);
        dibujarCarteraFondo(g2);
        dibujarTarjeta(g2);

        // Dibujar el bolsillo por encima solo si la tarjeta está guardada
        if (!arrastrando && Math.abs(tarjetaRect.y - POS_INICIAL.y) < 5) {
            dibujarCarteraBolsillo(g2);
        }
        
        if (completada) {
            g2.setColor(new Color(0, 255, 100, 40)); // Verde transparente
            g2.fillRect(0,0,ANCHO,ALTO);
        }
    }

    private void dibujarLector(Graphics2D g2) {
        // Cuerpo metálico
        GradientPaint metal = new GradientPaint(0, 100, new Color(70, 75, 80), 0, 300, new Color(40, 44, 48));
        g2.setPaint(metal);
        g2.fillRoundRect(50, 80, 700, 180, 20, 20);
        
        // Ranura oscura
        g2.setColor(new Color(20, 20, 20));
        g2.fillRoundRect(60, 180, 680, 40, 10, 10);

        // Pantalla Digital (Fondo)
        int xPantalla = 450;
        int anchoPantalla = 250;
        g2.setColor(new Color(20, 30, 20));
        g2.fillRect(xPantalla, 95, anchoPantalla, 60);
        
        // Texto Centrado en Pantalla
        g2.setColor(colorMensaje);
        g2.setFont(new Font("Monospaced", Font.BOLD, 26));
        FontMetrics fm = g2.getFontMetrics();
        int textoAncho = fm.stringWidth(mensaje);
        int xTexto = xPantalla + (anchoPantalla - textoAncho) / 2; // Cálculo para centrar
        g2.drawString(mensaje, xTexto, 135);

        // Luces LED
        g2.setColor(luzRoja ? new Color(255, 50, 50) : new Color(100, 0, 0));
        g2.fillOval(400, 110, 15, 15);
        
        g2.setColor(luzVerde ? new Color(50, 255, 50) : new Color(0, 100, 0));
        g2.fillOval(425, 110, 15, 15);
    }

    private void dibujarCarteraFondo(Graphics2D g2) {
        g2.setColor(new Color(101, 67, 33));
        AffineTransform old = g2.getTransform();
        g2.translate(20, 280);
        g2.rotate(Math.toRadians(-10));
        g2.fillRoundRect(0, 0, 300, 200, 30, 30);
        g2.setColor(new Color(80, 50, 20));
        g2.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, new float[]{5}, 0));
        g2.drawRoundRect(5, 5, 290, 190, 30, 30);
        g2.setTransform(old);
    }

    private void dibujarCarteraBolsillo(Graphics2D g2) {
        AffineTransform old = g2.getTransform();
        g2.translate(20, 280);
        g2.rotate(Math.toRadians(-10));
        
        GradientPaint cuero = new GradientPaint(0, 0, new Color(120, 80, 40), 0, 100, new Color(90, 60, 30));
        g2.setPaint(cuero);
        g2.fillRoundRect(0, 100, 300, 100, 30, 30);
        g2.setTransform(old);
    }

    private void dibujarTarjeta(Graphics2D g2) {
        int x = tarjetaRect.x;
        int y = tarjetaRect.y;
        int w = tarjetaRect.width;
        int h = tarjetaRect.height;

        g2.setColor(new Color(0,0,0,50));
        g2.fillRoundRect(x+5, y+5, w, h, 15, 15);

        g2.setColor(new Color(240, 240, 245));
        g2.fillRoundRect(x, y, w, h, 15, 15);

        g2.setColor(new Color(255, 140, 0)); 
        g2.fillRect(x, y + 20, w, 40);

        g2.setColor(new Color(200, 200, 200));
        g2.fillRect(x + 20, y + 70, 50, 60);
        
        g2.setColor(Color.GRAY);
        g2.fillRect(x + 80, y + 80, 100, 8);
        g2.fillRect(x + 80, y + 95, 80, 8);
        g2.fillRect(x + 80, y + 110, 60, 8);

        g2.setColor(new Color(30, 30, 30));
        g2.fillRect(x, y + 10, w, 10);
        
        // Texto en la tarjeta (También traducido)
        g2.setColor(Color.DARK_GRAY);
        g2.setFont(new Font("Arial", Font.BOLD, 10));
        g2.drawString("IDENTIFICACIÓN", x + 80, y + 75);

        g2.setColor(Color.BLACK);
        g2.setStroke(new BasicStroke(1));
        g2.drawRoundRect(x, y, w, h, 15, 15);
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (completada) return;
        if (tarjetaRect.contains(e.getPoint())) {
            arrastrando = true;
            timerRetorno.stop();
            tiempoInicio = System.currentTimeMillis();
            mensaje = "DESLIZANDO...";
            colorMensaje = Color.CYAN;
            luzRoja = true; luzVerde = false;
            repaint();
        }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (arrastrando) {
            int nuevoX = e.getX() - tarjetaRect.width / 2;
            if (nuevoX < 50) nuevoX = 50;
            if (nuevoX > 500) nuevoX = 500;
            
            int yFija = READER_Y - 20;
            
            tarjetaRect.setLocation(nuevoX, yFija);
            animX = nuevoX;
            animY = yFija;
            repaint();
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (arrastrando) {
            arrastrando = false;
            long tiempoTotal = System.currentTimeMillis() - tiempoInicio;
            boolean posicionCorrecta = tarjetaRect.x >= 450;

            if (!posicionCorrecta) {
                mensaje = "ERROR LECTURA";
                colorMensaje = Color.RED;
                luzRoja = true;
                timerRetorno.start();
            } else if (tiempoTotal < 300) {
                mensaje = "MUY RÁPIDO";
                colorMensaje = Color.RED;
                luzRoja = true;
                timerRetorno.start();
            } else if (tiempoTotal > 900) {
                mensaje = "MUY LENTO";
                colorMensaje = Color.RED;
                luzRoja = true;
                timerRetorno.start();
            } else {
                mensaje = "ACEPTADA";
                colorMensaje = new Color(0, 255, 100);
                luzRoja = false;
                luzVerde = true;
                completada = true;
            }
            repaint();
        }
    }

    public void mouseMoved(MouseEvent e) {}
    public void mouseClicked(MouseEvent e) {}
    public void mouseEntered(MouseEvent e) {}
    public void mouseExited(MouseEvent e) {}

    public static void main(String[] args) {
        JFrame f = new JFrame("Admin: Pasar Tarjeta");
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.add(new TareaTarjeta());
        f.pack();
        f.setLocationRelativeTo(null);
        f.setVisible(true);
    }
}