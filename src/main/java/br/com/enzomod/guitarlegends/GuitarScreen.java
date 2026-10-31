package br.com.enzomod.guitarlegends;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class GuitarScreen extends Screen {
    private static final int[] COLORS={0xFF20D050,0xFFE03030,0xFFE0D030,0xFF3080F0,0xFFF08020};
    private static final String[] KEYS={"F","G","H","J","K"};
    private static final int[] GLFW_KEYS={GLFW.GLFW_KEY_F,GLFW.GLFW_KEY_G,GLFW.GLFW_KEY_H,GLFW.GLFW_KEY_J,GLFW.GLFW_KEY_K};
    private static final String[] DIFFICULTIES={"FACIL","NORMAL","DIFICIL","IMPOSSIVEL"};
    private static final int NOTE_TRAVEL_MS=1800;

    private int song=0,mode=1,score=0,combo=0,bestCombo=0,misses=0,perfects=0,hits=0;
    private long startNanos=0L;
    private boolean playing=false,finished=false;
    private boolean[] done=new boolean[0],holding=new boolean[0];
    private SoundInstance currentSongSound;
    private final Properties records=new Properties();
    private final Path recordsFile=FabricLoader.getInstance().getConfigDir().resolve("guitarlegends-records.properties");

    public GuitarScreen(){super(Text.literal("Minecraft Guitar Legends"));loadRecords();}
    private void loadRecords(){if(!Files.exists(recordsFile))return;try(InputStream in=Files.newInputStream(recordsFile)){records.load(in);}catch(IOException ignored){}}
    private void saveRecords(){try{Files.createDirectories(recordsFile.getParent());try(OutputStream out=Files.newOutputStream(recordsFile)){records.store(out,"Minecraft Guitar Legends records");}}catch(IOException ignored){}}
    private String recordKey(){return SongCharts.IDS[song]+"."+mode;}
    private int record(){try{return Integer.parseInt(records.getProperty(recordKey(),"0"));}catch(NumberFormatException ignored){return 0;}}
    private int hitWindow(){return switch(mode){case 0->220;case 2->135;case 3->100;default->170;};}
    private int perfectWindow(){return switch(mode){case 0->100;case 2->55;case 3->40;default->70;};}
    private boolean activeNote(int i){if(mode==0)return SongCharts.EXTRA[song][i]==0&&(i%2==0);if(mode==1)return SongCharts.EXTRA[song][i]==0;return true;}

    private int holdDuration(int i){
        int[] times=SongCharts.TIMES_MS[song];
        int start=times[i];
        int next=i+1<times.length?times[i+1]:SongCharts.DURATIONS_MS[song];
        int gap=Math.max(0,next-start);
        if(gap<650)return 0;
        if(gap<1050&&((i+song)&1)!=0)return 0;
        return Math.min(2200,Math.max(450,gap-170));
    }

    private void begin(){
        stopSong();
        int n=SongCharts.TIMES_MS[song].length;
        done=new boolean[n];holding=new boolean[n];
        score=combo=bestCombo=misses=perfects=hits=0;
        playing=true;finished=false;startNanos=System.nanoTime();playSong();
    }

    private void playSong(){
        MinecraftClient client=MinecraftClient.getInstance();
        Identifier id=new Identifier("guitarlegends","song."+SongCharts.IDS[song]);
        currentSongSound=PositionedSoundInstance.master(SoundEvent.of(id),1.0f,1.0f);
        client.getSoundManager().play(currentSongSound);
    }
    private void stopSong(){if(currentSongSound!=null){MinecraftClient.getInstance().getSoundManager().stop(currentSongSound);currentSongSound=null;}}
    private long time(){return(System.nanoTime()-startNanos)/1_000_000L;}
    private boolean laneIsHeld(int lane){MinecraftClient c=MinecraftClient.getInstance();return GLFW.glfwGetKey(c.getWindow().getHandle(),GLFW_KEYS[lane])==GLFW.GLFW_PRESS;}
    private boolean laneHasActiveHold(int lane){byte[] lanes=SongCharts.LANES[song];for(int i=0;i<holding.length;i++)if(holding[i]&&!done[i]&&lanes[i]==lane)return true;return false;}

    private void hit(int lane){
        if(!playing)return;
        if(laneHasActiveHold(lane))return;
        long t=time();int best=-1;long diff=Long.MAX_VALUE;
        int[] times=SongCharts.TIMES_MS[song];byte[] lanes=SongCharts.LANES[song];
        for(int i=0;i<times.length;i++){
            if(done[i]||holding[i]||!activeNote(i)||lanes[i]!=lane)continue;
            long d=Math.abs(times[i]-t);if(d<diff){best=i;diff=d;}
        }
        if(best>=0&&diff<=hitWindow()){
            combo++;bestCombo=Math.max(bestCombo,combo);hits++;
            if(diff<=perfectWindow()){perfects++;score+=100*multiplier();}else score+=65*multiplier();
            if(holdDuration(best)>0)holding[best]=true;else done[best]=true;
        }else miss(true);
    }

    private int multiplier(){return Math.min(4,1+combo/10);}
    private void miss(boolean wrongKey){
        MinecraftClient c=MinecraftClient.getInstance();
        if(c.player!=null)c.player.playSound(SoundEvents.ENTITY_VILLAGER_NO,0.65f,wrongKey?0.55f:0.75f);
        misses++;combo=0;score=Math.max(0,score-25);
        if((mode==3&&misses>=1)||(mode==2&&misses>=20))finishSong();
    }
    private void finishSong(){if(!playing)return;playing=false;finished=true;stopSong();if(score>record()){records.setProperty(recordKey(),Integer.toString(score));saveRecords();}}

    private void updateHolds(long t){
        int[] times=SongCharts.TIMES_MS[song];byte[] lanes=SongCharts.LANES[song];
        for(int i=0;i<times.length;i++){
            if(!holding[i]||done[i])continue;
            int duration=holdDuration(i);long end=times[i]+duration;
            if(t<end-70&&!laneIsHeld(lanes[i])){
                holding[i]=false;done[i]=true;miss(false);if(!playing)return;
            }else if(t>=end){
                holding[i]=false;done[i]=true;score+=Math.max(40,duration/12)*multiplier();
            }
        }
    }

    private void updateMissed(long t){
        updateHolds(t);if(!playing)return;
        int[] times=SongCharts.TIMES_MS[song];
        for(int i=0;i<times.length;i++){
            if(done[i]||holding[i]||!activeNote(i))continue;
            if(t-times[i]>hitWindow()){done[i]=true;miss(false);if(!playing)return;}
        }
        if(t>=SongCharts.DURATIONS_MS[song]+350L)finishSong();
    }

    private int noteY(long noteTime,long t,int top,int bottom){long dt=noteTime-t;return bottom-(int)(dt*(bottom-top)/NOTE_TRAVEL_MS);}

    private void drawHighway(DrawContext c,long t){
        int highwayWidth=Math.min(500,width-28),laneWidth=highwayWidth/5,left=(width-laneWidth*5)/2,top=78,bottom=height-82,hitLine=bottom-3;
        c.fill(left-5,top-5,left+laneWidth*5+5,bottom+43,0xDD080D1B);c.fill(left-3,top-3,left+laneWidth*5+3,bottom+41,0xFF29354B);
        for(int lane=0;lane<5;lane++){
            int x=left+lane*laneWidth;c.fill(x+1,top,x+laneWidth-2,bottom+35,0xCC111827);c.fill(x+1,top,x+3,bottom+35,0xFF263248);c.fill(x+laneWidth-5,top,x+laneWidth-3,bottom+35,0xFF263248);
            for(int y=top+12;y<bottom+32;y+=34)c.fill(x+5,y,x+laneWidth-7,y+2,0x553F526D);c.fill(x+5,top+5,x+laneWidth-7,top+10,COLORS[lane]);
        }
        c.fill(left-9,hitLine,left+laneWidth*5+9,hitLine+4,0xFFEAF6FF);c.fill(left-9,hitLine-2,left+laneWidth*5+9,hitLine,0xFF536D8C);
        int[] times=SongCharts.TIMES_MS[song];byte[] lanes=SongCharts.LANES[song];
        for(int i=0;i<times.length;i++){
            if(done[i]||!activeNote(i))continue;
            int duration=holdDuration(i);long dt=times[i]-t,endDt=times[i]+duration-t;if(dt>NOTE_TRAVEL_MS||endDt<-hitWindow())continue;
            int lane=lanes[i],centerX=left+lane*laneWidth+laneWidth/2,y=holding[i]?bottom:noteY(times[i],t,top,bottom),noteWidth=Math.max(15,Math.min(30,laneWidth-12)),half=noteWidth/2;
            if(duration>0){int endY=noteY(times[i]+duration,t,top,bottom),tailTop=Math.max(top,Math.min(y,endY)),tailBottom=Math.min(bottom,Math.max(y,endY));if(tailBottom>=top&&tailTop<=bottom){c.fill(centerX-8,tailTop,centerX+8,tailBottom,COLORS[lane]);c.fill(centerX-3,tailTop,centerX+3,tailBottom,0xAAFFFFFF);}}
            if(y>=top-12&&y<=bottom+12){c.fill(centerX-half-4,y-11,centerX+half+4,y+11,0x553FFFFF);c.fill(centerX-half,y-8,centerX+half,y+8,COLORS[lane]);c.fill(centerX-half+4,y-5,centerX+half-4,y-2,0xCCFFFFFF);c.fill(centerX-half+3,y+4,centerX+half-3,y+6,0xAA101010);}
        }
        for(int lane=0;lane<5;lane++){
            int centerX=left+lane*laneWidth+laneWidth/2,padWidth=Math.min(36,laneWidth-10),half=padWidth/2;boolean pressed=laneIsHeld(lane);
            c.fill(centerX-half-2,bottom+7,centerX+half+2,bottom+35,0xFF05070D);c.fill(centerX-half,bottom+9,centerX+half,bottom+33,pressed?0xFFFFFFFF:COLORS[lane]);c.fill(centerX-half+4,bottom+12,centerX+half-4,bottom+15,pressed?COLORS[lane]:0xCCFFFFFF);c.drawCenteredTextWithShadow(textRenderer,KEYS[lane],centerX,bottom+18,pressed?0xFF111111:0xFFFFFFFF);
        }
    }

    private void drawResults(DrawContext c){
        int l=width/2-125,r=width/2+125,t=height/2-72,b=height/2+72;c.fill(l,t,r,b,0xEE080D1B);c.fill(l,t,r,t+2,0xFFFFCC55);
        c.drawCenteredTextWithShadow(textRenderer,"FIM DA MUSICA",width/2,t+12,0xFFFFD65C);c.drawCenteredTextWithShadow(textRenderer,"PONTOS: "+score+"   RECORDE: "+record(),width/2,t+31,0xFFFFFFFF);c.drawCenteredTextWithShadow(textRenderer,"PERFEITOS: "+perfects+"   ACERTOS: "+hits,width/2,t+47,0xFF9EF7B4);c.drawCenteredTextWithShadow(textRenderer,"ERROS: "+misses+"   MAIOR COMBO: "+bestCombo,width/2,t+63,0xFFFFAAAA);c.drawCenteredTextWithShadow(textRenderer,"ENTER: JOGAR DE NOVO   BACKSPACE: MENU",width/2,b-19,0xFF9EDBFF);
    }
    private String clock(long ms){long total=Math.max(0,ms)/1000L;return String.format("%d:%02d",total/60,total%60);}

    @Override public void render(DrawContext c,int mx,int my,float delta){
        renderBackground(c);c.fill(0,0,width,48,0xDD080D1B);c.fill(0,47,width,49,0xFF40516E);c.drawCenteredTextWithShadow(textRenderer,"MINECRAFT GUITAR LEGENDS",width/2,8,0xFFFFCC55);c.drawCenteredTextWithShadow(textRenderer,"F G H J K  |  TOQUE OU SEGURE A NOTA",width/2,27,0xFF9EDBFF);
        if(!playing&&!finished){
            c.drawCenteredTextWithShadow(textRenderer,"ESCOLHA UMA MUSICA  (CIMA / BAIXO)",width/2,57,0xFFFFFFFF);
            for(int i=0;i<SongCharts.NAMES.length;i++){int y=82+i*16;c.drawCenteredTextWithShadow(textRenderer,(i==song?">  ":"")+SongCharts.NAMES[i],width/2,y,i==song?0xFFFFFF55:0xFFD9E4F5);}
            c.drawCenteredTextWithShadow(textRenderer,"DIFICULDADE: "+DIFFICULTIES[mode]+"  (ESQUERDA / DIREITA)",width/2,height-45,0xFFFFFFFF);c.drawCenteredTextWithShadow(textRenderer,"RECORDE: "+record()+"   ENTER: JOGAR   SHIFT: SAIR",width/2,height-28,0xFF80FF80);
        }else if(playing){
            long t=time();updateMissed(t);c.drawTextWithShadow(textRenderer,"PONTOS "+score,10,56,0xFFFFFFFF);c.drawTextWithShadow(textRenderer,"COMBO "+combo+"  x"+multiplier(),10,69,0xFFFFD65C);c.drawTextWithShadow(textRenderer,"ERROS "+misses,Math.max(10,width-80),56,0xFFFF7777);c.drawCenteredTextWithShadow(textRenderer,SongCharts.NAMES[song]+"  "+clock(t)+" / "+clock(SongCharts.DURATIONS_MS[song]),width/2,56,0xFFFFFFFF);drawHighway(c,t);
        }
        if(finished)drawResults(c);super.render(c,mx,my,delta);
    }

    @Override public boolean keyPressed(int key,int scan,int mods){
        if(key==GLFW.GLFW_KEY_LEFT_SHIFT||key==GLFW.GLFW_KEY_RIGHT_SHIFT){close();return true;}
        if(finished){if(key==GLFW.GLFW_KEY_ENTER)begin();else if(key==GLFW.GLFW_KEY_BACKSPACE)finished=false;else return super.keyPressed(key,scan,mods);return true;}
        if(!playing){if(key==GLFW.GLFW_KEY_UP)song=(song+SongCharts.NAMES.length-1)%SongCharts.NAMES.length;else if(key==GLFW.GLFW_KEY_DOWN)song=(song+1)%SongCharts.NAMES.length;else if(key==GLFW.GLFW_KEY_LEFT)mode=(mode+DIFFICULTIES.length-1)%DIFFICULTIES.length;else if(key==GLFW.GLFW_KEY_RIGHT)mode=(mode+1)%DIFFICULTIES.length;else if(key==GLFW.GLFW_KEY_ENTER)begin();else return super.keyPressed(key,scan,mods);return true;}
        for(int i=0;i<GLFW_KEYS.length;i++)if(key==GLFW_KEYS[i]){hit(i);return true;}return super.keyPressed(key,scan,mods);
    }
    @Override public void close(){stopSong();super.close();}
    @Override public boolean shouldPause(){return false;}
}
