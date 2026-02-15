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


public class TareaDownload extends JPanel {

    private final int ANCHO = 600, ALTO = 400;
    private float progreso = 0.0f; // 0.0 a 100.0
    private boolean descargando = false;
    private boolean completada = false;
    private Timer timer;
    
    // Colores Among Us
    private Color colorFondo = new Color(20, 23, 28);
    private Color colorBarra = new Color(0, 255, 100); // Verde brillante
    private Color colorAzul = new Color(0, 153, 255);

    public TareaDownload() {
        setPreferredSize(new Dimension(ANCHO, ALTO));
        setBackground(colorFondo);
        
        // Botón de inicio
        JButton btnDownload = new JButton("DOWNLOAD");
        btnDownload.setFont(new Font("Monospaced", Font.BOLD, 20));
        btnDownload.setBackground(colorAzul);
        btnDownload.setForeground(Color.WHITE);
        btnDownload.setFocusPainted(false);
        
        // Lógica del Timer (se ejecuta cada 50ms)
        timer = new Timer(50, e -> {
            if (progreso < 100f) {
                progreso += 0.5f; // Velocidad de descarga
                repaint();
            } else {
                progreso = 100f;
                descargando = false;
                completada = true;
                timer.stop();
                repaint();
            }
        });

        btnDownload.addActionListener(e -> {
            if (!descargando && !completada) {
                descargando = true;
                btnDownload.setVisible(false);
                timer.start();
            }
        });

        setLayout(new BorderLayout());
        JPanel panelBoton = new JPanel();
        panelBoton.setOpaque(false);
        panelBoton.add(btnDownload);
        add(panelBoton, BorderLayout.SOUTH);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // --- DIBUJAR ICONOS (Carpetas) ---
        dibujarCarpeta(g2, 100, 100, "Tablet");
        dibujarCarpeta(g2, 450, 100, "Server");
        
        // Dibujar línea de conexión
        g2.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{9}, 0));
        g2.setColor(Color.DARK_GRAY);
        g2.drawLine(160, 130, 440, 130);

        // --- BARRA DE PROGRESO ---
        int bAncho = 400, bAlto = 40;
        int xBarra = (ANCHO - bAncho) / 2;
        int yBarra = 250;

        // Fondo de la barra
        g2.setColor(new Color(45, 48, 54));
        g2.fillRoundRect(xBarra, yBarra, bAncho, bAlto, 10, 10);
        
        // Progreso
        if (progreso > 0) {
            g2.setColor(colorBarra);
            g2.fillRoundRect(xBarra, yBarra, (int) (bAncho * (progreso / 100f)), bAlto, 10, 10);
        }

        // Borde
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(2));
        g2.drawRoundRect(xBarra, yBarra, bAncho, bAlto, 10, 10);

        // --- TEXTOS ---
        g2.setFont(new Font("Monospaced", Font.BOLD, 18));
        if (completada) {
            g2.setColor(Color.GREEN);
            g2.drawString("TAREA COMPLETADA", xBarra + 120, yBarra + 70);
        } else if (descargando) {
            g2.setColor(Color.WHITE);
            g2.drawString("Downloading... " + (int)progreso + "%", xBarra + 100, yBarra - 10);
        }
    }

    private void dibujarCarpeta(Graphics2D g2, int x, int y, String label) {
        g2.setColor(colorAzul);
        g2.fillRoundRect(x, y, 60, 45, 5, 5); // Cuerpo carpeta
        g2.fillRoundRect(x, y - 10, 25, 15, 5, 5); // Pestaña
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Monospaced", Font.PLAIN, 12));
        g2.drawString(label, x + 5, y + 65);
    }

    public static void main(String[] args) {
        JFrame f = new JFrame("Download Data");
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.add(new TareaDownload());
        f.pack();
        f.setLocationRelativeTo(null);
        f.setVisible(true);
    }
}