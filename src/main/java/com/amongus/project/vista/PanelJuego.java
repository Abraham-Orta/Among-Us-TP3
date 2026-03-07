package com.amongus.project.vista; // Paquete de la interfaz de usuario

import javax.swing.JPanel; // Clase base para lienzos de dibujo en Java Swing
import java.awt.Graphics;  // Proporciona herramientas básicas de dibujo
import java.awt.Graphics2D; // Versión avanzada de Graphics para transformaciones (como la cámara)
import java.awt.Color;     // Para usar colores
import java.awt.Dimension; // Para establecer el tamaño del panel
import java.awt.RadialGradientPaint; // Para el efecto de luz/sombra
import java.awt.Paint; // Para guardar el pincel original
import java.awt.geom.Point2D; // Para el centro del gradiente
import com.amongus.project.modelo.EstadoJuego; // Para obtener datos actuales del juego
import com.amongus.project.modelo.Jugador;     // Para obtener info de cada jugador
import com.amongus.project.modelo.Mapa;        // Para dibujar el entorno
import com.amongus.project.controlador.ManejadorEntrada; // Para conectar los controles

/**
 * PanelJuego
 * ==========
 * Esta es la pantalla principal mientras la partida está activa.
 * Hereda de JPanel, que es básicamente un lienzo en blanco donde nosotros
 * vamos a pintar el mapa, los jugadores, los cadáveres y la cámara frame por frame.
 */
public class PanelJuego extends JPanel {
    
    // Objeto que representa la pantalla superpuesta cuando hay una reunión
    private PantallaVotacion pantallaVotacion;

    /**
     * Constructor: Configura el lienzo al abrirse la ventana.
     */
    public PanelJuego() {
        // Establecemos un tamaño ideal para el área de juego
        setPreferredSize(new Dimension(800, 600));
        setBackground(Color.BLACK); // Fondo negro de emergencia por si el mapa falla
        setFocusable(true); // Muy importante: permite que el panel detecte cuando presionamos teclas
        
        // Conectamos nuestro controlador de teclado (ManejadorEntrada) al panel
        addKeyListener(new ManejadorEntrada());
        
        // Instanciamos el controlador interno de ratón y lo conectamos
        ManejadorEntrada.MouseHandler mouseHandler = new ManejadorEntrada.MouseHandler();
        addMouseListener(mouseHandler);       // Escucha clics
        addMouseMotionListener(mouseHandler); // Escucha el movimiento del ratón
        
        // Inicializamos la pantalla de votación (para cuando reporten un cuerpo)
        this.pantallaVotacion = new PantallaVotacion();
    }
    
    public PantallaVotacion getPantallaVotacion() {
        return pantallaVotacion;
    }
    
