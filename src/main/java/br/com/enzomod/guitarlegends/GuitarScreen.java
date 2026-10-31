package br.com.enzomod.guitarlegends;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import java.util.Random;
public class GuitarScreen extends Screen {
 private static final int[] COLORS={0xFF20D050,0xFFE03030,0xFFE0D030,0xFF3080F0,0xFFF08020};
 private static final String[] SONGS={"Wuthering Heights","Pense no Lula","Solinho do Fora PT","Sabor Picanha","Mega Funk Fora PT","Tropa do Capitao!"};
 private final Random random=new Random(123);
 private int song=0,mode=0,score=0,combo=0,misses=0; private long start=0; private boolean playing=false;
 private final int[] lanes=new int[220]; private final boolean[] done=new boolean[220];
 public GuitarScreen(){super(Text.literal("Guitar Legends"));}
 private void begin(){random.setSeed(song*91L+mode*31L);for(int i=0;i<lanes.length;i++){lanes[i]=random.nextInt(5);done[i]=false;}score=combo=misses=0;playing=true;start=System.nanoTime();}
 private long time(){return (System.nanoTime()-start)/1000000L;}
 private void hit(int lane){if(!playing)return;long t=time();int best=-1;long diff=999999;for(int i=0;i<lanes.length;i++){long d=Math.abs(2500L+i*(700-mode*140L)-t);if(!done[i]&&lanes[i]==lane&&d<diff){best=i;diff=d;}}if(best>=0&&diff<180){done[best]=true;combo++;score+=(diff<70?100:diff<120?75:50)*Math.min(4,1+combo/10);}else miss();}
 private void miss(){misses++;combo=0;score=Math.max(0,score-25);if((mode==1&&misses>=20)||(mode==2&&misses>=10)||(mode==3&&misses>=1))playing=false;}
 @Override public void render(DrawContext c,int mx,int my,float delta){renderBackground(c);c.drawCenteredTextWithShadow(textRenderer,"MINECRAFT GUITAR LEGENDS",width/2,15,0xFFFFCC55);
 if(!playing){c.drawCenteredTextWithShadow(textRenderer,"SELECIONE A MUSICA (CIMA/BAIXO)",width/2,45,0xFFFFFFFF);for(int i=0;i<SONGS.length;i++)c.drawCenteredTextWithShadow(textRenderer,(i==song?"> ":"")+SONGS[i],width/2,70+i*17,i==song?0xFFFFFF55:0xFFFFFFFF);c.drawCenteredTextWithShadow(textRenderer,"Dificuldade: "+new String[]{"Facil","Medio","Dificil","Impossivel"}[mode]+" (ESQUERDA/DIREITA)",width/2,190,0xFFFFFFFF);c.drawCenteredTextWithShadow(textRenderer,"ENTER jogar | SHIFT sair | AUDIO AINDA NAO IMPLEMENTADO",width/2,215,0xFFFF8888);}
 else{long t=time();int w=40,left=width/2-100,bottom=height-58,top=53;c.drawTextWithShadow(textRenderer,"Pontos: "+score+"  Combo: "+combo+"  Erros: "+misses,10,35,0xFFFFFFFF);for(int i=0;i<5;i++){int x=left+i*w;c.fill(x,top,x+w-2,bottom,0xAA202030);c.fill(x,bottom-3,x+w-2,bottom+30,COLORS[i]);c.drawCenteredTextWithShadow(textRenderer,"FGHJK".substring(i,i+1),x+w/2-3,bottom+8,0xFFFFFFFF);}
 for(int i=0;i<lanes.length;i++){if(done[i])continue;long dt=2500L+i*(700-mode*140L)-t;if(dt< -180){done[i]=true;miss();continue;}if(dt>1800)continue;int y=bottom-(int)(dt*(bottom-top)/1800);int x=left+lanes[i]*w;c.fill(x+3,y-7,x+w-5,y+7,COLORS[lanes[i]]);}if(t>2500L+lanes.length*(700-mode*140L)+500)playing=false;}
 super.render(c,mx,my,delta);}
 @Override public boolean keyPressed(int key,int scan,int mods){if(key==GLFW.GLFW_KEY_LEFT_SHIFT||key==GLFW.GLFW_KEY_RIGHT_SHIFT){close();return true;}if(!playing){if(key==GLFW.GLFW_KEY_UP)song=(song+5)%6;else if(key==GLFW.GLFW_KEY_DOWN)song=(song+1)%6;else if(key==GLFW.GLFW_KEY_LEFT)mode=(mode+3)%4;else if(key==GLFW.GLFW_KEY_RIGHT)mode=(mode+1)%4;else if(key==GLFW.GLFW_KEY_ENTER)begin();else return super.keyPressed(key,scan,mods);return true;}int[] keys={GLFW.GLFW_KEY_F,GLFW.GLFW_KEY_G,GLFW.GLFW_KEY_H,GLFW.GLFW_KEY_J,GLFW.GLFW_KEY_K};for(int i=0;i<5;i++)if(key==keys[i]){hit(i);return true;}return super.keyPressed(key,scan,mods);}
 @Override public boolean shouldPause(){return false;}
}