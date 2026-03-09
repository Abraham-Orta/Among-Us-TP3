package com.amongus.project;

import com.amongus.project.red.Cliente;
import com.amongus.project.red.Servidor;
import com.amongus.project.vista.PantallaLobby;
import javax.swing.SwingUtilities;

/**
 * PruebaDirecta
 * =============
 * Arranca un servidor local y abre 3 ventanas de Lobby simultáneas.
 * Jugador1 es el host → tiene el botón "Iniciar Partida" y el selector de mapa.
 * Jugador2 y Jugador3 son invitados → esperan a que el host arranque.
 *
 * Para iniciar la partida: presionar "Iniciar Partida" en la ventana de Jugador1.
 */
public class PruebaDirecta {

    public static void main(String[] args) {

        // -------------------------------------------------------
        // 1. Servidor — corre en su propio hilo de fondo
        // -------------------------------------------------------
        new Thread(() -> {
            System.out.println("[SERVIDOR] Iniciando servidor de prueba...");
            Servidor.main(new String[0]);
        }).start();

        // Esperamos a que el servidor esté escuchando antes de conectar clientes
        try { Thread.sleep(1000); } catch (InterruptedException e) { e.printStackTrace(); }

        // -------------------------------------------------------
        // 2. Lanzar 3 clientes con sus respectivas pantallas de lobby
        // -------------------------------------------------------
        lanzarCliente("Jugador1", true);   // host → ve el botón Iniciar Partida
        lanzarCliente("Jugador2", false);
        lanzarCliente("Jugador3", false);
        lanzarCliente("Jugador4", false);
        lanzarCliente("Jugador5", false);
        lanzarCliente("Jugador6", false);
    }

    // -------------------------------------------------------
    // Crea un cliente, lo conecta a localhost y abre su Lobby
    // -------------------------------------------------------
    private static void lanzarCliente(String nombre, boolean esHost) {

        // Usamos un hilo separado para la pausa de conexión,
        // y luego pasamos a Swing con invokeLater para la UI.
        new Thread(() -> {

            System.out.println("[CLIENTE] Lanzando: " + nombre);

            // Conectar al servidor local
            Cliente cliente = new Cliente("localhost");

            // Pequeña pausa para que el socket se establezca antes de enviar mensajes
            try { Thread.sleep(500); } catch (InterruptedException e) { e.printStackTrace(); }

            // Crear y mostrar el lobby en el hilo de Swing
            SwingUtilities.invokeLater(() -> {
                PantallaLobby lobby = new PantallaLobby(nombre, esHost, cliente);
                lobby.setVisible(true);

                // Presentarse al servidor (el servidor responde con LISTA_JUGADORES:)
                cliente.enviarMensaje("CONECTAR:" + nombre);
            });

        }).start();

        // Pequeña pausa entre cada cliente para evitar colisiones en la lista del servidor
        try { Thread.sleep(300); } catch (InterruptedException e) { e.printStackTrace(); }
    }
}