    /**
     * paintComponent
     * ==============
     * Este es el "corazón visual" del juego. Se llama automáticamente docenas
     * de veces por segundo para repintar la pantalla (gracias al BucleJuego).
     * 
     * @param g La "brocha" que Java nos da para dibujar cuadrados, líneas e imágenes.
     */
    @Override
    protected void paintComponent(Graphics g) {
        // Llamar siempre a super.paintComponent(g) para que Swing limpie el frame anterior
        super.paintComponent(g);
        
        // Obtenemos cómo está el juego ahora mismo (quién juega, en qué fase estamos)
        EstadoJuego estado = EstadoJuego.getInstancia();
        EstadoJuego.Fase fase = estado.getFaseActual();
        
        // Si estamos en medio de una votación, NO dibujamos el mapa,
        // le pasamos la brocha a la pantalla de Votación para que ella dibuje y salimos.
        if (fase == EstadoJuego.Fase.VOTACION) {
            pantallaVotacion.render(g);
            return; 
        }
        
        // --- LÓGICA DE CÁMARA (VISTA 2D SCROLLING) ---
        // La cámara debe seguir a NUESTRO jugador. En lugar de mover al jugador en la pantalla,
        // dejamos al jugador en el centro y movemos TODO EL MUNDO en dirección contraria.
        int camX = 0;
        int camY = 0;
        
        Jugador local = estado.getJugadorLocal();
        Mapa mapa = estado.getMapa();
        
        if (local != null && mapa != null) {
            // Calculamos dónde debe estar la esquina superior izquierda de la cámara
            // Fórmula: Posición del jugador - (Mitad del ancho de la ventana)
            camX = local.getX() - (getWidth() / 2);
            camY = local.getY() - (getHeight() / 2);
            
            // RESTRICCIÓN DE CÁMARA: No queremos ver el vacío negro fuera del mapa
            // Si la cámara se sale por la izquierda (menor a 0), la pegamos al borde (0)
            if (camX < 0) camX = 0;
            if (camY < 0) camY = 0;
            
            // Si la cámara se sale por la derecha, la pegamos al máximo límite (AnchoMapa - AnchoVentana)
            if (camX > mapa.getAncho() - getWidth()) camX = mapa.getAncho() - getWidth();
            if (camY > mapa.getAlto() - getHeight()) camY = mapa.getAlto() - getHeight();
        }
        
        // Usamos Graphics2D para tener acceso a traslaciones avanzadas
        Graphics2D g2d = (Graphics2D) g;
        
        // APLICAMOS LA CÁMARA: Desplazamos el lienzo entero en negativo.
        // Esto crea el efecto de que la cámara se movió sobre el mapa.
        g2d.translate(-camX, -camY);
        
        // DIBUJAR CAPA 1: EL MAPA Y ALCANTARILLAS
        // Le pasamos la brocha al mapa para que dibuje su imagen de fondo y las vías
        if (mapa != null) {
            mapa.render(g);
        }
        
        // DIBUJAR CAPA 2: LOS JUGADORES Y LOS PARALIZADOS
        for (Jugador j : estado.getJugadores()) {
            dibujarTripulante(g, j); // Dibujamos el personaje físico
            
            // Dibujamos el nombre de usuario flotando encima de su cabeza
            g.setColor(Color.WHITE);
            // x, y-10 pone el texto arribita del casco
            g.drawString(j.getNombre(), j.getX(), j.getY() - 10);
        }
        
        // Seguridad: A veces en pruebas locales el jugador host no está en la lista general.
        // Si es el caso, nos aseguramos de dibujarlo igual.
        if (local != null && !estado.getJugadores().contains(local)) {
             dibujarTripulante(g, local);
             g.setColor(Color.WHITE);
             g.drawString(local.getNombre(), local.getX(), local.getY() - 10);
        }

        // --- CAPA 3: NIEBLA DE GUERRA (CAMPO VISUAL) ---
        // Dibujamos la oscuridad encima de todo (mapa y jugadores), excepto la interfaz
        if (local != null && mapa != null) {
            dibujarCampoVisual(g2d, local, mapa.getAncho(), mapa.getAlto());
        }

        // DESHACER LA CÁMARA: Revertimos la traslación para volver al (0,0) real de la pantalla.
        // Esto sirve para dibujar cosas fijas de la UI (Interfaz de Usuario) como mapas minimizados o botones
        // que no deben moverse aunque el jugador camine.
        g2d.translate(camX, camY);
        
        // Si por alguna razón la sala está vacía y no arrancó bien, mostramos una advertencia fija en pantalla
        if (estado.getJugadores().isEmpty() && local == null) {
            g.setColor(Color.WHITE);
            g.drawString("No hay jugadores. Inicia partida.", 300, 300);
        }
    }

    /**
     * Dibuja una capa de oscuridad con un agujero de luz alrededor del jugador.
     * El tamaño del agujero depende del rol (Impostor ve más).
     */
    private void dibujarCampoVisual(Graphics2D g2d, Jugador local, int anchoMapa, int altoMapa) {
        // 1. Definir Radio de Visión según el rol y el estado de las luces
        float radioVision;
        
        if (local.isImpostor()) {
            // El impostor SIEMPRE ve bien (visión nocturna)
            radioVision = 350.0f;
        } else {
            // Si eres Tripulante, depende del sabotaje
            if (EstadoJuego.getInstancia().areLucesSaboteadas()) {
                radioVision = 50.0f; // ¡APAGÓN! Casi ciego
            } else {
                radioVision = 180.0f; // Normal
            }
        }
        
        // 2. Calcular el centro de la luz (centro del personaje)
        // El personaje mide aprox 30x40, así que sumamos la mitad
        float centroX = local.getX() + 15;
        float centroY = local.getY() + 20;

        // 3. Configurar el Gradiente Radial
        // Distancias: 0.0 (centro) -> 0.7 (empieza a oscurecer) -> 1.0 (oscuridad total)
        float[] distancias = {0.0f, 0.7f, 1.0f};
        
        // Colores (Alpha 0 es transparente, Alpha 255 es negro sólido)
        Color[] colores = {
            new Color(0, 0, 0, 0),   // Centro: Totalmente visible
            new Color(0, 0, 0, 120), // Transición: Penumbra
            new Color(0, 0, 0, 255)  // Exterior: Oscuridad total (Pitch Black)
        };

        // Prevenir errores si el radio es inválido
        if (radioVision <= 0) radioVision = 1.0f;

        // Creamos el pincel de gradiente
        RadialGradientPaint gradiente = new RadialGradientPaint(
            new Point2D.Float(centroX, centroY),
            radioVision,
            distancias,
            colores
        );

        // 4. Dibujar
        Paint pincelOriginal = g2d.getPaint(); // Guardamos el pincel anterior
        g2d.setPaint(gradiente); // Usamos nuestro gradiente de luz
        
        // Dibujamos un rectángulo gigante que cubre todo el mapa
        g2d.fillRect(0, 0, anchoMapa, altoMapa);

        g2d.setPaint(pincelOriginal); // Restauramos el pincel para lo siguiente
    }

