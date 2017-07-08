package com.example.zl.smoke;

import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Matrix;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;

import org.w3c.dom.Text;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private String password = null;
    private String ip = null;
    private int port = 0;
    private WifiAdmin wifiAdmin;
    private ListView lv;
    private MyAdapter adapter = null;
    private List<ScanResult> wifiList;
    private String realIp;
    private static int getNewData = 0;

    private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == getNewData){
                String dataS = (String) msg.obj;
                float dataF = Float.parseFloat(dataS);
                progress.setProgress(dataF);
                setData(dataF);
                chart_data.invalidate();
            }
        }
    };
    private ProgressBarView2 progress;
    private LineChart chart_data;
    private ArrayList<Entry> yVals1 = new ArrayList<Entry>();;
    private TextView tv_tix;


    private void setData(float value) {



//        for (int i = 0; i < count; i++) {
//            float mult = range / 2f;
//            float val = (float) (Math.random() * mult) + 50;
//            yVals1.add(new Entry(i, val));
//        }
        yVals1.add(new Entry(yVals1.size(),value));
        LineDataSet set1;

        if (chart_data.getData() != null &&
                chart_data.getData().getDataSetCount() > 0) {
            set1 = (LineDataSet) chart_data.getData().getDataSetByIndex(0);
            set1.setValues(yVals1);
            chart_data.getData().notifyDataChanged();
            chart_data.notifyDataSetChanged();
        } else {
            // create a dataset and give it a type
            set1 = new LineDataSet(yVals1, "浓度值");

            set1.setAxisDependency(YAxis.AxisDependency.LEFT);
            set1.setColor(ColorTemplate.getHoloBlue());
            set1.setCircleColor(Color.GRAY);
            set1.setLineWidth(2f);
            set1.setCircleRadius(3f);
            set1.setFillAlpha(65);
            set1.setFillColor(ColorTemplate.getHoloBlue());
            set1.setHighLightColor(Color.rgb(244, 117, 117));
            set1.setDrawCircleHole(false);
            //set1.setFillFormatter(new MyFillFormatter(0f));
            //set1.setDrawHorizontalHighlightIndicator(false);
            //set1.setVisible(false);
            //set1.setCircleHoleColor(Color.WHITE);


            // create a data object with the datasets
            LineData data = new LineData(set1);
            data.setValueTextColor(Color.WHITE);
            data.setValueTextSize(9f);

            // set data
            chart_data.setVisibleXRangeMaximum(5);
            chart_data.setData(data);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        wifiAdmin = WifiAdmin.getInstance(getApplicationContext());
        lv = (ListView) findViewById(R.id.lv);
        tv_tix = (TextView) findViewById(R.id.tv_tix);
        chart_data = (LineChart) findViewById(R.id.chart_data);
        progress = (ProgressBarView2) findViewById(R.id.progress);
        progress.setMax(10000);
        progress.setProgress(0);
        //不显示右边
        chart_data.getAxisRight().setEnabled(false);
        YAxis yAxis = chart_data.getAxisLeft();
        yAxis.setAxisMinimum(0f); // start at zero
        yAxis.setAxisMaximum(8000f); // the axis maximum is 100

//        XAxis x1 = chart_data.getXAxis();
//        // 几个x坐标轴之间才绘制？
//
//        chart_data.setVisibleXRangeMaximum(5);
//        YAxis axisLeft = chart_data.getAxisLeft();
//        axisLeft.setStartAtZero(false);
        //axisLeft.setAxisMaximum(5);

        XAxis xl = chart_data.getXAxis();
        xl.setSpaceMin(2);
        xl.setSpaceMax(10);

        //警戒线
        LimitLine ll = new LimitLine(4000, "警戒线");
        ll.setLineWidth(0.5f);
        ll.setLineColor(Color.RED);
        ll.setTextColor(Color.RED);
        ll.setTextSize(12);
        ll.setEnabled(true);


        Matrix m=new Matrix();
        m.postScale(1f, 1f);//两个参数分别是x,y轴的缩放比例。例如：将x轴的数据放大为之前的1.5倍
        chart_data.getViewPortHandler().refresh(m, chart_data, false);//将图表动画显示之前进行缩放

        chart_data.animateX(1000); // 立即执行的动画,x轴
       // 至此已经已经完成了左右滑动的效果。



        yAxis.addLimitLine(ll);
//        chart_data.setMaxHighlightDistance(10000);
//        chart_data.setMinimumHeight(100);


        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //连接WiFi
//                Log.e("sout","被点击了"+position);
                final ScanResult result = wifiList.get(position);

                ImageView lock = (ImageView) view.findViewById(R.id.lock);
                int visibility = lock.getVisibility();
                if (visibility == View.VISIBLE) {
                   // Toast.makeText(MainActivity.this, "--有密码--", Toast.LENGTH_SHORT).show();
                    // TODO: 2017/5/21 填密码
                    LayoutInflater factory = LayoutInflater.from(MainActivity.this);//提示框
                    final View viewEdit = factory.inflate(R.layout.editbox_layout, null);//这里必须是final的
                    final EditText edit=(EditText)viewEdit.findViewById(R.id.editText);//获得输入框对象
                    //edit.setText(password);
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("输入密码：")//提示框标题
                            .setView(viewEdit)
                            .setPositiveButton("确定",//提示框的两个按钮
                                    new android.content.DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog,
                                                            int which) {
                                            //事件
                                            password = edit.getText().toString().trim();
                                            if (password != null){
                                                connect(result, password);
                                            }
                                        }
                                    }).setNegativeButton("取消", null).create().show();

                } else if (visibility == View.INVISIBLE) {
                  //  Toast.makeText(MainActivity.this, "--无密码--", Toast.LENGTH_SHORT).show();
                    connect(result, "");
                    // TODO: 2017/5/22 无密码的连接不上
                }

            }
        });
    }

    private void connect(final ScanResult result, final String password) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (password.equals("")) {
                    wifiAdmin.addNetwork(wifiAdmin.createWifiInfo(result.SSID, "", 1, "wt"));

                } else {
                    WifiInfo wifiInfo = wifiAdmin.getWifiInfo();
                    wifiAdmin.disconnectWifi(wifiAdmin.getNetworkId());
                    wifiAdmin.addNetwork(wifiAdmin.createWifiInfo(result.SSID, password, 3, "wt"));
                    int ip = wifiAdmin.getIPAddress();
                    realIp = intToIp(ip);
                }

            }
        }).start();
    }

    private void receiveData() {
        LayoutInflater factory = LayoutInflater.from(MainActivity.this);//提示框
        final View viewEdit = factory.inflate(R.layout.editbox_layout2, null);//这里必须是final的
        final EditText ip_Edit=(EditText)viewEdit.findViewById(R.id.ip);//获得输入框对象
        final EditText port_Edit=(EditText)viewEdit.findViewById(R.id.port);//获得输入框对象
        ip_Edit.setText(ip);
        if (port != 0){
            port_Edit.setText(port+"");
        }
        new AlertDialog.Builder(MainActivity.this)
                .setTitle("输入ip和端口号：")//提示框标题
                .setView(viewEdit)
                .setPositiveButton("确定",//提示框的两个按钮
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                                int which) {
                                ip = ip_Edit.getText().toString().trim();
                                port = Integer.parseInt(port_Edit.getText().toString().trim());

                                if (ip != null && port != 0 ){
                                    lv.setVisibility(View.INVISIBLE);
                                    chart_data.setVisibility(View.VISIBLE);
                                    progress.setVisibility(View.VISIBLE);
                                    tv_tix.setVisibility(View.INVISIBLE);
                                    Log.e("sout","kaishisocket"+ip+port);
                                    new Thread(new Runnable() {
                                        @Override
                                        public void run() {
                                            Socket socket = null;
                                            InputStream inputStream = null;
                                            String data = null;
                                            try {
                                                Log.e("sout", realIp+"--===-");
                                                Log.e("sout","1");
                                                socket = new Socket();
                                                Log.e("sout","1.1");
                                                // TODO: 2017/5/22 这里的ip地址不是realIP ，这里填pc分配给360的IP，如果是点对点的话，就是realIP
                                                InetSocketAddress inetSocketAddress = new InetSocketAddress(ip, port);
                                                Log.e("sout","1.2");
                                                socket.connect(inetSocketAddress, 10000); //端口号为30000
                                                Log.e("sout","2");
                                                inputStream = socket.getInputStream();
//                            Log.e("sout","3");
                                                while (true){
                                                    byte[] buf = new byte[1024];
                                                    int len = inputStream.read(buf);
                                                    data = new String(buf, 0, len,"GBK");
                                                    Log.e("sout","=========="+data);

                                                    Message message = handler.obtainMessage();
                                                    message.what = getNewData;
                                                    message.obj = data;
                                                    handler.sendMessage(message);
                                                }
//                            Log.e("sout","4");
                                            } catch (Exception e) {
                                                Log.e("sout","5"+ e.getMessage());
                                                e.printStackTrace();
                                               // Toast.makeText(MainActivity.this, "出错了", Toast.LENGTH_SHORT).show();
                                            }finally {
                                                try {
                                                    if (socket != null){
                                                        socket.shutdownInput();
                                                        socket.close();
                                                    }
                                                } catch (IOException e) {
                                                    e.printStackTrace();
                                                }
                                            }
                                        }
                                    }).start();
                                }else {
                                    Log.e("sout","meiyou"+ip+port);
                                }
                            }
                        }).setNegativeButton("取消", null).create().show();
        //0.0.0.0:错误
