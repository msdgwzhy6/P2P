package com.example.p2p;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Rect;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.viewpager.widget.ViewPager;

import com.example.p2p.adapter.RvChatAdapter;
import com.example.p2p.adapter.RvEmojiAdapter;
import com.example.p2p.adapter.VpEmojiAdapter;
import com.example.p2p.base.activity.BaseActivity;
import com.example.p2p.bean.Audio;
import com.example.p2p.bean.Emoji;
import com.example.p2p.bean.File;
import com.example.p2p.bean.Image;
import com.example.p2p.bean.ItemType;
import com.example.p2p.bean.Mes;
import com.example.p2p.bean.MesType;
import com.example.p2p.bean.User;
import com.example.p2p.callback.IRecordedCallback;
import com.example.p2p.callback.ISendMessgeCallback;
import com.example.p2p.config.Constant;
import com.example.p2p.core.ConnectManager;
import com.example.p2p.core.MediaPlayerManager;
import com.example.p2p.db.EmojiDao;
import com.example.p2p.utils.FileUtils;
import com.example.p2p.utils.ImageUtils;
import com.example.p2p.utils.IntentUtils;
import com.example.p2p.utils.LogUtils;
import com.example.p2p.utils.SimpleTextWatchListener;
import com.example.p2p.widget.customView.AudioTextView;
import com.example.p2p.widget.customView.IndicatorView;
import com.example.p2p.widget.customView.SendButton;
import com.example.p2p.widget.customView.WrapViewPager;
import com.example.permission.PermissionHelper;
import com.example.permission.bean.Permission;
import com.example.permission.callback.IPermissionCallback;
import com.example.permission.callback.IPermissionsCallback;
import com.example.utils.DisplayUtil;
import com.example.utils.FileUtil;
import com.example.utils.KeyBoardUtil;
import com.example.utils.ToastUtil;
import com.nbsp.materialfilepicker.MaterialFilePicker;
import com.nbsp.materialfilepicker.ui.FilePickerActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import butterknife.BindView;
import butterknife.OnClick;

public class ChatActivity extends BaseActivity {

    @BindView(R.id.iv_back)
    ImageView ivBack;
    @BindView(R.id.tv_title)
    TextView tvTitle;
    @BindView(R.id.tool_bar)
    Toolbar toolBar;
    @BindView(R.id.rv_chat)
    RecyclerView rvChat;
    @BindView(R.id.srl_chat)
    SwipeRefreshLayout srlChat;
    @BindView(R.id.iv_audio)
    ImageView ivAudio;
    @BindView(R.id.ed_edit)
    EditText edEdit;
    @BindView(R.id.rl_edit)
    RelativeLayout rlEdit;
    @BindView(R.id.iv_emoji)
    ImageView ivEmoji;
    @BindView(R.id.iv_add)
    ImageView ivAdd;
    @BindView(R.id.rl_album)
    RelativeLayout rlAlbum;
    @BindView(R.id.rl_camera)
    RelativeLayout rlCamera;
    @BindView(R.id.rl_location)
    RelativeLayout rlLocation;
    @BindView(R.id.rl_file)
    RelativeLayout rlFile;
    @BindView(R.id.btn_send)
    SendButton btnSend;
    @BindView(R.id.cl_more)
    ConstraintLayout clMore;
    @BindView(R.id.vp_emoji)
    WrapViewPager vpEmoji;
    @BindView(R.id.idv_emoji)
    IndicatorView idvEmoji;
    @BindView(R.id.ll_emoji)
    LinearLayout llEmoji;
    @BindView(R.id.iv_scan)
    ImageView ivScan;
    @BindView(R.id.iv_keyborad)
    ImageView ivKeyborad;
    @BindView(R.id.tv_audio)
    AudioTextView tvAudio;

