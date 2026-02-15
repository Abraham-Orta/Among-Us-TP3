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

public class TareaNumeros extends JPanel {

    private final int ANCHO = 650, ALTO = 500;
    private ArrayList<Integer> numeros;
    private int siguienteEsperado = 1;
    private boolean completada = false;
    private JPanel panelBotones;
    private Color colorFondo = new Color(20, 23, 28);

    public TareaNumeros() {
        setPreferredSize(new Dimension(ANCHO, ALTO));
        setBackground(colorFondo);
        setLayout(new BorderLayout());
        
        // Título superior estilo terminal
        JLabel titulo = new JLabel("DESBLOQUEAR CONECTORES", SwingConstants.CENTER);
        titulo.setForeground(Color.CYAN);
        titulo.setFont(new Font("Monospaced", Font.BOLD, 28));
        titulo.setBorder(BorderFactory.createEmptyBorder(20, 0, 10, 0));
        add(titulo, BorderLayout.NORTH);

        panelBotones = new JPanel(new GridLayout(2, 5, 15, 15));
        panelBotones.setBackground(colorFondo);
        panelBotones.setBorder(BorderFactory.createEmptyBorder(20, 40, 40, 40));
        
        iniciar();
        add(panelBotones, BorderLayout.CENTER);
    }

    private void iniciar() {
        panelBotones.removeAll();
        numeros = new ArrayList<>();
        for (int i = 1; i <= 10; i++) numeros.add(i);
        Collections.shuffle(numeros);

        for (int num : numeros) {
            panelBotones.add(new BotonNumerico(num));
        }
        panelBotones.revalidate();
    }

    // Clase interna para botones con estilo Among Us
    private class BotonNumerico extends JButton {
        private int valor;
        private boolean activado = false;

        public BotonNumerico(int valor) {
            this.valor = valor;
            setText(String.valueOf(valor));
            setFont(new Font("Monospaced", Font.BOLD, 35));
            setForeground(Color.CYAN);
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);

            addActionListener(e -> {
                if (completada || activado) return;

                if (valor == siguienteEsperado) {
                    activado = true;
                    siguienteEsperado++;
                    if (siguienteEsperado > 10) completada = true;
                    repaint();
                } else {
                    efectoError();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Fondo del botón
            if (activado) {
                g2.setColor(new Color(0, 255, 150, 200)); // Verde neón brillante
            } else {
                g2.setColor(new Color(40, 45, 52));
            }
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);

            // Borde brillante
            g2.setStroke(new BasicStroke(3));
            g2.setColor(activado ? Color.WHITE : Color.CYAN.darker());
            g2.drawRoundRect(2, 2, getWidth() - 5, getHeight() - 5, 15, 15);

            // Brillo interno (Glass effect)
            if (!activado) {
                g2.setColor(new Color(255, 255, 255, 30));
                g2.fillRoundRect(5, 5, getWidth() - 10, getHeight() / 2, 10, 10);
            }

            super.paintComponent(g2);
            g2.dispose();
        }
    }

    private void efectoError() {
        // Simple feedback de error: parpadeo rojo
        new Thread(() -> {
            Color original = panelBotones.getBackground();
            panelBotones.setBackground(new Color(150, 0, 0));
            try { Thread.sleep(200); } catch (InterruptedException e) {}
            panelBotones.setBackground(original);
            reiniciarTarea();
        }).start();
    }

    private void reiniciarTarea() {
        siguienteEsperado = 1;
        for (Component c : panelBotones.getComponents()) {
            if (c instanceof BotonNumerico) {
                ((BotonNumerico) c).activado = false;
            }
        }
        repaint();
    }

    @Override
    protected void paintChildren(Graphics g) {
        super.paintChildren(g);
        if (completada) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setColor(new Color(0, 0, 0, 180));
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.setColor(Color.GREEN);
            g2.setFont(new Font("Monospaced", Font.BOLD, 50));
            g2.drawString("TASK COMPLETED", 110, ALTO / 2);
        }
    }

    public static void main(String[] args) {
        JFrame f = new JFrame("Unlock Manifolds");
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.add(new TareaNumeros());
        f.pack();
        f.setLocationRelativeTo(null);
        f.setVisible(true);
    }
}