package br.com.enzomod.guitarlegends;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import java.util.LinkedHashMap;
import java.util.Map;
public class GuitarLegends implements ModInitializer {
 public static final Map<String,Item> GUITARS=new LinkedHashMap<>();
 public static final String[] COLORS={"white","orange","magenta","light_blue","yellow","lime","pink","gray","light_gray","cyan","purple","blue","brown","green","red","black"};
 @Override public void onInitialize() {
  for(String color:COLORS) {
   Item guitar=Registry.register(Registries.ITEM,new Identifier("guitarlegends",color+"_guitar"),new Item(new Item.Settings().maxCount(1)));
   GUITARS.put(color,guitar);
  }
  ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(entries->GUITARS.values().forEach(entries::add));
 }
}
