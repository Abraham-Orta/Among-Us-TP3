package com.amongus.project.vista; // paquete de la interfaz de usuario

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.amongus.project.controlador.ManejadorEntrada;
import com.amongus.project.modelo.EstadoJuego;
import com.amongus.project.modelo.Jugador;

/**
 * PantallaVotacion
 * ================
 * muestra la interfaz para que los jugadores voten por quién creen que es el impostor.
 * cuenta los votos y elimina al jugador con más votos al terminar el tiempo.
 *
 * cada instancia tiene su propia referencia a EstadoJuego y ManejadorEntrada,
 * para que múltiples ventanas no compartan estado.
 */
public class PantallaVotacion {

    private Rectangle botonSkip; // botón para omitir el voto
    
    // --- nuevas variables para el requerimiento 4 ---
    // aumentamos el tiempo a 60 segundos (3600 frames) para que los jugadores tengan tiempo
    private int temporizadorVotacion = 3600; 
    private boolean mostrandoResultados = false; // bandera para saber si ya estamos mostrando quién perdió
    private int temporizadorResultados = 600; // 600 frames = 10 segundos mostrando el mensaje final
    private String mensajeResultado = ""; // el texto que dirá quién fue expulsado
    private Jugador jugadorExpulsado = null; // jugador que ha sido expulsado

    // referencia al estado de juego y al manejador de entrada de esta ventana
    private EstadoJuego estadoJuego;
    private ManejadorEntrada manejadorEntrada;
    
    // guarda el tamaño de la pantalla para el cálculo de clics
    private int ultimoAnchoPanel = 800;
    private int ultimoAltoPanel = 600;

    // --- chat ---
    private List<String> historialChat = new ArrayList<>();
    private Rectangle areaBotonChat = new Rectangle(20, 20, 60, 60);
    private boolean chatAbierto = false;

    // caché de la fuente del juego
    private static Font fuenteBase;

    // --- variables para el efecto máquina de escribir ---
    private int letrasMostradas = 0;
    private int ticksParaLetra = 0; // contador para controlar la velocidad de la máquina de escribir

    // --- variables para el efecto parallax (estrellas) ---
    private List<Estrella> estrellas;
    private Random random = new Random();

    // clase interna para manejar las estrellas del fondo
    private class Estrella {
        float x, y;
        float velocidad;
        int tamano;

        Estrella(int ancho, int alto) {
            x = random.nextInt(ancho > 0 ? ancho : 800);
            y = random.nextInt(alto > 0 ? alto : 600);
            tamano = random.nextInt(3) + 1; // tamaño entre 1 y 3
            velocidad = tamano * 0.5f; // las estrellas más grandes se mueven más rápido
        }

        void actualizar(int ancho, int alto) {
            x -= velocidad;
            if (x < 0) {
                x = ancho;
                y = random.nextInt(alto > 0 ? alto : 600);
            }
        }

        void dibujar(Graphics2D g2) {
            // color blanco con opacidad según el tamaño (las pequeñas son más tenues)
            int alpha = 100 + (tamano * 50);
            if (alpha > 255) alpha = 255;
            g2.setColor(new Color(255, 255, 255, alpha));
            g2.fillRect((int)x, (int)y, tamano, tamano);
        }
    }

    public PantallaVotacion(EstadoJuego estadoJuego, ManejadorEntrada manejadorEntrada) {
        this.estadoJuego = estadoJuego;
        this.manejadorEntrada = manejadorEntrada;
        botonSkip = new Rectangle(50, 500, 150, 50); // posición y tamaño del botón skip inicial
        
        // inicializar las estrellas
        estrellas = new ArrayList<>();
        for (int i = 0; i < 150; i++) {
            estrellas.add(new Estrella(800, 600)); // tamaño inicial por defecto
        }
    }
    
