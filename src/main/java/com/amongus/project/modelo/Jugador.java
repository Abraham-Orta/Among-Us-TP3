package com.amongus.project.modelo;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.List;
import com.amongus.project.controlador.ManejadorEntrada;

/**
 * Clase Jugador
 * =============
 * Representa a los participantes del juego.
 * actualizar(ManejadorEntrada) recibe el manejador de la ventana específica
 * del jugador, no un objeto estático compartido.
 *
 * Cada Jugador tiene una referencia a su EstadoJuego correspondiente,
 * para que múltiples clientes en el mismo proceso no compartan estado.
 */
public class Jugador extends Personaje {

    private String  nombre;
    private Color   color;
    private boolean impostor;
    private boolean vivo = true;

    // Red
    private int ultimoXEnviado = -1;
    private int ultimoYEnviado = -1;
    private com.amongus.project.red.Cliente clienteRed;

    // estadoJuego se hereda de Personaje

    // Votación
    private boolean haVotado    = false;
    private boolean votoSkip    = false;
    private Jugador votoJugador = null;

    // Animaciones
    private long tiempoInicioAsesinato = 0;
    private long tiempoInicioMuerte = 0;
    private boolean atacando = false;

    // Cooldowns (en frames a 60 FPS)
    private int cooldownAsesinato   = 0;
    private int cooldownVentilacion = 0;
    private int cooldownReporte     = 0;
    private int cooldownSabotaje    = 0;

    public Jugador(String nombre, int x, int y, Color color, boolean impostor) {
        super(x, y, 4);
        this.nombre   = nombre;
        this.color    = color;
        this.impostor = impostor;
    }

    public void setClienteRed(com.amongus.project.red.Cliente cliente) {
        this.clienteRed = cliente;
    }

    public void setEstadoJuego(EstadoJuego estadoJuego) {
        this.estadoJuego = estadoJuego;
    }

    public EstadoJuego getEstadoJuego() {
        return estadoJuego;
    }

    // ==========================================
    //  VOTACIÓN
    // ==========================================

    public void resetVoto() {
        haVotado    = false;
        votoSkip    = false;
        votoJugador = null;
    }

    public void votarSkip() {
        if (haVotado) return;
        haVotado = true; votoSkip = true; votoJugador = null;
        if (clienteRed != null) clienteRed.enviarMensaje("VOTO:" + nombre + ",SKIP");
    }

    public void votarJugador(Jugador objetivo) {
        if (haVotado) return;
        haVotado = true; votoSkip = false; votoJugador = objetivo;
        if (clienteRed != null) clienteRed.enviarMensaje("VOTO:" + nombre + "," + objetivo.getNombre());
    }

    public void recibirVotoRemotoSkip() {
        haVotado = true; votoSkip = true; votoJugador = null;
    }

    public void recibirVotoRemotoJugador(Jugador objetivo) {
        haVotado = true; votoSkip = false; votoJugador = objetivo;
    }

    public boolean yaVoto()         { return haVotado; }
    public boolean isVotoSkip()     { return votoSkip; }
    public Jugador getVotoJugador() { return votoJugador; }

    // ==========================================
    //  BUCLE PRINCIPAL
    // ==========================================

    /**
     * Se ejecuta ~60 veces por segundo desde BucleJuego.
     *
     * @param entrada El ManejadorEntrada de la ventana de ESTE jugador.
     *                Cada ventana tiene su propia instancia → múltiples
     *                ventanas abiertas no se interfieren entre sí.
     */
    public void actualizar(ManejadorEntrada entrada) {

        // Reducir cooldowns cada frame
        if (cooldownAsesinato   > 0) cooldownAsesinato--;
        if (cooldownVentilacion > 0) cooldownVentilacion--;
        if (cooldownReporte     > 0) cooldownReporte--;
        if (cooldownSabotaje    > 0) cooldownSabotaje--;

        // Jugador inhabilitado: no hace nada
        if (!vivo) return;

        // --- 1. ACCIONES DEL IMPOSTOR ---
        if (impostor) {
            if (entrada.accionMatar && cooldownAsesinato <= 0) {
                intentarParalizar();
            }
            if (entrada.accionVentilar && cooldownVentilacion <= 0) {
                intentarUsarAlcantarilla();
            }
            if (entrada.accionSabotaje && cooldownSabotaje <= 0) {
                if (clienteRed != null) {
                    boolean actual = estadoJuego.areLucesSaboteadas();
                    clienteRed.enviarMensaje(actual ? "SABOTAJE:LUCES:OFF" : "SABOTAJE:LUCES:ON");
                } else {
                    boolean actual = estadoJuego.areLucesSaboteadas();
                    estadoJuego.setLucesSaboteadas(!actual);
                    System.out.println("Sabotaje local: luces " + (!actual ? "APAGADAS" : "RESTAURADAS"));
                }
                cooldownSabotaje = 60;
            }
        }

        // --- 2. REPORTE (todos pueden reportar) ---
        if (entrada.accionReportar && cooldownReporte <= 0) {
            intentarReportar();
        }

        // --- 3. MOVIMIENTO ---
        int dx = 0, dy = 0;

        if (entrada.arriba)    dy -= 1;
        if (entrada.abajo)     dy += 1;
        if (entrada.izquierda) dx -= 1;
        if (entrada.derecha)   dx += 1;

        if (dx != 0 || dy != 0) {
            double mag = Math.sqrt(dx * dx + dy * dy);
            super.mover((int) Math.round((dx / mag) * velocidad),
                        (int) Math.round((dy / mag) * velocidad));
            enviarPosicionSiCambio();
        }
    }

