package com.amongus.project.controlador; // Paquete que agrupa los controladores (la lógica que une la vista y el modelo)

import java.awt.event.KeyAdapter; // Importamos la clase base para manejar eventos de teclado
import java.awt.event.KeyEvent; // Importamos la clase que representa una tecla presionada
import java.awt.event.MouseAdapter; // Importamos la clase base para manejar eventos del ratón
import java.awt.event.MouseEvent; // Importamos la clase que representa un evento del ratón
import com.amongus.project.modelo.EstadoJuego; // Importamos el estado global del juego

/**
 * ManejadorEntrada
 * ================
 * Esta clase se encarga de "escuchar" todo lo que el usuario hace con el teclado y el ratón.
 * Guarda el estado de las teclas (si están presionadas o no) en variables estáticas
 * para que cualquier parte del juego (como el Jugador) pueda consultarlas en tiempo real.
 */
public class ManejadorEntrada extends KeyAdapter { // Heredamos de KeyAdapter para no tener que implementar todos los métodos de teclado

    // Variables estáticas (globales) que indican si una tecla de movimiento está siendo presionada.
    // Se usan en la clase Jugador para mover al personaje.
    public static boolean arriba = false;    // ¿Está presionada la tecla W o Flecha Arriba?
    public static boolean abajo = false;     // ¿Está presionada la tecla S o Flecha Abajo?
    public static boolean izquierda = false; // ¿Está presionada la tecla A o Flecha Izquierda?
    public static boolean derecha = false;   // ¿Está presionada la tecla D o Flecha Derecha?
    
    // NUEVAS VARIABLES PARA PASOS 2 Y 3 (Matar y Alcantarilla)
    public static boolean accionMatar = false;    // ¿Está presionada la tecla Q (Matar)?
    public static boolean accionVentilar = false; // ¿Está presionada la tecla E (Entrar a alcantarilla)?
    
    // NUEVAS VARIABLES PARA REPORTE Y MODO DESARROLLADOR
    public static boolean accionReportar = false; // ¿Está presionada la tecla R (Reportar)?
    public static boolean accionSabotaje = false; // ¿Está presionada la tecla H (Sabotaje)?
    public static boolean modoDesarrollador = false; // ¿Está activado el modo desarrollador (F3)?
    
    // Variables para guardar el estado y posición del ratón.
    public static int mouseX = 0; // Posición X del puntero del ratón en la pantalla
    public static int mouseY = 0; // Posición Y del puntero del ratón en la pantalla
    public static boolean clickIzquierdo = false; // ¿Se hizo clic con el botón izquierdo?

    /**
     * MouseHandler
     * ============
     * Clase interna estática que maneja exclusivamente los eventos del ratón.
     */
    public static class MouseHandler extends MouseAdapter {
        
        // Este método se dispara automáticamente cuando el usuario PRESIONA un botón del ratón
        @Override
        public void mousePressed(MouseEvent e) {
            // Verificamos si el botón presionado fue el izquierdo (BUTTON1)
            if (e.getButton() == MouseEvent.BUTTON1) {
                clickIzquierdo = true; // Registramos que se hizo clic
                mouseX = e.getX(); // Guardamos la posición X exacta del clic
                mouseY = e.getY(); // Guardamos la posición Y exacta del clic
            }
        }

        // Este método se dispara cuando el usuario SUELTA el botón del ratón
        @Override
        public void mouseReleased(MouseEvent e) {
            // Verificamos si el botón que se soltó fue el izquierdo
            if (e.getButton() == MouseEvent.BUTTON1) {
                clickIzquierdo = false; // Registramos que ya no se está haciendo clic
            }
        }

        // Este método se dispara cuando el usuario MUEVE el ratón (sin hacer clic)
        @Override
        public void mouseMoved(MouseEvent e) {
            mouseX = e.getX(); // Actualizamos la posición X en tiempo real
            mouseY = e.getY(); // Actualizamos la posición Y en tiempo real
        }
        
