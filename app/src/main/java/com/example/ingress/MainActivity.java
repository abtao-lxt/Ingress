package com.example.ingress;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.MapsInitializer;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.MyLocationStyle;
import com.amap.api.services.core.ServiceSettings;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.amap.api.maps.model.BitmapDescriptor;
import com.amap.api.maps.model.BitmapDescriptorFactory;

public class MainActivity extends AppCompatActivity implements AMap.OnMyLocationChangeListener {

    private MapView mMapView = null; // 声明 MapView
    private AMap aMap; // 地图控制器对象
    private int score = 0; // 玩家得分
    private TextView scoreTextView; // 显示得分
    private List<Marker> coinMarkers = new ArrayList<>(); // 存储金币的标记
    private static final int MAX_COINS = 5; // 地图上最多存在的金币数量

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 设置高德地图的隐私合规配置
        MapsInitializer.updatePrivacyAgree(this, true);
        MapsInitializer.updatePrivacyShow(this, true, true);
        ServiceSettings.updatePrivacyShow(this, true, true);
        ServiceSettings.updatePrivacyAgree(this, true);

        // 初始化视图
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // 初始化 MapView
        mMapView = findViewById(R.id.map);
        mMapView.onCreate(savedInstanceState); // 此方法必须重写

        scoreTextView = findViewById(R.id.scoreTextView); // 关联 TextView 用于显示得分
        updateScore(); // 显示初始得分

        // 获取 AMap 对象
        if (aMap == null) {
            aMap = mMapView.getMap();
        }

        // 检查并请求权限
        checkAndRequestPermissions();
    }

    /**
     * 配置定位蓝点
     */
    private void setupLocationBlueDot() {
        MyLocationStyle myLocationStyle = new MyLocationStyle();
        myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE);
        myLocationStyle.strokeColor(Color.TRANSPARENT);
        myLocationStyle.radiusFillColor(Color.TRANSPARENT);

        aMap.setMyLocationStyle(myLocationStyle);
        aMap.setMyLocationEnabled(true);

        aMap.moveCamera(CameraUpdateFactory.zoomTo(15));

        // 直接将当前活动作为位置更改监听器
        aMap.setOnMyLocationChangeListener(this);
    }

    /**
     * 更新得分显示
     */
    private void updateScore() {
        scoreTextView.setText(getString(R.string.score_text, score));
    }

    /**
     * 在地图上放置指定数量的随机金币
     */
    private void placeRandomCoins(int numCoins) {
        // 确保已获得当前位置
        Location playerLocation = aMap.getMyLocation();
        if (playerLocation == null) {
            return;
        }

        LatLng playerLatLng = new LatLng(playerLocation.getLatitude(), playerLocation.getLongitude());

        Random random = new Random();
        for (int i = 0; i < numCoins; i++) {
            // 在1000米半径范围内生成随机的经纬度
            double radiusInDegrees = 500 / 111000f; // 1000米转换为经纬度差值 (1度约111公里)
            double randomLat = playerLatLng.latitude + (random.nextDouble() - 0.5) * 2 * radiusInDegrees;
            double randomLng = playerLatLng.longitude + (random.nextDouble() - 0.5) * 2 * radiusInDegrees;

            LatLng coinPosition = new LatLng(randomLat, randomLng);

            // 使用 coin.png 图标
            BitmapDescriptor coinIcon = BitmapDescriptorFactory.fromResource(R.drawable.coin);

            MarkerOptions markerOptions = new MarkerOptions()
                    .position(coinPosition)
                    .title("Gold Coin")
                    .snippet("Collect me!")
                    .icon(coinIcon); // 使用自定义的金币图标

            Marker coinMarker = aMap.addMarker(markerOptions);
            coinMarkers.add(coinMarker);
        }
    }

    /**
     * 检查玩家是否收集到金币
     */
    private void checkForCollectedCoins(LatLng playerPosition) {
        for (int i = 0; i < coinMarkers.size(); i++) {
            Marker coinMarker = coinMarkers.get(i);
            if (coinMarker != null && coinMarker.isVisible()) {
                float[] results = new float[1];
                Location.distanceBetween(playerPosition.latitude, playerPosition.longitude,
                        coinMarker.getPosition().latitude, coinMarker.getPosition().longitude, results);

                if (results[0] < 10) { // 如果距离小于10米，认为金币被收集
                    coinMarker.remove();
                    coinMarkers.remove(i);
                    score++;
                    updateScore();
                    Toast.makeText(this, "Collected a coin!", Toast.LENGTH_SHORT).show();

                    // 当收集一个金币后补充一个新的金币
                    placeRandomCoins(1);
                    break;
                }
            }
        }
    }

    @Override
    public void onMyLocationChange(Location location) {
        if (location != null) {
            // 获取玩家当前位置
            LatLng playerPosition = new LatLng(location.getLatitude(), location.getLongitude());

            // 检查并收集金币
            checkForCollectedCoins(playerPosition);

            // 如果金币数量不足5个，则生成新的金币
            if (coinMarkers.size() < MAX_COINS) {
                placeRandomCoins(MAX_COINS - coinMarkers.size());
            }
        }
    }

    // 检查并请求所需的定位权限
    private void checkAndRequestPermissions() {
        List<String> permissionList = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }

        if (!permissionList.isEmpty()) {
            String[] permissions = permissionList.toArray(new String[0]);
            ActivityCompat.requestPermissions(this, permissions, 1001);
        } else {
            setupLocationBlueDot();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1001) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            setupLocationBlueDot();
        }
    }
}
