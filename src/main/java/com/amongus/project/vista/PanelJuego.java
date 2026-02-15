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
        
        // Dibujar jugadores
        for (Jugador j : EstadoJuego.getInstancia().getJugadores()) {
            g.setColor(j.getColor());
            g.fillRect(j.getX(), j.getY(), 30, 50); // Dibujo simple por ahora
            g.setColor(Color.WHITE);
            g.drawString(j.getNombre(), j.getX(), j.getY() - 5);
        }
        
        // Mensaje si no hay jugadores
        if (EstadoJuego.getInstancia().getJugadores().isEmpty()) {
            g.setColor(Color.WHITE);
            g.drawString("No hay jugadores. Inicia partida.", 300, 300);
        }
    }
}
