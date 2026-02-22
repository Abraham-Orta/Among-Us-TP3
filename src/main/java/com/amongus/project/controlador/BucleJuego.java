package com.amongus.project.controlador;

import com.amongus.project.modelo.EstadoJuego;
import com.amongus.project.modelo.Jugador;
import com.amongus.project.vista.PanelJuego;

public class BucleJuego implements Runnable {
    
    private PanelJuego panelJuego;
    private EstadoJuego estado;
    private boolean corriendo = false;
    private Thread hiloJuego;
    
    // FPS objetivo
    private final int FPS = 60;
    // Tiempo objetivo por frame en nanosegundos
    private final long TIEMPO_OBJETIVO = 1000000000 / FPS;

    public BucleJuego(PanelJuego panelJuego) {
        this.panelJuego = panelJuego;
        this.estado = EstadoJuego.getInstancia();
    }
    
    public void iniciar() {
        if (corriendo) return;
        corriendo = true;
        hiloJuego = new Thread(this);
        hiloJuego.start();
    }
    
    public void detener() {
        corriendo = false;
        try {
            if (hiloJuego != null) hiloJuego.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        long tiempoInicial;
        long tiempoTranscurrido;
        long tiempoEspera;

        while (corriendo) {
            tiempoInicial = System.nanoTime();

            // 1. ACTUALIZAR (Lógica)
            actualizar();

            // 2. RENDERIZAR (Dibujo)
            renderizar();

            // 3. CONTROL DE TIEMPO (Sleep para mantener 60 FPS estables)
            tiempoTranscurrido = System.nanoTime() - tiempoInicial;
            tiempoEspera = TIEMPO_OBJETIVO - tiempoTranscurrido;

            if (tiempoEspera > 0) {
                try {
                    // Convertir nanosegundos a milisegundos
                    Thread.sleep(tiempoEspera / 1000000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    private void actualizar() {
        EstadoJuego.Fase fase = estado.getFaseActual();
        
        if (fase == EstadoJuego.Fase.VOTACION) {
            panelJuego.getPantallaVotacion().actualizar();
        } else if (fase == EstadoJuego.Fase.JUGANDO) {
             // Obtener el jugador local y actualizar su movimiento basado en teclas
            Jugador jugadorLocal = estado.getJugadorLocal();
            if (jugadorLocal != null) {
                jugadorLocal.actualizar();
            }
        }
        
        // Aquí podríamos actualizar otros elementos (tareas, timers, otros jugadores interpolados)
    }
    
    private void renderizar() {
        if (panelJuego != null) {
            panelJuego.repaint();
            // Toolkit.getDefaultToolkit().sync(); // A veces ayuda en Linux con el tearing
        }
    }
}