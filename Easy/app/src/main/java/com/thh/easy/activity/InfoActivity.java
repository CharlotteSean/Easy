package com.thh.easy.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import com.squareup.picasso.Picasso;
import com.thh.easy.R;
import com.thh.easy.entity.User;
import com.thh.easy.util.FileUtil;
import com.thh.easy.util.LogUtil;
import com.thh.easy.util.RoundedTransformation;

import java.io.File;

/**
 * 关于界面：
 *  icon + 项目名 + 版本号 + 小组成员
 *  // TODO 将侧滑栏的用户信息设置成全局的，不该设在这六个acitvity里面
 */
public class InfoActivity extends BaseDrawerActivity implements BaseDrawerActivity.OnStartActivityListener{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);
        setUserSp();
        setOnStartActivityListener(this);
    }

    /**
     * 控制返回跳到首页
     * @param targetActivity
     */
    @Override
    public void onStartActivity(Class<?> targetActivity) {

        if (InfoActivity.class == targetActivity)
            return;

        if (targetActivity == MainActivity.class) {
            finish();
            overridePendingTransition(0, 0);
            return;
        }

        Intent intent = new Intent(InfoActivity.this, targetActivity);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        startActivity(intent);
        finish();
        overridePendingTransition(0, 0);

    }

    /**
     * 设置用户配置信息
     */
    private void setUserSp() {

        // 验证用户之前是否登录
        sp = getSharedPreferences("user_sp", Context.MODE_PRIVATE);

        if (sp.getBoolean("user_login", false)) {
            u = (User) FileUtil.readObject(this, "user");
            if (u == null)
                return;
            setUserInfo(u);
        }

    }

    SharedPreferences sp;
    User u;

    /**
     * 设置用户信息
     * @param u
     */
    private void setUserInfo (User u) {
        username.setText(u.getUsername());

        if ("0".equals(u.getGender())) {
            gender.setBackgroundResource(R.mipmap.ic_user_female);
        }
        else if ("1".equals(u.getGender())) {
            gender.setBackgroundResource(R.mipmap.ic_user_male);
        }
        else {
            gender.setBackgroundResource(R.mipmap.ic_user_sox);
        }

        if (u.getAvatarFilePath() != null) {
            File avatar = new File (u.getAvatarFilePath());
            LogUtil.d(InfoActivity.this, "头像url :" + u.getAvatarFilePath());
            Picasso.with(getApplicationContext())
                    .load(avatar)
                    .centerCrop()
                    .resize(avatarSize, avatarSize)
                    .transform(new RoundedTransformation())
                    .into(ivMenuUserProfilePhoto);
        }
    }


}
