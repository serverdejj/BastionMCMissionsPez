# BastionMCMissions 1.0.0
Proyecto Maven Java 21 para Purpur 1.21.11. Requiere FancyNpcs compatible instalado y sus comandos activos. Compilar con GitHub Actions (Build) o mvn package. No incluye JAR compilado.

1. Instalar JAR, iniciar servidor, esperar a que FancyNpcs termine de cargar.
2. Como OP, ejecutar /misionpesca npc crear en el sitio elegido. La skin PNG se copia a FancyNpcs/skins; la resolución de textura depende de FancyNpcs. Si no carga, ejecutar /npc skin bastion_pescador pescador.png.
3. /misionpesca npc tp, center, sit, normal, eliminar.
4. /misionpesca menu espada, pezglobo, caña (también cana): clic en un objeto del inventario para copiarlo. No consume el original. Caña debe ser FISHING_ROD. Configurar antes de iniciar la misión: los peces anteriores con distinta metadata podrían no coincidir en el comercio nativo.
5. Clic en NPC: primera caña al suelo con propietario, luego entrega de peces. Shift+clic abre comercio; también se abre sin caña ni peces. Se necesitan 20 peces obtenidos de otros jugadores si se pierde la caña. El comercio usa la interfaz nativa. El callback de FancyNpcs puede admitir también clic izquierdo según versión.
6. /misionpesca reload, progreso <jugador>, reset <jugador>, resetall confirmar. Cambiar mission-id crea una misión nueva, invalida cañas/peces de la anterior. No borrar players.yml ni los datos de FancyNpcs.

Los comandos del plugin comprueban OP y se filtran del árbol de comandos de jugadores no OP. No otorgar permisos administrativos FancyNpcs a rangos normales. La ubicación real y postura se guardan mediante FancyNpcs; npc.yml contiene opciones de creación, no reemplaza su almacenamiento. Cambios de apariencia del NPC existente se pueden aplicar con comandos FancyNpcs. No se incluye holograma, partículas ni sonidos adicionales en esta versión. No se ha compilado ni probado en servidor: probar pesca, NPC, comercio, persistencia y visibilidad TAB antes de publicar.
