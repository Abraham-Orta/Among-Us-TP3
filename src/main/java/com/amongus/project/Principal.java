package com.amongus.project; // Paquete raíz del proyecto

import com.amongus.project.vista.MenuPrincipal; // Importamos la clase de nuestra interfaz
import javax.swing.SwingUtilities; // Herramienta para manejar hilos de interfaz gráfica

/**
 * Clase Principal
 */
public class Principal {// Clase pública principal del proyecto


    public static void main(String[] args) { // Punto de entrada prin
        // cipal de la aplicación
        
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
