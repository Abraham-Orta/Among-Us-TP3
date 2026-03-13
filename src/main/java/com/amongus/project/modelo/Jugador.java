package com.amongus.project.modelo;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.List;
import com.amongus.project.controlador.ManejadorEntrada;

/**
 * Clase Jugador
 * =============
 * Representa a los participantes del juego.
 * Cada Jugador tiene una referencia a su EstadoJuego correspondiente.
 */
public class Jugador extends Personaje {

    private String  nombre;
    private Color   color;
    private boolean impostor;
    private boolean vivo = true;
    private boolean fueExpulsado = false;
    private int xMuerte;
    private int yMuerte;
    private int direccionMuerte = 1;
    private String sombrero = "ninguno"; // ID del sombrero actual

    // Red
    private int ultimoXEnviado = -1;
    private int ultimoYEnviado = -1;
    private com.amongus.project.red.Cliente clienteRed;

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
    private int cooldownTarea       = 0;
    
    // Tareas
    private java.util.List<String> tareasPendientes = new java.util.ArrayList<>();
    private java.util.List<String> tareasCompletadas = new java.util.ArrayList<>();
    private int totalTareas = 0;
    
    // Contadores
    private int usosVentilacion = 0;
    private int usosSabotaje = 0;

    public Jugador(String nombre, int x, int y, Color color, boolean impostor) {
        super(x, y, 4); // Velocidad original de 4
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

    public void actualizar(ManejadorEntrada entrada, double delta) {
        // Reducir cooldowns
        if (cooldownAsesinato   > 0) cooldownAsesinato--;
        if (cooldownVentilacion > 0) cooldownVentilacion--;
        if (cooldownReporte     > 0) cooldownReporte--;
        if (cooldownSabotaje    > 0) cooldownSabotaje--;
        if (cooldownTarea       > 0) cooldownTarea--;

        if (vivo) {
            if (impostor) {
                if (entrada.accionMatar && cooldownAsesinato <= 0) {
                    intentarParalizar();
                }
                if (entrada.accionVentilar && cooldownVentilacion <= 0) {
                    intentarUsarAlcantarilla();
                }
                if (entrada.accionSabotaje && cooldownSabotaje <= 0) {
                    ejecutarSabotaje();
                }
            }
            if (entrada.accionUsar && cooldownTarea <= 0) {
                intentarRealizarTarea();
            }
            if (entrada.accionReportar && cooldownReporte <= 0) {
                intentarReportar();
            }
            if (entrada.accionEmergencia && cooldownReporte <= 0) {
                intentarBotonEmergencia();
            }
        }

        // Movimiento con Delta Time
        int dx = 0, dy = 0;
        if (entrada.arriba)    dy -= 1;
        if (entrada.abajo)     dy += 1;
        if (entrada.izquierda) dx -= 1;
        if (entrada.derecha)   dx += 1;

        if (dx != 0 || dy != 0) {
            double mag = Math.sqrt(dx * dx + dy * dy);
            double factorVelocidad = velocidad * 60.0 * delta;
            super.mover((int) Math.round((dx / mag) * factorVelocidad),
                        (int) Math.round((dy / mag) * factorVelocidad));
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
                // comparar al cuadrado evita Math.sqrt() costoso
                if (dx * dx + dy * dy <= 50 * 50) {
                    victima.setVivo(false);
                    this.iniciarAnimacionAtaque();
                    cooldownAsesinato = 600;
                    if (clienteRed != null) {
                        clienteRed.enviarMensaje("MATAR:" + nombre + "," + victima.getNombre() + "," + victima.getX() + "," + victima.getY());
                    }
                    break;
                }
            }
        }
    }

    private void intentarUsarAlcantarilla() {
        Mapa mapa = estadoJuego.getMapa();
        if (mapa == null) return;
        List<Rectangle> vias = mapa.getAlcantarillas();
        for (int i = 0; i < vias.size(); i++) {
            if (this.hitbox.intersects(vias.get(i))) {
                Rectangle destino = vias.get((i + 1) % vias.size());
                this.setX(destino.x + 10);
                this.setY(destino.y + 10);
                usosVentilacion++;
                cooldownVentilacion = 30; 
                if (usosVentilacion >= 4) {
                    cooldownVentilacion = 600; 
                    usosVentilacion = 0;
                }
                break;
            }
        }
    }

    private void ejecutarSabotaje() {
        if (clienteRed != null) {
            boolean actual = estadoJuego.areLucesSaboteadas();
            clienteRed.enviarMensaje(actual ? "SABOTAJE:LUCES:OFF" : "SABOTAJE:LUCES:ON");
        } else {
            estadoJuego.setLucesSaboteadas(!estadoJuego.areLucesSaboteadas());
        }
        usosSabotaje++;
        cooldownSabotaje = 30; 
        if (usosSabotaje >= 5) {
            cooldownSabotaje = 600;
            usosSabotaje = 0;
        }
    }

