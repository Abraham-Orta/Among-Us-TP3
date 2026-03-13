package com.amongus.project.red;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

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
    private String idSombrero = "ninguno"; // MEMORIA DEL SERVIDOR

    // Estado del movimiento del jugador, controlado por el servidor
    public boolean moviendoArriba = false;
    public boolean moviendoAbajo = false;
    public boolean moviendoIzquierda = false;
    public boolean moviendoDerecha = false;
    public boolean estaVivo = true;

    // Constructor: Se ejecuta cuando creamos el objeto con "new"
    public AtencionJugador(Socket socket) {
        this.socketJugador = socket;
        try {
            entrada = new BufferedReader(new InputStreamReader(socketJugador.getInputStream()));
            // Fix #1: BufferedOutputStream reduce el número de syscalls de escritura al socket.
            // Sin este buffer, cada println() era una llamada directa al kernel.
            salida = new PrintWriter(new BufferedOutputStream(socketJugador.getOutputStream(), 8192), false);
        } catch (IOException e) {
            System.out.println("Error al crear los canales del jugador");
            e.printStackTrace();
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
                // Fix #2: En vez de broadcast inmediato, encolamos la última posición.
                // El ScheduledExecutorService del Servidor enviará un BATCH_MOVER cada 50ms.
                else if (lineaRecibida.startsWith("MOVER:")) {
                    Servidor.pendingMoves.put(nombreJugador, lineaRecibida.substring(6));
                }
                // Cuando un cliente envía su nueva posición, la reenviamos a todos los demás
                else if (lineaRecibida.startsWith("POSICION:")) {
                    for (AtencionJugador otro : Servidor.listaJugadores) {
                        if (otro != this) otro.enviarMensaje(lineaRecibida);
                    }
                }
                // Si alguien es asesinado, informamos a todos
                else if (lineaRecibida.startsWith("MATAR:")) {
                    try {
                        String[] p = lineaRecibida.substring(6).split(",");
                        String victima = p.length > 1 ? p[1] : p[0];
                        for (AtencionJugador j : Servidor.listaJugadores) {
                            if (j.getNombreJugador() != null && j.getNombreJugador().equals(victima)) {
                                j.estaVivo = false;
                                break;
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("Error parsing victima for MATAR command");
                    }
                    Servidor.enviarATodos(lineaRecibida);
                    Servidor.verificarVictoria();
                }
                // si alguien reporta un cuerpo o usa emergencia, avisamos a todos para abrir la votación (o cinemática)
                else if (lineaRecibida.startsWith("REPORTAR_CUERPO:") || lineaRecibida.startsWith("REPORTAR_EMERGENCIA:")) {
                    Servidor.enviarATodos(lineaRecibida);
                    Servidor.iniciarVotacion();
                }
                // si alguien vota, reenviamos el voto a todos
                else if (lineaRecibida.startsWith("VOTO:")) {
                    Servidor.enviarATodos(lineaRecibida);
                    try {
                        String[] partes = lineaRecibida.substring(5).split(",");
                        Servidor.registrarVoto(partes[0], partes[1]);
                    } catch (Exception e) {}
                }
                // si alguien completa una tarea individual
                else if (lineaRecibida.startsWith("TAREA_COMPLETADA:")) {
                    String tarea = lineaRecibida.substring(17);
                    System.out.println("Jugador " + nombreJugador + " completo tarea: " + tarea);
                    Servidor.registrarProgresoTarea(nombreJugador, tarea);
                }
                // si alguien activa un sabotaje
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
                // Sincronizar cambio de sombrero: SOMBRERO:nombre:idSombrero
                else if (lineaRecibida.startsWith("SOMBRERO:")) {
                    String[] p = lineaRecibida.split(":");
                    if (p.length >= 3) {
                        this.idSombrero = p[2];
                    }
                    Servidor.enviarATodos(lineaRecibida);
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
            Servidor.verificarVotacionCompleta();
            
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
        salida.flush();          // Forzamos el envío (autoFlush desactivado para agrupar TCP)
    }

    // Metodo para obtener el nombre del jugador
    public String getNombreJugador() {
        return nombreJugador;
    }

    public String getIdSombrero() {
        return idSombrero;
    }

}