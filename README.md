# Abilities Plugin

Un plugin de Minecraft desarrollado en Java para servidores tipo Practice/HCF, enfocado en mecánicas de combate dinámicas mediante el uso de "Partner Items".

## Descripción
Abilities es un sistema de gestión de objetos especiales diseñado para mejorar la experiencia de PvP en servidores. El plugin permite a los administradores otorgar ítems únicos con habilidades tácticas, cooldowns globales e individuales, y sistemas de gestión de combate.

## Características
* **Arquitectura Orientada a Objetos:** Código limpio y modular para un fácil mantenimiento.
* **Gestión de Cooldowns:** Soporte para cooldowns globales e individuales por ítem.
* **Sistema de Hits:** Lógica avanzada de detección de golpes (`HitCounter`) para activar efectos condicionales.
* **Personalización:** Mensajes, nombres de ítems y propiedades configurables a través de `config.yml`.
* **Seguridad:** Gestión de permisos (`abilities.admin`) integrada.

## Ítems
* **Vampire:** Roba efectos al enemigo tras una secuencia de golpes.
* **Mixer:** Desordena la hotbar del oponente.
* **Anti Fall:** Protección temporal contra daño por caída.
* **Helmet Remover:** Remueve el casco del enemigo.
* **Rocket:** Impulso vertical para movilidad.
* **Snowball:** Aplica efectos negativos (Slow, Blindness, Confusion).
* **Switcher:** Intercambia posiciones con el enemigo.

## Comandos
| `/abilities help` | Muestra el menú de ayuda. | `abilities.admin` |
| `/abilities reload` | Recarga la configuración del plugin. | `abilities.admin` |
| `/abilities give <jugador> <item> [cant]` | Otorga un ítem especial a un jugador. | `abilities.admin` |
| `/abilities list` | Lista todos los ítems disponibles. | `abilities.admin` |
| `/abilities resetcd <jugador> [item]` | Limpia los cooldowns de un jugador. | `abilities.admin` |

## Instalación
1. Descarga el archivo `.jar`.
2. Colócalo en la carpeta `/plugins` de tu servidor.
3. Inicia el servidor para generar los archivos de configuración.
4. Ajusta los valores en `config.yml` según tus necesidades y reinicia.
