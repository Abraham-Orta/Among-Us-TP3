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
import com.amongus.project.red.Cliente;

public class PanelJuego extends JPanel implements Cliente.MensajeListener {
    
    private PantallaVotacion pantallaVotacion;
    private ManejadorEntrada manejadorEntrada;
    private Cliente cliente;
    private Jugador jugadorLocal; // Referencia propia — no depende del singleton

    public PanelJuego(Cliente cliente, Jugador jugadorLocal) {
        this.cliente = cliente;
        this.jugadorLocal = jugadorLocal;
        setPreferredSize(new Dimension(800, 600));
        setBackground(Color.BLACK);
        setFocusable(true);
        
        // Manejo de Teclado
        manejadorEntrada = new ManejadorEntrada(this.cliente);
        addKeyListener(manejadorEntrada);
        
        // Inicializar pantallas
        this.pantallaVotacion = new PantallaVotacion();
        
        // Este panel ahora escucha los mensajes del servidor (posiciones de otros jugadores)
        if (cliente != null) {
            cliente.setMensajeListener(this);
        }
    }
    
    /** Llamado por el Cliente cada vez que llega un mensaje del servidor */
    @Override
    public void onMensajeRecibido(String mensaje) {
        // POSICION:nombre:x:y  — actualizar posición de otro jugador
        if (mensaje.startsWith("POSICION:")) {
            String[] partes = mensaje.split(":");
            if (partes.length < 4) return;
            String nombre = partes[1];
            try {
                int x = Integer.parseInt(partes[2]);
                int y = Integer.parseInt(partes[3]);
                for (Jugador j : EstadoJuego.getInstancia().getJugadores()) {
                    if (j.getNombre().equals(nombre) && j != jugadorLocal) {
                        j.setX(x);
                        j.setY(y);
                        break;
                    }
                }
            } catch (NumberFormatException e) {
                System.out.println("PanelJuego: POSICION mal formada: " + mensaje);
            }
        }
        // Aquí se pueden manejar otros mensajes en juego (FIN, VOTACION, etc.)
    }

    public PantallaVotacion getPantallaVotacion() {
        return pantallaVotacion;
    }

    public ManejadorEntrada getManejadorEntrada() {
        return manejadorEntrada;
    }

    public Jugador getJugadorLocal() {
        return jugadorLocal;
    }
    
    public Cliente getCliente() {
        return cliente;
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
        
        // --- LÓGICA DE CÁMARA ---
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
            if (camX > mapa.getAncho() - getWidth())  camX = mapa.getAncho() - getWidth();
            if (camY > mapa.getAlto()  - getHeight()) camY = mapa.getAlto()  - getHeight();
        }
        
        // Aplicar traslación de cámara (el mundo se mueve, no el jugador)
        Graphics2D g2d = (Graphics2D) g;
        g2d.translate(-camX, -camY);
        
        // Dibujar mapa
        if (mapa != null) {
            mapa.render(g);
        }
        
        // Dibujar jugadores — priorizar jugadorLocal propio para evitar
        // conflictos con el singleton compartido en modo PruebaDirecta
        java.util.List<Jugador> jugadoresARenderizar = EstadoJuego.getInstancia().getJugadores();
        
        if (jugadorLocal != null && !jugadoresARenderizar.contains(jugadorLocal)) {
            // Si nuestro jugador no está en la lista del singleton, lo dibujamos igual
            dibujarTripulante(g, jugadorLocal);
            g.setColor(Color.WHITE);
            g.drawString(jugadorLocal.getNombre(), jugadorLocal.getX(), jugadorLocal.getY() - 10);
        }
        
        for (Jugador j : jugadoresARenderizar) {
            dibujarTripulante(g, j);
            g.setColor(Color.WHITE);
            g.drawString(j.getNombre(), j.getX(), j.getY() - 10);
        }

        // Revertir traslación para elementos fijos de la UI
        g2d.translate(camX, camY);

        if (jugadorLocal == null && jugadoresARenderizar.isEmpty()) {
            g.setColor(Color.WHITE);
            g.drawString("No hay jugadores. Inicia partida.", 300, 300);
        }
    }

    private void dibujarTripulante(Graphics g, Jugador j) {
        int x = j.getX();
        int y = j.getY();
        int w = 30;
        int h = 40; // Cuerpo un poco más bajo para las patas
        
        int dir = j.getDireccion(); // 1 derecha, -1 izquierda, 0 quieto
        if (dir == 0) dir = 1; // Por defecto derecha
        
        g.setColor(j.getColor());
        
        // Mochila
        int mochilaW = 10;
        int mochilaH = 25;
        if (dir == 1) { // Derecha → Mochila a la izquierda
            g.fillRect(x - 5, y + 10, mochilaW, mochilaH);
        } else {        // Izquierda → Mochila a la derecha
            g.fillRect(x + w - 5, y + 10, mochilaW, mochilaH);
        }
        
        // Cuerpo
        g.fillRoundRect(x, y, w, h, 15, 15);
        
        // Patas
        g.fillRect(x, y + h - 5, 10, 15);          // Pata izquierda
        g.fillRect(x + w - 10, y + h - 5, 10, 15); // Pata derecha
        
        // Visor (celeste/grisáceo)
        g.setColor(new Color(150, 200, 220));
        int visorW = 18;
        int visorH = 12;
        if (dir == 1) { // Mirando derecha
            g.fillRoundRect(x + 15, y + 10, visorW, visorH, 5, 5);
        } else {        // Mirando izquierda
            g.fillRoundRect(x - 3,  y + 10, visorW, visorH, 5, 5);
        }
    }
}