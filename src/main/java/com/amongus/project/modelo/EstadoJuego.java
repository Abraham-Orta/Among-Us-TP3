package com.amongus.project.modelo;

import java.util.ArrayList;
import java.util.List;

public class EstadoJuego {
    private static EstadoJuego instancia;
    private List<Jugador> jugadores;
    private Jugador jugadorLocal;
    
    // Estados del juego
    public enum Fase { MENU, LOBBY, JUGANDO, VOTACION, FINALIZADO }
    private Fase faseActual;
    private Mapa mapa;

    private EstadoJuego() {
        jugadores = new ArrayList<>();
        faseActual = Fase.MENU;
        mapa = new Mapa("mapa1.png");
    }

    public static synchronized EstadoJuego getInstancia() {
        if (instancia == null) {
            instancia = new EstadoJuego();
        }
        return instancia;
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
    public void setFaseActual(Fase f) { this.faseActual = f; }
    
    public Mapa getMapa() { return mapa; }
    public void setMapa(Mapa mapa) { this.mapa = mapa; }
}
