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
import java.util.ArrayList;
import java.util.Collections;

public class TareaCables extends JPanel implements MouseListener, MouseMotionListener {

    private final int ANCHO = 800, ALTO = 600;
    private final Color[] COLORES = {
        new Color(255, 17, 0),   // Rojo Neón
        new Color(0, 153, 255),  // Azul Eléctrico
        new Color(255, 230, 0),  // Amarillo
        new Color(200, 0, 255)   // Magenta
    };
    
    private ArrayList<Integer> ordenIzquierda, ordenDerecha;
    private boolean[] conectados;
    private int arrastrando = -1;
    private Point mousePos = new Point(0, 0);
    private boolean completada = false; 
    private TareaCompletadaListener listener;

    public void setTareaCompletadaListener(TareaCompletadaListener listener) {
        this.listener = listener;
    }

    public boolean isCompletada() { return completada; }

    public TareaCables() {
        setPreferredSize(new Dimension(ANCHO, ALTO));
        setBackground(new Color(30, 33, 36)); 
        addMouseListener(this);
        addMouseMotionListener(this);
        iniciar();
    }

    private void iniciar() {
        ordenIzquierda = new ArrayList<>();
        ordenDerecha = new ArrayList<>();
        for (int i = 0; i < COLORES.length; i++) {
            ordenIzquierda.add(i);
            ordenDerecha.add(i);
        }
        Collections.shuffle(ordenDerecha);
        conectados = new boolean[4];
        completada = false;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 1. DIBUJAR FONDO
        g2.setColor(new Color(45, 48, 54));
        g2.fillRect(50, 50, ANCHO - 100, ALTO - 100);
        g2.setColor(Color.BLACK);
        g2.drawRect(50, 50, ANCHO - 100, ALTO - 100);

        // 2. DIBUJAR CABLES (Primero para que queden "detrás" de las cajas)
        for (int i = 0; i < 4; i++) {
            if (conectados[i]) {
                int yIni = 130 + (i * 110);
                int indDer = ordenDerecha.indexOf(ordenIzquierda.get(i));
                int yFin = 130 + (indDer * 110);
                dibujarCableEstilizado(g2, 90, yIni, ANCHO - 90, yFin, COLORES[ordenIzquierda.get(i)]);
            }
        }

        if (arrastrando != -1) {
            int yIni = 130 + (arrastrando * 110);
            dibujarCableEstilizado(g2, 90, yIni, mousePos.x, mousePos.y, COLORES[ordenIzquierda.get(arrastrando)]);
        }

        // 3. DIBUJAR CONECTORES (Después de los cables)
        for (int i = 0; i < 4; i++) {
            // Izquierda
            disenarConector(g2, 60, 100 + (i * 110), COLORES[ordenIzquierda.get(i)], conectados[i], true);
            
            // Derecha
            int colorIndiceDer = ordenDerecha.get(i);
            boolean estaConectadoDer = verificarSiColorConectado(colorIndiceDer);
            disenarConector(g2, ANCHO - 100, 100 + (i * 110), COLORES[colorIndiceDer], estaConectadoDer, false);
        }

        // 4. OVERLAY DE COMPLETADO
        if (completada) {
            g2.setColor(new Color(0, 0, 0, 180));
            g2.fillRect(0, 0, ANCHO, ALTO);
            g2.setColor(Color.GREEN);
            g2.setFont(new Font("Monospaced", Font.BOLD, 60));
            g2.drawString("TAREA COMPLETADA", 150, ALTO / 2);
        }
    }

    private void dibujarCableEstilizado(Graphics2D g2, int x1, int y1, int x2, int y2, Color c) {
        g2.setColor(Color.BLACK);
        g2.setStroke(new BasicStroke(14));
        g2.drawLine(x1, y1, x2, y2);
        
        g2.setColor(c);
        g2.setStroke(new BasicStroke(8));
        g2.drawLine(x1, y1, x2, y2);
        
        g2.setColor(new Color(255, 255, 255, 100));
        g2.setStroke(new BasicStroke(2));
        g2.drawLine(x1, y1, x2, y2);
    }

    private void disenarConector(Graphics2D g2, int x, int y, Color c, boolean encendido, boolean izquierda) {
        // Cuerpo metálico
        g2.setColor(new Color(60, 60, 60));
        g2.fillRoundRect(x, y, 40, 60, 10, 10);
        
        // Indicador de color: Si está apagado, mostramos el color muy oscuro
        if (encendido) {
            g2.setColor(c); // Color brillante
        } else {
            g2.setColor(c.darker().darker()); // Color tenue para guía
        }
        
        if (izquierda) g2.fillRect(x + 30, y + 20, 10, 20); 
        else g2.fillRect(x, y + 20, 10, 20); 
        
        g2.setColor(Color.BLACK);
        g2.drawRoundRect(x, y, 40, 60, 10, 10);
    }

    private boolean verificarSiColorConectado(int colorIndice) {
        for (int i = 0; i < 4; i++) {
            if (ordenIzquierda.get(i) == colorIndice && conectados[i]) return true;
        }
        return false;
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (completada) return;
        int margenH = 20; // Hitbox más ancha
        for (int i = 0; i < 4; i++) {
            int yBox = 100 + (i * 110);
            if (e.getX() >= 60 - margenH && e.getX() <= 100 + margenH && 
                e.getY() >= yBox && e.getY() <= yBox + 60) {
                if (!conectados[i]) arrastrando = i;
            }
        }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        mousePos = e.getPoint();
        repaint();
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (arrastrando != -1) {
            int colorMio = ordenIzquierda.get(arrastrando);
            int margenTolerancia = 40; // Zona de "iman" para facilitar la conexión
            
            for (int i = 0; i < 4; i++) {
                int yBox = 100 + (i * 110);
                // Detección más flexible
                if (e.getX() >= ANCHO - 140 && e.getX() <= ANCHO - 40 && 
                    e.getY() >= yBox - 20 && e.getY() <= yBox + 80) {
                    
                    if (ordenDerecha.get(i) == colorMio) {
                        conectados[arrastrando] = true;
                        if (todoConectado()) {
                            completada = true;
                            if (listener != null) listener.onTareaCompletada();
                        }
                    }
                }
            }
            arrastrando = -1;
            repaint();
        }
    }

    private boolean todoConectado() {
        for (boolean b : conectados) if (!b) return false;
        return true;
    }

    public void mouseMoved(MouseEvent e) {}
    public void mouseClicked(MouseEvent e) {}
    public void mouseEntered(MouseEvent e) {}
    public void mouseExited(MouseEvent e) {}

    public static void main(String[] args) {
        JFrame f = new JFrame("Among Us Tasks - Java");
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.add(new TareaCables());
        f.pack();
        f.setLocationRelativeTo(null);
        f.setVisible(true);
    }
}
