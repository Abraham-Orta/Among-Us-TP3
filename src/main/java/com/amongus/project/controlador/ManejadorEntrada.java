package com.amongus.project.controlador;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import com.amongus.project.modelo.EstadoJuego;
import com.amongus.project.red.Cliente;

public class ManejadorEntrada extends KeyAdapter {

    private Cliente cliente;

    // Estado de las teclas (leído por Jugador.actualizar() cada frame)
    public boolean arriba      = false;
    public boolean abajo       = false;
    public boolean izquierda   = false;
    public boolean derecha     = false;
    public boolean accionMatar    = false;
    public boolean accionVentilar = false;

    public ManejadorEntrada(Cliente cliente) {
        this.cliente = cliente;
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int codigo = e.getKeyCode();

        switch (codigo) {
            case KeyEvent.VK_W:
            case KeyEvent.VK_UP:
                if (!arriba) { // Evitar spam de mensajes por key-repeat del SO
                    arriba = true;
                    if (cliente != null) cliente.enviarMensaje("MOVIMIENTO:PRESS:UP");
                }
                break;
            case KeyEvent.VK_S:
            case KeyEvent.VK_DOWN:
                if (!abajo) {
                    abajo = true;
                    if (cliente != null) cliente.enviarMensaje("MOVIMIENTO:PRESS:DOWN");
                }
                break;
            case KeyEvent.VK_A:
            case KeyEvent.VK_LEFT:
                if (!izquierda) {
                    izquierda = true;
                    if (cliente != null) cliente.enviarMensaje("MOVIMIENTO:PRESS:LEFT");
                }
                break;
            case KeyEvent.VK_D:
            case KeyEvent.VK_RIGHT:
                if (!derecha) {
                    derecha = true;
                    if (cliente != null) cliente.enviarMensaje("MOVIMIENTO:PRESS:RIGHT");
                }
                break;
            case KeyEvent.VK_E:
                accionMatar = true;
                if (cliente != null) cliente.enviarMensaje("ACCION:PRESS:KILL");
                break;
            case KeyEvent.VK_F:
                accionVentilar = true;
                if (cliente != null) cliente.enviarMensaje("ACCION:PRESS:VENT");
                break;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int codigo = e.getKeyCode();

        switch (codigo) {
            case KeyEvent.VK_W:
            case KeyEvent.VK_UP:
                arriba = false;
                if (cliente != null) cliente.enviarMensaje("MOVIMIENTO:RELEASE:UP");
                break;
            case KeyEvent.VK_S:
            case KeyEvent.VK_DOWN:
                abajo = false;
                if (cliente != null) cliente.enviarMensaje("MOVIMIENTO:RELEASE:DOWN");
                break;
            case KeyEvent.VK_A:
            case KeyEvent.VK_LEFT:
                izquierda = false;
                if (cliente != null) cliente.enviarMensaje("MOVIMIENTO:RELEASE:LEFT");
                break;
            case KeyEvent.VK_D:
            case KeyEvent.VK_RIGHT:
                derecha = false;
                if (cliente != null) cliente.enviarMensaje("MOVIMIENTO:RELEASE:RIGHT");
                break;
            case KeyEvent.VK_E:
                accionMatar = false;
                if (cliente != null) cliente.enviarMensaje("ACCION:RELEASE:KILL");
                break;
            case KeyEvent.VK_F:
                accionVentilar = false;
                if (cliente != null) cliente.enviarMensaje("ACCION:RELEASE:VENT");
                break;
        }
    }
}