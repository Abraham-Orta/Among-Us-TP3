package com.amongus.project.red; // Paquete de red

import java.io.BufferedReader; // Para leer texto
import java.io.InputStreamReader; // Para entender los bytes
import java.io.PrintWriter; // Para escribir texto facil
import java.net.Socket; // El enchufe de conexion

/**
 * Clase Cliente
 * Esta parte va DENTRO del juego de cada jugador.
 * Se encarga de llamar al servidor y mantener la charla.
 */
public class Cliente extends Thread { // Hereda de Thread para escuchar sin trabar el juego

    // Interfaz para notificar mensajes a quien escuche
    public interface MensajeListener {
        void onMensajeRecibido(String mensaje);
    }

    private Socket socket; // El cable de red virtual
    private BufferedReader entrada; // Por aqui escuchamos al servidor
    private PrintWriter salida; // Por aqui le hablamos al servidor
    private MensajeListener listener; // Quien escucha los mensajes
    
    // Constructor: Recibe la IP a la cual conectarse
    public Cliente(String direccionIP) {
        try {
            // Usamos la IP que nos pasaron (puede ser "localhost" o "192.168.1.5")
            System.out.println("Cliente: Intentando conectar a " + direccionIP + "...");
            socket = new Socket(direccionIP, 1234);
            socket.setTcpNoDelay(true); // OPTIMIZACIÓN: Enviar datos inmediatamente sin demora
            
            // Preparamos los canales para hablar y escuchar
            entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            salida = new PrintWriter(socket.getOutputStream(), true); // true = enviar rapido
            
            System.out.println("Cliente: ¡Conectado exitosamente!");
            
            // Arrancamos el hilo (el metodo run) para que se ponga a escuchar mensajes
            this.start();
            
        } catch (Exception e) {
            System.out.println("Cliente: No se pudo conectar. ¿Esta prendido el servidor?");
            e.printStackTrace();
        }
    }

    // Este metodo sirve para mandar mensajes al servidor desde el juego
    // Ejemplo: enviarMensaje("MOVER:50,50")
    public void enviarMensaje(String texto) {
        if (salida != null) { // Verificamos que estemos conectados
            salida.println(texto);
            // System.out.println("Cliente envio: " + texto); // Descomentar para depurar
        }
    }

    // Metodo para registrar quien escucha los mensajes
    public void setMensajeListener(MensajeListener listener) {
        this.listener = listener;
    }

    // Este metodo corre en fondo escuchando lo que dice el servidor
    @Override
    public void run() {
        try {
            String mensajeRecibido;
            // Bucle infinito mientras llegue data
            while ((mensajeRecibido = entrada.readLine()) != null) {
                
                // System.out.println("SERVIDOR DIJO: " + mensajeRecibido); // OPTIMIZACIÓN: Comentado para eliminar lag
                
                // Notificar al listener si existe
                if (listener != null) {
                    listener.onMensajeRecibido(mensajeRecibido);
                }
                
            }
        } catch (Exception e) {
            System.out.println("Cliente: Se corto la conexion con el servidor.");
        }
    }
}
