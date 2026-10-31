package br.com.enzomod.guitarlegends;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class GuitarLegends implements ModInitializer {
    public static final String[] COLORS = {
        "white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
        "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"
    };

    @Override
    public void onInitialize() {
        for (String color : COLORS) {
            Item item = Registry.register(Registries.ITEM,
                new Identifier("guitarlegends", color + "_guitar"),
                new Item(new Item.Settings().maxCount(1)));
            ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(entries -> entries.add(item));
        }
        RhythmNetwork.registerServer();
    }
}
