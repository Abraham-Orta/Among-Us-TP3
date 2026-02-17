package com.amongus.project.red; // Paquete donde guardamos las cosas de red

import com.amongus.project.data.GestorDatos; // Importamos el gestor para guardar el XML
import java.io.IOException; // Importamos esto por si falla la entrada o salida
import java.net.ServerSocket; // Esta es la libreria para crear el servidor
import java.net.Socket; // Esta es la libreria para los enchufes (conexiones)
import java.util.ArrayList; // Usamos esto para hacer una lista dinamica

/**
 * Clase Servidor
 * Esta clase es la que prende el servidor y acepta a los jugadores.
 */
public class Servidor { // Clase publica del servidor

    // Aqui guardamos el puerto, uso el 1234 porque es facil de acordarse
    private static final int PUERTO = 1234; 
    
    // El PDF dice maximo 10 jugadores
    private static final int MAX_JUGADORES = 10;
    
    // Esta lista va a guardar a todos los hilos de los jugadores que se conecten
    // La pongo static para poder acceder desde cualquier lado sin instanciar la clase
    public static ArrayList<AtencionJugador> listaJugadores = new ArrayList<>(); 

    public static void main(String[] args) { // Metodo principal main
        
        System.out.println("Iniciando el servidor de Among Us..."); // Mensaje para saber que arranco
        
        try { // Usamos try por si el puerto esta ocupado o algo falla
            
            // Creamos el socket del servidor en el puerto 1234
            ServerSocket servidorSocket = new ServerSocket(PUERTO); 
            
            System.out.println("El servidor esta listo y escuchando en el puerto: " + PUERTO); // Aviso
            
            // Hacemos un bucle infinito para aceptar jugadores todo el tiempo
            while (true) { // Mientras sea verdad (siempre)
                
                // Verificamos si ya esta llena la sala (Requisito del PDF: Max 10)
                if (listaJugadores.size() >= MAX_JUGADORES) {
                    System.out.println("La sala esta llena, rechazando conexion...");
                    // Aceptamos solo para decirle que no y cerrar (truco sucio pero funciona)
                    Socket rechazado = servidorSocket.accept();
                    rechazado.close();
                    continue; // Volvemos al inicio del while
                }
                
                System.out.println("Esperando a que alguien se conecte... (" + listaJugadores.size() + "/" + MAX_JUGADORES + ")"); // Aviso
                
                // El programa se detiene aqui hasta que alguien entre
                Socket socketDelCliente = servidorSocket.accept(); 
                
                System.out.println("¡Alguien se conecto! IP: " + socketDelCliente.getInetAddress()); // Aviso quien entro
                
                // Creamos un nuevo objeto para atender a este jugador especifico
                // Le pasamos el socket que acabamos de aceptar
                AtencionJugador nuevoJugador = new AtencionJugador(socketDelCliente); 
                
                // Agregamos a este jugador a la lista de conectados
                listaJugadores.add(nuevoJugador); 
                
                // Iniciamos el hilo para que corra en paralelo y no trabe el servidor
                nuevoJugador.start(); 
                
            } // Fin del while
            
        } catch (IOException error) { // Si pasa un error atrapamos la excepcion
            System.out.println("Uhh paso un error en el servidor: " + error.getMessage()); // Imprimimos el error
            error.printStackTrace(); // Esto imprime todas las lineas del error
        } // Fin del catch
        
    } // Fin del main

    // Este metodo sirve para enviarle un mensaje a TODOS los jugadores conectados
    public static void enviarATodos(String mensaje) { // Recibe el mensaje en texto
        
        // Recorremos la lista de jugadores uno por uno
        for (AtencionJugador jugador : listaJugadores) { // For each jugador
            try { // Try por si se desconecto justo
                jugador.enviarMensaje(mensaje); // Le mandamos el mensaje a ese jugador
            } catch (Exception e) { // Si falla
                System.out.println("No se pudo enviar mensaje a uno"); // Avisamos
            } // Fin catch
        } // Fin for
    } // Fin metodo enviarATodos

    // Metodo nuevo para asignar roles cuando ya esten todos
    public static void iniciarPartida() { // Metodo estatico
        System.out.println("Intentando iniciar partida..."); // Log
        
        // Requisito del PDF: Minimo 5 jugadores para iniciar
        if (listaJugadores.size() < 5) { 
            System.out.println("Hay muy poca gente para jugar (Minimo 5)"); // Aviso
            enviarATodos("CHAT:SISTEMA: Faltan jugadores para iniciar (Min 5).");
            return; // Salimos del metodo, no arranca
        }
        
        // Requisito del PDF: Asignar DOS impostores
        // Elegimos dos numeros al azar diferentes
        int impostor1 = (int) (Math.random() * listaJugadores.size());
        int impostor2 = (int) (Math.random() * listaJugadores.size());
        
        // Si por mala suerte salio el mismo numero, cambiamos el segundo hasta que sea distinto
        while (impostor2 == impostor1) {
            impostor2 = (int) (Math.random() * listaJugadores.size());
        }
        
        System.out.println("Los impostores son los indices: " + impostor1 + " y " + impostor2); // Log secreto
        
        // Recorremos la lista para avisarles que son
        for (int i = 0; i < listaJugadores.size(); i++) { // Bucle clasico
            AtencionJugador jugador = listaJugadores.get(i); // Sacamos al jugador
            
            // Si el indice coincide con alguno de los dos impostores
            if (i == impostor1 || i == impostor2) { 
                jugador.enviarMensaje("ROL:IMPOSTOR"); // Le decimos que es el malo
            } else { // Si no
                jugador.enviarMensaje("ROL:TRIPULANTE"); // Le decimos que es bueno
            }
        }
        
        enviarATodos("INICIO:La partida ha comenzado"); // Avisamos a todos que arranca
    }
    
    // Metodo para terminar la partida y guardar en el XML
    public static void finalizarPartida(String equipoGanador) {
        System.out.println("La partida termino. Ganaron: " + equipoGanador);
        
        // Avisamos a todos quien gano
        enviarATodos("FIN:" + equipoGanador);
        
        // Requisito del PDF: Guardar historial en XML
        // Llamamos a la clase estatica GestorDatos que ya teniamos
        GestorDatos.guardarPartida(equipoGanador, listaJugadores.size());
        
        System.out.println("Datos guardados en el XML.");
    }

    // Metodo para enviar lista actualizada de jugadores a todos
    public static void enviarListaJugadores() {
        StringBuilder lista = new StringBuilder("LISTA_JUGADORES:");
        
        for (int i = 0; i < listaJugadores.size(); i++) {
            AtencionJugador jugador = listaJugadores.get(i);
            String nombre = jugador.getNombreJugador();
            if (nombre != null) {
                lista.append(nombre);
                if (i < listaJugadores.size() - 1) {
                    lista.append(",");
                }
            }
        }
        
        System.out.println("Enviando lista: " + lista.toString());
        enviarATodos(lista.toString());
    }

} // Fin de la clase Servidor
