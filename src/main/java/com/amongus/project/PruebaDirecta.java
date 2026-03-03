package com.amongus.project;

import com.amongus.project.red.Cliente;
import com.amongus.project.red.Servidor;
import com.amongus.project.vista.PantallaLobby;
import javax.swing.SwingUtilities;

public class PruebaDirecta {

    public static void main(String[] args) {
        // 1. Iniciar el servidor en un hilo separado
        new Thread(() -> {
            System.out.println("Iniciando servidor de prueba...");
            Servidor.main(new String[0]);
        }).start();

        // Pequeña pausa para asegurar que el servidor esté escuchando
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // 2. Lanzar 5 clientes
        lanzarCliente("Jugador1", true); // El primer jugador será el host
        lanzarCliente("Jugador2", false);
        lanzarCliente("Jugador3", false);
        lanzarCliente("Jugador4", false);
        lanzarCliente("Jugador5", false);
    }

    private static void lanzarCliente(String nombre, boolean esHost) {
        // Cada cliente se ejecuta en el hilo de eventos de Swing
        SwingUtilities.invokeLater(() -> {
            System.out.println("Lanzando cliente: " + nombre);
            
            // Creamos una instancia de cliente para este jugador
            Cliente cliente = new Cliente();
            
            // Pausa para que el cliente establezca la conexión inicial
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // Abrimos la pantalla de Lobby para este cliente
            PantallaLobby lobby = new PantallaLobby(nombre, esHost, cliente);
            lobby.setVisible(true);

            // El cliente se presenta al servidor
            cliente.enviarMensaje("CONECTAR:" + nombre);
        });
    }
}
