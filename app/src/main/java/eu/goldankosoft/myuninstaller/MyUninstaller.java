package eu.goldankosoft.myuninstaller;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

public class MyUninstaller extends ListActivity implements SearchView.OnQueryTextListener {

    private final static String TAG = "MyUninstaller";
    private PackageManager mPkgMgr;
    private ListView mListView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPkgMgr = getPackageManager();
        mListView = getListView();
        mListView.setDividerHeight(0);
        mListView.setFastScrollEnabled(true);
        mListView.setTextFilterEnabled(true);

        setListAdapter(new AppAdapter(this.getApplication(), R.layout.activity_main, getApps()));
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        App app = (App) l.getItemAtPosition(position);
        Uri packageURI = Uri.parse("package:" + app.appIntName);
        startActivityForResult(new Intent(Intent.ACTION_DELETE, packageURI), position);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        ((AppAdapter) mListView.getAdapter()).reloadView();
    }

    private static final class App {
        private String appIntName;
        private String appName;
    }

    private static final class AppHolder {
        private ImageView appIcon;
        private TextView appName;
        private TextView appVersion;
        private TextView appPackageName;
        private TextView appPackagePath;
        private TextView appLastModified;
        private TextView appPackageSize;
        private String appIntName;
    }

    private class AppAdapter extends ArrayAdapter<App> {
        public ArrayList<App> items;

        private AppAdapter(Context context, int viewId, ArrayList<App> mApps) {
            super(context, viewId, mApps);
            items = mApps;
        }

        @Override
        public View getView(int position, View view, ViewGroup parent) {
            App app = items.get(position);
            AppHolder holder;

            if (view == null) {
                LayoutInflater li = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                view = li.inflate(R.layout.activity_main, null);
                holder = new AppHolder();
                holder.appIcon = (ImageView) view.findViewById(R.id.appIcon);
                holder.appName = (TextView) view.findViewById(R.id.appName);
                holder.appVersion = (TextView) view.findViewById(R.id.appVersion);
                holder.appPackageName = (TextView) view.findViewById(R.id.appPackageName);
                holder.appPackagePath = (TextView) view.findViewById(R.id.appPackagePath);
                holder.appLastModified = (TextView) view.findViewById(R.id.appLastModified);
                holder.appPackageSize = (TextView) view.findViewById(R.id.appPackageSize);
                view.setTag(holder);
            } else {
                holder = (AppHolder) view.getTag();
            }
            if (app != null) {
                holder.appIntName = app.appIntName;
                holder.appName.setText(app.appName);
                holder.appPackageName.setText(app.appIntName);
                new GetVersion().execute(holder);
                new GetIcon().execute(holder);
                new GetPackagePath().execute(holder);
                new GetLastModified().execute(holder);
                new GetPackageSize().execute(holder);
            }
            return view;
        }

        private void reloadView() {
            items.clear();
            items.addAll(getApps());
            notifyDataSetChanged();
        }
    }

    private ArrayList<App> getApps() {
        ArrayList<App> appsList = new ArrayList<App>();
        List<ApplicationInfo> appsInstalled = mPkgMgr.getInstalledApplications(
                PackageManager.GET_UNINSTALLED_PACKAGES);

        for (ApplicationInfo appInfo : appsInstalled) {
            if (!isSystemPackage(appInfo)) {
                App app = new App();
                app.appIntName = appInfo.packageName;
                app.appName = appInfo.loadLabel(mPkgMgr).toString();
                appsList.add(app);
            }
        }
        Collections.sort(appsList, new AppNameComparator());
        return appsList;
    }

    private class GetVersion extends AsyncTask<AppHolder, Void, CharSequence> {
        private AppHolder appHolder;

        @Override
        protected CharSequence doInBackground(AppHolder... params) {
            appHolder = params[0];
            CharSequence version;

            try {
                version = mPkgMgr.getPackageInfo(appHolder.appIntName, 0).versionName;
            } catch (NameNotFoundException e) {
                version = "unknown";
                Log.w(TAG, "version not found " + e);
            }
            return version;
        }

        @Override
        protected void onPostExecute(CharSequence result) {
            super.onPostExecute(result);
            appHolder.appVersion.setText(result);
        }
    }

    private class GetPackagePath extends AsyncTask<AppHolder, Void, CharSequence> {
        private AppHolder appHolder;

        @Override
        protected CharSequence doInBackground(AppHolder... params) {
            appHolder = params[0];
            CharSequence packagePath;

            try {
                packagePath = mPkgMgr.getApplicationInfo(appHolder.appIntName, 0).publicSourceDir.toString();
            } catch (NameNotFoundException e) {
                packagePath = "unknown";
                Log.w(TAG, "package path not found " + e);
            }
            return packagePath;
        }

        @Override
        protected void onPostExecute(CharSequence result) {
            super.onPostExecute(result);
            appHolder.appPackagePath.setText(result);
        }
    }

    private class GetLastModified extends AsyncTask<AppHolder, Void, CharSequence> {
        private AppHolder appHolder;

        @Override
        protected CharSequence doInBackground(AppHolder... params) {
            appHolder = params[0];
            String pkgName;
            CharSequence lastFileModified;
            File appFile;

            try {
                pkgName = mPkgMgr.getApplicationInfo(appHolder.appIntName, 0).sourceDir.toString();
                appFile = new File(pkgName);
                lastFileModified = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(new Date(appFile.lastModified())).toString();
            } catch (NameNotFoundException e) {
                lastFileModified = "unknown";
                Log.w(TAG, "last modified not found " + e);
            }
            return lastFileModified;
        }

        @Override
        protected void onPostExecute(CharSequence result) {
            super.onPostExecute(result);
            appHolder.appLastModified.setText(result);
        }
    }

    private class GetPackageSize extends AsyncTask<AppHolder, Void, CharSequence> {
        private AppHolder appHolder;

        @Override
        protected CharSequence doInBackground(AppHolder... params) {
            appHolder = params[0];
            String pkgName;
            CharSequence packageSize;
            File appFile;

            try {
                pkgName = mPkgMgr.getApplicationInfo(appHolder.appIntName, 0).sourceDir.toString();
                appFile = new File(pkgName);
                long size = appFile.length();
                packageSize = this.convertFileSize(size);
            } catch (NameNotFoundException e) {
                packageSize = "unknown";
                Log.w(TAG, "package size not found " + e);
            }
            return packageSize;
        }

        private String convertFileSize(long size) {
            String unit = null;

            float f = (float)size;

            if (f >= 1024.0F) {
                unit = "KB";
                f = (float)(f / 1024.0D);
                if (f >= 1024.0F) {
                    unit = "MB";
                    f = (float)(f / 1024.0D);
                    if (f >= 1024.0F) {
                        unit = "GB";
                        f = (float)(f / 1024.0D);
                    }
                }
            }

            return unit == null ? "0" : round(f, 2) + " " + unit;
        }

        private float round(float d, int decimalPlace) {
            return BigDecimal.valueOf(d).setScale(decimalPlace,BigDecimal.ROUND_HALF_UP).floatValue();
        }

        @Override
        protected void onPostExecute(CharSequence result) {
            super.onPostExecute(result);
            appHolder.appPackageSize.setText(result);
        }
    }

    private class GetIcon extends AsyncTask<AppHolder, Void, Bitmap> {
        private AppHolder appHolder;

        @Override
        protected Bitmap doInBackground(AppHolder... params) {
            appHolder = params[0];
            Drawable icon;

            try {
                icon = mPkgMgr.getApplicationInfo(appHolder.appIntName, 0).loadIcon(mPkgMgr);
            } catch (NameNotFoundException e) {
                icon = getResources().getDrawable(R.mipmap.ic_launcher);
                Log.w(TAG, "icon not found " + e);
            }
            return ((BitmapDrawable) icon).getBitmap();
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            super.onPostExecute(result);
            appHolder.appIcon.setImageBitmap(result);
        }
    }

    private boolean isSystemPackage(ApplicationInfo pkg) {
        return ((pkg.flags & ApplicationInfo.FLAG_SYSTEM) != 0) ? true : false;
    }

    private class AppNameComparator implements Comparator<App> {
        public int compare(App left, App right) {
            return left.appName.compareToIgnoreCase(right.appName);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);

        // Associate searchable configuration with the SearchView
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));

        searchView.setOnQueryTextListener(this);
        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        if (TextUtils.isEmpty(newText)) {
            mListView.clearTextFilter();
        } else {
            mListView.setFilterText(newText.toString());
            Toast.makeText(this, "Not implemented", Toast.LENGTH_SHORT).show();
        }
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        boolean result = true;
        int id = item.getItemId();
        switch (id) {
            case R.id.action_about: {
                displayAboutDialog();
                break;
            }
            default: {
                result = super.onOptionsItemSelected(item);
                break;
            }
        }

        return result;
    }

    private void displayAboutDialog() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setIcon(R.mipmap.about);
        builder.setTitle(getString(R.string.about_title));

        TextView resultMessage = new TextView(this);
        resultMessage.setTextSize(20);
		PackageInfo pInfo = null;
		try {
			pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
		String versionName = pInfo.versionName;
        
		resultMessage.setText(getString(R.string.about_desc) + "\n\n" + "Version: " + versionName + "\n");
        resultMessage.setGravity(Gravity.CENTER);

        builder.setView(resultMessage);

        builder.setPositiveButton(R.string.ok_dialog_text, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });

        builder.show();
    }
}
