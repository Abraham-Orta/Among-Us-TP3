package com.amongus.project.red; // El mismo paquete de red

import java.io.BufferedReader; // Para leer texto linea por linea
import java.io.IOException; // Para manejar errores de entrada/salida
import java.io.InputStreamReader; // Para convertir bytes a caracteres
import java.io.PrintWriter; // Para enviar texto facilmente
import java.net.Socket; // Para manejar la conexion

/**
 * Clase AtencionJugador
 * Esta clase es un Hilo (Thread) que se encarga de escuchar a UN solo jugador.
 * Se crea una instancia de esto por cada persona que entra.
 */
public class AtencionJugador extends Thread { // Heredamos de Thread para que corra en paralelo

    private Socket socketJugador; // Aqui guardamos el enchufe del jugador
    private BufferedReader entrada; // Canal para escuchar lo que dice el jugador
    private PrintWriter salida; // Canal para hablarle al jugador
    private String nombreJugador; // Guardamos el nombre (ej: "Samuel")
    public boolean esImpostor = false;

    // Estado del movimiento del jugador, controlado por el servidor
    public boolean moviendoArriba = false;
    public boolean moviendoAbajo = false;
    public boolean moviendoIzquierda = false;
    public boolean moviendoDerecha = false;
    public boolean estaVivo = true;

    // Constructor: Se ejecuta cuando creamos el objeto con "new"
    public AtencionJugador(Socket socket) { // Recibimos el socket desde el Servidor
        this.socketJugador = socket; // Guardamos el socket en la variable privada
        
        try { // Intentamos abrir los canales de comunicacion
            
            // Preparamos la entrada: Convertimos los bytes del socket en texto leible
            entrada = new BufferedReader(new InputStreamReader(socketJugador.getInputStream()));
            
            // Preparamos la salida: El 'true' al final es para que envie el mensaje rapido (autoFlush)
            salida = new PrintWriter(socketJugador.getOutputStream(), true);
            
        } catch (IOException e) { // Si falla algo al abrir canales
            System.out.println("Error al crear los canales del jugador"); // Avisamos
            e.printStackTrace(); // Mostramos el error
        }
    }

