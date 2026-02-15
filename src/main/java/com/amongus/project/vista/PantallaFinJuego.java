package com.amongus.project.vista;  // Define el paquete de la interfaz de usuario

import javax.swing.*; // Importa componentes de Swing (ventanas, botones, diálogos)
import java.awt.*;   // Importa componentes AWT (colores, fuentes, layouts)

/**
 * PantallaFinJuego
 * Es un cuadro de diálogo (JDialog) que salta al terminar la partida
 * Bloquea la ventana de atrás hasta que le das "Aceptar"
 */
public class PantallaFinJuego extends JDialog {// Hereda de JDialog para diálogo modal

    /**
     * Constructor de la pantalla
     * @param ventanaPadre Quien me invocó (el MenuPrincipal o el Juego).
     * @param mensajeGanador El texto que dice quién ganó.
     */
    public PantallaFinJuego(JFrame ventanaPadre, String mensajeGanador) {
        // "super" llama al constructor de la clase padre (JDialog).
        // true = MODAL (Bloquea la ventana padre).
        super(ventanaPadre, "Fin de la Partida", true);
        
        // Configuramos tamaño y posición
        setSize(400, 200);// Tamaño del diálogo: 400x200 píxeles
        // Centrar respecto a la ventana padre.
        setLocationRelativeTo(ventanaPadre);//Centra el diálogo sobre la ventana padre
        // Liberar memoria al cerrar.
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE); // Elimina el diálogo al cerrar

        // Panel principal con un borde para que no se vea todo pegado.
        JPanel panelPrincipal = new JPanel(new BorderLayout()); // Layout con 5 zonas
        panelPrincipal.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));// Márgenes internos
        
        // Etiqueta grande con el texto del ganador.
        JLabel etiquetaGanador = new JLabel(mensajeGanador, SwingConstants.CENTER);// Texto centrado
        etiquetaGanador.setFont(new Font("Arial", Font.BOLD, 22));  // Fuente grande y negrita
        
        // se Colorea el texto según quién ganó.
        // se convierte a minúsculas (.toLowerCase) para comparar sin problemas.
        if (mensajeGanador.toLowerCase().contains("impostor")) {
            etiquetaGanador.setForeground(Color.RED); // Rojo sangre para los malos.
        } else {
            // Azul cian para los tripulantes buenos.
            etiquetaGanador.setForeground(new Color(0, 150, 255));  // RGB: rojo=0, verde=150, azul=255
        }
        
        // Añadimos la etiqueta al centro.
        panelPrincipal.add(etiquetaGanador, BorderLayout.CENTER);  // Posición central del layout

        // Creamos un botón de Aceptar.
        JButton botonAceptar = new JButton("Aceptar");  // Botón con texto "Aceptar"
        // Le decimos que al hacer click cierre esta ventana (dispose).
        botonAceptar.addActionListener(evento -> dispose());  // Lambda que cierra el diálogo
        
        // Panel pequeño para contener el botón al sur.
        JPanel panelBoton = new JPanel();  // Panel con FlowLayout por defecto (centrado)
        panelBoton.add(botonAceptar);  // Añade el botón al panel

        panelPrincipal.add(panelBoton, BorderLayout.SOUTH);  // Añade panel del botón en zona sur

        // Agregamos todo el panel armado a la ventana.
        add(panelPrincipal);// Añade panel principal al diálogo
    }
}