        // Este método se dispara cuando el usuario ARRASTRA el ratón (moviéndolo mientras hace clic)
        @Override
        public void mouseDragged(MouseEvent e) {
            mouseX = e.getX(); // Actualizamos la posición X mientras arrastra
            mouseY = e.getY(); // Actualizamos la posición Y mientras arrastra
        }
    }

    // Este método se dispara cuando el usuario PRESIONA una tecla en su teclado
    @Override
    public void keyPressed(KeyEvent e) {
        int codigo = e.getKeyCode(); // Obtenemos el código numérico de la tecla presionada

        // MOVIMIENTO: Verificamos qué tecla de movimiento se presionó y activamos su bandera
        if (codigo == KeyEvent.VK_W || codigo == KeyEvent.VK_UP) { // W o Flecha Arriba
            arriba = true;
        }
        if (codigo == KeyEvent.VK_S || codigo == KeyEvent.VK_DOWN) { // S o Flecha Abajo
            abajo = true;
        }
        if (codigo == KeyEvent.VK_A || codigo == KeyEvent.VK_LEFT) { // A o Flecha Izquierda
            izquierda = true;
        }
        if (codigo == KeyEvent.VK_D || codigo == KeyEvent.VK_RIGHT) { // D o Flecha Derecha
            derecha = true;
        }
        
        // ACCIONES DE JUEGO: Matar (Q), Ventilar (E), Reportar (R)
        if (codigo == KeyEvent.VK_Q) { // Tecla Q para paralizar/matar
            accionMatar = true;
        }
        if (codigo == KeyEvent.VK_E) { // Tecla E para usar vías de acceso (alcantarillas)
            accionVentilar = true;
        }
        if (codigo == KeyEvent.VK_R) { // Tecla R para reportar un cuerpo
            accionReportar = true;
        }
        if (codigo == KeyEvent.VK_H) { // Tecla H para Sabotaje (Luces)
            accionSabotaje = true;
        }

        
        // TECLAS DE DEPURACIÓN (Solo para pruebas)
        if (codigo == KeyEvent.VK_V) { // Tecla V para forzar la pantalla de Votación
            EstadoJuego.getInstancia().setFaseActual(EstadoJuego.Fase.VOTACION);
            if (EstadoJuego.getInstancia().getJugadorLocal() != null) {
                EstadoJuego.getInstancia().getJugadorLocal().resetVoto();
            }
        }
        if (codigo == KeyEvent.VK_J) { // Tecla J para volver al juego desde la votación
            EstadoJuego.getInstancia().setFaseActual(EstadoJuego.Fase.JUGANDO);
        }
        
        // TOGGLE MODO DESARROLLADOR: Tecla F3 para ver las hitboxes
        if (codigo == KeyEvent.VK_F3) {
            modoDesarrollador = !modoDesarrollador; // Cambia entre true/false
            System.out.println("Modo desarrollador: " + (modoDesarrollador ? "ACTIVADO" : "DESACTIVADO"));
        }
    }

    // Este método se dispara cuando el usuario SUELTA una tecla
    @Override
    public void keyReleased(KeyEvent e) {
        int codigo = e.getKeyCode(); // Obtenemos el código numérico de la tecla que se soltó

        // Desactivamos la bandera correspondiente a la tecla soltada para detener el movimiento o acción
        if (codigo == KeyEvent.VK_W || codigo == KeyEvent.VK_UP) {
            arriba = false;
        }
        if (codigo == KeyEvent.VK_S || codigo == KeyEvent.VK_DOWN) {
            abajo = false;
        }
        if (codigo == KeyEvent.VK_A || codigo == KeyEvent.VK_LEFT) {
            izquierda = false;
        }
        if (codigo == KeyEvent.VK_D || codigo == KeyEvent.VK_RIGHT) {
            derecha = false;
        }
        
        // Desactivamos las acciones del impostor/tripulante cuando suelta la tecla
        if (codigo == KeyEvent.VK_Q) {
            accionMatar = false;
        }
        if (codigo == KeyEvent.VK_E) {
            accionVentilar = false;
        }
        if (codigo == KeyEvent.VK_R) {
            accionReportar = false;
        }
        if (codigo == KeyEvent.VK_H) {
            accionSabotaje = false;
        }
    }
}
