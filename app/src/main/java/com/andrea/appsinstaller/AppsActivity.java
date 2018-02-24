package com.andrea.appsinstaller;

import android.Manifest;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class AppsActivity extends AppCompatActivity {

    private RecyclerView mRecyclerView;
    private LinearLayoutManager mLayoutManager;
    private AppsAdapter mAdapter;
    private List<AppBuild> mBuildsList;
    private AppBuild mSelectedBuild;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_apps);
        getSupportActionBar().setTitle(R.string.available_apps_string);

        mBuildsList = getMockedAppList();

        mRecyclerView = (RecyclerView) findViewById(R.id.apps_list);
        mRecyclerView.setHasFixedSize(true);
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);

        // specify an adapter (see also next example)
        mAdapter = new AppsAdapter(mBuildsList, new AppsAdapter.RecyclerViewClickListener() {

            @Override
            public void onClick(View view, int position) {
                mSelectedBuild = mBuildsList.get(position);
                if (storagePermissionsGranted()) {
                    downloadAndInstall(mSelectedBuild);
                }
            }
        });
        mRecyclerView.setAdapter(mAdapter);
    }

    private List<AppBuild> getMockedAppList()  {
        List<AppBuild> mockedList = new ArrayList<>();
        mockedList.add(new AppBuild("PAX App", "A great app", "https://github.com/appium/sample-code/raw/master/sample-code/apps/ContactManager/ContactManager.apk", "Grab1.apk"));
        mockedList.add(new AppBuild("DAX App", "This is also pretty good", "https://github.com/appium/sample-code/raw/master/sample-code/apps/ContactManager/ContactManager.apk", "Grab2.apk"));
        return mockedList;
    }

    private boolean storagePermissionsGranted() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                Log.e("Permission error","You have permission");
                return true;
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                return false;
            }
        }
        else { //you dont need to worry about these stuff below api level 23
            Log.e("Permission error","You already have the permission");
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(grantResults[0]== PackageManager.PERMISSION_GRANTED){
            //you have the permission now.
            downloadAndInstall(mSelectedBuild);
        }
    }

    public static class AppsAdapter extends RecyclerView.Adapter<AppsAdapter.ViewHolder> {
        private List<AppBuild> mBuildsList;
        RecyclerViewClickListener mRecyclerViewClickListener;

        public interface RecyclerViewClickListener {
            void onClick(View view, int position);
        }

        public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
            public View mView;
            private RecyclerViewClickListener mListener;

            public ViewHolder(View v, RecyclerViewClickListener mRecyclerViewClickListener) {
                super(v);
                mListener = mRecyclerViewClickListener;
                v.setOnClickListener(this);
                mView = v;
            }

            @Override
            public void onClick(View view) {
                mListener.onClick(view, getAdapterPosition());
            }
        }

        public AppsAdapter(List<AppBuild> buildsList, RecyclerViewClickListener clickListener) {

            mBuildsList = buildsList;
            mRecyclerViewClickListener = clickListener;
        }

        @Override
        public AppsAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                         int viewType) {
            View row = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item, parent, false);
            ViewHolder vh = new ViewHolder(row, mRecyclerViewClickListener);
            return vh;
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            TextView fl = holder.mView.findViewById(R.id.firstLine);
            TextView sl = holder.mView.findViewById(R.id.secondLine);
            fl.setText(mBuildsList.get(position).getName());
            sl.setText(mBuildsList.get(position).getSubTitle());
        }

        @Override
        public int getItemCount() {
            return mBuildsList.size();
        }
    }

    private void downloadAndInstall(AppBuild selectedBuild)   {
        String destination = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/";
        String fileName = selectedBuild.getFileName();
        destination += fileName;

        //Delete update file if exists
        File file = new File(destination);
        if (file.exists())
            file.delete();

        //set downloadmanager
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(selectedBuild.getURL()));
        //request.setDescription(getString(R.string.notification_description));
        request.setTitle(getString(R.string.app_name));
        request.setDescription(getString(R.string.downloading_build_string));

        //set destination
        final Uri uri = Uri.parse("file://" + destination);
        request.setDestinationUri(uri);

        // get download service and enqueue file
        final DownloadManager manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        final long downloadId = manager.enqueue(request);

        //set BroadcastReceiver to install app when .apk is downloaded
        final String dest = destination;
        BroadcastReceiver onComplete = new BroadcastReceiver() {
            public void onReceive(Context ctxt, Intent intent) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    Uri contentUri = FileProvider.getUriForFile(ctxt, BuildConfig.APPLICATION_ID + ".provider", new File(dest));
                    Intent openFileIntent = new Intent(Intent.ACTION_VIEW);
                    openFileIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    openFileIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    openFileIntent.setData(contentUri);
                    startActivity(openFileIntent);
                    unregisterReceiver(this);
                } else {
                    Intent install = new Intent(Intent.ACTION_VIEW);
                    install.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    install.setDataAndType(uri,
                            "application/vnd.android.package-archive");
                    startActivity(install);
                    unregisterReceiver(this);
                }
            }
        };
        //register receiver for when .apk download is compete
        registerReceiver(onComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
    }
}