    // ==========================================
    //  MECÁNICAS
    // ==========================================

    private void intentarParalizar() {
        List<Jugador> todos = estadoJuego.getJugadores();
        for (Jugador victima : todos) {
            if (victima != this && !victima.isImpostor() && victima.isVivo()) {
                int dx = this.x - victima.getX();
                int dy = this.y - victima.getY();
                if (Math.sqrt(dx * dx + dy * dy) <= 50) {
                    victima.setVivo(false);
                    this.iniciarAnimacionAtaque();
                    System.out.println(nombre + " paralizó a " + victima.getNombre());
                    cooldownAsesinato = 600;
                    if (clienteRed != null) clienteRed.enviarMensaje("MATAR:" + nombre + "," + victima.getNombre());
                    break;
                }
            }
        }
    }

    private void intentarUsarAlcantarilla() {
        Mapa mapa = estadoJuego.getMapa();
        if (mapa == null) return;
        List<java.awt.Rectangle> vias = mapa.getAlcantarillas();
        for (int i = 0; i < vias.size(); i++) {
            if (this.hitbox.intersects(vias.get(i))) {
                Rectangle destino = vias.get((i + 1) % vias.size());
                this.setX(destino.x + 10);
                this.setY(destino.y + 10);
                System.out.println(nombre + " usó una alcantarilla.");
                cooldownVentilacion = 60;
                break;
            }
        }
    }

    private void intentarReportar() {
        List<Jugador> todos = estadoJuego.getJugadores();
        for (Jugador victima : todos) {
            if (victima != this && !victima.isVivo()) {
                int dx = this.x - victima.getX();
                int dy = this.y - victima.getY();
                if (Math.sqrt(dx * dx + dy * dy) <= 80) {
                    System.out.println(nombre + " reportó el cuerpo de " + victima.getNombre());
                    estadoJuego.setFaseActual(EstadoJuego.Fase.VOTACION);
                    for (Jugador j : todos) j.resetVoto();
                    if (estadoJuego.getJugadorLocal() != null)
                        estadoJuego.getJugadorLocal().resetVoto();
                    if (clienteRed != null) clienteRed.enviarMensaje("REPORTAR:");
                    cooldownReporte = 300;
                    break;
                }
            }
        }
    }

    // ==========================================
    //  RED
    // ==========================================

    // Throttling: enviar posición máximo cada 33ms (~30 FPS).
    // Esto balancea una red ligera (para no sobrecargar el servidor) con un movimiento fluido.
    private static final long INTERVALO_ENVIO_NS = 33_333_333L; // 33ms
    private long ultimoEnvioNano = 0;

    private void enviarPosicionSiCambio() {
        if (this.x != ultimoXEnviado || this.y != ultimoYEnviado) {
            long ahora = System.nanoTime();
            if (ahora - ultimoEnvioNano >= INTERVALO_ENVIO_NS) {
                if (clienteRed != null)
                    clienteRed.enviarMensaje("MOVER:" + nombre + "," + this.x + "," + this.y);
                ultimoXEnviado = this.x;
                ultimoYEnviado = this.y;
                ultimoEnvioNano = ahora;
            }
        }
    }

    // ==========================================
    //  GETTERS / SETTERS
    // ==========================================

    public Color   getColor()            { return color; }
    public String  getNombre()           { return nombre; }
    public boolean isImpostor()          { return impostor; }
    public boolean isVivo()              { return vivo; }
    
    public void setVivo(boolean vivo) { 
        if (this.vivo && !vivo) {
            this.tiempoInicioMuerte = System.currentTimeMillis();
        }
        this.vivo = vivo; 
    }

    public long getTiempoInicioMuerte() { return tiempoInicioMuerte; }

    public void iniciarAnimacionAtaque() {
        this.atacando = true;
        this.tiempoInicioAsesinato = System.currentTimeMillis();
    }

    public boolean isAtacando() {
        if (atacando && System.currentTimeMillis() - tiempoInicioAsesinato > 2880) { // 48 frames a ~60ms c/u
            atacando = false;
        }
        return atacando;
    }

    public long getTiempoInicioAsesinato() { return tiempoInicioAsesinato; }
}