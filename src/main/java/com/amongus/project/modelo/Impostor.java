package com.amongus.project.modelo;

import java.awt.Rectangle;
import java.util.List;

/**
 * Impostor
 * ========
 * Encapsula toda la lógica y el estado exclusivo del impostor.
 * Un {@link Jugador} recibe una instancia de esta clase al ser designado
 * impostor, y la descarta (null) si vuelve a ser tripulante.
 *
 * <p>El rol se asigna dinámicamente desde el servidor después de crear
 * los objetos, por lo que {@code Jugador} usa composición en lugar de
 * herencia: si {@code impostor != null} es impostor, si es {@code null}
 * es tripulante.</p>
 */
public class Impostor {

    // ---- Cooldowns (en frames a 60 FPS) ----
    private int cooldownAsesinato   = 0;
    private int cooldownVentilacion = 0;
    private int cooldownSabotaje    = 0;

    // ---- Contadores de uso (anti-spam progresivo) ----
    private int usosVentilacion = 0;
    private int usosSabotaje    = 0;

    // ---- Animación de ataque ----
    private boolean atacando              = false;
    private long    tiempoInicioAsesinato = 0;

    // =========================================================
    //  ACTUALIZACIÓN POR FRAME
    // =========================================================

    /** Reduce todos los cooldowns activos. Llamar una vez por frame. */
    public void actualizar() {
        if (cooldownAsesinato   > 0) cooldownAsesinato--;
        if (cooldownVentilacion > 0) cooldownVentilacion--;
        if (cooldownSabotaje    > 0) cooldownSabotaje--;
    }

    // =========================================================
    //  MECÁNICAS
    // =========================================================

    /**
     * Intenta paralizar al tripulante más cercano dentro del rango de 50px.
     *
     * @return {@code true} si el asesinato se realizó con éxito.
     */
    public boolean intentarParalizar(Jugador yo, EstadoJuego estado,
                                     com.amongus.project.red.Cliente red) {
        if (cooldownAsesinato > 0) return false;

        for (Jugador victima : estado.getJugadores()) {
            if (victima == yo || victima.isImpostor() || !victima.isVivo()) continue;

            int dx = yo.getX() - victima.getX();
            int dy = yo.getY() - victima.getY();
            if (dx * dx + dy * dy <= 50 * 50) {
                victima.setVivo(false);
                iniciarAnimacionAtaque();
                cooldownAsesinato = 600;
                if (red != null) {
                    red.enviarMensaje("MATAR:" + yo.getNombre() + ","
                            + victima.getNombre() + "," + victima.getX() + "," + victima.getY());
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Intenta usar la alcantarilla más cercana para teletransportarse.
     *
     * @return {@code true} si el impostor se teletransportó.
     */
    public boolean intentarUsarAlcantarilla(Jugador yo, Mapa mapa) {
        if (cooldownVentilacion > 0 || mapa == null) return false;

        List<Rectangle> vias = mapa.getAlcantarillas();
        for (int i = 0; i < vias.size(); i++) {
            if (yo.getHitbox().intersects(vias.get(i))) {
                Rectangle destino = vias.get((i + 1) % vias.size());
                yo.setX(destino.x + 10);
                yo.setY(destino.y + 10);
                usosVentilacion++;
                cooldownVentilacion = (usosVentilacion >= 4) ? 600 : 30;
                if (usosVentilacion >= 4) usosVentilacion = 0;
                return true;
            }
        }
        return false;
    }

    /**
     * Ejecuta un sabotaje de luces si no hay uno activo y el cooldown lo permite.
     *
     * @return {@code true} si se envió la orden de sabotaje.
     */
    public boolean ejecutarSabotaje(Jugador yo, EstadoJuego estado,
                                    com.amongus.project.red.Cliente red) {
        if (cooldownSabotaje > 0) return false;

        if (red != null) {
            if (estado.areLucesSaboteadas()) return false;
            red.enviarMensaje("SABOTAJE:LUCES:ON");
        } else {
            estado.setLucesSaboteadas(!estado.areLucesSaboteadas());
        }

        usosSabotaje++;
        cooldownSabotaje = (usosSabotaje >= 5) ? 600 : 30;
        if (usosSabotaje >= 5) usosSabotaje = 0;
        return true;
    }

    // =========================================================
    //  ANIMACIÓN DE ATAQUE
    // =========================================================

    /** Inicia la animación de ataque. */
    public void iniciarAnimacionAtaque() {
        this.atacando              = true;
        this.tiempoInicioAsesinato = System.currentTimeMillis();
    }

    /**
     * Devuelve si el impostor está en animación de ataque.
     * Auto-cancela tras 2880 ms.
     */
    public boolean isAtacando() {
        if (atacando && System.currentTimeMillis() - tiempoInicioAsesinato > 2880) {
            atacando = false;
        }
        return atacando;
    }

    /** Indica si hay al menos una víctima potencial a 50px de radio. */
    public boolean hayVictimaCerca(Jugador yo, EstadoJuego estado) {
        for (Jugador victima : estado.getJugadores()) {
            if (victima == yo || victima.isImpostor() || !victima.isVivo()) continue;
            int dx = yo.getX() - victima.getX();
            int dy = yo.getY() - victima.getY();
            if (dx * dx + dy * dy <= 50 * 50) return true;
        }
        return false;
    }

    // =========================================================
    //  GETTERS / SETTERS
    // =========================================================

    public int  getCooldownAsesinato()    { return cooldownAsesinato; }
    public int  getCooldownVentilacion()  { return cooldownVentilacion; }
    public int  getCooldownSabotaje()     { return cooldownSabotaje; }
    public void setCooldownSabotaje(int cd) { this.cooldownSabotaje = cd; }
    public long getTiempoInicioAsesinato() { return tiempoInicioAsesinato; }
}