# gAbilities

Un plugin de Minecraft desarrollado para la gestión de "Partner Items" en servidores de combate PvP.

## Descripción
**gAbilities** es un sistema de objetos especiales diseñado para mejorar la experiencia de juego en servidores PvP. Este proyecto fue desarrollado con fines educativos, permitiendo la implementación de ítems con habilidades tácticas, cooldowns globales/individuales y sistemas de gestión de combate.

## Compatibilidad
* **Versión probada:** Spigot/Paper 1.8.8.
* **Nota:** El plugin no ha sido probado en versiones superiores; por lo tanto, no se garantiza su funcionamiento en otros ejecutables o versiones.

## Características
* **Arquitectura Orientada a Objetos:** Código modular para facilitar la comprensión y modificación.
* **Gestión de Cooldowns:** Soporte para tiempos de espera globales e individuales por ítem.
* **Sistema de Hits:** Lógica de detección de golpes (`HitCounter`) para activar efectos condicionales.
* **Personalización:** Configuración completa de mensajes, nombres y propiedades a través de `config.yml`.
* **Código Abierto:** Eres libre de descargar el proyecto y realizar las modificaciones que desees.

## Ítems Disponibles
* **Vampire:** Roba efectos al enemigo tras una secuencia de golpes.
* **Mixer:** Desordena la hotbar del oponente.
* **Anti Fall:** Protección temporal contra daño por caída.
* **Helmet Remover:** Remueve el casco del enemigo temporalmente.
* **Rocket:** Impulso vertical para movilidad aérea.
* **Snowball:** Aplica efectos negativos (Slow, Blindness, Confusion).
* **Switcher:** Intercambia posiciones con el enemigo.

## Comandos
* `/abilities help` - Muestra el menú de ayuda. (`abilities.admin`)
* `/abilities reload` - Recarga la configuración del plugin. (`abilities.admin`)
* `/abilities give <jugador> <item> [cant]` - Otorga un ítem especial a un jugador. (`abilities.admin`)
* `/abilities list` - Lista todos los ítems disponibles. (`abilities.admin`)
* `/abilities resetcd <jugador> [item]` - Limpia los cooldowns de un jugador. (`abilities.admin`)

## Notas de Desarrollo
* **Entorno de desarrollo:** Spring Tool Suite 4.
* **JDK:** 21
* **Target:** Java 1.8
* **Estado:** El plugin cumple con los objetivos funcionales planteados inicialmente. Por este motivo, es poco probable que reciba actualizaciones o soporte a largo plazo.

## Instalación
1. Descarga el archivo `.jar`.
2. Colócalo en la carpeta `/plugins` de tu servidor (1.8.8).
3. Inicia el servidor para generar los archivos de configuración.
4. Ajusta los valores en `config.yml` y reinicia el servidor.

---
*Proyecto desarrollado con fines educativos. Se invita a la comunidad a explorar el código y realizar las mejoras o adaptaciones necesarias para sus propios proyectos.*
