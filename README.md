# Among Us Clone (Java)

Simulación del juego *Among Us* implementada en Java utilizando Swing y una arquitectura MVC personalizada. Este proyecto fue desarrollado como parte de la asignatura "Técnicas de Programación III".

## 🚀 Características
- **Motor Gráfico Propio**: Renderizado de personajes y mapa utilizando `Graphics2D` y doble buffer.
- **Sistema de Minijuegos**: Tareas interactivas completas (Cables, Tarjetas, Asteroides, etc.).
- **Arquitectura MVC**: Separación clara entre `Modelo` (Lógica), `Vista` (Renderizado) y `Controlador` (Input).
- **Modos de Juego**:
  - **Local**: Hospeda tu propia partida.
  - **En Red**: Conéctate a un servidor (en desarrollo).

## 📋 Requisitos
- **Java JDK 17** o superior.
- **Maven** 3.6+ (para gestionar dependencias y compilación).

## 🛠️ Instalación y Ejecución

### Desde la Terminal
1.  **Clonar el repositorio**:
    ```bash
    git clone https://github.com/tu-usuario/AmongUsProject.git
    cd AmongUsProject
    ```

2.  **Compilar**:
    ```bash
    mvn clean install
    ```

3.  **Ejecutar**:
    ```bash
    mvn exec:java -Dexec.mainClass="com.amongus.project.Principal"
    ```

### Desde IDE (IntelliJ, Eclipse, NetBeans)
1.  Abrir el proyecto como **Proyecto Maven**.
2.  Esperar a que se descarguen las dependencias.
3.  Buscar la clase `com.amongus.project.Principal`.
4.  Ejecutar el método `main()`.

## 📂 Estructura del Proyecto

El código está organizado en:

```text
src/main/java/com/amongus/project/
├── modelo/          # Lógica del juego (Jugador, EstadoJuego)
├── vista/           # Interfaz Gráfica (Ventanas, Paneles)
│   └── tareas/      # Minijuegos interactivos
├── controlador/     # Lógica de Control (Teclado, Game Loop)
└── red/             # Comunicación Cliente-Servidor
```

## 🎮 Controles
- **WASD**: Mover al personaje.
- **Mouse**: Interactuar con tareas y menús.

## 👥 Créditos
Desarrollado por [Tu Nombre / Equipo] para la asignatura de Técnicas de Programación III.