    /**
     * Dibuja pixel art aproximado de un personaje de Among Us (o su versión paralizada).
     * 
     * @param g La brocha gráfica.
     * @param j El jugador que contiene los datos (X, Y, Vivo/Paralizado, Dirección).
     */
    private void dibujarTripulante(Graphics g, Jugador j) {
        int x = j.getX();
        int y = j.getY();
        int w = 30; // Ancho base
        int h = 40; // Alto base del cuerpo
        
        // Obtenemos a dónde mira (1=derecha, -1=izquierda) para voltear el dibujo
        int dir = j.getDireccion(); 
        if (dir == 0) dir = 1; // Si está estático, asume que mira a la derecha
        
        // --- REQUISITO: JUGADOR INHABILITADO / PARALIZADO ---
        if (!j.isVivo()) {
            // Si el jugador fue tocado por el impostor, lo dibujamos tirado en el piso, 
            // más oscurecido y achatado, con un letrero arriba.
            
            g.setColor(j.getColor().darker().darker()); // Color marchito/apagado
            
            // Dibujamos un rectángulo aplastado en el suelo simulando estar tirado
            g.fillRoundRect(x, y + 25, w + 15, h / 2, 10, 10); 
            
            // Hueso asomando (típico de Among Us)
            g.setColor(Color.WHITE);
            g.fillOval(x + 15, y + 20, 10, 10);
            
            // Etiqueta visual para depuración
            g.setColor(Color.RED);
            g.drawString("PARALIZADO", x - 10, y - 5);
            
            return; // Termina la función aquí. No dibujamos piernas ni visor.
        }
        
        // --- JUGADOR VIVO ---
        
        // Seleccionamos el color del jugador
        g.setColor(j.getColor());
        
        // DIBUJAR MOCHILA
        int mochilaW = 10;
        int mochilaH = 25;
        if (dir == 1) { // Mira Derecha -> Mochila a la izquierda
            g.fillRect(x - 5, y + 10, mochilaW, mochilaH);
        } else { // Mira Izquierda -> Mochila a la derecha
            g.fillRect(x + w - 5, y + 10, mochilaW, mochilaH);
        }
        
        // DIBUJAR CUERPO (Cápsula principal)
        g.fillRoundRect(x, y, w, h, 15, 15);
        
        // DIBUJAR PIERNAS
        g.fillRect(x, y + h - 5, 10, 15); // Pierna izquierda
        g.fillRect(x + w - 10, y + h - 5, 10, 15); // Pierna derecha
        
        // DIBUJAR VISOR (Gafas)
        g.setColor(new Color(150, 200, 220)); // Color celeste/Grisáceo claro
        int visorW = 18;
        int visorH = 12;
        if (dir == 1) { // Mirando derecha
            // Dibujamos el visor tirado hacia la derecha
            g.fillRoundRect(x + 15, y + 10, visorW, visorH, 5, 5);
        } else { // Mirando izquierda
            // Dibujamos el visor tirado hacia la izquierda
            g.fillRoundRect(x - 3, y + 10, visorW, visorH, 5, 5);
        }
        
        // --- MODO DESARROLLADOR: DIBUJAR HITBOX DEL JUGADOR ---
        if (ManejadorEntrada.modoDesarrollador) {
            g.setColor(Color.GREEN);
            // La hitbox real del jugador, que se usa para colisiones y matar
            g.drawRect(j.getHitbox().x, j.getHitbox().y, j.getHitbox().width, j.getHitbox().height);
        }
        
        // Si eres tú (jugador local) y eres Impostor, dibujamos tu nombre en ROJO 
        // y mostramos un pequeño texto de ayuda visual
        Jugador local = EstadoJuego.getInstancia().getJugadorLocal();
        if (j == local && j.isImpostor()) {
            g.setColor(Color.RED);
            g.drawString(j.getNombre(), x, y - 10); // Sobrescribe el nombre blanco anterior
            
            // Indicador de controles debajo del personaje
            g.setColor(Color.ORANGE);
            g.drawString("[Q] Paralizar | [E] Alcantarilla", x - 30, y + 65);
        }
    }
}