    /**
     * carga la fuente personalizada del juego
     */
    private Font cargarFuente(float tamano) {
        if (fuenteBase == null) {
            try {
                String ruta = "in_your_face_joffrey/InYourFaceJoffrey.ttf";
                InputStream is = getClass().getClassLoader().getResourceAsStream(ruta);
                if (is == null) {
                    String[] rutas = {"src/main/resources/" + ruta, "resources/" + ruta};
                    for (String r : rutas) {
                        File f = new File(r);
                        if (f.exists()) { fuenteBase = Font.createFont(Font.TRUETYPE_FONT, f); break; }
                    }
                } else {
                    fuenteBase = Font.createFont(Font.TRUETYPE_FONT, is);
                }
            } catch (Exception e) {
                // silenciar error
            }
            if (fuenteBase == null) fuenteBase = new Font("Arial", Font.BOLD, 12);
        }
        return fuenteBase.deriveFont(tamano);
    }

    /**
     * resetea los temporizadores para que la votación empiece de cero la próxima vez.
     */
    public void reiniciarVotacion() {
        temporizadorVotacion = 3600;
        mostrandoResultados = false;
        temporizadorResultados = 600;
        mensajeResultado = "";
        jugadorExpulsado = null;
        letrasMostradas = 0;
        ticksParaLetra = 0;
        chatAbierto = false;
        historialChat.clear();
        if (manejadorEntrada != null) {
            manejadorEntrada.escribiendoChat = false;
            manejadorEntrada.entradaChat.setLength(0);
        }
    }

    /**
     * recibe un mensaje de chat y lo agrega al historial
     */
    public void recibirMensajeChat(String msj) {
        historialChat.add(msj);
        // mantener un limite de mensajes para no sobrecargar
        if (historialChat.size() > 15) {
            historialChat.remove(0);
        }
    }

    /**
     * se llama 60 veces por segundo mientras el estado del juego sea votacion.
     */
    public void actualizar() {
        // actualizar el fondo parallax siempre (durante la votación y resultados)
        for (Estrella e : estrellas) {
            e.actualizar(ultimoAnchoPanel, ultimoAltoPanel);
        }

        // 1. si estamos mostrando resultados (el veredicto final)
        if (mostrandoResultados) {
            temporizadorResultados--; // restamos tiempo
            
            // efecto de máquina de escribir: añadimos una letra cada cierto tiempo
            ticksParaLetra++;
            if (ticksParaLetra > 2) { // ajustar velocidad aquí (cada 2 frames = 1 letra)
                ticksParaLetra = 0;
                if (letrasMostradas < mensajeResultado.length()) {
                    letrasMostradas++;
                }
            }
            
            // si pasaron los 5 segundos de mostrar resultados
            if (temporizadorResultados <= 0) {
                estadoJuego.setFaseActual(EstadoJuego.Fase.JUGANDO); // volvemos al mapa
                reiniciarVotacion(); // limpiamos todo para el próximo reporte
            }
            return; // no hacemos nada más
        }

        // 2. si estamos en fase de votar
        temporizadorVotacion--; // el tiempo se acaba
        
        List<Jugador> jugadores = estadoJuego.getJugadores();
        
        // --- simulación de bots ELIMINADA para el multijugador real ---
        
        // 3. fin de votación por tiempo: forzamos el skip del jugador si no lo ha hecho
        if (temporizadorVotacion <= 0) {
            Jugador local = estadoJuego.getJugadorLocal();
            if (local != null && local.isVivo() && !local.yaVoto()) {
                local.votarSkip();
            }
            return;
        }

        // 4. leer el voto del jugador real (tú)
        Jugador jugadorLocal = estadoJuego.getJugadorLocal();
        if (jugadorLocal == null || !jugadorLocal.isVivo()) {
            return; // si estás muerto no puedes hacer clic en votación
        }

        // verificar clic izquierdo del ratón (usando la instancia del manejador, no static)
        if (manejadorEntrada.clickIzquierdo) {
            
            // si hizo clic en el icono del chat
            if (areaBotonChat.contains(manejadorEntrada.mouseX, manejadorEntrada.mouseY)) {
                ReproductorMusica.reproducirEfecto("UI_boton.wav");
                chatAbierto = !chatAbierto;
                manejadorEntrada.escribiendoChat = chatAbierto; // toggle
                manejadorEntrada.clickIzquierdo = false;
                return;
            }

            // Sync por si cerró el chat presionando ESCAPE
            if (chatAbierto && !manejadorEntrada.escribiendoChat) {
                chatAbierto = false;
            }

            // bloquear clicks de votación si el chat está abierto
            if (chatAbierto) {
                manejadorEntrada.clickIzquierdo = false; // ignorar clics detrás del chat
                return;
            }

            // si ya había votado antes, no dejamos votar de nuevo
            if (jugadorLocal.yaVoto()) return;
            
            // si hizo clic en el botón de skip
            if (botonSkip.contains(manejadorEntrada.mouseX, manejadorEntrada.mouseY)) {
                ReproductorMusica.reproducirEfecto("UI_boton.wav");
                jugadorLocal.votarSkip();
                manejadorEntrada.clickIzquierdo = false; // consumimos el clic para que no se presione dos veces
                return;
            }
            
            // si hizo clic en la tarjeta de algún jugador
            for (int i = 0; i < jugadores.size(); i++) {
                Jugador j = jugadores.get(i);
                if (!j.isVivo()) continue; // no se puede votar a los muertos
                
                Rectangle areaJugador = obtenerRectanguloJugador(i, jugadores.size(), ultimoAnchoPanel, ultimoAltoPanel);
                if (areaJugador.contains(manejadorEntrada.mouseX, manejadorEntrada.mouseY)) {
                    ReproductorMusica.reproducirEfecto("UI_boton.wav");
                    jugadorLocal.votarJugador(j); // enviamos el voto hacia esa persona
                    manejadorEntrada.clickIzquierdo = false;
                    return;
                }
            }
        }
    }
    
