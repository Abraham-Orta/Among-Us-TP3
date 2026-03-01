package com.amongus.project.controlador;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import com.amongus.project.modelo.EstadoJuego;

public class ManejadorEntrada extends KeyAdapter {

    // Estado de las teclas
    public static boolean arriba = false;
    public static boolean abajo = false;
        public static boolean izquierda = false; // Tecla de movimiento a la izquierda
        public static boolean derecha = false; // Tecla de movimiento a la derecha
        public static boolean accionMatar = false; // Tecla E para que el impostor paralice
        public static boolean accionVentilar = false; // Tecla F para usar la ventilacion (via de acceso rapido)
    
        // Estado del Ratón
    public static int mouseX = 0;
    public static int mouseY = 0;
    public static boolean clickIzquierdo = false;

    // Clase interna para manejar eventos de ratón
    public static class MouseHandler extends MouseAdapter {
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

    @Override
    public void keyPressed(KeyEvent e) {
        int codigo = e.getKeyCode();

        if (codigo == KeyEvent.VK_W || codigo == KeyEvent.VK_UP) {
            arriba = true;
        }
        if (codigo == KeyEvent.VK_S || codigo == KeyEvent.VK_DOWN) {
            abajo = true;
        }
        if (codigo == KeyEvent.VK_A || codigo == KeyEvent.VK_LEFT) {
            izquierda = true;
        }
        if (codigo == KeyEvent.VK_D || codigo == KeyEvent.VK_RIGHT) {
            derecha = true; // Activa movimiento a la derecha
        }

        // Tecla de accion para paralizar (E)
        if (codigo == KeyEvent.VK_E) { // Si se presiona la tecla E
            accionMatar = true; // Activa la bandera de accion para paralizar
        }

        // Tecla de accion para ventilar (F)
        if (codigo == KeyEvent.VK_F) { // Si se presiona la tecla F
            accionVentilar = true; // Activa la bandera de accion de la via rapida
        }
        
        // Teclas de depuración para probar la votación
        if (codigo == KeyEvent.VK_V) {
            EstadoJuego.getInstancia().setFaseActual(EstadoJuego.Fase.VOTACION);
            // Resetear votos al entrar (opcional, para pruebas)
            if (EstadoJuego.getInstancia().getJugadorLocal() != null) {
                EstadoJuego.getInstancia().getJugadorLocal().resetVoto();
            }
        }
        if (codigo == KeyEvent.VK_J) {
            EstadoJuego.getInstancia().setFaseActual(EstadoJuego.Fase.JUGANDO);
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int codigo = e.getKeyCode();

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
            derecha = false; // Desactiva movimiento a la derecha
        }

        // Tecla de accion para paralizar (E)
        if (codigo == KeyEvent.VK_E) { // Si se suelta la tecla E
            accionMatar = false; // Desactiva la bandera de accion para paralizar
        }

        // Tecla de accion para ventilar (F)
        if (codigo == KeyEvent.VK_F) { // Si se suelta la tecla F
            accionVentilar = false; // Desactiva la bandera de accion de la via rapida
        }
    }
}
