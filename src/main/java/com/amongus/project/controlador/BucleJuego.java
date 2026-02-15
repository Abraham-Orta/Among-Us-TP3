package com.amongus.project.controlador;

import com.amongus.project.modelo.EstadoJuego;
import com.amongus.project.vista.PanelJuego;

public class BucleJuego implements Runnable {
    private PanelJuego panelJuego;
    private EstadoJuego estado;
    private boolean corriendo = false;
    private final int FPS = 60;
    
    public BucleJuego(PanelJuego panelJuego) {
        this.panelJuego = panelJuego;
        this.estado = EstadoJuego.getInstancia();
    }
    
    public void iniciar() {
        corriendo = true;
        new Thread(this).start();
    }
    
    public void detener() {
        corriendo = false;
    }

    @Override
    public void run() {
        double tiempoPorFrame = 1000000000 / FPS;
        long ultimaActualizacion = System.nanoTime();
        
        while (corriendo) {
            long ahora = System.nanoTime();
            if (ahora - ultimaActualizacion >= tiempoPorFrame) {
                actualizar();
                renderizar();
                ultimaActualizacion = ahora;
            }
            try {
                Thread.sleep(1); // Pequeña pausa para no quemar CPU
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
    
    private void actualizar() {
        // Aquí actualizamos lógica (movimiento, etc)
        // Por ahora es simple, el movimiento se hace por eventos en ManejadorEntrada
        // Pero idealmente debería hacerse aquí verificando teclas presionadas
    }
    
    private void renderizar() {
        if (panelJuego != null) {
            panelJuego.repaint();
        }
    }
}
