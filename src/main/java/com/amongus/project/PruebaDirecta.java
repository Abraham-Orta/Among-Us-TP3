package com.amongus.project; // Paquete raíz del proyecto

import java.awt.Color; // Para asignar el color del personaje
import javax.swing.JFrame; // Para crear la ventana principal de prueba
import javax.swing.SwingUtilities; // Para manejar hilos de la interfaz gráfica

import com.amongus.project.controlador.BucleJuego; // Para iniciar el ciclo de actualización (60 FPS)
import com.amongus.project.modelo.EstadoJuego; // Para manejar el estado global (jugadores, fase, mapa)
import com.amongus.project.modelo.Jugador; // Para crear la instancia del personaje de prueba
import com.amongus.project.vista.PanelJuego; // Para mostrar los gráficos del juego

/**
 * PruebaDirecta
 * =============
 * Clase para saltarse los menús e ir directo al juego para probar las mecánicas.
 */
public class PruebaDirecta {

    public static void main(String[] args) { // Método principal de ejecución
        
        // Ejecutamos en el hilo de eventos de Swing para evitar problemas gráficos
        SwingUtilities.invokeLater(() -> {
            
            // 1. Configuramos el estado global del juego
            EstadoJuego estado = EstadoJuego.getInstancia(); // Obtenemos la instancia única (Singleton)
            estado.setFaseActual(EstadoJuego.Fase.JUGANDO); // Cambiamos la fase directamente a JUGANDO
            
            // 2. Crear un jugador de prueba (nosotros)
            // AQUÍ ESTABA EL DETALLE: El último parámetro estaba en 'false' (Tripulante).
            // Lo cambiamos a 'true' para que se asigne el rol de Impostor y podamos usar las alcantarillas.
            Jugador jugadorTest = new Jugador("Tester", 100, 100, Color.RED, true); 
            estado.setJugadorLocal(jugadorTest); // Establecemos este jugador como el principal
            
            // EXTRA: Agregamos varios muñecos de prueba (tripulantes)
            // Si solo hay 1 impostor y 1 tripulante, el juego termina en el primer frame (¡Gana impostor!)
            // Por eso necesitamos al menos 2 tripulantes para que la partida siga corriendo.
            Jugador munecoPrueba1 = new Jugador("Víctima 1", 200, 100, Color.BLUE, false);
            Jugador munecoPrueba2 = new Jugador("Víctima 2", 300, 150, Color.GREEN, false);
            Jugador munecoPrueba3 = new Jugador("Víctima 3", 150, 300, Color.YELLOW, false);
            
            estado.agregarJugador(munecoPrueba1); // Añadimos la víctima al mapa
            estado.agregarJugador(munecoPrueba2); // Añadimos la víctima al mapa
            estado.agregarJugador(munecoPrueba3); // Añadimos la víctima al mapa
            
            // 3. Crear la ventana (JFrame) y el panel (JPanel)
            JFrame frame = new JFrame("Among Us - PRUEBA DIRECTA"); // Creamos la ventana con título
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // Si cerramos la ventana, se apaga el programa
            
            PanelJuego panel = new PanelJuego(); // Instanciamos el lienzo de dibujo
            frame.add(panel); // Añadimos el lienzo a la ventana
            frame.pack(); // Ajustamos el tamaño de la ventana al tamaño preferido del panel (800x600)
            frame.setLocationRelativeTo(null); // Centramos la ventana en la pantalla
            frame.setVisible(true); // Hacemos que la ventana sea visible
            
            // 4. Iniciar el bucle de juego (Game Loop)
            BucleJuego bucle = new BucleJuego(panel); // Creamos el controlador de tiempo
            bucle.iniciar(); // Iniciamos el hilo a 60 FPS
            
            // Le damos el foco al panel para que empiece a recibir las teclas (WASD, Q, E) inmediatamente
            panel.requestFocus(); 
            
            System.out.println("Prueba directa iniciada con jugador 'Tester' (AHORA ES IMPOSTOR)"); // Mensaje en consola
        });
    }
}