    private final String TAG = this.getClass().getSimpleName();
    private final static int REQUEST_CODE_GET_IMAGE= 0x000;
    private final static int REQUEST_CODE_TAKE_IMAGE = 0x001;
    private final static int REQUEST_CODE_GET_FILE = 0x002;
    private ViewGroup mContentView;
    private boolean isKeyboardShowing;
    private int screenHeight;
    private List<RvEmojiAdapter> mEmojiAdapters;
    private List<Emoji> mEmojiBeans;
    private User mTargetUser;
    private User mUser;
    private RvChatAdapter mRvChatAdapter;
    private List<Mes> mMessageList;
    private int mLastPosition = -1;
    private Uri mTakedImageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mTargetUser = (User) getIntent().getSerializableExtra(Constant.EXTRA_TARGET_USER);
        mUser = (User) FileUtil.restoreObject(this, Constant.FILE_NAME_USER);
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        if (edEdit.hasFocus()) edEdit.clearFocus();
        super.onResume();
    }

    @Override
    public void onBackPressed() {
        KeyBoardUtil.closeKeyBoard(this, edEdit);
        if (clMore.isShown()) clMore.setVisibility(View.GONE);
        if (llEmoji.isShown()) llEmoji.setVisibility(View.GONE);
        super.onBackPressed();
    }
    
    @Override
    protected void onDestroy() {
        ConnectManager.getInstance().release();
        mMessageList.clear();
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if(resultCode != Activity.RESULT_OK) return;
        if(requestCode == REQUEST_CODE_GET_IMAGE) sendImage(data.getData());
        if(requestCode == REQUEST_CODE_TAKE_IMAGE) sendImage(mTakedImageUri);
        if(requestCode == REQUEST_CODE_GET_FILE) sendFile(data.getStringExtra(FilePickerActivity.RESULT_FILE_PATH));
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_chat;
    }

    @Override
    protected void initView() {
        tvTitle.setText(mTargetUser.getName());
        ivScan.setVisibility(View.GONE);
        PermissionHelper.getInstance().with(this).requestPermissions(
                new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                new IPermissionsCallback() {
                    @Override
                    public void onAccepted(List<Permission> permissions) {

                    }

                    @Override
                    public void onDenied(List<Permission> permissions) {
                        ToastUtil.showToast(ChatActivity.this, getString(R.string.toast_permission_rejected));
                        finish();
                    }
                }
        );
        screenHeight = DisplayUtil.getScreenHeight(ChatActivity.this);
        mContentView = getWindow().getDecorView().findViewById(android.R.id.content);
        getWindow().getDecorView().getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            //当前窗口可见区域的大小
            Rect rect = new Rect();
            getWindow().getDecorView().getWindowVisibleDisplayFrame(rect);
            //与当前可视区域的差值(软键盘的高度)
            int heightDiff = screenHeight - (rect.bottom - rect.top);
            isKeyboardShowing = heightDiff > screenHeight / 3;
        });

        //从数据库获取表情
        mEmojiBeans = EmojiDao.getInstance().getEmojiBeanList();
        //添加删除表情按钮信息
        Emoji emojiDelete = new Emoji(0, 000);
        int emojiDeleteCount = (int) Math.ceil(mEmojiBeans.size() * 1.0 / 21);
        for (int i = 1; i <= emojiDeleteCount; i++) {
            if (i == emojiDeleteCount) {
                mEmojiBeans.add(mEmojiBeans.size(), emojiDelete);
            } else {
                mEmojiBeans.add(i * 21 - 1, emojiDelete);
            }
        }
        //为每个Vp添加Rv，并初始化Rv
        List<View> views = new ArrayList<>();
        mEmojiAdapters = new ArrayList<>(emojiDeleteCount);
        for (int i = 0; i < emojiDeleteCount; i++) {
            RecyclerView recyclerView = (RecyclerView) LayoutInflater.from(this).inflate(R.layout.item_emoji_vp, vpEmoji, false);
            recyclerView.setLayoutManager(new GridLayoutManager(this, 7));
            if (i == emojiDeleteCount - 1) {
                mEmojiAdapters.add(new RvEmojiAdapter(mEmojiBeans.subList(i * 21, mEmojiBeans.size()), R.layout.item_emoji));
            } else {
                mEmojiAdapters.add(new RvEmojiAdapter(mEmojiBeans.subList(i * 21, i * 21 + 21), R.layout.item_emoji));
            }
            recyclerView.setAdapter(mEmojiAdapters.get(i));
            views.add(recyclerView);
            int index = i;
            //为每个Rv添加item监听
            mEmojiAdapters.get(i).setOnItemClickListener((adapter, view, position) -> {
                Emoji emojiBean = mEmojiBeans.get(position + index * 21);
                if (emojiBean.getId() == 0) {
                    edEdit.setText("");
                } else {
                    edEdit.setText(edEdit.getText().append(emojiBean.getUnicodeInt()));
                }
                edEdit.setSelection(edEdit.length());
            });

        }
        //初始化Vp
        VpEmojiAdapter vpEmojiAdapter = new VpEmojiAdapter(views);
        vpEmoji.setAdapter(vpEmojiAdapter);
        idvEmoji.setIndicatorCount(views.size());
        //初始化聊天的Rv
        mMessageList = new ArrayList<>();
        mRvChatAdapter = new RvChatAdapter(mMessageList);
        rvChat.setLayoutManager(new LinearLayoutManager(this));
        rvChat.setAdapter(mRvChatAdapter);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void initCallback() {
        //下拉刷新监听
        srlChat.setOnRefreshListener(() ->
                new Handler().postDelayed(() ->
                srlChat.setRefreshing(false), 2000)
        );
        //editText文本变化监听
        edEdit.addTextChangedListener(new SimpleTextWatchListener() {
            @Override
            public void afterTextChanged(Editable s) {
                int visibility = "".equals(s.toString().trim()) ? View.GONE : View.VISIBLE;
                btnSend.setVisibility(visibility);
            }
        });
        //editText触摸监听
        edEdit.setOnTouchListener((view, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP && isButtomLayoutShown()) {
                if (clMore.isShown()) clMore.setVisibility(View.GONE);
                if (llEmoji.isShown()) llEmoji.setVisibility(View.GONE);
            }
            return false;
        });
        //表情列表左右滑动监听
        vpEmoji.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                idvEmoji.setCurrentIndicator(position);
            }
        });
        //聊天列表触摸监听
        rvChat.setOnTouchListener((view, event) -> {
            edEdit.clearFocus();
            if(isKeyboardShowing) KeyBoardUtil.closeKeyBoard(ChatActivity.this, edEdit);
            if (clMore.isShown()) clMore.setVisibility(View.GONE);
            if (llEmoji.isShown()) llEmoji.setVisibility(View.GONE);
            return false;
        });
        //底部布局弹出,聊天列表上滑
        rvChat.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            if(bottom < oldBottom){
                if(mMessageList.isEmpty()) return;
                rvChat.post(() -> rvChat.smoothScrollToPosition(mMessageList.size() - 1));
            }
        });
        //接收消息回调监听
        ConnectManager.getInstance().addReceiveMessageCallback(mTargetUser.getIp(), message -> addMessage(message));
        //发送消息回调监听
        ConnectManager.getInstance().setSendMessgeCallback(new ISendMessgeCallback() {
            @Override
            public void onSendSuccess(Mes<?> message) {
                if(message.mesType == MesType.IMAGE || message.mesType == MesType.FILE){
                    return;
                }
                addMessage(message);
            }

            @Override
            public void onSendFail(Mes<?> message) {
                ToastUtil.showToast(ChatActivity.this, "发送消息失败");
            }
        });
        //录音结束回调
        tvAudio.setRecordedCallback(mTargetUser, new IRecordedCallback() {
            @Override
            public void onFinish(String audioPath, int duration) {
                sendAudio(audioPath, duration);
            }

            @Override
            public void onError() {
                ToastUtil.showToast(ChatActivity.this, getString(R.string.chat_audio_error));
            }
        });
        //聊天列表的item点击回调
        mRvChatAdapter.setOnItemClickListener((adapter, view, position) -> {
            Mes message = mMessageList.get(position);
            if(message.mesType == MesType.AUDIO){
                Audio audio = (Audio) message.data;
                ImageView imageView = view.findViewById(R.id.iv_message);
                Drawable drawable = imageView.getBackground();
                int audioBg = message.itemType == ItemType.SEND_AUDIO ? R.drawable.ic_audio_right_3 : R.drawable.ic_audio_left_3;
                int audioBgAnim =  message.itemType == ItemType.SEND_AUDIO ? R.drawable.anim_item_audio_right : R.drawable.anim_item_audio_left;
                if(drawable instanceof AnimationDrawable){
                    MediaPlayerManager.getInstance().stopPlayAudio();
                    imageView.setBackgroundResource(audioBg);
                }else {
                    if(mLastPosition != -1 && position != mLastPosition){
                        Mes lastMessage = mMessageList.get(mLastPosition);
                        int lastAudioBg = lastMessage.itemType == ItemType.SEND_AUDIO ? R.drawable.ic_audio_right_3 : R.drawable.ic_audio_left_3;
                        LinearLayoutManager manager = (LinearLayoutManager) rvChat.getLayoutManager();
                        View lastView = manager.findViewByPosition(mLastPosition);
                        if(lastView != null){
                            lastView.findViewById(R.id.iv_message).setBackgroundResource(lastAudioBg);
                        }
                    }
                    imageView.setBackgroundResource(audioBgAnim);
                    AnimationDrawable audioAnimDrawable = (AnimationDrawable)imageView.getBackground();
                    audioAnimDrawable.start();
                    MediaPlayerManager.getInstance().startPlayAudio(audio.audioPath, mp -> imageView.setBackgroundResource(audioBg));
                }
            }
            if(message.mesType == MesType.IMAGE){

            }
            if(message.mesType == MesType.FILE){
                File file = (File) message.data;
                FileUtils.openFile(ChatActivity.this, file.filePath);
            }
            mLastPosition = position;
        });
    }

    @OnClick({R.id.iv_add, R.id.iv_back, R.id.iv_emoji, R.id.btn_send, R.id.iv_audio, R.id.iv_keyborad, R.id.rl_album, R.id.rl_camera, R.id.rl_file})
    public void onViewClick(View view) {
        switch (view.getId()) {
            case R.id.iv_audio:
                changeAudioLayout();
                break;
            case R.id.iv_keyborad:
                changeEditLayout();
                break;
            case R.id.iv_add:
                changeMoreLayout();
                break;
            case R.id.iv_emoji:
                changeEmojiLayout();
                break;
            case R.id.btn_send:
                sendText();
                break;
            case R.id.rl_album:
                chooseImage();
                break;
            case R.id.rl_camera:
                takeImage();
                break;
            case R.id.iv_back:
                finish();
                break;
            case R.id.rl_file:
                chooseFile();
                break;
            default:
                break;
        }
    }

    /**
     * 选择文件
     */
    private void chooseFile() {
        String regex = ".*\\.(txt|ppt|doc|xls|pdf|apk|zip|pptx|docx|xlsx|mp3|mp4)$";
        new MaterialFilePicker()
                .withActivity(this)
                .withRequestCode(REQUEST_CODE_GET_FILE)
                .withHiddenFiles(false)
                .withFilter(Pattern.compile(regex))
                .withTitle(getString(R.string.chat_choose_file))
                .start();
    }

    /**
     * 拍照
     */
    private void takeImage() {
        PermissionHelper.getInstance().with(this).requestPermission(
                Manifest.permission.CAMERA,
                new IPermissionCallback() {
                    @Override
                    public void onAccepted(Permission permission) {
                        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        String imageFileName = System.currentTimeMillis() + ".png";
                        mTakedImageUri = ImageUtils.getImageUri(ChatActivity.this, FileUtils.getImagePath(mTargetUser.getIp(), ItemType.SEND_IMAGE), imageFileName);
                        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, mTakedImageUri);
                        startActivityForResult(cameraIntent, REQUEST_CODE_TAKE_IMAGE);
                    }

                    @Override
                    public void onDenied(Permission permission) {
                        ToastUtil.showToast(ChatActivity.this, getString(R.string.toast_permission_rejected));
                    }
                }
        );
    }

    /**
     * 选择照片
     */
    private void chooseImage() {
        startActivityForResult(IntentUtils.getChooseImageIntent(), REQUEST_CODE_GET_IMAGE);
    }

    /**
     * 发送文字消息
     */
    private void sendText() {
        String text = edEdit.getText().toString();
        Mes<String> message = new Mes<>(ItemType.SEND_TEXT, MesType.TEXT, mUser.getIp(), text);
        ConnectManager.getInstance().sendMessage(mTargetUser.getIp(), message);
        edEdit.setText("");
    }

    /**
     * 发送音频消息
     */
    private void sendAudio(String audioPath, int duration) {
        Audio audio = new Audio(duration, audioPath);
        Mes<Audio> message = new Mes<>(ItemType.SEND_AUDIO, MesType.AUDIO, mUser.getIp(), audio);
        ConnectManager.getInstance().sendMessage(mTargetUser.getIp(), message);
    }

    /**
     * 发送图片消息
     */
    private void sendImage(Uri imageUri) {
        Image image = new Image(imageUri);
        Mes<Image> message = new Mes<>(ItemType.SEND_IMAGE, MesType.IMAGE, mUser.getIp(), image);
        addMessage(message);
        final int sendingImagePostion = mMessageList.indexOf(message);
        ConnectManager.getInstance().sendMessage(mTargetUser.getIp(), message, progress -> {
            if(mMessageList.isEmpty()) return;
            Image sendingImage = (Image) mMessageList.get(sendingImagePostion).data;
            sendingImage.progress = progress;
            mRvChatAdapter.notifyItemChanged(sendingImagePostion);
        });
    }

    /**
     * 发送文件
     */
    private void sendFile(String filePath) {
        String fileType = filePath.substring(filePath.lastIndexOf(".") + 1).toLowerCase(Locale.getDefault());
        String size = FileUtils.getFileSize(filePath);
        String name = filePath.substring(filePath.lastIndexOf(java.io.File.separator) + 1, filePath.lastIndexOf("."));
        File file = new File(filePath, name, size, fileType);
        Mes<File> message = new Mes<>(ItemType.SEND_FILE, MesType.FILE, mUser.getIp(), file);
        addMessage(message);
        final int sendingFilePosition = mMessageList.indexOf(message);
        ConnectManager.getInstance().sendMessage(mTargetUser.getIp(), message, progress -> {
            if(mMessageList.isEmpty()) return;
            File sendingFile = (File) mMessageList.get(sendingFilePosition).data;
            sendingFile.progress = progress;
            mRvChatAdapter.notifyItemChanged(sendingFilePosition);
        });
    }

    /**
     * 往底部添加一条信息
     */
    private void addMessage(Mes<?> message) {
        mMessageList.add(message);
        mRvChatAdapter.notifyItemInserted(mMessageList.size());
        rvChat.smoothScrollToPosition(mMessageList.size() - 1);
    }

    /**
     * 改变输入键盘显示
     */
    private void changeEditLayout() {
        ivKeyborad.setVisibility(View.INVISIBLE);
        ivAudio.setVisibility(View.VISIBLE);
        if(clMore.isShown()) clMore.setVisibility(View.GONE);
        if(llEmoji.isShown()) llEmoji.setVisibility(View.GONE);
        if(!isKeyboardShowing) KeyBoardUtil.openKeyBoard(this, edEdit);
        edEdit.setVisibility(View.VISIBLE);
        tvAudio.setVisibility(View.INVISIBLE);
        edEdit.requestFocus();
    }

    /**
     * 改变音频布局显示
     */
    private void changeAudioLayout() {
        edEdit.clearFocus();
        ivAudio.setVisibility(View.INVISIBLE);
        ivKeyborad.setVisibility(View.VISIBLE);
        if(clMore.isShown()) clMore.setVisibility(View.GONE);
        if(llEmoji.isShown()) llEmoji.setVisibility(View.GONE);
        if(isKeyboardShowing) KeyBoardUtil.closeKeyBoard(this, edEdit);
        edEdit.setVisibility(View.INVISIBLE);
        tvAudio.setVisibility(View.VISIBLE);
    }


    /**
     * 改变更多布局显示
     */
    private void changeMoreLayout() {
        edEdit.clearFocus();
        int visibility = clMore.isShown() ? View.GONE : View.VISIBLE;
        if (!clMore.isShown() && !isKeyboardShowing) {//如果键盘没有显示，且更多布局也没有显示，只显示更多布局
            clMore.setVisibility(visibility);
            tvAudio.setVisibility(View.INVISIBLE);
            edEdit.setVisibility(View.VISIBLE);
            ivAudio.setVisibility(View.VISIBLE);
            ivKeyborad.setVisibility(View.INVISIBLE);
            if (llEmoji.isShown()) llEmoji.setVisibility(View.GONE);
        } else if (clMore.isShown() && !isKeyboardShowing) {//如果键盘没有显示，但更多布局显示，隐藏更多布局，显示键盘
            clMore.setVisibility(visibility);
            KeyBoardUtil.openKeyBoard(this, edEdit);
        } else if (!clMore.isShown() && isKeyboardShowing) {//如果只有键盘显示，就隐藏键盘，显示更多布局
            lockContentHeight();
            KeyBoardUtil.closeKeyBoard(this, edEdit);
            edEdit.postDelayed(() -> {
                unlockContentHeightDelayed();
                clMore.setVisibility(visibility);

            }, 200);
        }
    }

    /**
     * 改变表情布局显示
     */
    private void changeEmojiLayout() {
        int visibility = llEmoji.isShown() ? View.GONE : View.VISIBLE;
        if (!llEmoji.isShown() && !isKeyboardShowing) {
            llEmoji.setVisibility(visibility);
            tvAudio.setVisibility(View.INVISIBLE);
            edEdit.setVisibility(View.VISIBLE);
            edEdit.requestFocus();
            ivAudio.setVisibility(View.VISIBLE);
            ivKeyborad.setVisibility(View.INVISIBLE);
            if (clMore.isShown()) clMore.setVisibility(View.GONE);
        } else if (llEmoji.isShown() && !isKeyboardShowing) {
            llEmoji.setVisibility(visibility);
            KeyBoardUtil.openKeyBoard(this, edEdit);
        } else if (!llEmoji.isShown() && isKeyboardShowing) {
            lockContentHeight();
            KeyBoardUtil.closeKeyBoard(this, edEdit);
            edEdit.postDelayed(() -> {
                unlockContentHeightDelayed();
                llEmoji.setVisibility(visibility);

            }, 200);
        }
    }

    /**
     * 锁定内容高度，防止跳闪
     */
    private void lockContentHeight() {
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) mContentView.getLayoutParams();
        params.height = mContentView.getHeight();
        params.weight = 0.0F;
    }

    /**
     * 释放被锁定的内容高度
     */
    private void unlockContentHeightDelayed() {
        ((LinearLayout.LayoutParams) mContentView.getLayoutParams()).weight = 1.0F;
    }

    /**
     * 底部表情布局或底部更多布局是否显示
     */
    private boolean isButtomLayoutShown() {
        return clMore.isShown() || llEmoji.isShown();
    }

    public static void startActiivty(Activity context, User user, int code) {
        Intent intent = new Intent(context, ChatActivity.class);
        intent.putExtra(Constant.EXTRA_TARGET_USER, user);
        context.startActivityForResult(intent, code);
    }
}
