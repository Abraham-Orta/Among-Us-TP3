package com.amongus.project.controlador;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import com.amongus.project.modelo.EstadoJuego;
import com.amongus.project.modelo.Jugador;

/**
 * ManejadorEntrada
 * ================
 * Escucha teclado y ratón de UNA ventana específica.
 *
 * IMPORTANTE: Todos los campos son de INSTANCIA (no static).
 * Cada PanelJuego crea su propio ManejadorEntrada, por lo que
 * múltiples ventanas abiertas simultáneamente (PruebaDirecta) no
 * se interfieren entre sí — cada una maneja su propio teclado.
 */
public class ManejadorEntrada extends KeyAdapter {

    // --- MOVIMIENTO ---
    public boolean arriba    = false;
    public boolean abajo     = false;
    public boolean izquierda = false;
    public boolean derecha   = false;

    // --- ACCIONES ---
    public boolean accionMatar    = false; // Q → Paralizar
    public boolean accionVentilar = false; // E → Alcantarilla
    public boolean accionReportar = false; // R → Reportar cadáver
    public boolean accionSabotaje = false; // H → Sabotaje de luces
    public boolean accionEmergencia = false; // HUD → Botón de Emergencia

    // --- MODO DESARROLLADOR ---
    public boolean modoDesarrollador = false; // F3 → ver hitboxes

    // --- RATÓN ---
    public int     mouseX         = 0;
    public int     mouseY         = 0;
    public boolean clickIzquierdo = false;

    // --- CHAT ---
    public boolean escribiendoChat = false;
    public StringBuilder entradaChat = new StringBuilder();
    public boolean presionoEnterChat = false; // flag para disparar el mensaje

    // --- ESTADO DE JUEGO (instancia por cliente) ---
    private EstadoJuego estadoJuego;

    public ManejadorEntrada(EstadoJuego estadoJuego) {
        this.estadoJuego = estadoJuego;
    }

    /**
     * MouseHandler — clase interna para eventos del ratón.
     * También usa campos de instancia pasados por referencia al ManejadorEntrada padre.
     */
    public class MouseHandler extends MouseAdapter {

        @Override
        public void mousePressed(MouseEvent e) {
            if (e.getButton() == MouseEvent.BUTTON1) {
                clickIzquierdo = true;
                mouseX = e.getX();
                mouseY = e.getY();
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if (e.getButton() == MouseEvent.BUTTON1) {
                clickIzquierdo = false;
            }
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            mouseX = e.getX();
            mouseY = e.getY();
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            mouseX = e.getX();
            mouseY = e.getY();
        }
    }

    // ---------------------------------------------------------------
    //  TECLADO — PRESIONAR Y ESCRIBIR
    // ---------------------------------------------------------------
    @Override
    public void keyTyped(KeyEvent e) {
        if (escribiendoChat) {
            char t = e.getKeyChar();
            // Evitar agregar los caracteres de control a la cadena
            if (t != KeyEvent.VK_ENTER && t != KeyEvent.VK_BACK_SPACE && t != KeyEvent.VK_ESCAPE && t >= 32) {
                // LÍMITE ACTUALIZADO: 46 caracteres
                if (entradaChat.length() < 46) {
                    entradaChat.append(t);
                }
            }
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int c = e.getKeyCode();

        if (escribiendoChat) {
            if (c == KeyEvent.VK_ESCAPE) {
                escribiendoChat = false;
                entradaChat.setLength(0);
            } else if (c == KeyEvent.VK_BACK_SPACE) {
                if (entradaChat.length() > 0) {
                    entradaChat.deleteCharAt(entradaChat.length() - 1);
                }
            } else if (c == KeyEvent.VK_ENTER) {
                if (entradaChat.length() > 0) {
                    presionoEnterChat = true;
                }
                // Se mantiene escribiendoChat en true para seguir leyendo el chat
            }
            return; // Bloquea el movimiento u otras teclas mientras escribe
        }

        // Movimiento
        if (c == KeyEvent.VK_W || c == KeyEvent.VK_UP)    arriba    = true;
        if (c == KeyEvent.VK_S || c == KeyEvent.VK_DOWN)  abajo     = true;
        if (c == KeyEvent.VK_A || c == KeyEvent.VK_LEFT)  izquierda = true;
        if (c == KeyEvent.VK_D || c == KeyEvent.VK_RIGHT) derecha   = true;

        // acciones: obtenemos el jugador local para saber su rol
        Jugador local = estadoJuego.getJugadorLocal();
        boolean esImpostor = (local != null && local.isImpostor());

        if (c == KeyEvent.VK_Q) {
            // solo suena si el jugador es impostor
            if (!accionMatar && esImpostor) com.amongus.project.vista.ReproductorMusica.reproducirEfecto("UI_boton.wav");
            accionMatar    = true;
        }
        if (c == KeyEvent.VK_E) {
            // solo suena si el jugador es impostor
            if (!accionVentilar && esImpostor) com.amongus.project.vista.ReproductorMusica.reproducirEfecto("UI_boton.wav");
            accionVentilar = true;
        }
        if (c == KeyEvent.VK_R) {
            // este sonido de reportar suena para todos (tripulantes e impostores)
            if (!accionReportar) com.amongus.project.vista.ReproductorMusica.reproducirEfecto("sonido_Botones_votacion.wav");
            accionReportar = true;
        }
        if (c == KeyEvent.VK_H) {
            // solo suena si el jugador es impostor
            if (!accionSabotaje && esImpostor) com.amongus.project.vista.ReproductorMusica.reproducirEfecto("UI_boton.wav");
            accionSabotaje = true;
        }

        // Acciones: Emergencia
        if (c == KeyEvent.VK_V) {
            if (!accionEmergencia) com.amongus.project.vista.ReproductorMusica.reproducirEfecto("sonido_Botones_votacion.wav");
            accionEmergencia = true;
        }

        if (c == KeyEvent.VK_J) {
            estadoJuego.setFaseActual(EstadoJuego.Fase.JUGANDO);
        }

        // Toggle hitboxes
        if (c == KeyEvent.VK_F3) {
            modoDesarrollador = !modoDesarrollador;
            System.out.println("Modo desarrollador: " + (modoDesarrollador ? "ACTIVADO" : "DESACTIVADO"));
        }
    }

    // ---------------------------------------------------------------
    //  TECLADO — SOLTAR
    // ---------------------------------------------------------------
    @Override
    public void keyReleased(KeyEvent e) {
        int c = e.getKeyCode();

        if (c == KeyEvent.VK_W || c == KeyEvent.VK_UP)    arriba    = false;
        if (c == KeyEvent.VK_S || c == KeyEvent.VK_DOWN)  abajo     = false;
        if (c == KeyEvent.VK_A || c == KeyEvent.VK_LEFT)  izquierda = false;
        if (c == KeyEvent.VK_D || c == KeyEvent.VK_RIGHT) derecha   = false;

        if (c == KeyEvent.VK_Q) accionMatar    = false;
        if (c == KeyEvent.VK_E) accionVentilar = false;
        if (c == KeyEvent.VK_R) accionReportar = false;
        if (c == KeyEvent.VK_H) accionSabotaje = false;
        if (c == KeyEvent.VK_V) accionEmergencia = false;
    }
}