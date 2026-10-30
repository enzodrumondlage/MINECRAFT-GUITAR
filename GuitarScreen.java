package br.com.enzomod.guitarlegends;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
public class GuitarScreen extends Screen {
 private static final int[] COLORS={0xFF27CF55,0xFFEF4545,0xFFF2D33B,0xFF3E95F5,0xFFFF942F};
 private static final String[] TRACKS={"F","G","H","J","K"};
 public GuitarScreen(){super(Text.literal("Minecraft Guitar Legends"));}
 @Override public void render(DrawContext ctx,int mx,int my,float delta){
  renderBackground(ctx);
  ctx.drawCenteredTextWithShadow(textRenderer,"MINECRAFT GUITAR LEGENDS",width/2,18,0xFFFFDD66);
  ctx.drawCenteredTextWithShadow(textRenderer,"Protótipo da pista - músicas e pontuação ainda não implementadas",width/2,36,0xFFFFFFFF);
  int size=38, left=width/2-(5*size)/2;
  for(int i=0;i<5;i++){
   ctx.fill(left+i*size,65,left+(i+1)*size-2,height-56,0xAA1C1C28);
   ctx.fill(left+i*size,height-90,left+(i+1)*size-2,height-52,COLORS[i]);
   ctx.drawCenteredTextWithShadow(textRenderer,TRACKS[i],left+i*size+size/2,height-80,0xFFFFFFFF);
  }
  ctx.drawCenteredTextWithShadow(textRenderer,"SHIFT para sair",width/2,height-25,0xFFFFFFFF);
  super.render(ctx,mx,my,delta);
 }
 @Override public boolean keyPressed(int key,int scan,int mods){
  if(key==GLFW.GLFW_KEY_LEFT_SHIFT||key==GLFW.GLFW_KEY_RIGHT_SHIFT){close();return true;}
  return super.keyPressed(key,scan,mods);
 }
 @Override public boolean shouldPause(){return false;}
}
