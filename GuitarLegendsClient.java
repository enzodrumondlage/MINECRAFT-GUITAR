package br.com.enzomod.guitarlegends;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.TypedActionResult;
public class GuitarLegendsClient implements ClientModInitializer {
 @Override public void onInitializeClient() {
  UseItemCallback.EVENT.register((player,world,hand)->{
   var stack=player.getStackInHand(hand);
   if(!GuitarLegends.GUITARS.containsValue(stack.getItem())) return TypedActionResult.pass(stack);
   if(world.isClient) MinecraftClient.getInstance().setScreen(new GuitarScreen());
   return TypedActionResult.success(stack,world.isClient);
  });
 }
}
