package com.amongus.project.modelo;

import java.awt.Color; // Para colores graficos
import java.awt.Graphics; // Para dibujar elementos graficos en pantalla
import java.awt.Point; // Para representar coordenadas (x, y) de destino
import java.awt.Rectangle; // Para las cajas de colision (hitboxes)
import java.util.ArrayList; // Para instanciar listas dinamicas
import java.util.List; // Interfaz principal de listas

public class Mapa { // Clase publica Mapa
    private int ancho, alto; // Dimensiones de nuestro escenario
    private List<Rectangle> obstaculos; // Lista con todas las paredes fisicas
    private List<Conducto> conductos; // Lista con las alcantarillas (vias rapidas)

    public Mapa() { // Constructor de la clase Mapa
        this.ancho = 800; // Asignamos el ancho inicial (Tamaño del panel por defecto)
        this.alto = 600; // Asignamos el alto inicial
        this.obstaculos = new ArrayList<>(); // Inicializamos la memoria de la lista de obstaculos
        this.conductos = new ArrayList<>(); // Inicializamos la memoria de la lista de conductos de ventilacion
        crearMapaPrueba(); // Ejecutamos el metodo que llena el mapa con objetos
    }

    private void crearMapaPrueba() { // Metodo que coloca los elementos fijos de prueba en el mapa
        // Bordes (Paredes maestras que encierran el cuarto)
        obstaculos.add(new Rectangle(0, 0, ancho, 20)); // Añadimos la pared de Arriba
        obstaculos.add(new Rectangle(0, alto - 20, ancho, 20)); // Añadimos la pared de Abajo
        obstaculos.add(new Rectangle(0, 0, 20, alto)); // Añadimos la pared de la Izquierda
        obstaculos.add(new Rectangle(ancho - 20, 0, 20, alto)); // Añadimos la pared de la Derecha

        // Obstaculos internos (Ejemplo: mesas, cajas, columnas que bloquean paso)
        obstaculos.add(new Rectangle(200, 200, 100, 100)); // Una caja grande bloqueando en medio
        obstaculos.add(new Rectangle(500, 100, 50, 300)); // Una pared vertical separadora
        obstaculos.add(new Rectangle(100, 450, 200, 50)); // Una pared horizontal en la parte de abajo

        // Vias de acceso rapido (Alcantarillas para el punto 3 de los requerimientos)
        // Conectamos dos puntos del mapa lejanos entre si
        conductos.add(new Conducto(new Rectangle(100, 100, 40, 40), new Point(600, 100))); // Conducto izquierdo que transporta al lado derecho
        conductos.add(new Conducto(new Rectangle(600, 100, 40, 40), new Point(100, 100))); // Conducto derecho que transporta de regreso al izquierdo
    }

    public void render(Graphics g) { // Metodo responsable de dibujar todos los elementos graficos fijos
        g.setColor(Color.GRAY); // Define color gris base
        // Dibujar suelo (opcional, por ahora usamos el fondo negro del panel general)
        
        g.setColor(Color.DARK_GRAY); // Selecciona un tono mas oscuro para dar volumen a las paredes
        for (Rectangle obs : obstaculos) { // Bucle que recorre cada pared de la lista
            g.fillRect(obs.x, obs.y, obs.width, obs.height); // Rellena el rectangulo solido en pantalla
        }
        
        // Dibujar las vias de ventilacion (Alcantarillas)
        for (Conducto c : conductos) { // Bucle que recorre cada conducto activo
            g.setColor(new Color(50, 50, 50)); // Ponemos un color gris casi negro para simular profundidad
            g.fillRect(c.area.x, c.area.y, c.area.width, c.area.height); // Pintamos el hueco interior
            g.setColor(Color.LIGHT_GRAY); // Seleccionamos gris claro para los barrotes
            g.drawRect(c.area.x, c.area.y, c.area.width, c.area.height); // Dibujamos el contorno
            // Tres lineas horizontales para que parezca una rejilla realista
            g.drawLine(c.area.x, c.area.y + 10, c.area.x + c.area.width, c.area.y + 10);
            g.drawLine(c.area.x, c.area.y + 20, c.area.x + c.area.width, c.area.y + 20);
            g.drawLine(c.area.x, c.area.y + 30, c.area.x + c.area.width, c.area.y + 30);
        }
    }

    public boolean hayColision(Rectangle rectFuturo) { // Metodo matematico para saber si el jugador chocara con una pared
        for (Rectangle obs : obstaculos) { // Analiza cada bloque en el mapa
            if (obs.intersects(rectFuturo)) { // Compara si la posicion futura se solapa con una pared existente
                return true; // Efectivamente hay colision, devolvemos verdadero impidiendo el paso
            }
        }
        return false; // Al terminar de revisar sin chocar con nada, el camino esta libre
    }
    
    public Point verificarVentilacion(Rectangle hitboxJugador) { // Metodo que revisa si el jugador esta parado encima de una via rapida
        for (Conducto c : conductos) { // Recorremos todos los conductos disponibles en el mapa
            if (c.area.intersects(hitboxJugador)) { // Si la cajita de colision del jugador toca la zona de la alcantarilla
                return c.destino; // Devolvemos el Point de coordenadas a donde debe salir disparado
            }
        }
        return null; // Si no esta pisando ningun conducto, devolvemos vacio
    }
    
    public List<Rectangle> getObstaculos() { // Getter basico
        return obstaculos; // Retorna los obstaculos
    }
    
    // Clase interna anidada para definir que es un conducto de ventilacion
    public static class Conducto { // Clase Conducto
        public Rectangle area; // Define fisicamente donde esta la entrada en el suelo (x, y, ancho, alto)
        public Point destino; // Define magicamente a que otra coordenada de la pantalla nos llevara (x, y)
        
        public Conducto(Rectangle area, Point destino) { // Constructor del conducto
            this.area = area; // Asignamos el area local
            this.destino = destino; // Asignamos la coordenada destino
        }
    }
}
