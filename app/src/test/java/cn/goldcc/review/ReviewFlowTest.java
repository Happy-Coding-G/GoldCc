package cn.goldcc.review;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import java.io.File;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlertDialog;
import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=28)
public class ReviewFlowTest {
    private static TextView find(View root, String part) {
        if(root instanceof TextView && ((TextView)root).getText().toString().contains(part))return (TextView)root;
        if(root instanceof ViewGroup){
            ViewGroup group=(ViewGroup)root;
            for(int i=0;i<group.getChildCount();i++){
                TextView child=find(group.getChildAt(i),part);
                if(child!=null)return child;
            }
        }
        return null;
    }
    private static TextView await(Activity activity,String text) throws Exception {
        long end=System.currentTimeMillis()+15000;
        while(System.currentTimeMillis()<end){
            shadowOf(Looper.getMainLooper()).idle();
            TextView view=find(activity.getWindow().getDecorView(),text);
            if(view!=null)return view;
            Thread.sleep(30);
        }
        throw new AssertionError("Missing control: "+text);
    }
    private static void pickUnit(int index){
        AlertDialog dialog=ShadowAlertDialog.getLatestAlertDialog();
        dialog.getListView().performItemClick(null,index,index);
        dialog.dismiss();
    }
    @Test public void importRealSettlementWhenProvided() throws Exception {
        String sample=System.getenv("GOLDCC_TEST_IMAGE");
        org.junit.Assume.assumeTrue(sample!=null);
        ActivityController<ReviewActivity> controller=Robolectric.buildActivity(ReviewActivity.class).setup();
        ReviewActivity screen=controller.get();
        screen.onActivityResult(7,Activity.RESULT_OK,new Intent().setData(Uri.fromFile(new File(sample))));
        await(screen,"识别到 8 枚候选");
        for(String name:new String[]{"瑟提","纳尔","阿狸","蔚","卡尔玛","约里克","婕拉","芸阿娜"})
            assertNotNull(await(screen,"  "+name));
        controller.pause().stop().destroy();
    }
    @Test public void correctAndReopen() throws Exception {
        ActivityController<ReviewActivity> first=Robolectric.buildActivity(ReviewActivity.class).setup();
        ReviewActivity screen=first.get();
        await(screen,"从相册选择截图").performClick();
        assertEquals(Intent.ACTION_OPEN_DOCUMENT,shadowOf(screen).getNextStartedActivityForResult().intent.getAction());
        File picture=new File(screen.getCacheDir(),"blank.png");
        try(java.io.FileOutputStream out=new java.io.FileOutputStream(picture)){
            Bitmap.createBitmap(1200,600,Bitmap.Config.ARGB_8888).compress(Bitmap.CompressFormat.PNG,100,out);
        }
        screen.onActivityResult(7,Activity.RESULT_OK,new Intent().setData(Uri.fromFile(picture)));
        await(screen,"未找到可信头像");
        await(screen,"＋ 补上弈子").performClick();pickUnit(0);
        await(screen,"替换").performClick();pickUnit(1);
        await(screen,"＋ 补上弈子").performClick();pickUnit(2);
        await(screen,"删除").performClick();
        await(screen,"保存复盘").performClick();
        await(screen,"1 枚");
        first.pause().stop().destroy();
        ActivityController<ReviewActivity> next=Robolectric.buildActivity(ReviewActivity.class).setup();
        ReviewActivity reopened=next.get();
        await(reopened,"1 枚").performClick();
        await(reopened,"已保存的阵容");
        assertNotNull(await(reopened,"蕾欧娜"));
        next.pause().stop().destroy();
    }
}