    /**
     * calcula la posición de la tarjeta de un jugador para que quede centrado en pantalla.
     */
    private Rectangle obtenerRectanguloJugador(int index, int totalJugadores, int anchoPanel, int altoPanel) {
        int columnas = Math.min(3, totalJugadores); // máximo 3 columnas para no saturar
        if (columnas == 0) columnas = 1;
        int filas = (int) Math.ceil((double) totalJugadores / columnas);
        
        int tarjetaAncho = 200;
        int tarjetaAlto = 60;
        int gapX = 30;
        int gapY = 30;
        
        int totalAnchoGrid = columnas * tarjetaAncho + (columnas - 1) * gapX;
        int totalAltoGrid = filas * tarjetaAlto + (filas - 1) * gapY;
        
        int startX = (anchoPanel - totalAnchoGrid) / 2;
        int startY = (altoPanel - totalAltoGrid) / 2 - 20; // un poco más arriba para dejar espacio al skip
        
        int fila = index / columnas;
        int col = index % columnas;
        
        int x = startX + col * (tarjetaAncho + gapX);
        int y = startY + fila * (tarjetaAlto + gapY);
        
        return new Rectangle(x, y, tarjetaAncho, tarjetaAlto);
    }
    
    /**
     * Recibe los resultados del servidor y activa la pantalla de animación.
     */
    public void mostrarResultadosVotacion(String mensaje, Jugador expulsado) {
        mensajeResultado = mensaje;
        jugadorExpulsado = expulsado;
        mostrandoResultados = true;
        letrasMostradas = 0;
        ticksParaLetra = 0;
        
        ReproductorMusica.reproducirEfectoRepetido("expulcion music.wav", 2);
    }

    /**
     * dibuja un botón con el estilo del juego (esquinas redondeadas y borde blanco o gris)
     */
    private void dibujarBotonEstiloAmongUs(Graphics2D g2, Rectangle rect, Color bgColor, Color borderColor) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        g2.setColor(bgColor);
        g2.fillRoundRect(rect.x, rect.y, rect.width, rect.height, 30, 30);
        
