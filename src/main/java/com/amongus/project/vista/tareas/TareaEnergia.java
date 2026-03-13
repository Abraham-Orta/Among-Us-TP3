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
import java.awt.geom.AffineTransform;
import java.util.Random;
import javax.sound.sampled.*;

public class TareaEnergia extends JPanel {

    private final int ANCHO = 700, ALTO = 500;
    private Nodo[] nodos = new Nodo[6];
    private boolean completada = false;
    private TareaCompletadaListener listener;
    private final Color COLOR_FONDO = new Color(25, 25, 30);

    public void setTareaCompletadaListener(TareaCompletadaListener listener) {
        this.listener = listener;
    }
    private final Color COLOR_ENERGIA = new Color(255, 215, 0); 

    public TareaEnergia() {
        setPreferredSize(new Dimension(ANCHO, ALTO));
        setBackground(COLOR_FONDO);
        setLayout(null); 

        Random r = new Random();
        for (int i = 0; i < 6; i++) {
            int fila = i / 3;
            int col = i % 3;
            int rotInicial = (r.nextInt(3) + 1) * 90; // Evita que empiece en 0

            nodos[i] = new Nodo(150 + col * 180, 150 + fila * 150, rotInicial);
            final int index = i;
            
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (completada) return;
                    if (nodos[index].getBounds().contains(e.getPoint())) {
                        nodos[index].rotar();
                        // Sonido de "clic" mecánico
                        new Thread(() -> emitirSonidoMejorado(600, 50)).start();
                        verificarConexion();
                        repaint();
                    }
                }
            });
        }
    }

    // NUEVO MOTOR DE SONIDO: Con "Fade Out" para evitar ruidos feos
    private void emitirSonidoMejorado(int hz, int msecs) {
        try {
            byte[] buf = new byte[msecs * 8];
            for (int i = 0; i < buf.length; i++) {
                double angulo = i / (8000.0 / hz) * 2.0 * Math.PI;
                // Aplicamos un "envolvente" para que el volumen baje al final (evita el pop)
                double volumen = Math.min(1.0, (buf.length - i) / (double)(buf.length / 2));
                buf[i] = (byte) (Math.sin(angulo) * 100 * volumen);
            }
            AudioFormat af = new AudioFormat(8000f, 8, 1, true, false);
            SourceDataLine sdl = AudioSystem.getSourceDataLine(af);
            sdl.open(af);
            sdl.start();
            sdl.write(buf, 0, buf.length);
            sdl.drain();
            sdl.close();
        } catch (Exception ex) { }
    }

    private void verificarConexion() {
        boolean todoAlineado = true;
        for (Nodo n : nodos) {
            if (n.angulo % 360 != 0) {
                todoAlineado = false;
                break;
            }
        }
        if (todoAlineado) {
            completada = true;
            if (listener != null) listener.onTareaCompletada();
            // Sonido de éxito
            new Thread(() -> {
                emitirSonidoMejorado(800, 100);
                emitirSonidoMeriorado(1000, 200);
            }).start();
        }
    }

    private void emitirSonidoMeriorado(int hz, int ms) { emitirSonidoMejorado(hz, ms); }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Fondo y cables
        g2.setStroke(new BasicStroke(10));
        g2.setColor(new Color(40, 40, 45));
        g2.drawLine(100, 225, 600, 225); 

        for (Nodo n : nodos) n.dibujar(g2, completada);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Monospaced", Font.BOLD, 22));
        g2.drawString("DISTRIBUIR ENERGÍA", 230, 50);
        
        if (completada) {
            g2.setColor(COLOR_ENERGIA);
            g2.drawString("¡FLUJO ESTABLE!", 260, 450);
        }
    }

    private class Nodo {
        int x, y, angulo;
        int radio = 45;

        Nodo(int x, int y, int angulo) { this.x = x; this.y = y; this.angulo = angulo; }

        void rotar() { this.angulo += 90; }

        Rectangle getBounds() { return new Rectangle(x - radio, y - radio, radio * 2, radio * 2); }

        void dibujar(Graphics2D g2, boolean iluminado) {
            AffineTransform old = g2.getTransform();
            g2.translate(x, y);
            g2.rotate(Math.toRadians(angulo));

            // Estética de la pieza
            g2.setColor(new Color(45, 48, 52));
            g2.fillOval(-radio, -radio, radio * 2, radio * 2);
            g2.setColor(Color.GRAY);
            g2.setStroke(new BasicStroke(2));
            g2.drawOval(-radio, -radio, radio * 2, radio * 2);

            // Luz de conexión
            if (iluminado || angulo % 360 == 0) {
                g2.setColor(COLOR_ENERGIA);
                // Efecto de resplandor
                g2.setStroke(new BasicStroke(14));
            } else {
                g2.setColor(new Color(30, 30, 35));
                g2.setStroke(new BasicStroke(14));
            }
            
            g2.drawLine(-radio + 15, 0, radio - 15, 0);
            g2.fillOval(radio - 25, -5, 10, 10);

            g2.setTransform(old);
        }
    }

    public static void main(String[] args) {
        JFrame f = new JFrame("Admin: Distribución");
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.add(new TareaEnergia());
        f.pack();
        f.setLocationRelativeTo(null);
        f.setVisible(true);
    }
}