package com.amongus.project.vista;

import javax.swing.JPanel;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.Dimension;
import com.amongus.project.modelo.EstadoJuego;
import com.amongus.project.modelo.Jugador;
import com.amongus.project.modelo.Mapa;
import com.amongus.project.controlador.ManejadorEntrada;

public class PanelJuego extends JPanel {
    
    private PantallaVotacion pantallaVotacion;

    public PanelJuego() {
        setPreferredSize(new Dimension(800, 600));
        setBackground(Color.BLACK);
        setFocusable(true);
        
        // Manejo de Teclado
        addKeyListener(new ManejadorEntrada());
        
        // Manejo de Ratón
        ManejadorEntrada.MouseHandler mouseHandler = new ManejadorEntrada.MouseHandler();
        addMouseListener(mouseHandler);
        addMouseMotionListener(mouseHandler);
        
        // Inicializar pantallas
        this.pantallaVotacion = new PantallaVotacion();
    }
    
    public PantallaVotacion getPantallaVotacion() {
        return pantallaVotacion;
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        EstadoJuego estado = EstadoJuego.getInstancia();
        EstadoJuego.Fase fase = estado.getFaseActual();
        
        if (fase == EstadoJuego.Fase.VOTACION) {
            pantallaVotacion.render(g);
            return; 
        }
        
        // --- LOGICA DE CAMARA ---
        int camX = 0;
        int camY = 0;
        
        Jugador local = estado.getJugadorLocal();
        Mapa mapa = estado.getMapa();
        
        if (local != null && mapa != null) {
            // Centrar al jugador: posJugador - (tamañoPantalla / 2)
            camX = local.getX() - (getWidth() / 2);
            camY = local.getY() - (getHeight() / 2);
            
            // Limitar cámara a los bordes del mapa
            if (camX < 0) camX = 0;
            if (camY < 0) camY = 0;
            if (camX > mapa.getAncho() - getWidth()) camX = mapa.getAncho() - getWidth();
            if (camY > mapa.getAlto() - getHeight()) camY = mapa.getAlto() - getHeight();
        }
        
        // Aplicar traslación de cámara (invertida para que el mundo se mueva)
        Graphics2D g2d = (Graphics2D) g;
        g2d.translate(-camX, -camY);
        
        // Dibujar mapa
        if (mapa != null) {
            mapa.render(g);
        }
        
        // Dibujar jugadores
        for (Jugador j : estado.getJugadores()) {
            dibujarTripulante(g, j);
            
            // Nombre encima
            g.setColor(Color.WHITE);
            g.drawString(j.getNombre(), j.getX(), j.getY() - 10);
        }
        
        // Dibujar nosotros mismos si no estamos en la lista general (a veces pasa en local)
        if (local != null && !estado.getJugadores().contains(local)) {
             dibujarTripulante(g, local);
             g.setColor(Color.WHITE);
             g.drawString(local.getNombre(), local.getX(), local.getY() - 10);
        }

        // Revertir traslación para elementos fijos de la UI (si hubiera)
        g2d.translate(camX, camY);
        
        // Mensaje si no hay jugadores
        if (estado.getJugadores().isEmpty() && local == null) {
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
