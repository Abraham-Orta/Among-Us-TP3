package com.amongus.project.modelo;

import java.util.ArrayList;
import java.util.List;

/**
 * EstadoJuego
 * ===========
 * Contiene el estado de juego para UN cliente específico.
 * 
 * Ya NO es Singleton: cada ventana/cliente crea su propia instancia,
 * así múltiples clientes en el mismo proceso (PruebaDirecta) no se
 * interfieren entre sí.
 */
public class EstadoJuego {
    private List<Jugador> jugadores;
    private Jugador jugadorLocal;
    
    // Estados del juego
    public enum Fase { MENU, LOBBY, JUGANDO, VOTACION, FINALIZADO }
    private Fase faseActual;
    private Mapa mapa;
    
    // SABOTAJES
    private boolean lucesSaboteadas = false;

    public EstadoJuego() {
        jugadores = new ArrayList<>();
        faseActual = Fase.MENU;
        mapa = new Mapa("mapa1.png");
    }
    
    // Getters y Setters para sabotajes
    public boolean areLucesSaboteadas() { return lucesSaboteadas; }
    public void setLucesSaboteadas(boolean estado) { 
        if (this.lucesSaboteadas == estado) return; // solo actuar si el estado cambia realmente
        this.lucesSaboteadas = estado; 
        if (estado) {
            com.amongus.project.vista.ReproductorMusica.manejarAlarma("Alarm_sabotaje.wav", true);
        } else {
            com.amongus.project.vista.ReproductorMusica.manejarAlarma("Alarm_sabotaje.wav", false);
        }
    }

    public void agregarJugador(Jugador j) {
        jugadores.add(j);
    }

    public List<Jugador> getJugadores() {
        return jugadores;
    }

    public Jugador getJugadorLocal() {
        return jugadorLocal;
    }

    public void setJugadorLocal(Jugador jugadorLocal) {
        this.jugadorLocal = jugadorLocal;
        this.agregarJugador(jugadorLocal);
    }

    public Fase getFaseActual() { return faseActual; }
    public void setFaseActual(Fase f) { 
        if (this.faseActual != f && f == Fase.VOTACION) {
            com.amongus.project.vista.ReproductorMusica.reproducirEfecto("reporte.wav");
        }

        this.faseActual = f; 
    }    
    public Mapa getMapa() { return mapa; }
    public void setMapa(Mapa mapa) { this.mapa = mapa; }
}
