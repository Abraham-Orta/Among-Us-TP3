package com.amongus.project.red;

import com.amongus.project.data.GestorDatos;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CopyOnWriteArrayList;

public class Servidor {

    private static final int PUERTO        = 1234;
    private static final int MAX_JUGADORES = 10;

    // Mínimo de jugadores para iniciar.
    // En producción poner 5 (requisito del PDF).
    // En pruebas locales con PruebaDirecta usamos 3.
    private static final int MIN_JUGADORES = 3;

    public static CopyOnWriteArrayList<AtencionJugador> listaJugadores = new CopyOnWriteArrayList<>();

    public static void main(String[] args) {
        System.out.println("Iniciando el servidor de Among Us...");

        try {
            ServerSocket servidorSocket = new ServerSocket(PUERTO);
            System.out.println("Servidor listo en el puerto: " + PUERTO);

            while (true) {
                if (listaJugadores.size() >= MAX_JUGADORES) {
                    System.out.println("Sala llena, rechazando conexión...");
                    servidorSocket.accept().close();
                    continue;
                }

                System.out.println("Esperando conexión... (" + listaJugadores.size() + "/" + MAX_JUGADORES + ")");
                Socket socketDelCliente = servidorSocket.accept();
                System.out.println("¡Conexión recibida! IP: " + socketDelCliente.getInetAddress());

                AtencionJugador nuevoJugador = new AtencionJugador(socketDelCliente);
                listaJugadores.add(nuevoJugador);
                nuevoJugador.start();
            }

        } catch (IOException error) {
            System.out.println("Error en el servidor: " + error.getMessage());
            error.printStackTrace();
        }
    }

    public static void enviarATodos(String mensaje) {
        for (AtencionJugador jugador : listaJugadores) {
            try { jugador.enviarMensaje(mensaje); }
            catch (Exception e) { System.out.println("No se pudo enviar a un jugador."); }
        }
    }

    public static void iniciarPartida(String mapaElegido) {
        System.out.println("Intentando iniciar partida con mapa: " + mapaElegido);

        // Verificar mínimo de jugadores
        if (listaJugadores.size() < MIN_JUGADORES) {
            System.out.println("Faltan jugadores (Mínimo " + MIN_JUGADORES + ")");
            enviarATodos("CHAT:SISTEMA: Faltan jugadores para iniciar (Mínimo " + MIN_JUGADORES + ").");
            return;
        }

        // --- Asignar roles al azar ---
        // Siempre hay al menos 1 impostor
        int impostor1 = (int) (Math.random() * listaJugadores.size());
        int impostor2 = -1;

        // Segundo impostor solo si hay más de 3 jugadores
        if (listaJugadores.size() > 3) {
            impostor2 = (int) (Math.random() * listaJugadores.size());
            while (impostor2 == impostor1) {
                impostor2 = (int) (Math.random() * listaJugadores.size());
            }
        }

        System.out.println("Impostores: índice " + impostor1
                + (impostor2 != -1 ? " y " + impostor2 : " (solo 1 con <= 3 jugadores)"));

        // Marcar rol en cada AtencionJugador
        for (int i = 0; i < listaJugadores.size(); i++) {
            listaJugadores.get(i).esImpostor = (i == impostor1 || i == impostor2);
        }

        // Susurrar el rol individualmente → nadie sabe el rol de los demás
        for (AtencionJugador j : listaJugadores) {
            j.enviarMensaje("ROL:" + (j.esImpostor ? "IMPOSTOR" : "TRIPULANTE"));
        }

        // Avisar a todos que la partida arranca con el mapa elegido
        enviarATodos("JUEGO_INICIADO:" + mapaElegido);
    }

    public static void finalizarPartida(String equipoGanador) {
        System.out.println("La partida terminó. Ganaron: " + equipoGanador);
        enviarATodos("FIN:" + equipoGanador);
        GestorDatos.guardarPartida(equipoGanador, listaJugadores.size());
        System.out.println("Datos guardados en el XML.");
    }

    public static void enviarListaJugadores() {
        StringBuilder lista = new StringBuilder("LISTA_JUGADORES:");
        for (int i = 0; i < listaJugadores.size(); i++) {
            String nombre = listaJugadores.get(i).getNombreJugador();
            if (nombre != null) {
                lista.append(nombre);
                if (i < listaJugadores.size() - 1) lista.append(",");
            }
        }
        System.out.println("Enviando lista: " + lista);
        enviarATodos(lista.toString());
    }
}