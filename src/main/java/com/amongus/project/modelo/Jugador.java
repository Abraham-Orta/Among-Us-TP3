package com.amongus.project.modelo;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.List;
import com.amongus.project.controlador.ManejadorEntrada;

/**
 * Clase Jugador
 * =============
 * Representa a cualquier participante del juego, ya sea tripulante o impostor.
 *
 * <p>El rol se determina por composición: si {@code impostor != null} el jugador
 * es impostor; si es {@code null} es tripulante. Esto permite asignar el rol
 * dinámicamente desde el servidor sin necesidad de reemplazar el objeto.</p>
 */
public class Jugador extends Personaje {

    private String  nombre;
    private Color   color;
    private boolean vivo          = true;
    private boolean fueExpulsado  = false;
    private int     xMuerte;
    private int     yMuerte;
    private int     direccionMuerte = 1;
    private String  sombrero        = "ninguno";
    private boolean dispararAnimacionBoton = false;

    // Composición: null = tripulante, no-null = impostor
    private Impostor impostor = null;

    // Red
    private int     ultimoXEnviado = -1;
    private int     ultimoYEnviado = -1;
    private com.amongus.project.red.Cliente clienteRed;

    // Votación
    private boolean haVotado    = false;
    private boolean votoSkip    = false;
    private Jugador votoJugador = null;

    // Animación de muerte
    private long tiempoInicioMuerte = 0;

    // Cooldowns compartidos
    private int cooldownReporte = 0;
    private int cooldownTarea   = 0;

    // Tareas
    private java.util.List<String> tareasPendientes  = new java.util.ArrayList<>();
    private java.util.List<String> tareasCompletadas = new java.util.ArrayList<>();
    private int totalTareas = 0;

