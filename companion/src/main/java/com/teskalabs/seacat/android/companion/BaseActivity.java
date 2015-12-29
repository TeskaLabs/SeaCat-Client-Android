package com.teskalabs.seacat.android.companion;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.view.ViewStub;
import android.widget.AdapterView;
import android.widget.ListView;

public class BaseActivity extends ActionBarActivity {
    protected ViewStub contentStub;
    protected DrawerLayout mDrawerLayout;
    protected ListView mDrawerList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_nav_drawer);
        contentStub = (ViewStub) findViewById(R.id.content_stub);

        // Drawer navigation
        DrawerNavItem[] drawerNavItems = new DrawerNavItem[4];

        drawerNavItems[0] = new DrawerNavItem(-1, "Dashboard");
        drawerNavItems[1] = new DrawerNavItem(R.drawable.parabolic4, "Profiles");
        drawerNavItems[2] = new DrawerNavItem(R.drawable.smartphone88, "Http Client");
        drawerNavItems[3] = new DrawerNavItem(R.drawable.analytics2, "Diagnostics");

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);

        mDrawerList.setAdapter(new DrawerNavAdapter(this, R.layout.drawer_nav_item, drawerNavItems));
        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());
    }

    private class DrawerItemClickListener implements ListView.OnItemClickListener {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            switch(position)
            {
                case 0:
                    startActivity(new Intent(BaseActivity.this, DashboardActivity.class));
                    finish();
                    break;
                case 1:
                    startActivity(new Intent(BaseActivity.this, LocalDiscoverActivity.class));
                    finish();
                    break;
                case 2:
                    startActivity(new Intent(BaseActivity.this, HttpClientActivity.class));
                    finish();
                    break;
                case 3:
                    break;
            }

        }

    }
}
