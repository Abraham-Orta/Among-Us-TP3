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
import java.util.ArrayList;
import java.util.Random;

public class TareaAsteroides extends JPanel implements MouseListener, MouseMotionListener {

    private final int ANCHO = 800, ALTO = 600;
    private ArrayList<Asteroide> listaAsteroides = new ArrayList<>();
    private ArrayList<Particula> listaExplosiones = new ArrayList<>();
    private int destruidos = 0;
    private final int OBJETIVO = 20;
    private Timer timerJuego;
    private Point mousePos = new Point(0, 0);
    private boolean completada = false;
    private Random random = new Random();

    public TareaAsteroides() {
        setPreferredSize(new Dimension(ANCHO, ALTO));
        setBackground(new Color(5, 5, 15)); // Espacio profundo
        addMouseListener(this);
        addMouseMotionListener(this);

        timerJuego = new Timer(20, e -> actualizar());
        timerJuego.start();
    }

    private void actualizar() {
        if (completada) return;

        // Aparición de asteroides
        if (random.nextInt(100) < 6 && listaAsteroides.size() < 12) {
            crearAsteroide();
        }

        // Actualizar Asteroides
        for (int i = 0; i < listaAsteroides.size(); i++) {
            Asteroide a = listaAsteroides.get(i);
            a.actualizar();
            if (a.fueraDePantalla()) {
                listaAsteroides.remove(i);
                i--;
            }
        }

        // Actualizar Partículas de Explosión
        for (int i = 0; i < listaExplosiones.size(); i++) {
            Particula p = listaExplosiones.get(i);
            p.actualizar();
            if (p.vida <= 0) {
                listaExplosiones.remove(i);
                i--;
            }
        }
        repaint();
    }

    private void crearAsteroide() {
        int lado = random.nextInt(4);
        int x = 0, y = 0;
        switch(lado) {
            case 0: x = random.nextInt(ANCHO); y = -50; break;
            case 1: x = random.nextInt(ANCHO); y = ALTO + 50; break;
            case 2: x = -50; y = random.nextInt(ALTO); break;
            case 3: x = ANCHO + 50; y = random.nextInt(ALTO); break;
        }
        double vX = (ANCHO/2 - x) / 120.0 + (random.nextDouble() * 2 - 1);
        double vY = (ALTO/2 - y) / 120.0 + (random.nextDouble() * 2 - 1);
        listaAsteroides.add(new Asteroide(x, y, vX, vY, 40 + random.nextInt(40)));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Estrellas de fondo
        g2.setColor(Color.WHITE);
        for(int i=0; i<60; i++) {
            Random r = new Random(i * 999);
            int brillo = r.nextInt(255);
            g2.setColor(new Color(brillo, brillo, 255, 150));
            g2.fillOval(r.nextInt(ANCHO), r.nextInt(ALTO), r.nextInt(3), r.nextInt(3));
        }

        // Dibujar Asteroides
        for (Asteroide a : listaAsteroides) a.dibujar(g2);

        // Dibujar Explosiones
        for (Particula p : listaExplosiones) p.dibujar(g2);

        // Mira de Puntería (Crosshair)
        dibujarMira(g2);

        // Interfaz
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Monospaced", Font.BOLD, 22));
        g2.drawString("PUNTUACIÓN: " + destruidos + "/" + OBJETIVO, 25, 40);

        if (completada) {
            g2.setColor(new Color(0, 0, 0, 180));
            g2.fillRect(0, 0, ANCHO, ALTO);
            g2.setColor(new Color(0, 255, 100));
            g2.setFont(new Font("Monospaced", Font.BOLD, 55));
            g2.drawString("TAREA COMPLETADA", 140, ALTO / 2);
        }
    }

