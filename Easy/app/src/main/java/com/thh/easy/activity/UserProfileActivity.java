package com.thh.easy.activity;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.ext.HttpCallback;
import com.android.volley.ext.RequestInfo;
import com.android.volley.ext.tools.HttpTools;
import com.squareup.picasso.Picasso;
import com.thh.easy.R;
import com.thh.easy.adapter.UserProfileAdapter;
import com.thh.easy.constant.StringConstant;
import com.thh.easy.util.ImageUtils;
import com.thh.easy.util.LogUtil;
import com.thh.easy.util.RoundedTransformation;
import com.thh.easy.util.Utils;
import com.thh.easy.view.RevealBackgroundView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 *   个人信息界面:
 *         个人信息
 *         我的图集
 *
 *  //TODO 测试更改头像
 *  //TODO 增加图片
 * @author cloud
 * @time 2015 10 24
 *
 */
public class UserProfileActivity extends AppCompatActivity implements RevealBackgroundView.OnStateChangeListener {

    public static final String ARG_REVEAL_START_LOCATION = "reveal_start_location";
    public static final String TAG = "UserProfileActivity";


    @Bind(R.id.iv_user_profile_photo)
    ImageView ivUserPhoto;           // 用户头像

    @Bind(R.id.tv_profile_user_name)
    TextView tvUserName;             // 用户名

    @Bind(R.id.tv_profile_user_rp)
    TextView tvUserRP;               // 用户节操值

    @Bind(R.id.tv_profile_user_email)
    TextView tvEmail;                 // 用户简介

    @Bind(R.id.tv_profile_post_num)
    TextView tvPosts;                // 发过的帖子数

    @Bind(R.id.tv_profile_org_act)
    TextView tvOrgActs;              // 发起的活动数

    @Bind(R.id.tv_profile_collect)
    TextView tvCollects;             // 收藏的帖子数

    @Bind(R.id.vRevealBackground)
    RevealBackgroundView vRevealBackground; // 展开动画背景

    @Bind(R.id.rvUserProfile)
    RecyclerView rvUserProfile;                   // 图集
    private UserProfileAdapter userPhotosAdapter; // 图集adapter


    private int avatarSize;
    HttpTools httpTools;

