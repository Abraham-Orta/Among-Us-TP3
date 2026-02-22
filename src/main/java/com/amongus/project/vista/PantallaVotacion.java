package com.amongus.project.vista;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.List;

import com.amongus.project.controlador.ManejadorEntrada;
import com.amongus.project.modelo.EstadoJuego;
import com.amongus.project.modelo.Jugador;

public class PantallaVotacion {

    private Rectangle botonSkip;
    private Rectangle[] botonesJugadores;
    
    public PantallaVotacion() {
        botonSkip = new Rectangle(50, 500, 150, 50);
    }

    public void actualizar() {
        Jugador jugadorLocal = EstadoJuego.getInstancia().getJugadorLocal();
        if (jugadorLocal == null || !jugadorLocal.isVivo() || jugadorLocal.yaVoto()) {
            return; // Si está muerto o ya votó, no procesamos clics de voto
        }

        // Verificar clic en Skip
        if (ManejadorEntrada.clickIzquierdo) {
            if (botonSkip.contains(ManejadorEntrada.mouseX, ManejadorEntrada.mouseY)) {
                jugadorLocal.votarSkip();
                ManejadorEntrada.clickIzquierdo = false; // Consumir clic
                return;
            }
            
            // Verificar clic en jugadores
            List<Jugador> jugadores = EstadoJuego.getInstancia().getJugadores();
            int x = 50;
            int y = 50;
            
            for (Jugador j : jugadores) {
                // No puedes votarte a ti mismo (regla opcional, pero común) ni a muertos
                if (!j.isVivo()) continue; 
                
                Rectangle areaJugador = new Rectangle(x, y, 200, 60);
                if (areaJugador.contains(ManejadorEntrada.mouseX, ManejadorEntrada.mouseY)) {
                    jugadorLocal.votarJugador(j);
                    ManejadorEntrada.clickIzquierdo = false; // Consumir clic
                    return;
                }
                
                y += 70;
                if (y > 400) { // Nueva columna si se llena
                    y = 50;
                    x += 250;
                }
            }
        }
    }

    public void render(Graphics g) {
        // Fondo oscuro semitransparente
        g.setColor(new Color(0, 0, 0, 200));
        g.fillRect(0, 0, 800, 600);
        
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 24));
        g.drawString("¿Quién es el Impostor?", 250, 30);
        
        List<Jugador> jugadores = EstadoJuego.getInstancia().getJugadores();
        int x = 50;
        int y = 50;

        for (Jugador j : jugadores) {
            // Dibujar tarjeta de jugador
            if (j.isVivo()) {
                g.setColor(Color.GRAY);
            } else {
                g.setColor(Color.RED); // Muertos en rojo/oscuro
            }
            g.fillRect(x, y, 200, 60);
            
            // Icono del jugador (cuadrado simple por ahora)
            g.setColor(j.getColor());
            g.fillRect(x + 10, y + 10, 40, 40);
            
            // Nombre
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.PLAIN, 16));
            g.drawString(j.getNombre(), x + 60, y + 35);
            
            // Estado de voto (si ya votó)
            if (j.yaVoto()) {
                g.setColor(Color.GREEN);
                g.fillOval(x + 180, y + 20, 10, 10);
                g.drawString("Votó", x + 160, y + 15);
            } else if (!j.isVivo()) {
                g.setColor(Color.RED);
                g.drawString("Muerto", x + 140, y + 35);
            }
            
            // Resultado (solo visible si todos votaron o tiempo acabó - Lógica pendiente)
            // Por ahora solo mostramos si votaron o no
            
            y += 70;
            if (y > 400) {
                y = 50;
                x += 250;
            }
        }
        
        // Botón Skip
        g.setColor(Color.LIGHT_GRAY);
        g.fillRect(botonSkip.x, botonSkip.y, botonSkip.width, botonSkip.height);
        g.setColor(Color.BLACK);
        g.drawString("SKIP VOTE", botonSkip.x + 40, botonSkip.y + 30);
        
        // Mensaje para el jugador local
        Jugador local = EstadoJuego.getInstancia().getJugadorLocal();
        if (local != null) {
            g.setColor(Color.YELLOW);
            if (!local.isVivo()) {
                g.drawString("Estás muerto. No puedes votar.", 300, 550);
            } else if (local.yaVoto()) {
                g.drawString("Has votado. Esperando a los demás...", 280, 550);
            } else {
                g.drawString("Haz clic en un jugador o Skip para votar.", 250, 550);
            }
        }
    }
}