    public Jugador(String nombre, int x, int y, Color color, boolean esImpostor) {
        super(x, y, 4);
        this.nombre   = nombre;
        this.color    = color;
        this.impostor = esImpostor ? new Impostor() : null;
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
        if (cooldownReporte > 0) cooldownReporte--;
        if (cooldownTarea   > 0) cooldownTarea--;

        if (vivo) {
            if (impostor != null) {
                impostor.actualizar();
                if (entrada.accionMatar)    impostor.intentarParalizar(this, estadoJuego, clienteRed);
                if (entrada.accionVentilar) impostor.intentarUsarAlcantarilla(this, estadoJuego != null ? estadoJuego.getMapa() : null);
                if (entrada.accionSabotaje) impostor.ejecutarSabotaje(this, estadoJuego, clienteRed);
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
    //  MECÁNICAS COMPARTIDAS
    // ==========================================

    public void intentarReportar() {
        for (Jugador victima : estadoJuego.getJugadores()) {
            if (victima == this || victima.isVivo() || victima.isFueExpulsado()) continue;
            int bx = victima.getXMuerte() == 0 ? victima.getX() : victima.getXMuerte();
            int by = victima.getYMuerte() == 0 ? victima.getY() : victima.getYMuerte();
            int dx = this.x - bx;
            int dy = this.y - by;

            if (dx * dx + dy * dy <= 80 * 80) {
                if (clienteRed != null) {
                    clienteRed.enviarMensaje("REPORTAR_CUERPO:" + this.nombre + ":" + victima.getNombre());
                }
                cooldownReporte = 300;
                break;
            }
        }
    }

    public void iniciarVotacion() {
        if (!vivo) return;
        if (clienteRed != null) clienteRed.enviarMensaje("REPORTAR_EMERGENCIA:" + this.nombre);
        cooldownReporte = 300;
    }

    public void presionarBotonEmergencia() {
        if (!vivo) return;
        this.dispararAnimacionBoton = true;
    }

    public boolean isDispararAnimacionBoton()          { return dispararAnimacionBoton; }
    public void    setDispararAnimacionBoton(boolean b) { this.dispararAnimacionBoton = b; }

    private void intentarBotonEmergencia() {
        Mapa mapa = estadoJuego.getMapa();
        if (mapa == null) return;
        for (Rectangle btn : mapa.getBotones()) {
            int dx = x - btn.x;
            int dy = y - btn.y;
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
                if (tareasPendientes.contains(tarea.getNombre()) || impostor != null ||
                    (tarea.getNombre().equals("calibrar") && estadoJuego.areLucesSaboteadas())) {

                    estadoJuego.setTareaAabrir(tarea.getNombre());
                    cooldownTarea = 60;
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

    public Color  getColor()  { return color; }
    public String getNombre() { return nombre; }

    /** @return {@code true} si este jugador tiene el rol de impostor. */
    public boolean isImpostor() { return impostor != null; }

    /**
     * Asigna o revoca el rol de impostor dinámicamente.
     * Crea o destruye la instancia de {@link Impostor} según corresponda.
     */
    public void setImpostor(boolean esImpostor) {
        this.impostor = esImpostor ? new Impostor() : null;
    }

    public boolean isVivo()          { return vivo; }
    public boolean isFueExpulsado()  { return fueExpulsado; }
    public int     getXMuerte()      { return xMuerte; }
    public int     getYMuerte()      { return yMuerte; }
    public void    setXMuerte(int x) { this.xMuerte = x; }
    public void    setYMuerte(int y) { this.yMuerte = y; }
    public int     getDireccionMuerte()  { return direccionMuerte; }
    public String  getSombrero()         { return sombrero; }
    public void    setSombrero(String s) { this.sombrero = s; }

    public void setVivo(boolean vivo) { setVivo(vivo, false); }

    public void setVivo(boolean vivo, boolean expulsado) {
        if (this.vivo && !vivo) {
            this.tiempoInicioMuerte = System.currentTimeMillis();
            this.fueExpulsado       = expulsado;
            this.xMuerte            = this.x;
            this.yMuerte            = this.y;
            this.direccionMuerte    = this.getDireccion();
        }
        this.vivo = vivo;
    }

    public long getTiempoInicioMuerte() { return tiempoInicioMuerte; }

    // -- Delegados al Impostor (seguros para llamar sobre cualquier jugador) --

    public void iniciarAnimacionAtaque() {
        if (impostor != null) impostor.iniciarAnimacionAtaque();
    }

    public boolean isAtacando() {
        return impostor != null && impostor.isAtacando();
    }

    public long getTiempoInicioAsesinato() {
        return impostor != null ? impostor.getTiempoInicioAsesinato() : 0;
    }

    public int getCooldownAsesinato() {
        return impostor != null ? impostor.getCooldownAsesinato() : 0;
    }

    public int getCooldownVentilacion() {
        return impostor != null ? impostor.getCooldownVentilacion() : 0;
    }

    public int getCooldownSabotaje() {
        return impostor != null ? impostor.getCooldownSabotaje() : 0;
    }

    public void setCooldownSabotaje(int cd) {
        if (impostor != null) impostor.setCooldownSabotaje(cd);
    }

    public int getCooldownReporte() { return cooldownReporte; }
    public int getCooldownTarea()   { return cooldownTarea; }

    public boolean hayVictimaCerca() {
        return impostor != null && estadoJuego != null
                && impostor.hayVictimaCerca(this, estadoJuego);
    }

    public boolean hayCuerpoCerca() {
        if (!vivo) return false;
        for (Jugador victima : estadoJuego.getJugadores()) {
            if (victima == this || victima.isVivo() || victima.isFueExpulsado()) continue;
            int bx = victima.getXMuerte() == 0 ? victima.getX() : victima.getXMuerte();
            int by = victima.getYMuerte() == 0 ? victima.getY() : victima.getYMuerte();
            int dx = x - bx;
            int dy = y - by;
            if (dx * dx + dy * dy <= 80 * 80) return true;
        }
        return false;
    }

    public java.util.List<String> getTareasPendientes()  { return tareasPendientes; }
    public java.util.List<String> getTareasCompletadas() { return tareasCompletadas; }
    public int  getTotalTareas()      { return totalTareas; }
    public void setTotalTareas(int t) { this.totalTareas = t; }
}