        g2.setColor(borderColor);
        g2.setStroke(new BasicStroke(3));
        g2.drawRoundRect(rect.x + 1, rect.y + 1, rect.width - 3, rect.height - 3, 30, 30);
    }

    /**
     * dibuja toda la interfaz de votación en la pantalla
     */
    public void render(Graphics g, int anchoPanel, int altoPanel) {
        this.ultimoAnchoPanel = anchoPanel;
        this.ultimoAltoPanel = altoPanel;
        
        Graphics2D g2 = (Graphics2D) g.create();
        
        // --- pantalla de resultados (cuando ya se votó) ---
        if (mostrandoResultados) {
            // fondo de espacio negro profundo para resaltar las estrellas
            g2.setColor(new Color(5, 5, 10));
            g2.fillRect(0, 0, anchoPanel, altoPanel);

            // dibujar estrellas parallax
            for (Estrella e : estrellas) {
                e.dibujar(g2);
            }

            // animar al jugador expulsado
            if (jugadorExpulsado != null) {
                int ticksAnim = 600 - temporizadorResultados;
                
                // frames del 1 al 6 (rotación infinita mientras dure la animación)
                int frame = 1 + ((ticksAnim / 15) % 6);
                
                String spritePath = "sprites/expulción/mainscreenCrew" + frame + ".png";
                BufferedImage spriteColoreado = PanelJuego.obtenerSpriteColoreado(spritePath, jugadorExpulsado.getColor(), "expulsion_" + frame);
                
                if (spriteColoreado != null) {
                    // animarlo flotando de izquierda a derecha y rotando
                    int xAnim = (int) ((-150 + ticksAnim * 3) * ((double) anchoPanel / 800));
                    int yAnim = altoPanel / 2 - 75; // centrado en el eje y aproximado
                    g2.drawImage(spriteColoreado, xAnim, yAnim, 150, 150, null);
                }
            }

            // dibujar texto con efecto máquina de escribir
            String textoMostrar = "";
            if (mensajeResultado != null && !mensajeResultado.isEmpty() && letrasMostradas > 0) {
                textoMostrar = mensajeResultado.substring(0, Math.min(letrasMostradas, mensajeResultado.length()));
            }
            
            g2.setFont(cargarFuente(28f)); // fuente un poco más grande
            FontMetrics fm = g2.getFontMetrics();
            int xCentro = (anchoPanel - fm.stringWidth(textoMostrar)) / 2; // centrar el texto actual
            
            // sombra del texto (más gruesa)
            g2.setColor(Color.BLACK);
            g2.drawString(textoMostrar, xCentro + 3, altoPanel / 2 + 3);
            g2.drawString(textoMostrar, xCentro + 2, altoPanel / 2 + 2);
            
            // texto en blanco (dibujado doble para simular negrita/grosor)
            g2.setColor(Color.WHITE);
            g2.drawString(textoMostrar, xCentro, altoPanel / 2);
            g2.drawString(textoMostrar, xCentro + 1, altoPanel / 2);
            
            g2.dispose();
            return; // salimos, no dibujamos botones
        }
        
        // --- pantalla normal de votar ---
        // dibujar imagen de fondo para la votación
        Image bgVotacion = PanelJuego.obtenerImagenFija("votacion.png");
        if (bgVotacion != null) {
            g2.drawImage(bgVotacion, 0, 0, anchoPanel, altoPanel, null);
        } else {
            // si no hay imagen, usar el fondo de estrellas
            g2.setColor(new Color(5, 5, 10));
            g2.fillRect(0, 0, anchoPanel, altoPanel);
            for (Estrella e : estrellas) {
                e.dibujar(g2);
            }
        }
        
        // velo oscuro encima del fondo para mejorar lectura
        g2.setColor(new Color(0, 0, 0, 100));
        g2.fillRect(0, 0, anchoPanel, altoPanel);

        g2.setColor(Color.WHITE);
        g2.setFont(cargarFuente(30f));
        FontMetrics fmTit = g2.getFontMetrics();
        // mostramos el tiempo restante convirtiendo los frames a segundos (/ 60) sin acentos ni interrogación invertida
        String tituloVot = "QUIEN ES EL IMPOSTOR? TIEMPO: " + (temporizadorVotacion / 60);
        g2.drawString(tituloVot, (anchoPanel - fmTit.stringWidth(tituloVot)) / 2, 60);
        
        List<Jugador> jugadores = estadoJuego.getJugadores();

        // dibuja la tarjeta de cada jugador centrándolas dinámicamente
        for (int i = 0; i < jugadores.size(); i++) {
            Jugador j = jugadores.get(i);
            Rectangle rect = obtenerRectanguloJugador(i, jugadores.size(), anchoPanel, altoPanel);
            
            boolean hover = rect.contains(manejadorEntrada.mouseX, manejadorEntrada.mouseY);
            
            // color de fondo y borde de la tarjeta con transparencia
            Color colorFondo;
            Color colorBorde;
            if (!j.isVivo()) {
                colorFondo = new Color(150, 0, 0, 150); // rojo oscuro transparente si está muerto
                colorBorde = new Color(100, 100, 100);
            } else if (hover) {
                colorFondo = new Color(60, 60, 60, 150); // gris transparente si pasas el ratón
                colorBorde = Color.WHITE; // borde blanco brillante
            } else {
                colorFondo = new Color(0, 0, 0, 150); // negro transparente por defecto
                colorBorde = Color.LIGHT_GRAY; // borde gris claro
            }
            
            // dibujar estilo botón para la tarjeta del jugador
            dibujarBotonEstiloAmongUs(g2, rect, colorFondo, colorBorde);
            
            // icono del jugador (cuadrado de su color)
            g2.setColor(j.getColor());
            g2.fillRect(rect.x + 15, rect.y + 10, 40, 40);
            
            // nombre del jugador
            Jugador local = estadoJuego.getJugadorLocal();
            if (j.isImpostor() && local != null && local.isImpostor()) {
                g2.setColor(Color.RED);
            } else {
                g2.setColor(Color.WHITE);
            }
            g2.setFont(cargarFuente(20f));
            g2.drawString(j.getNombre(), rect.x + 70, rect.y + 35);
            
            // marcar si ya votó (punto verde) sin acentos
            if (j.yaVoto()) {
                g2.setColor(Color.GREEN);
                g2.fillOval(rect.x + 175, rect.y + 25, 10, 10);
                g2.setFont(cargarFuente(14f));
                g2.drawString("VOTO", rect.x + 130, rect.y + 35);
            } else if (!j.isVivo()) {
                // etiqueta para muertos (con margen ajustado para que no se superponga al ícono)
                g2.setColor(Color.RED);
                g2.setFont(cargarFuente(16f));
                g2.drawString("MUERTO", rect.x + 120, rect.y + 50); 
            }
        }
        
        // actualizamos posicion del boton skip centrado abajo, un poco más arriba del texto
        botonSkip.width = 150;
        botonSkip.height = 50;
        botonSkip.x = (anchoPanel - botonSkip.width) / 2;
        botonSkip.y = altoPanel - 110;

        // --- botón de skip vote ---
        boolean hoverSkip = botonSkip.contains(manejadorEntrada.mouseX, manejadorEntrada.mouseY);
        Color colorFondoSkip = hoverSkip ? new Color(60, 60, 60, 150) : new Color(0, 0, 0, 150);
        Color colorBordeSkip = hoverSkip ? Color.WHITE : Color.LIGHT_GRAY;
        
        dibujarBotonEstiloAmongUs(g2, botonSkip, colorFondoSkip, colorBordeSkip);
        
        g2.setColor(Color.WHITE);
        g2.setFont(cargarFuente(20f));
        FontMetrics fmSkip = g2.getFontMetrics();
        int skipX = botonSkip.x + (botonSkip.width - fmSkip.stringWidth("SKIP VOTE")) / 2;
        int skipY = botonSkip.y + ((botonSkip.height - fmSkip.getHeight()) / 2) + fmSkip.getAscent();
        g2.drawString("SKIP VOTE", skipX, skipY);
        
        // --- mensaje inferior (estado local) ---
        Jugador local = estadoJuego.getJugadorLocal();
        if (local != null) {
            g2.setFont(cargarFuente(22f));
            FontMetrics fmLocal = g2.getFontMetrics();
            String msjStatus = "";
            if (!local.isVivo()) {
                msjStatus = "ESTAS MUERTO. NO PUEDES VOTAR.";
            } else if (local.yaVoto()) {
                msjStatus = "HAS VOTADO. ESPERANDO A LOS DEMAS...";
            } else {
                msjStatus = "HAZ CLIC EN UN JUGADOR O SKIP PARA VOTAR.";
            }
            int txtX = (anchoPanel - fmLocal.stringWidth(msjStatus)) / 2;
            int txtY = altoPanel - 30; // texto en la parte inferior

            // sombra del texto (negro, desplazado 2px)
            g2.setColor(Color.BLACK);
            g2.drawString(msjStatus, txtX + 2, txtY + 2);
            
            // texto amarillo encima
            g2.setColor(Color.YELLOW);
            g2.drawString(msjStatus, txtX, txtY);
        }
        
        // --- CHAT INTERFAZ ---
        dibujarInterfazChat(g2, anchoPanel, altoPanel);
        
        g2.dispose();
    }

    private void dibujarInterfazChat(Graphics2D g2, int anchoPanel, int altoPanel) {
        // Area del chat box general (arriba a la izquierda)
        int chatX = 20;
        int chatY = 20;
        int chatW = 300;
        
        // El boton de chat para toggler la ventana
        areaBotonChat.x = chatX;
        areaBotonChat.y = chatY;
        
        boolean hoverChatBtn = areaBotonChat.contains(manejadorEntrada.mouseX, manejadorEntrada.mouseY);
        Color chatBtnBg = hoverChatBtn ? new Color(60, 60, 60, 200) : new Color(30, 30, 30, 200);
        dibujarBotonEstiloAmongUs(g2, areaBotonChat, chatBtnBg, Color.WHITE);
        
        // icono de chat aproximado
        g2.setColor(Color.WHITE);
        g2.fillRoundRect(chatX + 15, chatY + 15, 30, 20, 10, 10);
        int[] px = {chatX + 20, chatX + 25, chatX + 30};
        int[] py = {chatY + 30, chatY + 45, chatY + 30};
        g2.fillPolygon(px, py, 3);
        
        // --- Si no está abierto, no dibujar el resto ---
        if (!chatAbierto) {
            return;
        }
        
        int boxHeight = 250;
        
        // Dibujamos el historial abajo del botón
        int histY = chatY + 70;
        
        // fondo del historial
        g2.setColor(new Color(0, 0, 0, 150));
        g2.fillRoundRect(chatX, histY, chatW, boxHeight, 15, 15);
        
        // Dibujar mensajes pasados
        g2.setFont(cargarFuente(28f));
        int msgY = histY + 25;
        Jugador local = estadoJuego.getJugadorLocal();
        boolean localIsImp = local != null && local.isImpostor();
        
        for (String msj : historialChat) {
            String autor = "";
            String contenido = msj;
            if (msj.contains(":")) {
                int splitIdx = msj.indexOf(':');
                autor = msj.substring(0, splitIdx).trim();
                contenido = msj.substring(splitIdx + 1).trim();
                if (contenido.startsWith("CHAT:")) {
                    contenido = contenido.substring(5).trim();
                }
            } else {
                autor = "Sistema";
            }
            
            // Comprobar si al autor se lo reconoce como impostor
            boolean msgIsImp = false;
            for (Jugador j : estadoJuego.getJugadores()) {
                if (j.getNombre().equals(autor) && j.isImpostor()) {
                    msgIsImp = true; break;
                }
            }

            if (msgIsImp && localIsImp) g2.setColor(Color.RED);
            else g2.setColor(Color.CYAN);
            
            g2.drawString(autor + ":", chatX + 10, msgY);
            
            g2.setColor(Color.WHITE);
            g2.drawString(contenido, chatX + 15 + g2.getFontMetrics().stringWidth(autor + ":"), msgY);
            msgY += 24; // Espaciado entre mensajes más grande por el tamaño de fuente
        }

        // Mostrar recuadro de texto de escritura si está activo
        if (manejadorEntrada.escribiendoChat) {
            int inputY = histY + boxHeight + 10;
            g2.setColor(new Color(0, 0, 0, 200));
            g2.fillRoundRect(chatX, inputY, chatW, 40, 10, 10);
            g2.setColor(Color.WHITE);
            g2.drawRoundRect(chatX, inputY, chatW, 40, 10, 10);
            
            g2.setFont(cargarFuente(22f));
            String txt = manejadorEntrada.entradaChat.toString();
            // Cursor parpadeante
            if ((System.currentTimeMillis() / 500) % 2 == 0) txt += "|";
            g2.drawString(txt, chatX + 10, inputY + 25);
            
            g2.setColor(Color.GRAY);
            g2.setFont(cargarFuente(14f));
            g2.drawString("PRESIONA ENTER PARA ENVIAR O ESC PARA CANCELAR", chatX, inputY + 55);
        }
    }
}