package com.amongus.project; // Paquete raíz del proyecto

import com.amongus.project.vista.MenuPrincipal; // Importamos la clase de nuestra interfaz
import javax.swing.SwingUtilities; // Herramienta para manejar hilos de interfaz gráfica
import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.MouseEvent;
import javax.swing.JButton;

/**
 * Clase Principal
 */
public class Principal {// Clase pública principal del proyecto


    public static void main(String[] args) { // Punto de entrada prin
        // cipal de la aplicación
        
        // Agregar un escuchador global para que suene un efecto en CADA botón del juego (Swing)
        Toolkit.getDefaultToolkit().addAWTEventListener(new AWTEventListener() {
            public void eventDispatched(AWTEvent e) {
                if (e.getID() == MouseEvent.MOUSE_PRESSED) {
                    MouseEvent me = (MouseEvent) e;
                    Component c = me.getComponent();
                    // Si el componente clickeado es un botón de Swing, suena el click
                    if (c instanceof JButton) {
                        com.amongus.project.vista.ReproductorMusica.reproducirEfecto("UI_boton.wav");
                    }
                }
            }
        }, AWTEvent.MOUSE_EVENT_MASK);

        // "SwingUtilities.invokeLater" es una buena práctica obligatoria.
        // Significa: "Por favor, crea la ventana en el hilo dedicado a los gráficos,
        // no en el hilo principal, para que no se trabe".
        SwingUtilities.invokeLater(() -> { // Ejecuta código en el Event Dispatch Thread (EDT)
            
            // Creamos una nueva instancia de nuestro menú principal.
            MenuPrincipal menu = new MenuPrincipal();// Crea objeto del menú principal
            

            menu.setVisible(true);// Hace visible la ventana del menú principal
            
        });
    }
}
