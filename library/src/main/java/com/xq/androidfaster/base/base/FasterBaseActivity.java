package com.xq.androidfaster.base.base;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import com.xq.androidfaster.util.tools.ActivityUtils;
import com.xq.androidfaster.util.tools.FragmentUtils;
import com.xq.androidfaster.util.tools.ReflectUtils;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public abstract class FasterBaseActivity<T extends IFasterBaseBehavior> extends AppCompatActivity implements IFasterBaseBehavior<T>, FragmentUtils.OnBackClickListener, OnStartFragmentBehavior {

    {
        ReflectUtils.reflect(this).field("mLifecycleRegistry",getLifecycle());
    }

    private T another;
    {
        if (another == null)
            another = createBindView();
        if (another == null)
            another = createBindPresenter();
    }

    protected abstract T createBindView();

    protected abstract T createBindPresenter();

    @Deprecated
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) isRestoreState = true;

        //布局初始化
        {
            if (getLayoutId() != 0 || (getBindAnother() != null && getBindAnother().getLayoutId() != 0))
            {
                setContentView(getLayoutId() == 0?getBindAnother().getLayoutId() : getLayoutId());
                rootView = ((ViewGroup)getWindow().getDecorView().findViewById(android.R.id.content)).getChildAt(0);

                autoFindView(this);
                if (getBindAnother() != null)   autoFindView(getBindAnother());
            }
        }

        //传参初始化
        {
            Bundle bundle = getIntent().getExtras();
            if (bundle == null) bundle = new Bundle();

            resolveBundle(bundle);
            if (getBindAnother() != null) getBindAnother().resolveBundle(bundle);
        }

        //自定义初始化
        {
            init();
            if (getBindAnother() != null) getBindAnother().init();
        }

        create(savedInstanceState);
    }

    @Deprecated
    @Override
    public void onRestart() {
        super.onRestart();
    }

    @Deprecated
    @Override
    public void onStart() {
        super.onStart();
    }

    @Deprecated
    @Override
    public void onResume() {
        super.onResume();
        visible();
    }

    @Deprecated
    @Override
    public void onPause() {
        super.onPause();
        invisible();
        if (isFinishing())  destroy();
    }

    @Deprecated
    @Override
    public void onStop() {
        super.onStop();
    }

    @Deprecated
    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void create(Bundle savedInstanceState) {
        getLifecycle().handleCreate(savedInstanceState);
    }

    @Override
    public void visible() {
        getLifecycle().handleVisible();
    }

    @Override
    public void invisible() {
        isFirstVisible = false;
        getLifecycle().handleInvisible();
    }

    @Override
    public void destroy() {
        getLifecycle().handleDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        getLifecycle().handleSaveInstanceState(outState);
    }

    private FasterLifecycleRegistry lifecycleRegistry;
    @Override
    public FasterLifecycleRegistry getLifecycle() {
        if (lifecycleRegistry == null) lifecycleRegistry = new FasterLifecycleRegistry(this);
        return lifecycleRegistry;
    }

    //封装startActivityForResult成回调的形式
    private SparseArray<ResultCallback> spa_resultCallback = new SparseArray();
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        getLifecycle().handleActivityResult(requestCode,resultCode,data);

        ResultCallback callback = spa_resultCallback.get(requestCode);
        spa_resultCallback.remove(requestCode);
        if (null != callback)
        {
            switch (resultCode)
            {
                case Activity.RESULT_OK:
                    callback.onSuccess(data);
                    break;
                case Activity.RESULT_CANCELED:
                    callback.onCancel();
                    break;
            }
        }
    }

    public void  startActivityForResult(Intent intent, ResultCallback callback){
        startActivityForResult(intent,0,0,callback);
    }

    public void  startActivityForResult(Intent intent,int enterAnim,int exitAnim, ResultCallback callback){
        int requestCode = callback.hashCode();
        requestCode &= 0x0000ffff;
        spa_resultCallback.append(requestCode,callback);

        Bundle bundle = new Bundle(intent.getExtras() != null?intent.getExtras():new Bundle());
        bundle.putInt("enterAnim",enterAnim);
        bundle.putInt("exitAnim",exitAnim);
        intent.putExtras(bundle);

        startActivityForResult(intent,requestCode,ActivityUtils.getOptionsBundle(getContext(),enterAnim,exitAnim));
    }

    @SuppressLint("RestrictedApi")
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Deprecated
    @Override
    public void startActivityForResult(Intent intent, int requestCode, @Nullable Bundle options) {
        super.startActivityForResult(intent, requestCode, options);
    }

    @Deprecated
    @Override
    public void startActivityForResult(Intent intent, int requestCode) {
        super.startActivityForResult(intent, requestCode);
    }

    public void startActivity(Intent intent,int enterAnim,int exitAnim){

        Bundle bundle = new Bundle(intent.getExtras() != null?intent.getExtras():new Bundle());
        bundle.putInt("enterAnim",enterAnim);
        bundle.putInt("exitAnim",exitAnim);
        intent.putExtras(bundle);

        startActivity(intent,ActivityUtils.getOptionsBundle(getContext(),enterAnim,exitAnim));
    }


    @Override
    public void startFragment(Fragment fragment,int containerId,int enterAnim,int exitAnim) {

        Bundle bundle = fragment.getArguments();
        if (bundle == null)
        {
            bundle = new Bundle();
            fragment.setArguments(bundle);
        }
        bundle.putInt("enterAnim",enterAnim);
        bundle.putInt("exitAnim",exitAnim);

        List<Fragment> containerFragments = new LinkedList<>();
        for (Fragment f : FragmentUtils.getFragments(getTopFragmentManager())){
            if (f.getArguments() != null && f.getArguments().getInt(FragmentUtils.ARGS_ID,0) == containerId){
                containerFragments.add(f);
            }
        }
        if (!containerFragments.isEmpty())
            FragmentUtils.hide(containerFragments.get(containerFragments.size()-1));

        FragmentUtils.add(getTopFragmentManager(),fragment,containerId,true,enterAnim,exitAnim,enterAnim,exitAnim);
    }

    private Map<Integer,ResultCallback> fragmentResultMap = new LinkedHashMap<>();
    @Override
    public void startFragmentForResult(Fragment fragment,int containerId,int enterAnim,int exitAnim,ResultCallback callback){

        int requestCode = callback.hashCode();
        requestCode &= 0x0000ffff;
        fragmentResultMap.put(requestCode,callback);

        Bundle bundle = fragment.getArguments();
        if (bundle == null)
        {
            bundle = new Bundle();
            fragment.setArguments(bundle);
        }
        bundle.putInt("requestCode",requestCode);
        bundle.putInt("enterAnim",enterAnim);
        bundle.putInt("exitAnim",exitAnim);

        List<Fragment> containerFragments = new LinkedList<>();
        for (Fragment f : FragmentUtils.getFragments(getTopFragmentManager())){
            if (f.getArguments() != null && f.getArguments().getInt(FragmentUtils.ARGS_ID,0) == containerId){
                containerFragments.add(f);
            }
        }
        if (!containerFragments.isEmpty())
            FragmentUtils.hide(containerFragments.get(containerFragments.size()-1));

        FragmentUtils.add(getTopFragmentManager(),fragment,containerId,true,enterAnim,exitAnim,enterAnim,exitAnim);
    }

    @Override
    public void onFragmentResult(int requestCode, int resultCode, Intent intent) {
        getLifecycle().handleActivityResult(requestCode,resultCode,intent);

        ResultCallback callback = fragmentResultMap.get(requestCode);
        fragmentResultMap.remove(requestCode);
        if (null != callback)
        {
            switch (resultCode)
            {
                case Activity.RESULT_OK:
                    callback.onSuccess(intent);
                    break;
                case Activity.RESULT_CANCELED:
                    callback.onCancel();
                    break;
            }
        }
    }

    @Override
    public T getBindAnother() {
        return another;
    }

    @Override
    public void init() {

    }

    public Activity getActivity(){
        return this;
    }

    @Override
    public Context getContext() {
        return this;
    }

    @Override
    public Fragment areFragment() {
        return null;
    }

    @Override
    public Activity areActivity() {
        return this;
    }

    @Override
    public void resolveBundle(Bundle bundle) {

    }

    private View rootView;
    @Override
    public View getRootView() {
        return rootView;
    }

    private boolean isFirstVisible = true;
    @Override
    public boolean isFirstVisible() {
        return isFirstVisible;
    }

    private boolean isRestoreState = false;
    @Override
    public boolean isRestoreState() {
        return isRestoreState;
    }

    @Override
    public void initFragment(Fragment fragment) {

    }

    @Deprecated
    @Override
    public void onBackPressed() {
        if (!FragmentUtils.dispatchBackPress(getTopFragmentManager()) || !onBackClick()) {
            super.onBackPressed();
        }
    }

    @Override
    public void finish() {
        super.finish();
        if (getIntent() != null && getIntent().getExtras() != null){
            Bundle bundle = getIntent().getExtras();
            int enterAnim = bundle.getInt("enterAnim",0);
            int exitAnim = bundle.getInt("exitAnim",0);
            if (enterAnim != 0 || exitAnim != 0)
                overridePendingTransition(enterAnim,exitAnim);
        }
    }

    @Override
    public boolean onBackClick() {
        return false;
    }

    //解决方法覆盖问题
    @Override
    public <T_View extends View> T_View findViewById(int id) {
        return super.findViewById(id);
    }

}
