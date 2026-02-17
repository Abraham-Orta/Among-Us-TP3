package com.amongus.project;

import java.awt.Color;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import com.amongus.project.controlador.BucleJuego;
import com.amongus.project.modelo.EstadoJuego;
import com.amongus.project.modelo.Jugador;
import com.amongus.project.vista.PanelJuego;

public class PruebaDirecta {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            // 1. Configurar estado del juego
            EstadoJuego estado = EstadoJuego.getInstancia();
            estado.setFaseActual(EstadoJuego.Fase.JUGANDO);
            
            // 2. Crear un jugador de prueba (nosotros)
            Jugador jugadorTest = new Jugador("Tester", 100, 100, Color.RED, false);
            estado.setJugadorLocal(jugadorTest);
            
            // 3. Crear ventana y panel
            JFrame frame = new JFrame("Among Us - PRUEBA DIRECTA");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            
            PanelJuego panel = new PanelJuego();
            frame.add(panel);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
            
            // 4. Iniciar bucle
            BucleJuego bucle = new BucleJuego(panel);
            bucle.iniciar();
            
            panel.requestFocus();
            
            System.out.println("Prueba directa iniciada con jugador 'Tester'");
        });
    }
}
