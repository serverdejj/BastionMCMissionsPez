# BastionMCMissions 1.0.1
Proyecto Maven Java 21 para Purpur 1.21.11. Requiere Vault con proveedor de economía y FancyNpcs compatible instalado y sus comandos activos. Compilar con GitHub Actions (Build) o mvn package. No incluye JAR compilado.

1. Instalar JAR, iniciar servidor, esperar a que FancyNpcs termine de cargar.
2. Como OP, ejecutar /misionpesca npc crear en el sitio elegido. La skin PNG se copia a FancyNpcs/skins; la resolución de textura depende de FancyNpcs. Si no carga, ejecutar /npc skin bastion_pescador pescador.png.
3. /misionpesca npc tp, center, sit, normal, eliminar.
4. /misionpesca menu espada, pezglobo, caña (también cana): clic en un objeto del inventario para copiarlo. No consume el original. Caña debe ser FISHING_ROD. Los peces de la misión se reconocen por su identificador interno.
5. Clic en NPC: primera caña al suelo con propietario, luego entrega de peces. Sin caña se ofrece compra por 10000 monedas mediante Vault; Shift+clic sin caña permite comprar sin entregar peces. No hay comercio de peces. Los botones usan una acción interna no registrada y una oferta individual temporal. /misionpesca give <caña|pezglobo|espada> <jugador> [cantidad] es exclusivo de OP.
6. /misionpesca reload, progreso <jugador>, reset <jugador>, resetall confirmar. Cambiar mission-id crea una misión nueva, invalida cañas/peces de la anterior. No borrar players.yml ni los datos de FancyNpcs.

Los comandos del plugin comprueban OP y se filtran del árbol de comandos de jugadores no OP. No otorgar permisos administrativos FancyNpcs a rangos normales. La ubicación real y postura se guardan mediante FancyNpcs; npc.yml contiene opciones de creación, no reemplaza su almacenamiento. Cambios de apariencia del NPC existente se pueden aplicar con comandos FancyNpcs. No se incluye holograma, partículas ni sonidos adicionales en esta versión. No se ha compilado ni probado en servidor: probar pesca, NPC, compra, persistencia y visibilidad TAB antes de publicar.

Actualización: mantener players.yml, items.yml, npc.yml y los datos FancyNpcs. Agregar replacement-price y offer-seconds a config.yml y los nuevos mensajes si se desea personalizarlos. Los valores por defecto de mensajes nuevos se cargan aunque falten en el archivo existente.
