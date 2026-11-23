package cn.goldcc.review;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.AtomicFile;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

public class ReviewActivity extends Activity {
    private final int pine=0xff194b3b, ink=0xff20372f, cream=0xfff7f5eb;
    private final ArrayList<String> names=new ArrayList<>(), costs=new ArrayList<>();
    private final ArrayList<String> ids=new ArrayList<>();
    private final ArrayList<Bitmap> icons=new ArrayList<>();
    private final ArrayList<int[]> refs=new ArrayList<>();
    private final ArrayList<Integer> chosen=new ArrayList<>();
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private LinearLayout page;
    private Bitmap photo;
    private int row, task;
    private boolean dirty;
    private String record, feedback;
    private File archive;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(pine);
        getWindow().setNavigationBarColor(pine);
        archive=new File(getFilesDir(),"reviews");
        archive.mkdirs();
        try (BufferedReader in=new BufferedReader(new InputStreamReader(getAssets().open("units.tsv"),"UTF-8"))) {
            String line;
            while ((line=in.readLine())!=null) {
                String[] fields=line.split("\t");
                try(InputStream stream=getAssets().open(fields[0]+".png")) {
                    Bitmap icon=BitmapFactory.decodeStream(stream);
                    int[] pixels=new int[4096];
                    icon.getPixels(pixels,0,64,0,0,64,64);
                    ids.add(fields[0]); refs.add(pixels); icons.add(icon); names.add(fields[1]); costs.add(fields[2]);
                }
            }
            home();
        } catch(Exception e) { toast("头像资料加载失败，请重新安装"); finish(); }
    }
    private int dp(int n){return Math.round(getResources().getDisplayMetrics().density*n);}
    private void toast(String msg){Toast.makeText(this,msg,Toast.LENGTH_LONG).show();}
    private GradientDrawable box(int color,int radius){
        GradientDrawable bg=new GradientDrawable();bg.setColor(color);bg.setCornerRadius(dp(radius));return bg;
    }
    private void layout(String headline){
        ScrollView scroll=new ScrollView(this);scroll.setBackgroundColor(cream);
        page=new LinearLayout(this);page.setOrientation(1);page.setPadding(dp(18),dp(22),dp(18),dp(32));
        scroll.addView(page);setContentView(scroll);
        TextView title=label(headline,30);title.setTypeface(null,Typeface.BOLD);
        android.graphics.drawable.Drawable leaf=getDrawable(R.drawable.leaf);leaf.setBounds(0,0,dp(32),dp(32));
        title.setCompoundDrawables(leaf,null,null,null);title.setCompoundDrawablePadding(dp(10));
    }
    private TextView label(String text,int size){
        TextView v=new TextView(this);v.setTextColor(ink);v.setTextSize(size);v.setText(text);
        v.setPadding(0,dp(5),0,dp(6));page.addView(v);return v;
    }
    private Button action(LinearLayout parent,String text,Runnable fn,boolean primary){
        Button b=new Button(this);b.setText(text);b.setAllCaps(false);b.setTextSize(16);
        b.setTextColor(primary?0xffffffff:pine);b.setBackground(box(primary?pine:0xffe4ecdf,12));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(parent==page?-1:-2,-2);
        lp.setMargins(0,dp(4),dp(3),dp(4));parent.addView(b,lp);
        b.setOnClickListener(v->fn.run());return b;
    }
    private static String date(String id){
        return new SimpleDateFormat("yyyy-MM-dd HH:mm",Locale.CHINA).format(new Date(Long.parseLong(id)));
    }
    private JSONObject meta(String id) throws Exception {
        return new JSONObject(new String(new AtomicFile(new File(new File(archive,id),"review.json")).readFully(),"UTF-8"));
    }
    private void home(){
        task++;photo=null;record=null;dirty=false;layout("林间复盘");
        label("自然之力  ·  S18",17);
        label("对局结束后，打开「特质/职业」页，让排名、出场阵容和八行玩家完整入镜。选择未裁剪的横屏截图。",16);
        action(page,"从相册选择截图",()->{
            Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("image/*").addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(i,7);
        },true);
        label("我的复盘",23);
        File[] files=archive.listFiles(f->f.getName().matches("[0-9]+"));
        int count=0;
        if(files!=null){
            Arrays.sort(files,(a,b)->b.getName().compareTo(a.getName()));
            for(File f:files)try {
                String id=f.getName();JSONObject data=meta(id);
                action(page,date(id)+"  ·  第 "+(data.getInt("row")+1)+" 名  ·  "+data.getJSONArray("units").length()+" 枚",()->open(id),false);
                count++;
            }catch(Exception e){label("一条复盘的记录文件无法读取",14);}
        }
        if(count==0)label("保存后，可在这里回看截图和校正后的阵容。",15);
    }
    private void open(String id){
        try{
            JSONObject data=meta(id);Bitmap loaded=BitmapFactory.decodeFile(new File(new File(archive,id),"screen.jpg").getPath());
            if(loaded==null)throw new IOException();
            chosen.clear();JSONArray units=data.getJSONArray("units");
            for(int j=0;j<units.length();j++)chosen.add(ids.indexOf(units.getString(j)));
            photo=loaded;row=data.getInt("row");record=id;dirty=false;feedback="已保存的阵容";editor();
        }catch(Exception e){home();toast("无法打开这条复盘");}
    }
    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);
        if(request!=7||result!=RESULT_OK||data==null||data.getData()==null)return;
        Uri uri=data.getData();int ticket=++task;layout("正在读取截图");
        worker.execute(()->{
            try{
                BitmapFactory.Options opts=new BitmapFactory.Options();opts.inJustDecodeBounds=true;
                try(InputStream s=getContentResolver().openInputStream(uri)){BitmapFactory.decodeStream(s,null,opts);}
                if(opts.outWidth<1||opts.outHeight<1)throw new IOException();
                opts.inJustDecodeBounds=false;opts.inSampleSize=1;
                while(Math.max(opts.outWidth,opts.outHeight)/opts.inSampleSize>3600)opts.inSampleSize*=2;
                Bitmap bitmap;
                try(InputStream s=getContentResolver().openInputStream(uri)){bitmap=BitmapFactory.decodeStream(s,null,opts);}
                if(bitmap==null)throw new IOException();
                runOnUiThread(()->{if(ticket==task){photo=bitmap;record=null;row=0;dirty=true;chosen.clear();scan();}});
            }catch(Exception e){runOnUiThread(()->{if(ticket==task){home();toast("无法读取，请选择 PNG 或 JPEG 游戏截图");}});}
        });
    }
    private void scan(){
        int ticket=++task;Bitmap src=photo;int rank=row;
        layout("正在比对头像");label("第 "+(rank+1)+" 名",16);
        worker.execute(()->{
            int w=Math.round(src.getWidth()*600f/src.getHeight());
            ArrayList<Integer> found=new ArrayList<>();
            if(w>=950&&w<=1600){
                Bitmap small=Bitmap.createScaledBitmap(src,w,600,true);int[] px=new int[w*600];
                small.getPixels(px,0,w,0,0,w,600);
                for(Matcher.Hit hit:Matcher.recognize(px,w,rank,refs,names))found.add(hit.unit);
            }
            runOnUiThread(()->{if(task!=ticket)return;
                chosen.clear();chosen.addAll(found);dirty=true;
                feedback=found.isEmpty()?"未找到可信头像。请核对截图版式和名次，或手动补上弈子。":"识别到 "+found.size()+" 枚候选。低置信度位置已略过，请对照截图补全。";
                editor();
            });
        });
    }
    private void picture(Bitmap bitmap,int maxDp){
        ImageView view=new ImageView(this);view.setImageBitmap(bitmap);view.setAdjustViewBounds(true);
        view.setMaxHeight(dp(maxDp));view.setScaleType(ImageView.ScaleType.FIT_CENTER);page.addView(view,-1,-2);
        view.setContentDescription("查看截图细节");
        view.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("双指缩放 · 拖动查看")
            .setView(new PhotoDetail(this,bitmap)).setPositiveButton("返回",null).show());
    }
    private void editor(){
        layout("阵容校正");picture(photo,250);
        action(page,"查看第 "+(row+1)+" 名  ▾",()->{
            String[] ranks=new String[8];for(int i=0;i<8;i++)ranks[i]="第 "+(i+1)+" 名";
            new AlertDialog.Builder(this).setTitle("选择名次并重新识别").setItems(ranks,(d,n)->{
                if(n!=row)new AlertDialog.Builder(this).setMessage("切换后将重新识别并替换当前列表。")
                    .setPositiveButton("切换",(a,b)->{row=n;scan();}).setNegativeButton("取消",null).show();
            }).show();
        },false);
        int w=photo.getWidth(),h=photo.getHeight();
        float scale=h/600f;
        int x=Math.max(0,Math.round(w*.5f-251*scale)), y=Math.min(h-1,Math.round((93+52.8f*row)*scale));
        int cw=Math.min(w-x,Math.max(1,Math.round(424*scale))),ch=Math.min(h-y,Math.max(1,Math.round(48*scale)));
        picture(Bitmap.createBitmap(photo,x,y,cw,ch),110);
        label(feedback,15);
        for(int i=0;i<chosen.size();i++){
            final int pos=i;int unit=chosen.get(i);
            LinearLayout bar=new LinearLayout(this);bar.setGravity(Gravity.CENTER_VERTICAL);page.addView(bar);
            ImageView icon=new ImageView(this);icon.setImageBitmap(icons.get(unit));bar.addView(icon,dp(40),dp(40));
            TextView name=new TextView(this);name.setText("  "+names.get(unit));name.setTextColor(ink);name.setTextSize(17);
            bar.addView(name,new LinearLayout.LayoutParams(0,-2,1));
            action(bar,"替换",()->select(pos),false);
            action(bar,"删除",()->{chosen.remove(pos);modified();},false);
        }
        action(page,"＋ 补上弈子",()->select(-1),false);
        action(page,"保存复盘",this::save,true);
        action(page,"返回我的复盘",this::leave,false);
    }
    private void modified(){dirty=true;feedback="已手动校正";editor();}
    private void select(int position){
        LinkedHashMap<String,Integer> options=new LinkedHashMap<>();
        for(int i=0;i<names.size();i++)options.putIfAbsent(costs.get(i)+"费  "+names.get(i),i);
        String[] labels=options.keySet().toArray(new String[0]);Integer[] ids=options.values().toArray(new Integer[0]);
        new AlertDialog.Builder(this).setTitle(position<0?"补上弈子":"替换弈子").setItems(labels,(d,n)->{
            if(position<0)chosen.add(ids[n]);else chosen.set(position,ids[n]);modified();
        }).setNegativeButton("取消",null).show();
    }
    private void save(){
        try {
            String id=record==null?String.valueOf(System.currentTimeMillis()):record;
            File dir=new File(archive,record==null?"draft-"+id:id);dir.mkdirs();
            if(record==null){
                try(FileOutputStream out=new FileOutputStream(new File(dir,"screen.jpg"))){
                    if(!photo.compress(Bitmap.CompressFormat.JPEG,95,out))throw new IOException();
                    out.getFD().sync();
                }
            }
            JSONObject data=new JSONObject();data.put("row",row);JSONArray units=new JSONArray();
            for(int unit:chosen)units.put(ids.get(unit));
            data.put("units",units);
            AtomicFile file=new AtomicFile(new File(dir,"review.json"));FileOutputStream stream=file.startWrite();
            try {stream.write(data.toString().getBytes("UTF-8"));file.finishWrite(stream);}
            catch(IOException e){file.failWrite(stream);throw e;}
            if(record==null && !dir.renameTo(new File(archive,id)))throw new IOException();
            home();toast("已保存复盘");
        }catch(Exception e){toast("保存失败，请检查存储空间后重试");}
    }
    private void leave(){
        if(!dirty){home();return;}
        new AlertDialog.Builder(this).setMessage("离开会放弃本次修改。").setPositiveButton("放弃修改",(d,n)->home()).setNegativeButton("继续校正",null).show();
    }
    @Override public void onBackPressed(){if(photo!=null)leave();else super.onBackPressed();}
    @Override protected void onDestroy(){task++;worker.shutdownNow();super.onDestroy();}
}