//        if(!realIp.equals("0.0.0.0")){
        }
//    }

    //将获取的int转为真正的ip地址,参考的网上的，修改了下
    private String intToIp(int paramInt) {
        return (paramInt & 0xFF) + "." + (0xFF & paramInt >> 8) + "." + (0xFF & paramInt >> 16) + "."
                + (0xFF & paramInt >> 24);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            if (wifiAdmin.mWifiManager.isWifiEnabled()) {
                Toast.makeText(this, "wifi已经打开了", Toast.LENGTH_SHORT).show();
            } else {
                wifiAdmin.OpenWifi();
            }
            return true;
        }
        if (id == R.id.search_AP) {

            wifiAdmin.startScan();
            wifiAdmin.setWifiList();
            // StringBuilder stringBuilder = wifiAdmin.lookUpScan();
            // Log.e("sout",stringBuilder.toString()+"---");
            wifiList = wifiAdmin.getWifiList();
//            Log.e("sout", wifiList.size()+"---");
//            for (ScanResult result : wifiList) {
//                Log.e("sout",result.SSID+"---");
//            }
            adapter = new MyAdapter(wifiList, this);
            lv.setVisibility(View.VISIBLE);
            lv.setAdapter(adapter);
            chart_data.setVisibility(View.INVISIBLE);
            progress.setVisibility(View.INVISIBLE);
            tv_tix.setVisibility(View.INVISIBLE);
            return true;
        }
        if (id == R.id.data_deal){

            receiveData();
        }


        return super.onOptionsItemSelected(item);
    }
}
