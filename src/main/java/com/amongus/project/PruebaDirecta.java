package com.amongus.project;

import com.amongus.project.red.Cliente;
import com.amongus.project.red.Servidor;
import com.amongus.project.vista.PantallaLobby;
import java.io.IOException;
import javax.swing.SwingUtilities;

/**
 * PruebaDirecta
 * =============
 * Arranca un servidor local y abre 6 ventanas de Lobby simultáneas de forma automática.
 * Para iniciar la partida: presionar "Iniciar Partida" en la ventana de Jugador1.
 */
public class PruebaDirecta {

    public static void main(String[] args) {

        // 1. Servidor — corre en su propio hilo de fondo
        new Thread(() -> {
            System.out.println("[SERVIDOR] Iniciando servidor de prueba...");
            Servidor.main(new String[0]);
        }).start();

        // Esperamos a que el servidor esté escuchando antes de conectar clientes
        try { Thread.sleep(1500); } catch (InterruptedException e) { e.printStackTrace(); }

        // 2. Lanzar 6 clientes con sus respectivas pantallas de lobby
        lanzarCliente("Jugador1", true);   // host
        lanzarCliente("Jugador2", false);
        lanzarCliente("Jugador3", false);
        lanzarCliente("Jugador4", false);
        lanzarCliente("Jugador5", false);
        lanzarCliente("Jugador6", false);

        System.out.println("[INFO] Se han lanzado los 6 clientes automáticamente.");
    }

    private static void lanzarCliente(String nombre, boolean esHost) {

        new Thread(() -> {
            System.out.println("[CLIENTE] Lanzando: " + nombre);

            // Conectar al servidor local
            Cliente cliente = new Cliente("localhost");

            // Pausa para estabilizar conexión
            try { Thread.sleep(600); } catch (InterruptedException e) { e.printStackTrace(); }

            // Crear y mostrar el lobby en el hilo de Swing
            SwingUtilities.invokeLater(() -> {
                try {
                    PantallaLobby lobby = new PantallaLobby(nombre, esHost, cliente);
                    lobby.setVisible(true);

                    // Presentarse al servidor
                    cliente.enviarMensaje("CONECTAR:" + nombre);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            });

        }).start();

        // Pausa entre cada cliente para evitar colisiones
        try { Thread.sleep(400); } catch (InterruptedException e) { e.printStackTrace(); }
    }
}
