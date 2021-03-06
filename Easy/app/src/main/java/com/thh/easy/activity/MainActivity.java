package com.thh.easy.activity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;

import com.android.volley.ext.HttpCallback;
import com.android.volley.ext.RequestInfo;
import com.android.volley.ext.tools.HttpTools;
import com.squareup.picasso.Picasso;
import com.thh.easy.R;
import com.thh.easy.adapter.PostRVAdapter;
import com.thh.easy.constant.StringConstant;
import com.thh.easy.entity.Post;
import com.thh.easy.entity.User;
import com.thh.easy.util.FileUtil;
import com.thh.easy.util.LogUtil;
import com.thh.easy.util.RoundedTransformation;
import com.thh.easy.util.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 *   主界面:
 *         显示帖子
 */
public class MainActivity extends BaseDrawerActivity implements
        PostRVAdapter.OnPostItemClickListener, BaseDrawerActivity.OnStartActivityListener {

    @Bind(R.id.rv_post)
    public RecyclerView rvPost;                 // 主界面帖子列表

    @Bind(R.id.iv_logo)
    public ImageView ivLogo;                    // toolbar的logo

    @Bind(R.id.ib_new_post)
    public FloatingActionButton btnCreate;      // floating action button

    @Bind(R.id.cl_main_container)
    CoordinatorLayout clContainer;              // snakebar的根布局

    @Bind(R.id.sr_main_container)
    SwipeRefreshLayout srContainer;             // 布局刷新


    private boolean pendingIntroAnimation;      // 是否开始进入动画
    private MenuItem inboxMenuItem;             // toolbar上的meun

    private PostRVAdapter postRVAdapter;        // rvPost的适配器
    List<Post> postList = new ArrayList<>();    // 最新帖子的数据集合
    LinearLayoutManager linearLayoutManager;    // recyclerview的布局方式-线性

    boolean isLoading = false;
    int currentPage = 1;                        // 当前页

    HttpTools httpTools;                        // 网络操作工具

    SharedPreferences sp;                       // 写入登陆信息，部分用户信息
    User u = null;                              // 进入此app的用户


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 设置进入动画
        if (savedInstanceState == null) {
            pendingIntroAnimation = true;
        }

        loadInitData();    // 加载初始化信息
        loadPosts();      // 加载post数据
        setupPost();      // 设置RecycleView
        loadTouchEvent(); // 设置刷新事件

    }

    /**
     * 初始化布局、网络
     */
    private void loadInitData() {
        setOnStartActivityListener(this);
        ButterKnife.bind(this);
        HttpTools.init(this);
        httpTools = new HttpTools(this);
        setUserSp();
    }

    /**
     * 设置用户配置信息
     */
    private void setUserSp() {

        // 从登陆页面拿到用户信息
        Intent intent = getIntent();
        if (intent.getBooleanExtra("login_success", false)) {
            Bundle bundle = intent.getBundleExtra("user");
            u = (User) bundle.get("user");
            setUserInfo(u);
            return;
        }

        // 验证用户之前是否登录
        sp = getSharedPreferences("user_sp", Context.MODE_PRIVATE);

        if (sp.getBoolean("user_login", false)) {
            u = (User)FileUtil.readObject(this, "user");
            if (u == null)
                return;
            setUserInfo(u);
        }

    }

    /**
     * 设置刷新事件
     */
    private void  loadTouchEvent() {
        srContainer.setColorSchemeResources(R.color.easy_primary_light, R.color.easy_primary, R.color.easy_primary_dark);
        srContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if (srContainer.isRefreshing()) {
                    LogUtil.i(MainActivity.this, "正在刷新请求", "120");

                    // 网络检查
                    if (Utils.checkNetConnection(getApplicationContext())) {
                        LogUtil.i(MainActivity.this, "没错我联网了", "123");
                        currentPage = 1;
                        rvPost.removeAllViews();
                        postRVAdapter.notifyItemRangeRemoved(0, postList.size());
                        postList.clear();
                        loadPosts();
                        rvPost.scrollToPosition(0);
                    } else {
                        Snackbar.make(clContainer, "少年呦 你联网了嘛", Snackbar.LENGTH_SHORT).show();
                    }
                }
            }
        });

        rvPost.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_MOVE:
                        srContainer.setEnabled(false);
                        break;

                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        srContainer.setEnabled(true);
                        break;

                }
                return false;
            }
        });

    }
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
            LogUtil.d(MainActivity.this, "头像url :" + u.getAvatarFilePath());
            Picasso.with(getApplicationContext())
                    .load(avatar)
                    .centerCrop()
                    .resize(avatarSize, avatarSize)
                    .transform(new RoundedTransformation())
                    .into(ivMenuUserProfilePhoto);
        }
    }


    /**
     * 设置帖子recyclerview的相应初始数据
     */
    private void setupPost() {
        linearLayoutManager = new LinearLayoutManager(this);
        rvPost.setLayoutManager(linearLayoutManager);

        rvPost.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                int lastVisibleItems = linearLayoutManager.findLastVisibleItemPosition();
                int totalItemCount = linearLayoutManager.getItemCount();

                if (lastVisibleItems == totalItemCount - 1 && dy > 0) {
                    if (isLoading) {
                        LogUtil.i(MainActivity.this, "正在加载数据呢", "228");
                    } else {

                        // loadPosts中控制isLoading
                        loadPosts();
                        LogUtil.i(MainActivity.this, "新的数据来啦", "223");
                        isLoading = false;
                    }
                }
            }
        });
    }


    /**
     * 请求帖子数据
     */
    private void loadPosts() {

        if (postRVAdapter == null) {
            postRVAdapter = new PostRVAdapter(MainActivity.this, postList);
            postRVAdapter.setOnPostItemClickListener(MainActivity.this);
        }

        // 向服务器发送数据
        Map<String, String> params = new HashMap<String, String>(2);
        params.put(StringConstant.CURRENT_PAGE_KEY, currentPage+"");
        params.put(StringConstant.PER_PAGE_KEY, StringConstant.PER_PAGE_COUNT + "");

        // 如果登陆了，就把数据设置进去
        if(u!= null){
            params.put(StringConstant.COMMENT_UID, u.getId()+"");
        }

        RequestInfo info = new RequestInfo(StringConstant.SERVER_NEWPOST_URL, params);
        httpTools.post(info, new HttpCallback() {
            @Override
            public void onStart() {
                isLoading = true;
            }

            @Override
            public void onFinish() {
                // 一共加载多少条
                isLoading = false;
                srContainer.setRefreshing(false);
            }

            @Override
            public void onResult(String s) {

                if (StringConstant.NULL_VALUE.equals(s)) {
                    Snackbar.make(clContainer, "网络貌似粗错了", Snackbar.LENGTH_SHORT).show();
                    return;
                }

                if ("[]".equals(s)) {
                     Snackbar.make(clContainer, "什么帖子都没有呦", Snackbar.LENGTH_SHORT).show();
                    return;
                }

                onReadJson(s);
                currentPage++;
            }

            @Override
            public void onError(Exception e) {
            }

            @Override
            public void onCancelled() {
                srContainer.setRefreshing(false);
            }

            @Override
            public void onLoading(long l, long l1) {

            }
        });
    }

    public void onReadJson(String json) {
        int insertPos = postRVAdapter.getItemCount();
        try {

            JSONArray jsonArray = new JSONArray(json);

            for (int i = 0 ; i < jsonArray.length() ; i ++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                JSONObject postObj = jsonObject.getJSONObject("posts");
                JSONObject userObj = postObj.getJSONObject("users");

                Post post = null;

                String imageUrl = null;
                if (!postObj.isNull("image")) {
                    imageUrl = postObj.getJSONObject("image").getString("urls");
                }

                String avatar = null;
                if (!postObj.getJSONObject("users").isNull("image"))
                    avatar = postObj.getJSONObject("users").getJSONObject("image").getString("urls");

                LogUtil.d(MainActivity.this, "avatar=" + avatar, "330");
                post = new Post(postObj.getInt("id"),
                        userObj.getInt("id"),
                        userObj.getString("name"),
                        postObj.getString("contents"),
                        postObj.getString("dates"),
                        imageUrl,
                        avatar,
                        jsonObject.getInt("likes"),
                        jsonObject.getInt("isLike"));
                post.setCollectflag(jsonObject.getInt("collect"));
                postList.add(post);
            }

            LogUtil.d(MainActivity.this, "postList.size=", "342");

            postRVAdapter.notifyItemRangeInserted(insertPos, postList.size() - insertPos);
            postRVAdapter.notifyItemRangeChanged(insertPos, postList.size() - insertPos);

            currentPage++;

        } catch (JSONException e) {
            LogUtil.e(MainActivity.this, "解析帖子数据json出错" + e.getMessage(), "223");
        }
    }

    /**
     * 设置menu上的进入动画
     * @param menu
     * @return
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        inboxMenuItem = menu.findItem(R.id.action_search);
        inboxMenuItem.setActionView(R.layout.menu_item_view);
        if (pendingIntroAnimation) {
            // 开始动画~\(≧▽≦)/~啦啦啦
            pendingIntroAnimation = false;
            startIntroAnimation();
        }
        return true;
    }


    /**
     * toolbar 动画
     */
    private static final int ANIM_DURATION_TOOLBAR = 300;
    private void startIntroAnimation() {
        btnCreate.setTranslationY(2 * getResources().getDimensionPixelOffset(R.dimen.btn_fab_size));
        int actionbarSize = Utils.dpToPx(56);
        getToolbar().setTranslationY(-actionbarSize);
        ivLogo.setTranslationY(-actionbarSize);
        inboxMenuItem.getActionView().setTranslationY(-actionbarSize);

        getToolbar().animate()
                .translationY(0)
                .setDuration(ANIM_DURATION_TOOLBAR)
                .setStartDelay(300);
        ivLogo.animate()
                .translationY(0)
                .setDuration(ANIM_DURATION_TOOLBAR)
                .setStartDelay(400);
        inboxMenuItem.getActionView().animate()
                .translationY(0)
                .setDuration(ANIM_DURATION_TOOLBAR)
                .setStartDelay(500)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        startContentAnimation();
                    }
                })
                .start();
    }


    /**
     *  floatingActionButton 进入动画
     */
    private static final int ANIM_DURATION_FAB = 400;

    private void startContentAnimation() {
        btnCreate.animate()
                .translationY(0)
                .setInterpolator(new OvershootInterpolator(1.f))
                .setStartDelay(300)
                .setDuration(ANIM_DURATION_FAB)
                .start();

        // 动画结束再设置帖子内容

        // 网络检查
        if (!Utils.checkNetConnection(getApplicationContext())) {
            Snackbar.make(clContainer, "少年呦 你联网了嘛", Snackbar.LENGTH_SHORT).show();
        }

        rvPost.setAdapter(postRVAdapter);
        rvPost.scrollToPosition(0);
        isLoading = false;
    }


    /**
     * 点击帖子中的头像后，进入个人信息界面
     * @param v
     * @param position
     */
    @Override
    public void onProfileClick(View v, int position) {

        // 检查联网
        if (!Utils.checkNetConnection(getApplicationContext())) {
            Snackbar.make(clContainer, "少年呦 你联网了嘛?", Snackbar.LENGTH_LONG).show();
        }

        // 获得点击头像时的位置
        int[] startingLocation = new int[2];
        v.getLocationOnScreen(startingLocation);
        startingLocation[0] += v.getWidth() / 2;

        // 进入用户信息界面时，设置进入动画
        UserProfileActivity.startUserProfileFromLocation(postList.get(position).getUid(),
                startingLocation, this);

        LogUtil.d(MainActivity.this, "" + postList.get(position).getUid());
        overridePendingTransition(0, 0);
    }


    /**
     * 回复点击事件
     * @param v
     * @param position
     */
    @Override
    public void onCommentsClick(View v, int position) {
        final Intent intent = new Intent(this, CommentsActivity.class);
        int[] startingLocation = new int[2];
        v.getLocationOnScreen(startingLocation);
        intent.putExtra(CommentsActivity.COMMENT_DRAWING_START_LOCATION, startingLocation[1]);
        intent.putExtra("postID", postList.get(position).getId());
        startActivity(intent);
        overridePendingTransition(0, 0);
    }

    /**
     * 点赞
     * @param v
     * @param position
     */
    @Override
    public void onLikeClick(View v, final int position) {
        User u = (User) FileUtil.readObject(getApplicationContext(), "user");
        if (u == null) {
            Snackbar.make(clContainer, "岂可修!要先登陆啊", Snackbar.LENGTH_SHORT).show();
            return;
        }

        Map<String, String> params = new HashMap<>(2);
        params.put(StringConstant.LIKE_UID, u.getId()+"");
        params.put(StringConstant.LIKE_POST_ID, postList.get(position).getId() + "");

        RequestInfo info = new RequestInfo(StringConstant.SERVER_LIKE_URL, params);
        httpTools.post(info, new HttpCallback() {
            @Override
            public void onStart() {
            }

            @Override
            public void onFinish() {
            }

            @Override
            public void onResult(String s) {
                if ("1".equals(s)){
                    Snackbar.make(clContainer, "点了个赞", Snackbar.LENGTH_SHORT).show();
                    postRVAdapter.onClickLike(position, PostRVAdapter.LIKE);
                }


                else if ("0".equals(s)){
                    Snackbar.make(clContainer, "赞失败了诶", Snackbar.LENGTH_SHORT).show();
                }

                else if("2".equals(s)){
                    Snackbar.make(clContainer, "取消点赞", Snackbar.LENGTH_SHORT).show();
                    postRVAdapter.onClickLike(position, PostRVAdapter.CANCEL_LIKE);
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

    /**
     * 收藏帖子
     * @param v
     * @param position
     */
    @Override
    public void onMoreClick(View v, final int position) {

        User u = (User) FileUtil.readObject(getApplicationContext(), "user");
        if (u == null) {
            Snackbar.make(clContainer, "岂可修! 要先登陆啊", Snackbar.LENGTH_SHORT).show();
            return;
        }

        Map<String, String> params = new HashMap<>(2);
        params.put(StringConstant.COLLECT_USER_ID, u.getId()+"");
        params.put(StringConstant.COLLECT_POST_ID, postList.get(position).getId() + "");

        // collects.posts.id=2&collects.users.id=1
        RequestInfo info = new RequestInfo(StringConstant.SERVER_COLLECT_URL, params);
        httpTools.post(info, new HttpCallback() {
            @Override
            public void onStart() {
            }

            @Override
            public void onFinish() {
            }

            @Override
            public void onResult(String s) {
                if ("1".equals(s)){
                    Snackbar.make(clContainer, "收藏成功", Snackbar.LENGTH_SHORT).show();
                    postRVAdapter.onClickCollect(position, PostRVAdapter.LIKE);
                } else if ("0".equals(s)){
                    Snackbar.make(clContainer, "收藏失败", Snackbar.LENGTH_SHORT).show();
                } else if("2".equals(s)){
                    Snackbar.make(clContainer, "取消收藏", Snackbar.LENGTH_SHORT).show();
                    postRVAdapter.onClickCollect(position, PostRVAdapter.CANCEL_LIKE);
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


    /**
     *  点击更多跳转到更多帖子界面
     */
    @OnClick(R.id.more_hot_post)
    void morePost() {
        // TODO 考虑是否加入动画
        Intent intent = new Intent(MainActivity.this, HotPostActivity.class);

        if(u!= null){
            intent.putExtra(StringConstant.USER_ID, u.getId());
        }

        startActivity(intent);
    }


    @Override
    public void onStartActivity(Class targetActivity) {
        if (MainActivity.class == targetActivity)
            return;

        startActivity(new Intent(MainActivity.this, targetActivity));
        overridePendingTransition(0, 0);
    }

    /**
     * 点击主界面fab 进入发帖界面
     */
    @OnClick(R.id.ib_new_post)
    void onClickPostFAB(){
        startActivity(new Intent(MainActivity.this, AddPostActivity.class));
    }
}
