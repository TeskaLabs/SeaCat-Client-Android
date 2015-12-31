package com.teskalabs.seacat.android.companion.Base;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.view.ViewStub;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.teskalabs.seacat.android.companion.DashboardActivity;
import com.teskalabs.seacat.android.companion.HttpClientActivity;
import com.teskalabs.seacat.android.companion.LocalDiscoverActivity;
import com.teskalabs.seacat.android.companion.R;

public class BaseActivity extends ActionBarActivity {
    protected ViewStub contentStub;
    protected DrawerLayout mDrawerLayout;
    protected ListView mDrawerList;

    protected boolean isHome=false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_nav_drawer);
        contentStub = (ViewStub) findViewById(R.id.content_stub);

        // Drawer navigation
        DrawerNavItem[] drawerNavItems = new DrawerNavItem[3];

        drawerNavItems[0] = new DrawerNavItem(-1, "Dashboard");
        drawerNavItems[1] = new DrawerNavItem(R.drawable.parabolic4, "Profiles");
        drawerNavItems[2] = new DrawerNavItem(R.drawable.smartphone88, "Http Client");
        //drawerNavItems[3] = new DrawerNavItem(R.drawable.analytics2, "Diagnostics");

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);
        mDrawerList.setAdapter(new DrawerNavAdapter(this, R.layout.drawer_nav_item, drawerNavItems));
        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());
    }

    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            switch(position) {
                case 0:
                    if (isHome)
                        break;
                    Intent intent = new Intent(BaseActivity.this, DashboardActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    break;
                case 1:
                    startActivity(new Intent(BaseActivity.this, LocalDiscoverActivity.class));
                    break;
                case 2:
                    startActivity(new Intent(BaseActivity.this, HttpClientActivity.class));
                    break;
                case 3:
                    mDrawerLayout.closeDrawer(mDrawerList);
                    Toast.makeText(getApplicationContext(), "TODO", Toast.LENGTH_SHORT)
                            .show();
                    return;
            }
            mDrawerLayout.closeDrawer(mDrawerList);
            if (!isHome)
                finish();
        }
    }
}
