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

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class GuitarScreen extends Screen {
    private static final int[] COLORS={0xFF45E02A,0xFFF03A3A,0xFFFFD62E,0xFF3B8CFF,0xFFFF8A26};
    private static final String[] KEYS={"F","G","H","J","K"};
    private static final int[] KEYCODES={GLFW.GLFW_KEY_F,GLFW.GLFW_KEY_G,GLFW.GLFW_KEY_H,GLFW.GLFW_KEY_J,GLFW.GLFW_KEY_K};
    private static final int TRAVEL=2400,HIT=175,PERFECT=55,GREAT=105;
    private static final int ANCHOR_STEP=4;
    private static final int[] BEAT_COUNTS={558,324,305,336,359,249};
    private static final int[][] BEAT_ANCHORS={
        new int[] {116,2136,4110,6084,8150,10147,12144,14257,16115,18112,20178,22175,24126,26215,28212,30163,32160,34110,36130,38127,40124,42121,44118,46115,48158,50155,52152,54172,56123,58143,60140,62113,64134,66177,68174,70171,72144,74118,76138,78135,80179,82152,84126,86169,88166,90163,92160,94134,96154,98174,100148,102121,104141,106138,108135,110132,112152,114149,116169,118166,120163,122137,124134,126131,128151,130148,132191,134165,136162,138159,140132,142176,144173,146146,148167,150140,152160,154180,156177,158174,160148,162122,164119,166185,168205,170202,172199,174196,176193,178190,180164,182161,184157,186154,188175,190171,192168,194188,196185,198136,200156,202176,204196,206193,208190,210164,212161,214181,216178,218175,220172,222192,224189,226186,228206,230179,232176,234173,236170,238167,240187,242207,244181,246178,248175,250195,252192,254189,256163,258183,260180,262177,264150,266170,268167,270164,272184,274181,276155,278175,278686},
        new int[] {209,2113,3947,5759,7593,9404,11215,13026,14861,16672,18483,20317,22129,23963,25774,27585,29420,31231,33042,34876,36688,38499,40310,42144,43955,45767,47578,49412,51223,53058,54869,56680,58491,60325,62137,63948,65782,67593,69404,71216,73050,74861,76672,78507,80318,82129,83940,85775,87586,89420,91231,93042,94877,96688,98499,100333,102145,103956,105767,107601,109412,111224,113035,114869,116680,118515,120303,122137,123948,125759,127594,129405,131216,133050,134861,136673,138507,140318,142129,143964,145775,147168},
        new int[] {139,2090,4017,5944,7895,9822,11749,13700,15627,17578,19505,21432,23382,25310,27237,29187,31115,33042,34992,36920,38847,40797,42725,44652,46602,48530,50480,52384,54335,56285,58212,60140,62090,64017,65945,67895,69822,71773,73700,75627,77555,79505,81432,83360,85310,87237,89188,91115,93042,94993,96920,98871,100798,102725,104652,106603,108530,110457,112408,114335,116262,118213,120163,122091,124018,125945,127896,129823,131750,133700,135628,137578,139505,141433,143360,145287,147122},
        new int[] {93,1858,3599,5201,6989,8754,10542,12307,14095,15859,17647,19412,21200,22965,24752,26540,28305,30093,31858,33646,35434,37198,38963,40751,42353,44071,45859,47647,49412,51200,52965,54753,56517,58305,60024,61649,63414,65202,66966,68638,70310,72075,73863,75651,77415,79203,80898,82524,84312,86100,87864,89652,91417,93205,94970,96758,98545,100310,102098,103863,105558,107183,108971,110759,112524,114289,116077,117841,119629,121417,123182,124970,126758,128522,130310,132075,133863,135628,137416,139204,140968,142756,144521,146309,147632},
        new int[] {93,2043,4040,5875,7709,9567,11517,13468,15325,17160,19017,20852,22686,24543,26401,28235,30070,31927,33785,35596,37407,39311,41169,43003,44861,46695,48530,50387,52245,54079,55937,57771,59652,61486,63321,65155,67013,68847,70705,72539,74397,76231,78089,79923,81781,83615,85473,87307,89165,90999,92857,94691,96549,98383,100241,102075,103933,105767,107624,109459,111316,113174,115008,116843,118700,120535,122392,124227,126084,127919,129776,131634,133468,135303,137160,139018,140852,142710,144544,146402,148236,150094,151928,153786,155620,157478,159312,161170,163004,164815,165698},
        new int[] {139,2229,4180,6037,7941,9868,11819,13793,15720,17670,19598,21548,23475,25588,27632,29652,31626,33553,35503,37431,39381,41332,43282,45232,47322,49296,51246,53197,55124,57075,59002,60976,62903,64993,66943,68894,70867,72795,74745,76696,78646,80573,82524,84474,86425,88375,90326,92253,94203,96154,98104,100032,101982,103933,106046,107973,109946,111897,113824,115775,117725,119652,121603}
    };

    private record Note(int time,int mask,int hold) {}

    private int song=0,score,combo,bestCombo,misses,perfects,greats,goods,hits;
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
    private String recordKey(){return SongCharts.IDS[song]+".guitarhero";}
    private int record(){try{return Integer.parseInt(records.getProperty(recordKey(),"0"));}catch(Exception e){return 0;}}

    private int beatTime(int s,int beat){
        int count=BEAT_COUNTS[s];
        if(beat<=0)return BEAT_ANCHORS[s][0];
        if(beat>=count-1)return BEAT_ANCHORS[s][BEAT_ANCHORS[s].length-1];
        int a=beat/ANCHOR_STEP,baseBeat=a*ANCHOR_STEP;int[] anchors=BEAT_ANCHORS[s];
        if(a>=anchors.length-1)return anchors[anchors.length-1];
        int nextBeat=Math.min(baseBeat+ANCHOR_STEP,count-1),span=Math.max(1,nextBeat-baseBeat);
        double f=(beat-baseBeat)/(double)span;
        return (int)Math.round(anchors[a]+(anchors[a+1]-anchors[a])*f);
    }

    private Note[] buildChart(int s){
        ArrayList<Note> out=new ArrayList<>();int count=BEAT_COUNTS[s],i=1;
        while(i<count-1){
            int t=beatTime(s,i),lane=Math.floorMod(i*2+s*3+i/4,5),mask=1<<lane;
            boolean chord=(i%8==0)||(i%12==5);
            if(chord){int lane2=Math.floorMod(lane+2+(i/8)%2,5);if(lane2==lane)lane2=(lane+1)%5;mask|=1<<lane2;}
            if(i%32==20){int lane3=(lane+4)%5;mask|=1<<lane3;}
            int hold=0;boolean longNote=(i%16==7)||(i%24==14);
            if(longNote&&i+2<count){int end=beatTime(s,i+2);hold=Math.max(430,end-t-95);out.add(new Note(t,mask,hold));i+=2;}
            else{out.add(new Note(t,mask,0));i++;}
        }
        return out.toArray(new Note[0]);
    }

    private void begin(){stopSong();chart=buildChart(song);resolved=new boolean[chart.length];holding=new boolean[chart.length];keyDown=new boolean[5];score=combo=bestCombo=misses=perfects=greats=goods=hits=0;judgment="";playing=true;finished=false;startNanos=System.nanoTime();playSong();}
    private void playSong(){Identifier id=new Identifier("guitarlegends","song."+SongCharts.IDS[song]);sound=PositionedSoundInstance.master(SoundEvent.of(id),1.0f,1.0f);MinecraftClient.getInstance().getSoundManager().play(sound);}
    private void stopSong(){if(sound!=null){MinecraftClient.getInstance().getSoundManager().stop(sound);sound=null;}}
    private long time(){return(System.nanoTime()-startNanos)/1_000_000L;}
    private int mask(){int m=0;for(int i=0;i<5;i++)if(keyDown[i])m|=1<<i;return m;}
    private int mult(){return Math.min(4,1+combo/10);}
    private void judge(String s,int c){judgment=s;judgmentColor=c;judgmentAt=System.currentTimeMillis();}

    private void hitAttempt(){
        if(!playing)return;long t=time();int down=mask();if(down==0)return;int best=-1;long diff=Long.MAX_VALUE;
        for(int i=0;i<chart.length;i++){if(resolved[i]||holding[i])continue;long d=Math.abs(chart[i].time-t);if(d<=HIT&&d<diff){best=i;diff=d;}}
        if(best<0)return;Note n=chart[best];if(down!=n.mask)return;
        hits++;combo++;bestCombo=Math.max(bestCombo,combo);int base;
        if(diff<=PERFECT){perfects++;base=100;judge("PERFECT!",0xFFFFE66A);}else if(diff<=GREAT){greats++;base=80;judge("GREAT!",0xFF70F5FF);}else{goods++;base=60;judge("GOOD",0xFF9CFF8E);}
        score+=base*mult();long now=System.currentTimeMillis();for(int lane=0;lane<5;lane++)if((n.mask&(1<<lane))!=0)laneFlash[lane]=now+120;
        if(n.hold>0)holding[best]=true;else resolved[best]=true;
    }

    private void miss(int i){if(i>=0&&i<resolved.length){resolved[i]=true;holding[i]=false;}misses++;combo=0;score=Math.max(0,score-20);judge("MISS",0xFFFF5E5E);MinecraftClient c=MinecraftClient.getInstance();if(c.player!=null)c.player.playSound(SoundEvents.ENTITY_VILLAGER_NO,0.32f,0.62f);}
    private void update(long t){int down=mask();for(int i=0;i<chart.length;i++){if(resolved[i])continue;Note n=chart[i];if(holding[i]){long end=n.time+n.hold;if(t<end-70&&(down&n.mask)!=n.mask)miss(i);else if(t>=end){holding[i]=false;resolved[i]=true;score+=Math.max(40,n.hold/10)*mult();judge("SUSTAIN!",0xFFB890FF);}}else if(t-n.time>HIT)miss(i);}if(t>=SongCharts.DURATIONS_MS[song]+350L)finish();}
    private void finish(){if(!playing)return;playing=false;finished=true;stopSong();if(score>record()){records.setProperty(recordKey(),Integer.toString(score));saveRecords();}}

    private int hy(){return Math.max(68,height/7);}private int hitY(){return height-84;}private double topW(){return Math.min(170.0,width*.23);}private double botW(){return Math.min(720.0,width-34.0);}private double roadW(double p){return topW()+(botW()-topW())*p;}private double left(double p){return width/2.0-roadW(p)/2.0;}private double center(int lane,double p){double w=roadW(p)/5.0;return left(p)+w*(lane+.5);}private int y(double p){return hy()+(int)Math.round((hitY()-hy())*p);}private double depth(long note,long t){double p=1.0-(note-t)/(double)TRAVEL;if(p<0)return 0;if(p>1)return 1;return Math.pow(p,1.35);}

    private void stage(DrawContext c){
        c.fill(0,0,width,height,0xFF060912);c.fill(0,0,width,hy()+18,0xFF0B1020);int cx=width/2;
        for(int i=-7;i<=7;i++){int h=8+(Math.abs(i*37)%35);c.fill(cx+i*13-4,hy()-h-16,cx+i*13+4,hy()-16,0xFF6F28C9);}
        c.drawCenteredTextWithShadow(textRenderer,"MINECRAFT GUITAR LEGENDS",cx,12,0xFFFFC83D);c.drawCenteredTextWithShadow(textRenderer,"F   G   H   J   K",cx,29,0xFFC5D8FF);
        int bottom=hitY()+42;for(int i=0;i<13;i++){double p=i/12.0;int yy=hy()+(int)((bottom-hy())*p),ww=7+(int)(22*p),gap=(int)(roadW(p)/2)+16+(int)(22*p),lx=cx-gap-ww,rx=cx+gap;c.fill(lx,yy-18,lx+ww,yy+26,0xFF343945);c.fill(rx,yy-18,rx+ww,yy+26,0xFF343945);c.fill(lx+ww/2-2,yy-5,lx+ww/2+3,yy+3,0xFFFFA52C);c.fill(rx+ww/2-2,yy-5,rx+ww/2+3,yy+3,0xFFFFA52C);}steve(c);
    }

    private void steve(DrawContext c){
        if(width<780||height<430)return;int x=28,yy=Math.max(88,height/4),s=Math.max(2,Math.min(4,height/190));
        c.fill(x+18*s,yy+52*s,x+25*s,yy+72*s,0xFF3436A8);c.fill(x+28*s,yy+52*s,x+35*s,yy+72*s,0xFF3436A8);c.fill(x+14*s,yy+25*s,x+39*s,yy+55*s,0xFF27B8C6);c.fill(x+17*s,yy+4*s,x+36*s,yy+25*s,0xFFB37A58);c.fill(x+17*s,yy+4*s,x+36*s,yy+10*s,0xFF3D2A20);c.fill(x+18*s,yy+9*s,x+22*s,yy+19*s,0xFF3D2A20);c.fill(x+31*s,yy+9*s,x+36*s,yy+19*s,0xFF3D2A20);c.fill(x+8*s,yy+29*s,x+15*s,yy+49*s,0xFF27B8C6);c.fill(x+39*s,yy+29*s,x+46*s,yy+49*s,0xFF27B8C6);c.fill(x+10*s,yy+42*s,x+34*s,yy+62*s,0xFF74C9EE);c.fill(x+14*s,yy+47*s,x+30*s,yy+58*s,0xFF1B283A);for(int i=0;i<28;i++){int gx=x+(30+i)*s,gy=yy+(44-i/3)*s;c.fill(gx,gy,gx+s,gy+3*s,0xFFB9C0CC);}
    }

    private void highway(DrawContext c,long t){
        int h=hy(),hit=hitY();
        for(int yy=h;yy<=hit;yy+=3){double p=(yy-h)/(double)Math.max(1,hit-h),w=roadW(p),l=left(p),lw=w/5.0;for(int lane=0;lane<5;lane++){int x1=(int)Math.round(l+lw*lane),x2=(int)Math.round(l+lw*(lane+1));c.fill(x1,yy,x2,Math.min(hit+1,yy+3),lane%2==0?0xFF10131B:0xFF0C0F16);int rw=Math.max(2,(int)(2+p*2));c.fill(x1+1,yy,x1+rw,Math.min(hit+1,yy+3),0xFF8A8F99);c.fill(x2-rw,yy,x2-1,Math.min(hit+1,yy+3),0xFF8A8F99);}}
        for(int i=0;i<30;i++){double p=Math.pow(i/29.0,1.45);int yy=y(p),thick=Math.max(1,(int)(1+p*3));double w=roadW(p),l=left(p),lw=w/5.0;for(int lane=0;lane<5;lane++){int x1=(int)(l+lw*lane+lw*.08),x2=(int)(l+lw*(lane+1)-lw*.08);c.fill(x1,yy,x2,yy+thick,0xFF6A5133);}}
        for(int i=0;i<chart.length;i++){if(resolved[i])continue;Note n=chart[i];long dt=n.time-t,end=n.time+n.hold-t;if(dt>TRAVEL||end<-HIT)continue;double hp=holding[i]?1.0:depth(n.time,t),tp=n.hold>0?depth(n.time+n.hold,t):hp;if(n.hold>0)tail(c,n.mask,tp,hp);heads(c,n.mask,hp,Integer.bitCount(n.mask)>1);}
        int ll=(int)left(1)-5,rr=(int)(left(1)+roadW(1))+5;c.fill(ll,hit-3,rr,hit+2,0xFFE8F6FF);c.fill(ll,hit-6,rr,hit-3,0x6646D8FF);carts(c);
    }

    private void tail(DrawContext c,int mask,double a,double b){double from=Math.min(a,b),to=Math.max(a,b);for(int lane=0;lane<5;lane++)if((mask&(1<<lane))!=0){for(double p=from;p<=to;p+=.018){int yy=y(p),x=(int)Math.round(center(lane,p));double lw=roadW(p)/5.0;int half=Math.max(2,(int)(lw*.09));c.fill(x-half,yy-2,x+half,yy+3,(COLORS[lane]&0x00FFFFFF)|0xCC000000);c.fill(x-Math.max(1,half/4),yy-1,x+Math.max(1,half/4),yy+2,0x88FFFFFF);}}}
    private void heads(DrawContext c,int mask,double p,boolean chord){int yy=y(p);double lw=roadW(p)/5.0;for(int lane=0;lane<5;lane++)if((mask&(1<<lane))!=0){int x=(int)Math.round(center(lane,p)),hw=Math.max(5,(int)(lw*(chord?.31:.27))),hh=Math.max(3,(int)(4+8*p));c.fill(x-hw-3,yy-hh-3,x+hw+3,yy+hh+3,0x66000000);c.fill(x-hw,yy-hh,x+hw,yy+hh,COLORS[lane]);c.fill(x-hw+3,yy-hh+2,x+hw-3,yy-Math.max(1,hh/3),0xCCFFFFFF);}}
    private void carts(DrawContext c){int yy=hitY()+14;long now=System.currentTimeMillis();for(int lane=0;lane<5;lane++){int x=(int)Math.round(center(lane,1)),lw=(int)(roadW(1)/5.0),w=Math.max(34,Math.min(72,lw-14)),body=now<laneFlash[lane]?0xFFFFFFFF:COLORS[lane];c.fill(x-w/2-4,yy-3,x+w/2+4,yy+29,0xFF05070B);c.fill(x-w/2,yy,x+w/2,yy+24,body);c.fill(x-w/2+5,yy+4,x+w/2-5,yy+12,0xFF20252C);c.fill(x-w/2+7,yy+5,x+w/2-7,yy+9,0xFF090B0E);c.fill(x-w/2+5,yy+24,x-w/2+14,yy+29,0xFF111111);c.fill(x+w/2-14,yy+24,x+w/2-5,yy+29,0xFF111111);c.drawCenteredTextWithShadow(textRenderer,KEYS[lane],x,yy+34,COLORS[lane]);}}

    private void hud(DrawContext c,long t){c.fill(10,8,176,52,0xCC090D16);c.drawTextWithShadow(textRenderer,"SCORE",18,15,0xFFC5D8FF);c.drawTextWithShadow(textRenderer,String.format("%08d",score),18,30,0xFFFFD43B);c.fill(width-156,8,width-10,52,0xCC090D16);c.drawCenteredTextWithShadow(textRenderer,"COMBO",width-83,15,0xFFC5D8FF);c.drawCenteredTextWithShadow(textRenderer,"x"+combo,width-83,31,combo>0?0xFFFFD43B:0xFFFFFFFF);c.drawCenteredTextWithShadow(textRenderer,SongCharts.NAMES[song]+"  "+clock(t)+" / "+clock(SongCharts.DURATIONS_MS[song]),width/2,height-20,0xFFFFFFFF);if(!judgment.isEmpty()&&System.currentTimeMillis()-judgmentAt<650)c.drawCenteredTextWithShadow(textRenderer,judgment,width/2,hitY()-45,judgmentColor);}
    private String clock(long ms){long sec=Math.max(0,ms)/1000;return String.format("%d:%02d",sec/60,sec%60);}

    private void menu(DrawContext c){c.fill(0,0,width,height,0xFF070A12);stage(c);c.fill(width/2-215,58,width/2+215,height-42,0xE5090D17);c.drawCenteredTextWithShadow(textRenderer,"ESCOLHA A MUSICA",width/2,72,0xFFFFFFFF);for(int i=0;i<SongCharts.NAMES.length;i++){int yy=98+i*20;c.drawCenteredTextWithShadow(textRenderer,(i==song?">  ":"   ")+SongCharts.NAMES[i],width/2,yy,i==song?0xFFFFE45C:0xFFD6E4F5);}c.drawCenteredTextWithShadow(textRenderer,"ENTER: JOGAR  |  CIMA/BAIXO: MUSICA  |  ESC: SAIR",width/2,height-70,0xFF9CFFAA);c.drawCenteredTextWithShadow(textRenderer,"Acordes: aperte as cores juntas  |  Notas longas: segure ate o fim",width/2,height-54,0xFF9EDBFF);}
    private void results(DrawContext c){c.fill(0,0,width,height,0xEE050812);int l=width/2-190,r=width/2+190,t=height/2-105,b=height/2+105;c.fill(l,t,r,b,0xF20B101D);c.fill(l,t,r,t+3,0xFFFFC63B);c.drawCenteredTextWithShadow(textRenderer,"FIM DA MUSICA",width/2,t+17,0xFFFFD75C);c.drawCenteredTextWithShadow(textRenderer,"PONTOS: "+score+"   RECORDE: "+record(),width/2,t+42,0xFFFFFFFF);c.drawCenteredTextWithShadow(textRenderer,"PERFECT: "+perfects+"   GREAT: "+greats+"   GOOD: "+goods,width/2,t+65,0xFFB8F7FF);c.drawCenteredTextWithShadow(textRenderer,"ACERTOS: "+hits+"   ERROS: "+misses+"   MAIOR COMBO: "+bestCombo,width/2,t+88,0xFFFFD0D0);c.drawCenteredTextWithShadow(textRenderer,"ENTER: JOGAR DE NOVO  |  BACKSPACE: MENU",width/2,b-28,0xFF9CFFAA);}

    @Override public void render(DrawContext c,int mx,int my,float delta){if(!playing&&!finished)menu(c);else if(playing){long t=time();update(t);stage(c);highway(c,t);hud(c,t);}else results(c);super.render(c,mx,my,delta);}
    @Override public boolean keyPressed(int key,int scan,int mods){if(finished){if(key==GLFW.GLFW_KEY_ENTER||key==GLFW.GLFW_KEY_KP_ENTER){begin();return true;}if(key==GLFW.GLFW_KEY_BACKSPACE){finished=false;return true;}}if(!playing){if(key==GLFW.GLFW_KEY_UP){song=(song+SongCharts.NAMES.length-1)%SongCharts.NAMES.length;return true;}if(key==GLFW.GLFW_KEY_DOWN){song=(song+1)%SongCharts.NAMES.length;return true;}if(key==GLFW.GLFW_KEY_ENTER||key==GLFW.GLFW_KEY_KP_ENTER){begin();return true;}}if(playing)for(int lane=0;lane<5;lane++)if(key==KEYCODES[lane]){if(!keyDown[lane]){keyDown[lane]=true;hitAttempt();}return true;}if(key==GLFW.GLFW_KEY_ESCAPE){close();return true;}return super.keyPressed(key,scan,mods);}
    @Override public boolean keyReleased(int key,int scan,int mods){if(playing)for(int lane=0;lane<5;lane++)if(key==KEYCODES[lane]){keyDown[lane]=false;long t=time();for(int i=0;i<chart.length;i++)if(holding[i]&&!resolved[i]){Note n=chart[i];if((n.mask&(1<<lane))!=0&&t<n.time+n.hold-70)miss(i);}return true;}return super.keyReleased(key,scan,mods);}
    @Override public void close(){stopSong();super.close();}
    @Override public boolean shouldPause(){return false;}
}