    // 保存头像的文件
    File imageFile;
    private Bitmap bitmap;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);
        ButterKnife.bind(UserProfileActivity.this);

        userId = getIntent().getIntExtra(StringConstant.COMMENT_UID, 1);

        avatarSize = UserProfileActivity.this
                .getResources().getDimensionPixelSize(R.dimen.comment_avatar_size);

        LogUtil.d(UserProfileActivity.this, "进入userProfileActivty----> userID:" + userId, "93");

        HttpTools.init(this);
        httpTools = new HttpTools(this);

        loadProfileData();
        setupUserProfileGrid();
        setupRevealBackground(savedInstanceState);
    }

    private int userId;

    /**
     * 加载用户信息
     */
    private void loadProfileData(){
        Map<String , String> params = new HashMap<>();
        params.put(StringConstant.USER_ID, "" + getIntent().getIntExtra(StringConstant.COMMENT_UID, 1));
        RequestInfo info = new RequestInfo(StringConstant.SERVER_PROFILE_URL, params);

        httpTools.post(info, new HttpCallback() {
            @Override
            public void onStart() {
                LogUtil.i(UserProfileActivity.this, "开始联网啦");
            }

            @Override
            public void onFinish() {
                LogUtil.i(UserProfileActivity.this, "结束联网啦");
            }

            @Override
            public void onResult(String s) {
                LogUtil.d(UserProfileActivity.this, "服务器返回的用户信息： " + s , "122");
                readJson(s);
            }

            @Override
            public void onError(Exception e) {
                LogUtil.i(UserProfileActivity.this, "联网出错啦");
            }

            @Override
            public void onCancelled() {
                LogUtil.i(UserProfileActivity.this, "取消联网啦");
            }

            @Override
            public void onLoading(long l, long l1) {
                LogUtil.i(UserProfileActivity.this, "正在联网呢");
            }
        });
    }

    /**
     * 解析网络数据
     * @param jsonResult
     */
    private void readJson(String jsonResult){
            LogUtil.i(UserProfileActivity.this, "开始读取json数据");
        try {
            JSONArray jsonArray = new JSONArray(jsonResult);

            JSONObject jsonObject = jsonArray.getJSONObject(0);

            JSONObject userObject = jsonObject.getJSONObject("users");

            String imgUrl = userObject.getJSONObject("image").getString("urls");

            tvUserName.setText(userObject.getString("name"));             // 用户名
            tvUserRP.setText("rp: " + userObject.getInt("rp"));           // 节操值
            tvEmail.setText(userObject.getString("email"));               // 用户邮箱
            tvPosts.setText(""+jsonObject.getInt("post"));                 // 发过的帖子数
            tvCollects.setText(""+jsonObject.getInt("collect"));             // 收藏的帖子数
            tvOrgActs.setText(""+jsonObject.getInt("act"));

            // 加载头像
            if(imgUrl!=null){
                Picasso.with(this)
                        .load(StringConstant.SERVER_IP + imgUrl)
                        .centerCrop()
                        .resize(avatarSize, avatarSize)
                        .transform(new RoundedTransformation())
                        .placeholder(R.mipmap.bili_default_avatar)
                        .into(ivUserPhoto);
            }
        } catch (JSONException e) {
            e.printStackTrace();
            Log.e(TAG,"171-->"+" JSON 解析出错");
        }
    }



    /**
     * 初始化展开动画
     * @param savedInstanceState
     */
    private void setupRevealBackground(Bundle savedInstanceState) {
        vRevealBackground.setOnStateChangeListener(this);
        if (savedInstanceState == null) {
            final int[] startingLocation =
                    getIntent().getIntArrayExtra(ARG_REVEAL_START_LOCATION);

            vRevealBackground.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    vRevealBackground.getViewTreeObserver().removeOnPreDrawListener(this);
                    vRevealBackground.startFromLocation(startingLocation);
                    return false;
                }
            });
        } else {
            userPhotosAdapter.setLockedAnimations(true);
            vRevealBackground.setToFinishedFrame();
        }
    }


    /**
     * 设置图集内容
     */
    private void setupUserProfileGrid() {
        final StaggeredGridLayoutManager layoutManager =
                new StaggeredGridLayoutManager(3, StaggeredGridLayoutManager.VERTICAL);

        rvUserProfile.setLayoutManager(layoutManager);
        rvUserProfile.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                userPhotosAdapter.setLockedAnimations(true);
            }
        });
    }


    /**
     * 进入界面时，先有个水波纹展开界面，再开始图集的动画
     * @param state
     */
    @Override
    public void onStateChange(int state) {
        if (RevealBackgroundView.STATE_FINISHED == state) {

            // 界面展开之后，先将图集设置为可见，再开始动画
            rvUserProfile.setVisibility(View.VISIBLE);
            userPhotosAdapter = new UserProfileAdapter(this);
            rvUserProfile.setAdapter(userPhotosAdapter);
        } else {

            // 页面展开动画还没结束，用户图集设置为不可见
            rvUserProfile.setVisibility(View.INVISIBLE);
        }
    }


    /**
     * 从特定的地方开始动画
     * @param startingLocation
     * @param startingActivity
     */
    public static void startUserProfileFromLocation(int usersId, int[] startingLocation, Activity startingActivity) {
        Intent intent = new Intent(startingActivity, UserProfileActivity.class);
        intent.putExtra(StringConstant.COMMENT_UID, usersId);
        intent.putExtra(ARG_REVEAL_START_LOCATION, startingLocation);
        startingActivity.startActivity(intent);
    }

    /**
     * toolbar返回
     */
    @OnClick(R.id.iv_profile_back)
    void onToolbarBackPress() {
        onBackPressed();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case ImageUtils.REQUEST_CODE_FROM_ALBUM:
                if(resultCode == RESULT_CANCELED) {
                    return;
                }
                Uri imageUri = data.getData();
                bitmap = ImageUtils.getBitmapFromUri(imageUri, UserProfileActivity.this);
                ivUserPhoto.setImageBitmap(bitmap);

                break;
            case ImageUtils.REQUEST_CODE_FROM_CAMERA:
                if(resultCode == RESULT_CANCELED) {
                    ImageUtils.deleteImageUri(this, ImageUtils.imageUriFromCamera);
                } else {
                    Uri imageUriCamera = ImageUtils.imageUriFromCamera;
                    bitmap = ImageUtils.getBitmapFromUri(imageUriCamera, UserProfileActivity.this);
                    ivUserPhoto.setImageBitmap(bitmap);
                }
                break;

            default:
                break;
        }
    }



    @OnClick(R.id.iv_user_profile_photo)
    void onChangeAvater(){
        ImageUtils.showImagePickDialog(UserProfileActivity.this);

        try {
            imageFile = ImageUtils.saveFile(UserProfileActivity.this, bitmap,
                    "alter_avater" + Utils.getUserId(UserProfileActivity.this)+".jpg");
            sendAlterAvater();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(UserProfileActivity.this, "保存文件失败", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * 请求数据
     */
    private void sendAlterAvater() {
        Map<String, Object> params = new HashMap<>();
        params.put("user.id", imageFile);
        params.put(StringConstant.USER_ID, Utils.getUserId(UserProfileActivity.this));

        httpTools.upload(StringConstant.ALTER_AVATER_URL, params, new HttpCallback() {
            @Override
            public void onStart() {

            }

            @Override
            public void onFinish() {

            }

            @Override
            public void onResult(String s) {
                Log.e("服务器返回更改头像结果数据-->", s);

                if ("1".equals(s)) {
                    Toast.makeText(UserProfileActivity.this, "修改头像成功", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(UserProfileActivity.this, "修改头像失败", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onError(Exception e) {

            }

            @Override
            public void onCancelled() {

            }

            @Override
            public void onLoading(long l, long l1) {

            }
        });
    }

}
