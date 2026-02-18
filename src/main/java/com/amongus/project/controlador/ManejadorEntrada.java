package com.amongus.project.controlador;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class ManejadorEntrada extends KeyAdapter {

    // Estado de las teclas (static para acceso global fácil desde el Jugador)
    public static boolean arriba = false;
    public static boolean abajo = false;
    public static boolean izquierda = false;
    public static boolean derecha = false;

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
            derecha = true;
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
            derecha = false;
        }
    }
}