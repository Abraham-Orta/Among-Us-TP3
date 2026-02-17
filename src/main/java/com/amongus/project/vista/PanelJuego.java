package com.amongus.project.vista;

import javax.swing.JPanel;
import java.awt.Graphics;
import java.awt.Color;
import java.awt.Dimension;
import com.amongus.project.modelo.EstadoJuego;
import com.amongus.project.modelo.Jugador;
import com.amongus.project.controlador.ManejadorEntrada;

public class PanelJuego extends JPanel {
    
    public PanelJuego() {
        setPreferredSize(new Dimension(800, 600));
        setBackground(Color.BLACK);
        setFocusable(true);
        addKeyListener(new ManejadorEntrada());
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        // Dibujar mapa
        if (EstadoJuego.getInstancia().getMapa() != null) {
            EstadoJuego.getInstancia().getMapa().render(g);
        }
        
        // Dibujar jugadores
        // Dibujar jugadores
        for (Jugador j : EstadoJuego.getInstancia().getJugadores()) {
            dibujarTripulante(g, j);
            
            // Nombre encima
            g.setColor(Color.WHITE);
            g.drawString(j.getNombre(), j.getX(), j.getY() - 10);
        }
        
        // Mensaje si no hay jugadores
        if (EstadoJuego.getInstancia().getJugadores().isEmpty()) {
            g.setColor(Color.WHITE);
            g.drawString("No hay jugadores. Inicia partida.", 300, 300);
        }
    }

    private void dibujarTripulante(Graphics g, Jugador j) {
        int x = j.getX();
        int y = j.getY();
        // Usamos el tamaño del hitbox como referencia (30x50), pero dibujamos un poco mas grande
        int w = 30;
        int h = 40; // Cuerpo un poco mas bajo para las patas
        
        int dir = j.getDireccion(); // 1 derecha, -1 izquierda, 0 quieto (usar ultimo)
        if (dir == 0) dir = 1; // Por defecto derecha
        
        g.setColor(j.getColor());
        
        // Mochila
        int mochilaW = 10;
        int mochilaH = 25;
        if (dir == 1) { // Derecha -> Mochila a la izquierda
            g.fillRect(x - 5, y + 10, mochilaW, mochilaH);
        } else { // Izquierda -> Mochila a la derecha
            g.fillRect(x + w - 5, y + 10, mochilaW, mochilaH);
        }
        
        // Cuerpo
        g.fillRoundRect(x, y, w, h, 15, 15);
        
        // Patas
        g.fillRect(x, y + h - 5, 10, 15); // Pata izquierda
        g.fillRect(x + w - 10, y + h - 5, 10, 15); // Pata derecha
        
        // Visor (Celeste/GrisÃ¡ceo)
        g.setColor(new Color(150, 200, 220));
        int visorW = 18;
        int visorH = 12;
        if (dir == 1) { // Mirando derecha
            g.fillRoundRect(x + 15, y + 10, visorW, visorH, 5, 5);
        } else { // Mirando izquierda
            g.fillRoundRect(x - 3, y + 10, visorW, visorH, 5, 5);
        }
    }
}
