package mc.bastion.missions;

import de.oliver.fancynpcs.api.FancyNpcsPlugin;
import de.oliver.fancynpcs.api.Npc;
import de.oliver.fancynpcs.api.NpcData;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

public final class Missions extends JavaPlugin implements Listener, TabExecutor {
    private YamlConfiguration messages, menus, items, npcCfg, players;
    private NamespacedKey key;
    private final List<String> kinds=List.of("espada","pezglobo","caña");
    private final Set<UUID> magicHooks=new HashSet<>();
    private final Map<UUID,Long> clicks=new HashMap<>();
    private final Map<UUID,Merchant> trades=new HashMap<>();
    private YamlConfiguration load(String name){ File f=new File(getDataFolder(),name+".yml"); if(!f.exists())saveResource(name+".yml",false); return YamlConfiguration.loadConfiguration(f); }
    private void save(YamlConfiguration c,String name){try{c.save(new File(getDataFolder(),name+".yml"));}catch(IOException e){throw new IllegalStateException("Cannot save "+name,e);}}
    private void loadAll(){reloadConfig();messages=load("messages");menus=load("menus");items=load("items");npcCfg=load("npc");players=load("players");}
    @Override public void onEnable(){saveDefaultConfig();loadAll();key=new NamespacedKey(this,"mission_item");getCommand("misionpesca").setExecutor(this);getCommand("misionpesca").setTabCompleter(this);Bukkit.getPluginManager().registerEvents(this,this);
        try {Path dir=Bukkit.getPluginManager().getPlugin("FancyNpcs").getDataFolder().toPath().resolve("skins");Files.createDirectories(dir);Path dest=dir.resolve("pescador.png");if(!Files.exists(dest))try(InputStream in=getResource("pescador.png")){Files.copy(in,dest);}}catch(Exception e){getLogger().warning("Skin: "+e.getMessage());}
        Bukkit.getScheduler().runTaskLater(this,()->{Npc n=npc();if(n!=null)bind(n);},220L);
    }
    private String color(String s){Matcher m=Pattern.compile("&#([0-9a-fA-F]{6})").matcher(s);StringBuffer b=new StringBuffer();while(m.find()){StringBuilder h=new StringBuilder("§x");for(char c:m.group(1).toCharArray())h.append('§').append(c);m.appendReplacement(b,Matcher.quoteReplacement(h.toString()));}m.appendTail(b);return ChatColor.translateAlternateColorCodes('&',b.toString());}
    private void msg(CommandSender p,String k){p.sendMessage(color(messages.getString(k,"")));}
    private int required(){return Math.max(1,getConfig().getInt("required-fish",500));}
    private String mission(){return getConfig().getString("mission-id","espada-pez-1");}
    private String path(UUID u){return "missions."+mission()+"."+u;}
    private String path(Player p){return path(p.getUniqueId());}
    private ItemStack item(String kind){ItemStack i=items.getItemStack(kind);if(i==null){i=new ItemStack(switch(kind){case "espada"->Material.WOODEN_SWORD;case "pezglobo"->Material.PUFFERFISH;default->Material.FISHING_ROD;});ItemMeta m=i.getItemMeta();m.setDisplayName(color(menus.getString("labels."+kind,kind)));if(!kind.equals("espada"))m.setEnchantmentGlintOverride(true);i.setItemMeta(m);if(kind.equals("caña")){i.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.LURE,3);i.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.UNBREAKING,3);}}else i=i.clone();i.setAmount(1);if(!kind.equals("espada")){ItemMeta m=i.getItemMeta();m.getPersistentDataContainer().set(key,PersistentDataType.STRING,mission()+":"+kind);i.setItemMeta(m);}return i;}
    private boolean tagged(ItemStack i,String k){return i!=null&&i.hasItemMeta()&&(mission()+":"+k).equals(i.getItemMeta().getPersistentDataContainer().get(key,PersistentDataType.STRING));}
    private boolean hasRod(Player p){for(ItemStack i:p.getInventory().getContents())if(tagged(i,"caña"))return true;return false;}
    private int count(Player p){int n=0;for(ItemStack i:p.getInventory().getStorageContents())if(tagged(i,"pezglobo"))n+=i.getAmount();return n;}
    private void take(Player p,int n){ItemStack[] a=p.getInventory().getStorageContents();for(int x=0;x<a.length&&n>0;x++)if(tagged(a[x],"pezglobo")){int t=Math.min(n,a[x].getAmount());n-=t;if(t==a[x].getAmount())a[x]=null;else a[x].setAmount(a[x].getAmount()-t);}p.getInventory().setStorageContents(a);}
    private void lines(Player p,String k,int n){for(String s:messages.getStringList(k))p.sendMessage(color(s.replace("%progress%",String.valueOf(n)).replace("%required%",String.valueOf(required()))));}
    private Npc npc(){return FancyNpcsPlugin.get().getNpcManager().getNpc(npcCfg.getString("name","bastion_pescador"));}
    private void bind(Npc n){n.getData().setOnClick(p->Bukkit.getScheduler().runTask(this,()->interact(p)));}
    private void interact(Player p){Npc n=npc();if(n==null||!p.isOnline())return;Location l=n.getData().getLocation();if(l==null||!p.getWorld().equals(l.getWorld())||p.getLocation().distanceSquared(l)>Math.pow(getConfig().getDouble("interaction-distance",6),2))return;long now=System.currentTimeMillis();if(now-clicks.getOrDefault(p.getUniqueId(),0L)<600)return;clicks.put(p.getUniqueId(),now);String s=path(p);int progress=players.getInt(s+".progress");
        if(!players.getBoolean(s+".started")){players.set(s+".started",true);save(players,"players");lines(p,"intro",0);Item drop=l.getWorld().dropItem(l.clone().add(0,0.7,0),item("caña"));drop.setOwner(p.getUniqueId());drop.setPickupDelay(10);return;}
        if(players.getBoolean(s+".claimed")){msg(p,"already");return;}
        if(progress<required()&&(p.isSneaking()||(!hasRod(p)&&count(p)==0))){trade(p);return;}
        int t=Math.min(count(p),Math.max(0,required()-progress));if(t>0){take(p,t);progress+=t;players.set(s+".progress",progress);save(players,"players");}lines(p,"progress",progress);
        if(progress>=required()){ItemStack reward=item("espada");if(p.getInventory().firstEmpty()==-1){msg(p,"full");return;}players.set(s+".claimed",true);save(players,"players");p.getInventory().addItem(reward);msg(p,"completed");}
    }
    private void trade(Player p){Merchant m=Bukkit.createMerchant(color(menus.getString("trade-title","Pescador")));int cost=Math.max(1,Math.min(64,getConfig().getInt("replacement-cost",20)));ItemStack fish=item("pezglobo");fish.setAmount(cost);MerchantRecipe r=new MerchantRecipe(item("caña"),Integer.MAX_VALUE);r.addIngredient(fish);r.setExperienceReward(false);m.setRecipes(List.of(r));trades.put(p.getUniqueId(),m);p.openMerchant(m,true);}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void fish(PlayerFishEvent e){Player p=e.getPlayer();if(e.getState()==PlayerFishEvent.State.FISHING){ItemStack rod=e.getHand()==org.bukkit.inventory.EquipmentSlot.OFF_HAND?p.getInventory().getItemInOffHand():p.getInventory().getItemInMainHand();if(tagged(rod,"caña")){UUID hook=e.getHook().getUniqueId();magicHooks.add(hook);Bukkit.getScheduler().runTaskLater(this,()->magicHooks.remove(hook),72000L);}return;}if(e.getState()!=PlayerFishEvent.State.CAUGHT_FISH||!(e.getCaught() instanceof Item caught))return;if(!magicHooks.remove(e.getHook().getUniqueId())||!hasRod(p))return;if(Math.random()*100<getConfig().getDouble("chance",33)){if(getConfig().getBoolean("replace-vanilla-catch",true))caught.setItemStack(item("pezglobo"));else caught.getWorld().dropItem(caught.getLocation(),item("pezglobo"));}}
    private static final class Editor implements InventoryHolder{final String kind;Inventory inv;Editor(String k){kind=k;}public Inventory getInventory(){return inv;}}
    private void editor(Player p,String k){Editor h=new Editor(k);int size=Math.max(9,Math.min(54,menus.getInt("size",27)/9*9));h.inv=Bukkit.createInventory(h,size,color(k.isEmpty()?menus.getString("main-title"):menus.getString("editor-title").replace("%item%",k)));if(k.isEmpty()){for(String x:kinds){int slot=menus.getInt("slots."+x);if(slot>=0&&slot<size)h.inv.setItem(slot,item(x));}}else{h.inv.setItem(size/2,item(k));msg(p,"select");}p.openInventory(h.inv);}
    @EventHandler public void click(InventoryClickEvent e){if(e.getView().getTopInventory().getHolder() instanceof Editor h){e.setCancelled(true);if(!(e.getWhoClicked() instanceof Player p)||!p.isOp()){e.getWhoClicked().closeInventory();return;}if(h.kind.isEmpty()){for(String k:kinds)if(e.getRawSlot()==menus.getInt("slots."+k))Bukkit.getScheduler().runTask(this,()->editor(p,k));}else if(e.getClickedInventory()==p.getInventory()&&e.getCurrentItem()!=null&&!e.getCurrentItem().getType().isAir()){ItemStack i=e.getCurrentItem().clone();i.setAmount(1);if(h.kind.equals("caña")&&i.getType()!=Material.FISHING_ROD){msg(p,"error");return;}items.set(h.kind,i);save(items,"items");msg(p,"saved");Bukkit.getScheduler().runTask(this,()->editor(p,h.kind));}return;}
        if(e.getWhoClicked() instanceof Player p&&e.getView().getTopInventory() instanceof MerchantInventory mi&&mi.getMerchant()==trades.get(p.getUniqueId())&&e.getRawSlot()==2){ItemStack input=mi.getItem(0);if(!tagged(input,"pezglobo")||input.getAmount()<Math.max(1,Math.min(64,getConfig().getInt("replacement-cost",20))))e.setCancelled(true);}}
    @EventHandler public void drag(InventoryDragEvent e){if(e.getView().getTopInventory().getHolder() instanceof Editor)e.setCancelled(true);}
    @EventHandler public void close(InventoryCloseEvent e){if(e.getInventory() instanceof MerchantInventory mi&&mi.getMerchant()==trades.get(e.getPlayer().getUniqueId()))trades.remove(e.getPlayer().getUniqueId());}
    @EventHandler public void commands(PlayerCommandSendEvent e){if(!e.getPlayer().isOp())e.getCommands().removeIf(c->c.equalsIgnoreCase("misionpesca")||c.toLowerCase(Locale.ROOT).endsWith(":misionpesca"));}
    @EventHandler public void preprocess(PlayerCommandPreprocessEvent e){String c=e.getMessage().split(" ",2)[0].toLowerCase(Locale.ROOT);if(!e.getPlayer().isOp()&&(c.equals("/misionpesca")||c.endsWith(":misionpesca"))){e.setCancelled(true);msg(e.getPlayer(),"denied");}}
    private void fancy(Player p,String cmd){Bukkit.dispatchCommand(p,"npc "+cmd);}
    private void persistNpc(){Bukkit.dispatchCommand(Bukkit.getConsoleSender(),"fancynpcs save");}
    private void npcCommand(Player p,String action){Npc n=npc();String name=npcCfg.getString("name","bastion_pescador");if(action.equals("crear")){if(n!=null){bind(n);msg(p,"saved");return;}NpcData d=new NpcData(name,p.getUniqueId(),p.getLocation());d.setDisplayName(npcCfg.getString("display-name","@none"));d.setSkin(npcCfg.getString("skin","pescador.png"));Material hand=Material.matchMaterial(npcCfg.getString("hand","FISHING_ROD"));if(hand!=null)d.setEquipment(new HashMap<>(Map.of(de.oliver.fancynpcs.api.utils.NpcEquipmentSlot.MAINHAND,new ItemStack(hand))));n=FancyNpcsPlugin.get().getNpcAdapter().apply(d);FancyNpcsPlugin.get().getNpcManager().registerNpc(n);n.create();n.spawnForAll();bind(n);fancy(p,"displayname "+name+" "+npcCfg.getString("display-name","@none"));fancy(p,"attribute "+name+" set pose "+npcCfg.getString("pose","standing"));}
        else{if(n==null){msg(p,"missing");return;}switch(action){case "tp"->{fancy(p,"move_here "+name);fancy(p,"rotate "+name+" "+p.getLocation().getYaw()+" "+p.getLocation().getPitch());}case "center"->fancy(p,"center "+name);case "sit","normal"->{String pose=action.equals("sit")?"sitting":"standing";fancy(p,"attribute "+name+" set pose "+pose);npcCfg.set("pose",pose);save(npcCfg,"npc");}case "eliminar"->{n.removeForAll();FancyNpcsPlugin.get().getNpcManager().removeNpc(n);}default->{msg(p,"help");return;}}}persistNpc();msg(p,"saved");}
    @Override public boolean onCommand(CommandSender sender,Command command,String label,String[] a){if(!(sender instanceof Player p)||!p.isOp()){msg(sender,"denied");return true;}try{if(a.length==0){msg(p,"help");return true;}switch(a[0].toLowerCase(Locale.ROOT)){case "menu"->{String k=a.length>1?a[1].toLowerCase(Locale.ROOT):"";if(k.equals("cana"))k="caña";if(k.isEmpty()||kinds.contains(k))editor(p,k);else msg(p,"help");}case "npc"->{if(a.length==2)npcCommand(p,a[1].toLowerCase(Locale.ROOT));else msg(p,"help");}case "reload"->{loadAll();Npc n=npc();if(n!=null)bind(n);msg(p,"saved");}case "resetall"->{if(a.length==2&&a[1].equalsIgnoreCase("confirmar")){players.set("missions."+mission(),null);save(players,"players");msg(p,"saved");}else msg(p,"confirm");}case "reset","progreso"->{if(a.length!=2){msg(p,"help");break;}UUID u=Bukkit.getOfflinePlayer(a[1]).getUniqueId();if(a[0].equalsIgnoreCase("reset")){players.set(path(u),null);save(players,"players");msg(p,"saved");}else lines(p,"progress",players.getInt(path(u)+".progress"));}default->msg(p,"help");}}catch(Exception ex){getLogger().log(java.util.logging.Level.SEVERE,"Mission command failed",ex);msg(p,"error");}return true;}
    @Override public List<String> onTabComplete(CommandSender s,Command c,String alias,String[] a){if(!(s instanceof Player p)||!p.isOp())return List.of();List<String> options=a.length==1?List.of("menu","npc","reload","reset","resetall","progreso"):a.length==2?switch(a[0].toLowerCase(Locale.ROOT)){case "menu"->kinds;case "npc"->List.of("crear","tp","center","sit","normal","eliminar");case "resetall"->List.of("confirmar");case "reset","progreso"->Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();default->List.of();}:List.of();return options.stream().filter(x->x.toLowerCase(Locale.ROOT).startsWith(a[a.length-1].toLowerCase(Locale.ROOT))).toList();}
}