    // Este es el metodo que corre cuando le damos .start() al hilo
    @Override
    public void run() { // Sobreescribimos el metodo run
        
        try { // Try para manejar desconexiones
            
            System.out.println("Hilo de jugador iniciado"); // Log de control
            
            // Leemos el primer mensaje que deberia ser el nombre o login
            String lineaRecibida; // Variable temporal para guardar lo que llega
            
            // Bucle infinito: Leemos mensajes mientras la conexion siga viva
            while ((lineaRecibida = entrada.readLine()) != null) { // Leemos una linea
                
                // Imprimimos en la consola del servidor lo que llego (ocultamos los MOVER para no saturar)
                if (!lineaRecibida.startsWith("MOVER:")) {
                    System.out.println("El jugador dijo: " + lineaRecibida); 
                }
                
                // AQUI ANALIZAMOS QUE NOS DIJO EL JUGADOR
                
                // Si el mensaje empieza con "CONECTAR:"
                if (lineaRecibida.startsWith("CONECTAR:")) { 
                    // Guardamos el nombre, cortando el string despues de los dos puntos
                    this.nombreJugador = lineaRecibida.substring(9); 
                    System.out.println("El nombre del jugador es: " + this.nombreJugador); // Avisamos
                    
                    // Le avisamos a TODOS que entro alguien nuevo
                    Servidor.enviarATodos("CHAT:El jugador " + this.nombreJugador + " ha entrado a la nave.");
                    
                    // Enviar lista actualizada de jugadores
                    Servidor.enviarListaJugadores();
                }
                else if (lineaRecibida.startsWith("MOVIMIENTO:")) {
                    String[] partes = lineaRecibida.split(":");
                    boolean press = partes[1].equals("PRESS");
                    String dir = partes[2];

                    switch (dir) {
                        case "UP": moviendoArriba = press; break;
                        case "DOWN": moviendoAbajo = press; break;
                        case "LEFT": moviendoIzquierda = press; break;
                        case "RIGHT": moviendoDerecha = press; break;
                    }
                }
                // Cuando un cliente envía su nueva posición (MOVER:nombre,x,y), la reenviamos a todos los demás
                else if (lineaRecibida.startsWith("MOVER:")) {
                    for (AtencionJugador otro : Servidor.listaJugadores) {
                        if (otro != this) { // No se la enviamos al que la mandó
                            otro.enviarMensaje(lineaRecibida);
                        }
                    }
                }
                // Cuando un cliente envía su nueva posición, la reenviamos a todos los demás
                else if (lineaRecibida.startsWith("POSICION:")) {
                    for (AtencionJugador otro : Servidor.listaJugadores) {
                        if (otro != this) { // No se la enviamos al que la mandó
                            otro.enviarMensaje(lineaRecibida);
                        }
                    }
                }
                // Si alguien es asesinado, informamos a todos
                else if (lineaRecibida.startsWith("MATAR:")) {
                    String victima = lineaRecibida.substring(6);
                    for (AtencionJugador j : Servidor.listaJugadores) {
                        if (j.getNombreJugador() != null && j.getNombreJugador().equals(victima)) {
                            j.estaVivo = false;
                            break;
                        }
                    }
                    Servidor.enviarATodos(lineaRecibida);
                    Servidor.verificarVictoria();
                }
                // Si alguien reporta un cuerpo, avisamos a todos para abrir la votación
                else if (lineaRecibida.equals("REPORTAR:")) {
                    Servidor.enviarATodos(lineaRecibida);
                }
                // Si alguien termina todas sus tareas
                else if (lineaRecibida.startsWith("TAREA_LISTA:")) {
                    Servidor.enviarATodos(lineaRecibida);
                }
                // Si alguien activa un sabotaje
                else if (lineaRecibida.startsWith("SABOTAJE:")) {
                    Servidor.enviarATodos(lineaRecibida);
                }
                // Si alguien escribe INICIAR (digamos que es el boton de Start)
                else if (lineaRecibida.startsWith("COMANDO:INICIAR")) {
                    System.out.println("Un jugador pidio iniciar la partida"); // Log
                    
                    // Extraemos el mapa elegido si el host lo envió (ej. COMANDO:INICIAR:mapa2.png)
                    String mapaElegido = "mapa1.png"; // Por defecto
                    if (lineaRecibida.contains(":")) {
                        String[] partes = lineaRecibida.split(":");
                        if (partes.length >= 3) {
                            mapaElegido = partes[2];
                        }
                    }
                    
                    Servidor.iniciarPartida(mapaElegido); // Llamamos al metodo estatico del servidor con el mapa
                }
                // Si los tripulantes ganan (completaron tareas o echaron a todos)
                else if (lineaRecibida.equals("COMANDO:GANAR_TRIPULANTES")) {
                    Servidor.finalizarPartida("Tripulantes"); // Guardamos en XML
                }
                // Si los impostores ganan (mataron a todos o sabotaje critico)
                else if (lineaRecibida.equals("COMANDO:GANAR_IMPOSTORES")) {
                    Servidor.finalizarPartida("Impostores"); // Guardamos en XML
                }
                // Si es cualquier otra cosa (como chat normal)
                else {
                    // Lo reenviamos a todos tal cual
                    Servidor.enviarATodos("CHAT:" + this.nombreJugador + ": " + lineaRecibida);
                }
                
            }
            
        } catch (IOException e) { // Si hay error de conexion (se fue el internet o cerro el juego)
            System.out.println("Parece que el jugador se desconecto: " + e.getMessage()); // Avisamos
        } finally { // Esto se ejecuta SIEMPRE al final, haya error o no
            
            // Borramos al jugador de la lista del servidor
            Servidor.listaJugadores.remove(this); 
            System.out.println("Jugador eliminado de la lista"); // Log
            
            // Enviar lista actualizada
            Servidor.enviarListaJugadores();
            Servidor.verificarVictoria();
            
            try { // Intentamos cerrar el socket bien
                socketJugador.close(); // Cerramos conexion
            } catch (IOException e) { // Si falla al cerrar
                System.out.println("No se pudo cerrar el socket"); // Da igual, avisamos
            }
            
        }
        
    }

    // Metodo auxiliar para enviarle un mensaje a ESTE jugador
    public void enviarMensaje(String mensaje) { // Recibe el texto
        salida.println(mensaje); // Lo manda por el canal de salida
    }

    // Metodo para obtener el nombre del jugador
    public String getNombreJugador() {
        return nombreJugador;
    }

}