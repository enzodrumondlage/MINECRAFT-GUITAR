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
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class GuitarScreen extends Screen {
    private static final int[] COLORS={0xFF45E02A,0xFFF03A3A,0xFFFFD62E,0xFF3B8CFF,0xFFFF8A26};
    private static final String[] KEYS={"F","G","H","J","K"};
    private static final int[] KEYCODES={GLFW.GLFW_KEY_F,GLFW.GLFW_KEY_G,GLFW.GLFW_KEY_H,GLFW.GLFW_KEY_J,GLFW.GLFW_KEY_K};
    private static final String[] DIFFICULTIES={"FACIL","NORMAL","DIFICIL","IMPOSSIVEL"};
    private static final long SYNC_OFFSET_NS=100_000_000L;

    private record Note(int time,int mask,int hold) {}

    private int song=0,difficulty=1,score,combo,bestCombo,misses,perfects,greats,goods,hits;
    private boolean playing,finished;
    private long startNanos;
    private Note[] chart=new Note[0];
    private boolean[] resolved=new boolean[0],holding=new boolean[0],keyDown=new boolean[5];
    private long[] laneFlash=new long[5];
    private String judgment="";
    private int judgmentColor=0xFFFFFFFF;
    private long judgmentAt;
    private SoundInstance sound;
    private final Properties records=new Properties();
    private final Path recordsFile=FabricLoader.getInstance().getConfigDir().resolve("guitarlegends-records.properties");

    public GuitarScreen(){super(Text.literal("Minecraft Guitar Legends"));loadRecords();}

    private void loadRecords(){if(!Files.exists(recordsFile))return;try(InputStream in=Files.newInputStream(recordsFile)){records.load(in);}catch(IOException ignored){}}
    private void saveRecords(){try{Files.createDirectories(recordsFile.getParent());try(OutputStream out=Files.newOutputStream(recordsFile)){records.store(out,"Minecraft Guitar Legends records");}}catch(IOException ignored){}}
    private String recordKey(){return SongCharts.IDS[song]+".v050."+difficulty;}
    private int record(){try{return Integer.parseInt(records.getProperty(recordKey(),"0"));}catch(Exception e){return 0;}}

    private int travel(){return switch(difficulty){case 0->3400;case 1->3000;case 2->2550;default->2250;};}
    private int hitWindow(){return switch(difficulty){case 0->320;case 1->260;case 2->200;default->170;};}
    private int perfectWindow(){return switch(difficulty){case 0->130;case 1->95;case 2->65;default->50;};}
    private int greatWindow(){return switch(difficulty){case 0->230;case 1->175;case 2->125;default->95;};}
    private int holdGrace(){return switch(difficulty){case 0->170;case 1->140;case 2->100;default->75;};}
    private int noteStep(){return switch(difficulty){case 0->4;case 1->2;default->1;};}

    private Note[] buildChart(int s){
        int[] times=SongCharts.TIMES_MS[s];
        byte[] lanes=SongCharts.LANES[s];
        byte[] extra=SongCharts.EXTRA[s];
        List<Integer> beats=new ArrayList<>();
        for(int i=0;i<times.length;i++)if(extra[i]==0)beats.add(i);
        List<Note> out=new ArrayList<>();
        int step=noteStep(),noteNo=0;
        for(int pos=Math.min(step-1,Math.max(0,beats.size()-1));pos<beats.size();pos+=step,noteNo++){
            int idx=beats.get(pos),t=times[idx],lane=Math.floorMod(lanes[idx],5),mask=1<<lane;
            boolean chord=false,triple=false;
            if(difficulty==1) chord=noteNo>5&&noteNo%16==0;
            else if(difficulty==2) chord=noteNo%11==0||noteNo%17==8;
            else if(difficulty==3){chord=noteNo%7==0||noteNo%11==4;triple=noteNo>12&&noteNo%32==15;}
            if(chord){int lane2=(lane+2+(noteNo/8)%2)%5;if(lane2==lane)lane2=(lane+1)%5;mask|=1<<lane2;}
            if(triple){int lane3=(lane+4)%5;if((mask&(1<<lane3))!=0)lane3=(lane+3)%5;mask|=1<<lane3;}

            boolean longNote=difficulty==0?noteNo>3&&noteNo%9==6:difficulty==1?noteNo>4&&noteNo%11==7:difficulty==2?noteNo%10==6:noteNo%8==5||noteNo%15==10;
            int hold=0;
            if(longNote&&pos+1<beats.size()){
                int end=times[beats.get(pos+1)];
                if(end-t>=380)hold=Math.min(1800,Math.max(420,end-t-90));
            }
            out.add(new Note(t,mask,hold));
        }
        return out.toArray(new Note[0]);
    }

    private void begin(){
        stopSong();chart=buildChart(song);resolved=new boolean[chart.length];holding=new boolean[chart.length];keyDown=new boolean[5];
        score=combo=bestCombo=misses=perfects=greats=goods=hits=0;judgment="";playing=true;finished=false;
        playSong();startNanos=System.nanoTime()+SYNC_OFFSET_NS;
    }
    private void playSong(){Identifier id=new Identifier("guitarlegends","song."+SongCharts.IDS[song]);sound=PositionedSoundInstance.master(SoundEvent.of(id),1.0f,1.0f);MinecraftClient.getInstance().getSoundManager().play(sound);}
    private void stopSong(){if(sound!=null){MinecraftClient.getInstance().getSoundManager().stop(sound);sound=null;}}
    private long time(){return(System.nanoTime()-startNanos)/1_000_000L;}
    private int mask(){int m=0;for(int i=0;i<5;i++)if(keyDown[i])m|=1<<i;return m;}
    private int mult(){return Math.min(4,1+combo/10);}
    private void judge(String s,int c){judgment=s;judgmentColor=c;judgmentAt=System.currentTimeMillis();}

    private void hitAttempt(){
        if(!playing)return;long t=time();int down=mask();if(down==0)return;int best=-1;long diff=Long.MAX_VALUE;
        for(int i=0;i<chart.length;i++){if(resolved[i]||holding[i])continue;long d=Math.abs(chart[i].time-t);if(d<=hitWindow()&&d<diff){best=i;diff=d;}}
        if(best<0)return;Note n=chart[best];boolean match=difficulty<=1?(down&n.mask)==n.mask:down==n.mask;if(!match)return;
        hits++;combo++;bestCombo=Math.max(bestCombo,combo);int base;
        if(diff<=perfectWindow()){perfects++;base=100;judge("PERFECT!",0xFFFFE66A);}else if(diff<=greatWindow()){greats++;base=80;judge("GREAT!",0xFF70F5FF);}else{goods++;base=60;judge("GOOD",0xFF9CFF8E);}
        score+=base*mult();long now=System.currentTimeMillis();for(int lane=0;lane<5;lane++)if((n.mask&(1<<lane))!=0)laneFlash[lane]=now+140;
        if(n.hold>0)holding[best]=true;else resolved[best]=true;
    }

    private void miss(int i){if(i>=0&&i<resolved.length){resolved[i]=true;holding[i]=false;}misses++;combo=0;score=Math.max(0,score-20);judge("MISS",0xFFFF5E5E);MinecraftClient c=MinecraftClient.getInstance();if(c.player!=null)c.player.playSound(SoundEvents.ENTITY_VILLAGER_NO,0.25f,0.65f);}
    private void update(long t){int down=mask();for(int i=0;i<chart.length;i++){if(resolved[i])continue;Note n=chart[i];if(holding[i]){long end=n.time+n.hold;if(t<end-holdGrace()&&(down&n.mask)!=n.mask)miss(i);else if(t>=end){holding[i]=false;resolved[i]=true;score+=Math.max(40,n.hold/10)*mult();judge("SUSTAIN!",0xFFB890FF);}}else if(t-n.time>hitWindow())miss(i);}if(t>=SongCharts.DURATIONS_MS[song]+450L)finish();}
    private void finish(){if(!playing)return;playing=false;finished=true;stopSong();if(score>record()){records.setProperty(recordKey(),Integer.toString(score));saveRecords();}}

    private int hy(){return Math.max(68,height/7);}private int hitY(){return height-84;}private double topW(){return Math.min(170.0,width*.23);}private double botW(){return Math.min(720.0,width-34.0);}private double roadW(double p){return topW()+(botW()-topW())*p;}private double left(double p){return width/2.0-roadW(p)/2.0;}private double center(int lane,double p){double w=roadW(p)/5.0;return left(p)+w*(lane+.5);}private int y(double p){return hy()+(int)Math.round((hitY()-hy())*p);}private double depth(long note,long t){double p=1.0-(note-t)/(double)travel();if(p<0)return 0;if(p>1)return 1;return Math.pow(p,1.35);}

    private void stage(DrawContext c){
        c.fill(0,0,width,height,0xFF060912);c.fill(0,0,width,hy()+18,0xFF0B1020);int cx=width/2;
        for(int i=-7;i<=7;i++){int h=8+(Math.abs(i*37)%35);c.fill(cx+i*13-4,hy()-h-16,cx+i*13+4,hy()-16,0xFF6F28C9);}
        c.drawCenteredTextWithShadow(textRenderer,"MINECRAFT GUITAR LEGENDS",cx,12,0xFFFFC83D);c.drawCenteredTextWithShadow(textRenderer,"F   G   H   J   K",cx,29,0xFFC5D8FF);
        int bottom=hitY()+42;for(int i=0;i<13;i++){double p=i/12.0;int yy=hy()+(int)((bottom-hy())*p),ww=7+(int)(22*p),gap=(int)(roadW(p)/2)+16+(int)(22*p),lx=cx-gap-ww,rx=cx+gap;c.fill(lx,yy-18,lx+ww,yy+26,0xFF343945);c.fill(rx,yy-18,rx+ww,yy+26,0xFF343945);c.fill(lx+ww/2-2,yy-5,lx+ww/2+3,yy+3,0xFFFFA52C);c.fill(rx+ww/2-2,yy-5,rx+ww/2+3,yy+3,0xFFFFA52C);}
        steve(c);
    }
    private void steve(DrawContext c){if(width<780||height<430)return;int x=28,yy=Math.max(88,height/4),s=Math.max(2,Math.min(4,height/190));c.fill(x+18*s,yy+52*s,x+25*s,yy+72*s,0xFF3436A8);c.fill(x+28*s,yy+52*s,x+35*s,yy+72*s,0xFF3436A8);c.fill(x+14*s,yy+25*s,x+39*s,yy+55*s,0xFF27B8C6);c.fill(x+17*s,yy+4*s,x+36*s,yy+25*s,0xFFB37A58);c.fill(x+17*s,yy+4*s,x+36*s,yy+10*s,0xFF3D2A20);c.fill(x+18*s,yy+9*s,x+22*s,yy+19*s,0xFF3D2A20);c.fill(x+31*s,yy+9*s,x+36*s,yy+19*s,0xFF3D2A20);c.fill(x+8*s,yy+29*s,x+15*s,yy+49*s,0xFF27B8C6);c.fill(x+39*s,yy+29*s,x+46*s,yy+49*s,0xFF27B8C6);c.fill(x+10*s,yy+42*s,x+34*s,yy+62*s,0xFF74C9EE);c.fill(x+14*s,yy+47*s,x+30*s,yy+58*s,0xFF1B283A);for(int i=0;i<28;i++){int gx=x+(30+i)*s,gy=yy+(44-i/3)*s;c.fill(gx,gy,gx+s,gy+3*s,0xFFB9C0CC);}}

    private void highway(DrawContext c,long t){
        int h=hy(),hit=hitY();
        for(int yy=h;yy<=hit;yy+=3){double p=(yy-h)/(double)Math.max(1,hit-h),w=roadW(p),l=left(p),lw=w/5.0;for(int lane=0;lane<5;lane++){int x1=(int)Math.round(l+lw*lane),x2=(int)Math.round(l+lw*(lane+1));c.fill(x1,yy,x2,Math.min(hit+1,yy+3),lane%2==0?0xFF10131B:0xFF0C0F16);int rw=Math.max(2,(int)(2+p*2));c.fill(x1+1,yy,x1+rw,Math.min(hit+1,yy+3),0xFF8A8F99);c.fill(x2-rw,yy,x2-1,Math.min(hit+1,yy+3),0xFF8A8F99);}}
        for(int i=0;i<30;i++){double p=Math.pow(i/29.0,1.45);int yy=y(p),thick=Math.max(1,(int)(1+p*3));double w=roadW(p),l=left(p),lw=w/5.0;for(int lane=0;lane<5;lane++){int x1=(int)(l+lw*lane+lw*.08),x2=(int)(l+lw*(lane+1)-lw*.08);c.fill(x1,yy,x2,yy+thick,0xFF6A5133);}}
        for(int i=0;i<chart.length;i++){if(resolved[i])continue;Note n=chart[i];long dt=n.time-t,end=n.time+n.hold-t;if(dt>travel()||end<-hitWindow())continue;double hp=holding[i]?1.0:depth(n.time,t),tp=n.hold>0?depth(n.time+n.hold,t):hp;if(n.hold>0)tail(c,n.mask,tp,hp);heads(c,n.mask,hp,Integer.bitCount(n.mask)>1);}
        int ll=(int)left(1)-5,rr=(int)(left(1)+roadW(1))+5,zone=difficulty==0?24:difficulty==1?18:difficulty==2?12:9;c.fill(ll,hit-zone,rr,hit+zone,0x2A3FE7FF);c.fill(ll,hit-4,rr,hit+3,0xFFFFFFFF);c.fill(ll,hit-8,rr,hit-4,0x8846D8FF);carts(c,t);
    }
    private void tail(DrawContext c,int mask,double a,double b){double from=Math.min(a,b),to=Math.max(a,b);for(int lane=0;lane<5;lane++)if((mask&(1<<lane))!=0)for(double p=from;p<=to;p+=.018){int yy=y(p),x=(int)Math.round(center(lane,p));double lw=roadW(p)/5.0;int half=Math.max(2,(int)(lw*.09));c.fill(x-half,yy-2,x+half,yy+3,(COLORS[lane]&0x00FFFFFF)|0xCC000000);c.fill(x-Math.max(1,half/4),yy-1,x+Math.max(1,half/4),yy+2,0x88FFFFFF);}}
    private void heads(DrawContext c,int mask,double p,boolean chord){int yy=y(p);double lw=roadW(p)/5.0;for(int lane=0;lane<5;lane++)if((mask&(1<<lane))!=0){int x=(int)Math.round(center(lane,p)),hw=Math.max(5,(int)(lw*(chord?.31:.27))),hh=Math.max(3,(int)(4+8*p));c.fill(x-hw-3,yy-hh-3,x+hw+3,yy+hh+3,0x66000000);c.fill(x-hw,yy-hh,x+hw,yy+hh,COLORS[lane]);c.fill(x-hw+3,yy-hh+2,x+hw-3,yy-Math.max(1,hh/3),0xCCFFFFFF);}}
    private int targetMask(long t){int m=0;for(int i=0;i<chart.length;i++){if(resolved[i]||holding[i])continue;Note n=chart[i];if(Math.abs(n.time-t)<=hitWindow())m|=n.mask;}return m;}
    private void carts(DrawContext c,long t){int yy=hitY()+14;long now=System.currentTimeMillis();int target=targetMask(t);for(int lane=0;lane<5;lane++){int x=(int)Math.round(center(lane,1)),lw=(int)(roadW(1)/5.0),w=Math.max(34,Math.min(72,lw-14));boolean ready=(target&(1<<lane))!=0;int body=(now<laneFlash[lane]||(difficulty<=1&&ready))?0xFFFFFFFF:COLORS[lane];c.fill(x-w/2-4,yy-3,x+w/2+4,yy+29,0xFF05070B);c.fill(x-w/2,yy,x+w/2,yy+24,body);c.fill(x-w/2+5,yy+4,x+w/2-5,yy+12,0xFF20252C);c.fill(x-w/2+7,yy+5,x+w/2-7,yy+9,0xFF090B0E);c.fill(x-w/2+5,yy+24,x-w/2+14,yy+29,0xFF111111);c.fill(x+w/2-14,yy+24,x+w/2-5,yy+29,0xFF111111);c.drawCenteredTextWithShadow(textRenderer,KEYS[lane],x,yy+34,COLORS[lane]);}}

    private void hud(DrawContext c,long t){c.fill(10,8,176,52,0xCC090D16);c.drawTextWithShadow(textRenderer,"SCORE",18,15,0xFFC5D8FF);c.drawTextWithShadow(textRenderer,String.format("%08d",score),18,30,0xFFFFD43B);c.fill(width-156,8,width-10,52,0xCC090D16);c.drawCenteredTextWithShadow(textRenderer,"COMBO",width-83,15,0xFFC5D8FF);c.drawCenteredTextWithShadow(textRenderer,"x"+combo,width-83,31,combo>0?0xFFFFD43B:0xFFFFFFFF);c.drawCenteredTextWithShadow(textRenderer,SongCharts.NAMES[song]+"  "+clock(t)+" / "+clock(SongCharts.DURATIONS_MS[song])+"  |  "+DIFFICULTIES[difficulty],width/2,height-20,0xFFFFFFFF);if(!judgment.isEmpty()&&System.currentTimeMillis()-judgmentAt<650)c.drawCenteredTextWithShadow(textRenderer,judgment,width/2,hitY()-48,judgmentColor);if(difficulty<=1&&targetMask(t)!=0)c.drawCenteredTextWithShadow(textRenderer,"AGORA!",width/2,hitY()-31,0xFFFFFFFF);}
    private String clock(long ms){long sec=Math.max(0,ms)/1000;return String.format("%d:%02d",sec/60,sec%60);}

    private void menu(DrawContext c){c.fill(0,0,width,height,0xFF070A12);stage(c);c.fill(width/2-220,58,width/2+220,height-42,0xE5090D17);c.drawCenteredTextWithShadow(textRenderer,"ESCOLHA A MUSICA",width/2,72,0xFFFFFFFF);for(int i=0;i<SongCharts.NAMES.length;i++){int yy=98+i*20;c.drawCenteredTextWithShadow(textRenderer,(i==song?">  ":"   ")+SongCharts.NAMES[i],width/2,yy,i==song?0xFFFFE45C:0xFFD6E4F5);}c.drawCenteredTextWithShadow(textRenderer,"DIFICULDADE: <  "+DIFFICULTIES[difficulty]+"  >",width/2,height-91,0xFFFFD75C);c.drawCenteredTextWithShadow(textRenderer,"ENTER: JOGAR | CIMA/BAIXO: MUSICA | ESQ/DIR: DIFICULDADE",width/2,height-72,0xFF9CFFAA);String info=difficulty==0?"FACIL: 1/4 das notas, sem acordes, janela de acerto grande":difficulty==1?"NORMAL: metade das notas, poucos acordes de 2 teclas":"Acordes: aperte as cores juntas | Notas longas: segure ate o fim";c.drawCenteredTextWithShadow(textRenderer,info,width/2,height-55,0xFF9EDBFF);}
    private void results(DrawContext c){c.fill(0,0,width,height,0xEE050812);int l=width/2-200,r=width/2+200,t=height/2-105,b=height/2+105;c.fill(l,t,r,b,0xF20B101D);c.fill(l,t,r,t+3,0xFFFFC63B);c.drawCenteredTextWithShadow(textRenderer,"FIM DA MUSICA",width/2,t+17,0xFFFFD75C);c.drawCenteredTextWithShadow(textRenderer,"PONTOS: "+score+"   RECORDE: "+record()+"   "+DIFFICULTIES[difficulty],width/2,t+42,0xFFFFFFFF);c.drawCenteredTextWithShadow(textRenderer,"PERFECT: "+perfects+"   GREAT: "+greats+"   GOOD: "+goods,width/2,t+65,0xFFB8F7FF);c.drawCenteredTextWithShadow(textRenderer,"ACERTOS: "+hits+"   ERROS: "+misses+"   MAIOR COMBO: "+bestCombo,width/2,t+88,0xFFFFD0D0);c.drawCenteredTextWithShadow(textRenderer,"ENTER: JOGAR DE NOVO | BACKSPACE: MENU",width/2,b-28,0xFF9CFFAA);}

    @Override public void render(DrawContext c,int mx,int my,float delta){if(!playing&&!finished)menu(c);else if(playing){long t=time();update(t);stage(c);highway(c,t);hud(c,t);}else results(c);super.render(c,mx,my,delta);}
    @Override public boolean keyPressed(int key,int scan,int mods){if(finished){if(key==GLFW.GLFW_KEY_ENTER||key==GLFW.GLFW_KEY_KP_ENTER){begin();return true;}if(key==GLFW.GLFW_KEY_BACKSPACE){finished=false;return true;}}if(!playing){if(key==GLFW.GLFW_KEY_UP){song=(song+SongCharts.NAMES.length-1)%SongCharts.NAMES.length;return true;}if(key==GLFW.GLFW_KEY_DOWN){song=(song+1)%SongCharts.NAMES.length;return true;}if(key==GLFW.GLFW_KEY_LEFT){difficulty=(difficulty+DIFFICULTIES.length-1)%DIFFICULTIES.length;return true;}if(key==GLFW.GLFW_KEY_RIGHT){difficulty=(difficulty+1)%DIFFICULTIES.length;return true;}if(key==GLFW.GLFW_KEY_ENTER||key==GLFW.GLFW_KEY_KP_ENTER){begin();return true;}}if(playing)for(int lane=0;lane<5;lane++)if(key==KEYCODES[lane]){if(!keyDown[lane]){keyDown[lane]=true;hitAttempt();}return true;}if(key==GLFW.GLFW_KEY_ESCAPE){close();return true;}return super.keyPressed(key,scan,mods);}
    @Override public boolean keyReleased(int key,int scan,int mods){if(playing)for(int lane=0;lane<5;lane++)if(key==KEYCODES[lane]){keyDown[lane]=false;long t=time();for(int i=0;i<chart.length;i++)if(holding[i]&&!resolved[i]){Note n=chart[i];if((n.mask&(1<<lane))!=0&&t<n.time+n.hold-holdGrace())miss(i);}return true;}return super.keyReleased(key,scan,mods);}
    @Override public void close(){stopSong();super.close();}
    @Override public boolean shouldPause(){return false;}
}