    private void dibujarMira(Graphics2D g2) {
        g2.setColor(new Color(0, 255, 0, 180));
        g2.setStroke(new BasicStroke(2));
        g2.drawOval(mousePos.x - 20, mousePos.y - 20, 40, 40);
        g2.drawLine(mousePos.x - 30, mousePos.y, mousePos.x + 30, mousePos.y);
        g2.drawLine(mousePos.x, mousePos.y - 30, mousePos.x, mousePos.y + 30);
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (completada) return;

        boolean impacto = false;
        for (int i = 0; i < listaAsteroides.size(); i++) {
            Asteroide a = listaAsteroides.get(i);
            if (a.contienePunto(e.getX(), e.getY())) {
                // Crear explosión
                for(int j=0; j<12; j++) 
                    listaExplosiones.add(new Particula(a.x + a.radio, a.y + a.radio));
                
                listaAsteroides.remove(i);
                destruidos++;
                impacto = true;
                if (destruidos >= OBJETIVO) completada = true;
                break;
            }
        }
    }

    // --- CLASES INTERNAS ---

    private class Asteroide {
        double x, y, velX, velY;
        int radio;
        Path2D silueta;
        ArrayList<Point> crateres = new ArrayList<>();

        Asteroide(double x, double y, double vX, double vY, int diametro) {
            this.x = x; this.y = y; this.velX = vX; this.velY = vY;
            this.radio = diametro / 2;
            generarFormaIrregular();
        }

        private void generarFormaIrregular() {
            silueta = new Path2D.Double();
            int puntos = 8 + random.nextInt(5);
            for (int i = 0; i < puntos; i++) {
                double angulo = Math.PI * 2 * i / puntos;
                double dist = radio * (0.8 + random.nextDouble() * 0.4);
                double px = radio + Math.cos(angulo) * dist;
                double py = radio + Math.sin(angulo) * dist;
                if (i == 0) silueta.moveTo(px, py);
                else silueta.lineTo(px, py);
            }
            silueta.closePath();
            // Añadir algunos cráteres aleatorios
            for(int i=0; i<3; i++) 
                crateres.add(new Point(random.nextInt(radio), random.nextInt(radio)));
        }

        void actualizar() { x += velX; y += velY; }
        
        boolean fueraDePantalla() {
            return (x < -100 || x > ANCHO + 100 || y < -100 || y > ALTO + 100);
        }

        boolean contienePunto(int px, int py) {
            return new Ellipse2D.Double(x, y, radio*2, radio*2).contains(px, py);
        }

        void dibujar(Graphics2D g2) {
            AffineTransform old = g2.getTransform();
            g2.translate(x, y);
            
            // Cuerpo del asteroide
            g2.setColor(new Color(100, 80, 70));
            g2.fill(silueta);
            
            // Sombra y Cráteres
            g2.setColor(new Color(60, 45, 40));
            for(Point p : crateres) g2.fillOval(p.x + radio/2, p.y + radio/2, radio/4, radio/5);
            
            g2.setStroke(new BasicStroke(2));
            g2.draw(silueta);
            g2.setTransform(old);
        }
    }

    private class Particula {
        double x, y, vx, vy;
        int vida = 255;
        Color color;

        Particula(double x, double y) {
            this.x = x; this.y = y;
            this.vx = (random.nextDouble() - 0.5) * 8;
            this.vy = (random.nextDouble() - 0.5) * 8;
            this.color = random.nextBoolean() ? Color.ORANGE : Color.DARK_GRAY;
        }

        void actualizar() {
            x += vx; y += vy;
            vida -= 10; // Se desvanece
        }

        void dibujar(Graphics2D g2) {
            if (vida <= 0) return;
            g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), vida));
            g2.fillOval((int)x, (int)y, 6, 6);
        }
    }

    public void mouseMoved(MouseEvent e) { mousePos = e.getPoint(); repaint(); }
    public void mouseDragged(MouseEvent e) { mouseMoved(e); }
    public void mouseClicked(MouseEvent e) {}
    public void mouseReleased(MouseEvent e) {}
    public void mouseEntered(MouseEvent e) {}
    public void mouseExited(MouseEvent e) {}

    public static void main(String[] args) {
        JFrame f = new JFrame("Among Us: Defensa de Asteroides");
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.add(new TareaAsteroides());
        f.pack();
        f.setLocationRelativeTo(null);
        f.setVisible(true);
    }
}