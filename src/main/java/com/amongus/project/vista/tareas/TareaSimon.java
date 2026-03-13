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
import javax.sound.sampled.*; // Para el manejo de audio

public class TareaSimon extends JPanel {

    private final int ANCHO = 600, ALTO = 450;
    private ArrayList<Integer> secuencia = new ArrayList<>();
    private int pasoJugador = 0;
    private boolean mostrandoSecuencia = false;
    private boolean completada = false;
    
    private JButton[] botones = new JButton[9];
    private JLabel etiquetaEstado;
    private TareaCompletadaListener listener;
    private final Color COLOR_REPOSO = new Color(40, 45, 52);
    private final Color COLOR_ACTIVO = new Color(0, 255, 255);

    public void setTareaCompletadaListener(TareaCompletadaListener listener) {
        this.listener = listener;
    }

    public TareaSimon() {
        setPreferredSize(new Dimension(ANCHO, ALTO));
        setBackground(new Color(20, 23, 28));
        setLayout(new BorderLayout(10, 10));

        etiquetaEstado = new JLabel("INICIANDO REACCIÓN...", SwingConstants.CENTER);
        etiquetaEstado.setForeground(Color.CYAN);
        etiquetaEstado.setFont(new Font("Monospaced", Font.BOLD, 24));
        etiquetaEstado.setBorder(BorderFactory.createEmptyBorder(15, 0, 15, 0));
        add(etiquetaEstado, BorderLayout.NORTH);

        JPanel panelGrid = new JPanel(new GridLayout(3, 3, 8, 8));
        panelGrid.setBackground(new Color(20, 23, 28));
        panelGrid.setBorder(BorderFactory.createEmptyBorder(0, 80, 20, 80));

        for (int i = 0; i < 9; i++) {
            final int id = i;
            botones[i] = new JButton();
            botones[i].setBackground(COLOR_REPOSO);
            botones[i].setFocusPainted(false);
            botones[i].setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 2));
            botones[i].addActionListener(e -> procesarClickJugador(id));
            panelGrid.add(botones[i]);
        }
        add(panelGrid, BorderLayout.CENTER);
        
        iniciarNuevoNivel();
    }

    // --- MOTOR DE SONIDO SINTETIZADO ---
    private void emitirSonido(int hz, int msecs) {
        try {
            byte[] buf = new byte[msecs * 8];
            for (int i = 0; i < buf.length; i++) {
                double angulo = i / (8000.0 / hz) * 2.0 * Math.PI;
                buf[i] = (byte) (Math.sin(angulo) * 100);
            }
            AudioFormat af = new AudioFormat(8000f, 8, 1, true, false);
            SourceDataLine sdl = AudioSystem.getSourceDataLine(af);
            sdl.open(af);
            sdl.start();
            sdl.write(buf, 0, buf.length);
            sdl.drain();
            sdl.close();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void iniciarNuevoNivel() {
        if (secuencia.size() >= 5) {
            finalizarTarea();
            return;
        }
        secuencia.add((int) (Math.random() * 9));
        reproducirSecuencia();
    }

    private void reproducirSecuencia() {
        mostrandoSecuencia = true;
        pasoJugador = 0;
        etiquetaEstado.setText("OBSERVA...");
        etiquetaEstado.setForeground(Color.YELLOW);

        new Thread(() -> {
            try {
                Thread.sleep(800);
                for (int id : secuencia) {
                    iluminarBoton(id, 400);
                    Thread.sleep(150);
                }
                mostrandoSecuencia = false;
                etiquetaEstado.setText("TU TURNO");
                etiquetaEstado.setForeground(Color.GREEN);
            } catch (InterruptedException e) {}
        }).start();
    }

    private void iluminarBoton(int id, int tiempo) {
        botones[id].setBackground(COLOR_ACTIVO);
        // El tono sube según el botón (frecuencia base 440Hz + id*50)
        emitirSonido(440 + (id * 50), tiempo);
        botones[id].setBackground(COLOR_REPOSO);
    }

    private void procesarClickJugador(int id) {
        if (mostrandoSecuencia || completada || pasoJugador >= secuencia.size()) return;

        if (id == secuencia.get(pasoJugador)) {
            new Thread(() -> iluminarBoton(id, 150)).start();
            pasoJugador++;
            
            if (pasoJugador == secuencia.size()) {
                mostrandoSecuencia = true; // Bloqueo inmediato de clicks
                new Thread(() -> {
                    try { Thread.sleep(800); } catch (Exception e) {}
                    iniciarNuevoNivel();
                }).start();
            }
        } else {
            errorDeSecuencia();
        }
    }

    private void errorDeSecuencia() {
        etiquetaEstado.setText("¡ERROR!");
        etiquetaEstado.setForeground(Color.RED);
        secuencia.clear();
        
        new Thread(() -> {
            // Sonido de error grave y largo
            emitirSonido(150, 500); 
            for (int i = 0; i < 2; i++) {
                for (JButton b : botones) b.setBackground(new Color(150, 0, 0));
                try { Thread.sleep(200); } catch (InterruptedException e) {}
                for (JButton b : botones) b.setBackground(COLOR_REPOSO);
                try { Thread.sleep(200); } catch (InterruptedException e) {}
            }
            iniciarNuevoNivel();
        }).start();
    }

    private void finalizarTarea() {
        completada = true;
        etiquetaEstado.setText("TAREA COMPLETADA");
        etiquetaEstado.setForeground(Color.GREEN);
        
        if (listener != null) listener.onTareaCompletada();
        
        // Sonido de victoria (Escala ascendente rápida)
        new Thread(() -> {
            emitirSonido(523, 100); emitirSonido(659, 100); emitirSonido(783, 100); emitirSonido(1046, 300);
        }).start();
        
        for (JButton b : botones) {
            b.setEnabled(false);
            b.setBackground(new Color(0, 100, 0));
        }
    }

    public static void main(String[] args) {
        JFrame f = new JFrame("Reactor: Simon Dice");
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.add(new TareaSimon());
        f.pack();
        f.setLocationRelativeTo(null);
        f.setVisible(true);
    }
}
