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

    private Socket socket; // El cable de red virtual
    private BufferedReader entrada; // Por aqui escuchamos al servidor
    private PrintWriter salida; // Por aqui le hablamos al servidor
    
    // Constructor: Se intenta conectar apenas creamos el objeto
    public Cliente() {
        try {
            // "localhost" soy yo mismo. 1234 es el puerto que abrimos en el Servidor.
            // Si quisieras jugar con un amigo, aqui pondrias la IP de tu amigo (ej: 192.168.1.15)
            System.out.println("Cliente: Intentando conectar al servidor...");
            socket = new Socket("localhost", 1234);
            
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

    // Este metodo corre en fondo escuchando lo que dice el servidor
    @Override
    public void run() {
        try {
            String mensajeRecibido;
            // Bucle infinito mientras llegue data
            while ((mensajeRecibido = entrada.readLine()) != null) {
                
                System.out.println("SERVIDOR DIJO: " + mensajeRecibido);
                
                // AQUI MAS ADELANTE PONDREMOS LA LOGICA DEL JUEGO
                // Ejemplo: Si llega "MOVER:Juan,10,10", movemos el muñeco de Juan.
                // Por ahora solo lo imprimimos en consola.
                
            }
        } catch (Exception e) {
            System.out.println("Cliente: Se corto la conexion con el servidor.");
        }
    }
}
