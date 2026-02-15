package com.amongus.project.controlador;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import com.amongus.project.modelo.EstadoJuego;
import com.amongus.project.modelo.Jugador;

public class ManejadorEntrada extends KeyAdapter {
    
    @Override
    public void keyPressed(KeyEvent e) {
        Jugador jugador = EstadoJuego.getInstancia().getJugadorLocal();
        if (jugador == null) return;
        
        switch (e.getKeyCode()) {
            case KeyEvent.VK_W: jugador.mover(0, -1); break;
            case KeyEvent.VK_S: jugador.mover(0, 1); break;
            case KeyEvent.VK_A: jugador.mover(-1, 0); break;
            case KeyEvent.VK_D: jugador.mover(1, 0); break;
        }
    }
}
