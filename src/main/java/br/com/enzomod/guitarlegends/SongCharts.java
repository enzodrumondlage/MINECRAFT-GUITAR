package br.com.enzomod.guitarlegends;

import java.io.InputStream;

public final class SongCharts {
    private SongCharts() {}
    public record Chart(int[] times, byte[] masks, short[] holds) {}
    public static final String[] NAMES = {
        "Mega Funk Fora PT","Pense no Lula","Sabor Picanha","Solinho do Fora PT","Tropa do Capitao!","Wuthering Heights",
        "Aaron Smith - Dancin - Krono Remix ft. Luvli","Alan Walker, Dash Berlin & Vikkstar - Better Off (Alone, Pt. III)",
        "Calvin Harris & Disciples - How Deep Is Your Love","Clean Bandit - Rockabye (feat. Sean Paul & Anne-Marie)",
        "Coopex, Nito Onna, CPX - Smells Like Teen Spirit","David Guetta Feat. Kid Cudi - Memories (Cat Dealers Remix)",
        "Disclosure - You & Me (Rivo Remix) ft. Eliza Doolittle","DJ Snake - Let Me Love You ft. Justin Bieber",
        "Duke Dumont - Ocean Drive (Ivahn Johan Afro House Remix)","Edward Maya feat. Vika Jigulina - Stereo Love (Twelve Remix)",
        "Felix Jaehn, Whitney Houston - It's Not Right But It's Okay","Flip Capella, Emdey, Treetalk - Down Under ft. Leines",
        "Gabry Ponte x LUM!X x Prezioso - Thunder","Gryffin, ZOHARA - Remember","Illenium - Lost (ft. Emilie Brandt)",
        "Kato feat. Jon - Turn The Lights Off (Dj Dark Remix)","Lana Del Rey vs Cedric Gervais 'Summertime Sadness' Remix",
        "Lola Young - Messy (NOVEMBER KID Remix)","Ofenbach - Overdrive feat. Norma Jean Martine",
        "R3HAB x A Touch Of Class - All Around The World (La La La)","Rihanna - S&M (NORTKASH & 5ROCK Remix)",
        "Sabrina Carpenter Vs Kaaze - Espresso Papi (Djs From Mars Bootleg)","SAINt JHN - Roses (Imanbek Remix)",
        "Starley - Call On Me (Ryan Riback Remix)","The Weeknd - Popular (Mindloco Remix)","THYPONYX - Seven Nation Army","Tiësto - The Business"
    };
    public static final String[] IDS = {
        "mega_funk_fora_pt","pense_no_lula","sabor_picanha","solinho_fora_pt","tropa_do_capitao","wuthering_heights",
        "aaron_smith_dancin_krono_remix_ft_luvli","alan_walker_dash_berlin_vikkstar_better_off_alone_pt_iii_off",
        "calvin_harris_disciples_how_deep_is_your_love","clean_bandit_rockabye_feat_sean_paul_anne_marie",
        "coopex_nito_onna_cpx_smells_like_teen_spirit","david_guetta_feat_kid_cudi_memories_cat_dealers_remix",
        "disclosure_you_me_rivo_remix_ft_eliza_doolittle","dj_snake_let_me_love_you_ft_justin_bieber",
        "duke_dumont_ocean_drive_ivahn_johan_afro_house_remix","edward_maya_feat_vika_jigulina_stereo_love_twelve_remix",
        "felix_jaehn_whitney_houston_it_s_not_right_but_it_s_okay","flip_capella_emdey_treetalk_down_under_ft_leines",
        "gabry_ponte_x_lum_x_x_prezioso_thunder_dance_video","gryffin_zohara_remember","illenium_lost_ft_emilie_brandt",
        "kato_feat_jon_turn_the_lights_off_dj_dark_remix","lana_del_rey_vs_cedric_gervais_summertime_sadness_remix",
        "lola_young_messy_november_kid_remix","ofenbach_overdrive_feat_norma_jean_martine",
        "r3hab_x_a_touch_of_class_all_around_the_world_la_la_la","rihanna_s_m_nortkash_5rock_remix",
        "sabrina_carpenter_vs_kaaze_espresso_papi_djs_from_mars_bootl","saint_jhn_roses_imanbek_remix",
        "starley_call_on_me_ryan_riback_remix","the_weeknd_popular_mindloco_remix_free_download","thyponyx_seven_nation_army","tiesto_the_business"
    };
    public static final int[] DURATIONS_MS = {166139,147383,148114,147122,122149,279824,243171,151081,260516,253887,186027,270710,208585,205323,234440,212823,213426,179885,162830,221739,200533,176437,225675,124540,179049,147830,201280,177610,177853,197532,204011,215226,191901};
    public static final byte[] AVG_DIFFICULTY = new byte[]{2,3,3,1,1,0,0,2,2,1,1,1,2,3,0,3,3,0,3,2,2,1,0,3,0,2,3,1,3,0,1,2,0};
    public static final int[] BPM = {129,129,136,123,123,117,117,129,123,136,117,123,162,129,117,129,129,123,136,123,136,129,123,129,129,123,123,129,123,103,117,129,123};
    private static final Chart[][] CACHE = new Chart[NAMES.length][4];

    // Legacy view used by the current screen while the UI is migrated to chart(song,difficulty).
    // It is generated from the Normal chart so old callers compile without duplicating chart data.
    public static final int[][] TIMES_MS = new int[NAMES.length][];
    public static final byte[][] LANES = new byte[NAMES.length][];
    public static final byte[][] EXTRA = new byte[NAMES.length][];
    static {
        for (int s=0;s<NAMES.length;s++) {
            Chart c=chart(s,1);
            TIMES_MS[s]=c.times();
            LANES[s]=new byte[c.masks().length];
            EXTRA[s]=new byte[c.masks().length];
            for(int i=0;i<c.masks().length;i++) {
                int mask=c.masks()[i]&31;
                LANES[s][i]=(byte)(mask==0?0:Integer.numberOfTrailingZeros(mask));
            }
        }
    }

    public static Chart chart(int song,int difficulty) {
        song=Math.floorMod(song,NAMES.length); difficulty=Math.max(0,Math.min(3,difficulty));
        if(CACHE[song][difficulty]!=null)return CACHE[song][difficulty];
        String path="/assets/guitarlegends/charts/"+IDS[song]+"_"+difficulty+".bin";
        try(InputStream in=SongCharts.class.getResourceAsStream(path)) {
            if(in==null)return CACHE[song][difficulty]=new Chart(new int[0],new byte[0],new short[0]);
            byte[] raw=in.readAllBytes();int n=raw.length/7;int[] times=new int[n];byte[] masks=new byte[n];short[] holds=new short[n];
            for(int i=0,p=0;i<n;i++,p+=7){times[i]=((raw[p]&255)<<24)|((raw[p+1]&255)<<16)|((raw[p+2]&255)<<8)|(raw[p+3]&255);masks[i]=raw[p+4];holds[i]=(short)(((raw[p+5]&255)<<8)|(raw[p+6]&255));}
            return CACHE[song][difficulty]=new Chart(times,masks,holds);
        }catch(Exception e){return CACHE[song][difficulty]=new Chart(new int[0],new byte[0],new short[0]);}
    }
}
