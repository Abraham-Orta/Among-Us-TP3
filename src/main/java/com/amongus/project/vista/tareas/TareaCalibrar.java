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

public class TareaCalibrar extends JPanel {

    private final int ANCHO = 800, ALTO = 500;
    private Distribuidor[] distribuidores = new Distribuidor[3];
    private int etapaActual = 0; 
    private Timer timerAnimacion;
    private boolean completada = false;
    private TareaCompletadaListener listener;

    public void setTareaCompletadaListener(TareaCompletadaListener listener) {
        this.listener = listener;
    }

    public TareaCalibrar() {
        setPreferredSize(new Dimension(ANCHO, ALTO));
        setBackground(new Color(15, 15, 20));

        // Inicializar distribuidores
        distribuidores[0] = new Distribuidor(150, 250, Color.CYAN, 2.0);
        distribuidores[1] = new Distribuidor(400, 250, new Color(255, 0, 255), 3.5);
        distribuidores[2] = new Distribuidor(650, 250, Color.YELLOW, 5.0);

        timerAnimacion = new Timer(20, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!completada) {
                    for (int i = etapaActual; i < 3; i++) {
                        distribuidores[i].girar();
                    }
                    repaint();
                }
            }
        });
        timerAnimacion.start();

        // MÉTODO DE DETECCIÓN MEJORADO
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (completada) return;
                
                System.out.println("Clic detectado en: " + e.getX() + "," + e.getY()); // Debug
                
                // Comprobamos el botón del distribuidor que toca calibrar
                if (distribuidores[etapaActual].botonRect.contains(e.getPoint())) {
                    System.out.println("¡Le diste al botón " + (etapaActual + 1) + "!");
                    verificarCalibracion();
                }
            }
        });
    }

    private void verificarCalibracion() {
        double ang = distribuidores[etapaActual].angulo % 360;
        int margen = 25; // Margen más generoso (25 grados)

        // El objetivo es 0 grados (donde está el cable a la derecha)
        if (ang > (360 - margen) || ang < margen) {
            distribuidores[etapaActual].calibrado = true;
            System.out.println("-> CALIBRADO");
            etapaActual++;
            if (etapaActual > 2) {
                completada = true;
                timerAnimacion.stop();
                if (listener != null) listener.onTareaCompletada();
            }
        } else {
            System.out.println("-> FALLO: Reiniciando...");
            etapaActual = 0;
            for (int i = 0; i < 3; i++) {
                distribuidores[i].calibrado = false;
            }
        }
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Monospaced", Font.BOLD, 28));
        g2.drawString("CALIBRAR DISTRIBUIDORES", 220, 70);

        for (int i = 0; i < 3; i++) {
            distribuidores[i].dibujar(g2, i == etapaActual);
        }

        if (completada) {
            g2.setColor(new Color(0, 255, 0, 180));
            g2.setFont(new Font("Monospaced", Font.BOLD, 45));
            g2.drawString("SISTEMA CALIBRADO", 190, 460);
        }
    }

    private class Distribuidor {
        int x, y, radio = 75;
        double angulo = 0;
        double velocidad;
        Color color;
        boolean calibrado = false;
        Rectangle botonRect;

        Distribuidor(int x, int y, Color c, double vel) {
            this.x = x; this.y = y; this.color = c; this.velocidad = vel;
            // Botón más alto para que sea más fácil de clickear
            this.botonRect = new Rectangle(x - 50, y + 100, 100, 60);
        }

        void girar() {
            angulo += velocidad;
            if (angulo >= 360) angulo -= 360;
        }

        void dibujar(Graphics2D g2, boolean esActivo) {
            // Cable
            g2.setColor(calibrado ? color : new Color(60, 60, 60));
            g2.setStroke(new BasicStroke(12));
            g2.drawLine(x + radio, y, x + radio + 40, y);

            // Fondo
            g2.setColor(new Color(30, 30, 35));
            g2.fillOval(x - radio, y - radio, radio * 2, radio * 2);
            
            // Muesca (Arco gris)
            g2.setStroke(new BasicStroke(20));
            g2.setColor(calibrado ? color : Color.DARK_GRAY);
            // La muesca es lo que debe coincidir con el cable (0 grados)
            g2.drawArc(x-radio+10, y-radio+10, (radio*2)-20, (radio*2)-20, (int)-angulo - 20, 40);

            // Botón visual
            g2.setColor(esActivo ? Color.WHITE : new Color(70, 70, 75));
            g2.fill(botonRect);
            g2.setColor(Color.BLACK);
            g2.draw(botonRect);
            g2.setFont(new Font("Arial", Font.BOLD, 14));
            g2.drawString("CLICK", botonRect.x + 25, botonRect.y + 35);
            
            // LED
            if (calibrado) g2.setColor(Color.GREEN);
            else if (esActivo) g2.setColor(Color.RED);
            else g2.setColor(new Color(20, 20, 20));
            g2.fillOval(x - 15, y - 15, 30, 30);
        }
    }

    public static void main(String[] args) {
        JFrame f = new JFrame("Calibrador de Energía");
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.add(new TareaCalibrar());
        f.pack();
        f.setLocationRelativeTo(null);
        f.setVisible(true);
    }
}