    public void intentarReportar() {
        List<Jugador> todos = estadoJuego.getJugadores();
        for (Jugador victima : todos) {
            if (victima != this && !victima.isVivo() && !victima.isFueExpulsado()) {
                int bx = victima.getXMuerte();
                int by = victima.getYMuerte();
                if (bx == 0 && by == 0) { bx = victima.getX(); by = victima.getY(); }
                int dx = this.x - bx;
                int dy = this.y - by;
                
                if (dx * dx + dy * dy <= 80 * 80) {
                    presionarBotonEmergencia();
                    break;
                }
            }
        }
    }

    public void presionarBotonEmergencia() {
        if (!vivo) return;
        estadoJuego.setFaseActual(EstadoJuego.Fase.VOTACION);
        for (Jugador j : estadoJuego.getJugadores()) j.resetVoto();
        if (clienteRed != null) clienteRed.enviarMensaje("REPORTAR:");
        cooldownReporte = 300;
    }

    private void intentarBotonEmergencia() {
        Mapa mapa = estadoJuego.getMapa();
        if (mapa == null) return;
        for (Rectangle btn : mapa.getBotones()) {
            int dx = x - btn.x;
            int dy = y - btn.y;
            // Fix #2: sin sqrt
            if (dx * dx + dy * dy <= 100 * 100) {
                presionarBotonEmergencia();
                break;
            }
        }
    }

    private void intentarRealizarTarea() {
        if (estadoJuego == null) return;
        Mapa mapa = estadoJuego.getMapa();
        if (mapa == null) return;

        for (TareaMapa tarea : mapa.getTareasDisponibles()) {
            if (this.hitbox.intersects(tarea.getZona())) {
                // Verificar si el jugador tiene esta tarea pendiente o si es impostor fingiendo
                if (tareasPendientes.contains(tarea.getNombre()) || impostor) {
                    estadoJuego.setTareaAabrir(tarea.getNombre());
                    cooldownTarea = 60; // 1 segundo de cooldown
                    break;
                }
            }
        }
    }

    // ==========================================
    //  RED
    // ==========================================

    private static final long INTERVALO_ENVIO_NS = 33_333_333L;
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
    public void setImpostor(boolean impostor) { this.impostor = impostor; }
    public boolean isVivo()              { return vivo; }
    public boolean isFueExpulsado()      { return fueExpulsado; }
    public int getXMuerte()              { return xMuerte; }
    public int getYMuerte()              { return yMuerte; }
    public void setXMuerte(int x)        { this.xMuerte = x; }
    public void setYMuerte(int y)        { this.yMuerte = y; }
    public int getDireccionMuerte()      { return direccionMuerte; }
    public String getSombrero()          { return sombrero; }
    public void setSombrero(String s)    { this.sombrero = s; }
    
    public void setVivo(boolean vivo) { setVivo(vivo, false); }
    
    public void setVivo(boolean vivo, boolean expulsado) { 
        if (this.vivo && !vivo) {
            this.tiempoInicioMuerte = System.currentTimeMillis();
            this.fueExpulsado = expulsado;
            this.xMuerte = this.x;
            this.yMuerte = this.y;
            this.direccionMuerte = this.getDireccion();
        }
        this.vivo = vivo; 
    }

    public long getTiempoInicioMuerte() { return tiempoInicioMuerte; }

    public void iniciarAnimacionAtaque() {
        this.atacando = true;
        this.tiempoInicioAsesinato = System.currentTimeMillis();
    }

    public boolean isAtacando() {
        if (atacando && System.currentTimeMillis() - tiempoInicioAsesinato > 2880) atacando = false;
        return atacando;
    }

    public long getTiempoInicioAsesinato() { return tiempoInicioAsesinato; }
    public int getCooldownAsesinato()      { return cooldownAsesinato; }
    public int getCooldownSabotaje()       { return cooldownSabotaje; }
    public int getCooldownReporte()        { return cooldownReporte; }
    public int getCooldownVentilacion()    { return cooldownVentilacion; }

    public java.util.List<String> getTareasPendientes() { return tareasPendientes; }
    public java.util.List<String> getTareasCompletadas() { return tareasCompletadas; }
    public int getTotalTareas() { return totalTareas; }
    public void setTotalTareas(int t) { this.totalTareas = t; }

    public boolean hayVictimaCerca() {
        if (!isVivo() || !isImpostor()) return false;
        for (Jugador victima : estadoJuego.getJugadores()) {
            if (victima != this && !victima.isImpostor() && victima.isVivo()) {
                int dx = x - victima.getX();
                int dy = y - victima.getY();
                
                if (dx * dx + dy * dy <= 50 * 50) return true;
            }
        }
        return false;
    }

    public boolean hayCuerpoCerca() {
        if (!isVivo()) return false;
        for (Jugador victima : estadoJuego.getJugadores()) {
            if (victima != this && !victima.isVivo() && !victima.isFueExpulsado()) {
                int bx = victima.getXMuerte() == 0 ? victima.getX() : victima.getXMuerte();
                int by = victima.getYMuerte() == 0 ? victima.getY() : victima.getYMuerte();
                int dx = x - bx;
                int dy = y - by;
                
                if (dx * dx + dy * dy <= 80 * 80) return true;
            }
        }
        return false;
    }
}