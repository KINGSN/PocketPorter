package com.cscodetech.pocketporter.activity;

import static com.cscodetech.pocketporter.utility.SessionManager.coupon;
import static com.cscodetech.pocketporter.utility.SessionManager.couponid;
import static com.cscodetech.pocketporter.utility.SessionManager.currency;
import static com.cscodetech.pocketporter.utility.SessionManager.wallet;
import static com.cscodetech.pocketporter.utility.Utility.paymentsucsses;
import static com.cscodetech.pocketporter.utility.Utility.tragectionID;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.cscodetech.pocketporter.R;
import com.cscodetech.pocketporter.adepter.WheelerAdapter;
import com.cscodetech.pocketporter.map.FetchURL;
import com.cscodetech.pocketporter.map.TaskLoadedCallback;
import com.cscodetech.pocketporter.model.PaymentItem;
import com.cscodetech.pocketporter.model.RestResponse;
import com.cscodetech.pocketporter.model.User;
import com.cscodetech.pocketporter.model.WCategory;
import com.cscodetech.pocketporter.model.Wheele;
import com.cscodetech.pocketporter.model.Wheeleritem;
import com.cscodetech.pocketporter.retrofit.APIClient;
import com.cscodetech.pocketporter.retrofit.GetResult;
import com.cscodetech.pocketporter.utility.CustPrograssbar;
import com.cscodetech.pocketporter.utility.Drop;
import com.cscodetech.pocketporter.utility.Pickup;
import com.cscodetech.pocketporter.utility.SessionManager;
import com.cscodetech.pocketporter.utility.Utility;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;

public class BookWheelerActivity extends AppCompatActivity implements OnMapReadyCallback, TaskLoadedCallback, WheelerAdapter.RecyclerTouchListener, GetResult.MyListener {

    @BindView(R.id.recycleview_wheele)
    RecyclerView recycleviewWheele;
    @BindView(R.id.txt_pickupcontect)
    TextView txtPickupcontect;
    @BindView(R.id.btn_bookwheele)
    TextView btnBookwheele;
    @BindView(R.id.txt_goodtype)
    TextView txtGoodtype;
    @BindView(R.id.txt_applycode)
    TextView txtApplycode;
    @BindView(R.id.lvl_view)
    ScrollView lvlView;
    Pickup pickup;
    Drop drop;
    GoogleMap mMap;
    private Polyline currentPolyline;
    CustPrograssbar custPrograssbar;
    SessionManager sessionManager;
    double dist = 0;
    User user;

    List<PaymentItem> paymentList = new ArrayList<>();
    List<WCategory> categoryList = new ArrayList<>();
    WCategory wCategor;
    Wheeleritem wheelitem;
    double tWallet = 0;
    double itmeprice = 0;
    double totalprice = 0;
    double time = 0;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_wheeler);
        ButterKnife.bind(this);

        custPrograssbar = new CustPrograssbar();
        sessionManager = new SessionManager(this);
        sessionManager.setIntData(coupon, 0);
        user = sessionManager.getUserDetails();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        pickup = getIntent().getParcelableExtra("pickup");
        drop = getIntent().getParcelableExtra("drop");
        Log.d("kingsn", "onCreate: "+pickup.getAddress()+"\n"+drop.getAddress());
        txtPickupcontect.setText("" + drop.getRname() + "-" + drop.getRmobile());

        LinearLayoutManager mLayoutManager2 = new LinearLayoutManager(this);
        mLayoutManager2.setOrientation(LinearLayoutManager.HORIZONTAL);
        recycleviewWheele.setLayoutManager(mLayoutManager2);
        recycleviewWheele.setItemAnimator(new DefaultItemAnimator());
       // dist = distance(pickup.getLat(), pickup.getLog(), drop.getLat(), drop.getLog());

        Log.d("KINGSN", "onCreate: "+dist);
        String dist2 = null;
       /* try {
          *//*  dist2 = distance2(pickup.getAddress(), drop.getAddress());
            Log.d("KINGSN", "DDDD: "+dist2);
            dist= Double.parseDouble(dist2);*//*
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }*/

        try {
            distance2(pickup.getAddress(), drop.getAddress());
        } catch (JSONException e) {
            e.printStackTrace();
        }


        vehiclelist();

    }

    public void distance2(String address, String address1) throws JSONException {
        String sb="https://maps.googleapis.com/maps/api/distancematrix/json?origins="+address+"&destinations="+address1+"&units=metric&key="+getString(R.string.API_KEY);
        Log.d("KINGSN", "distance2: "+sb);
        String api=sb;
        StringRequest stringRequest = new StringRequest(Request.Method.GET, api,
                new Response.Listener<String>() {
                    @SuppressLint("SetTextI18n")
                    @Override
                    public void onResponse(String response) {
                        System.out.println(response);
                        Log.d("KINGSN", "onResponse:Superactivity1 "+response);
                        try {
                            JSONObject jsonObject = new JSONObject(response);
                            JSONObject distanceJson = new JSONObject(response)
                                    .getJSONArray("rows")
                                    .getJSONObject(0)
                                    .getJSONArray("elements")
                                    .getJSONObject(0)
                                    .getJSONObject("distance");
                           Double  distanceDouble = null ;
                            String distance = distanceJson.get("text").toString();
                            distance=(distance.replace("km", ""));
                            distance=(distance.replace(" ", ""));
                            if (distance.contains("km")){
                               // distanceDouble = Double.parseDouble(distance.replace("km", ""));
                                distanceDouble = Double.parseDouble(distance.replace(",", ""));


                            }else {
                               // distanceDouble = Double.parseDouble(String.valueOf(Integer.parseInt(distance)/1000));
                                distanceDouble = Double.parseDouble(distance.replace(",", ""));
                            }

                            Log.d("KINGSN","array_rows:"+distanceDouble);
                            dist=distanceDouble;
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }


                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        //progressDialog.dismiss();
                        //   method.loadingDialogg(MainActivity.this);
                        Log.e("Error", "" + error.getMessage());
                    }

                }) {

            @Override
            protected Map<String, String> getParams() {


                return new HashMap<>();

            }

        };

        stringRequest.setShouldCache(false);
        RequestQueue requestQueue = Volley.newRequestQueue(BookWheelerActivity.this);
        requestQueue.add(stringRequest);
        /*  Log.d("KINGSN", "distance2: "+data1);
        OkHttpClient client = new OkHttpClient().newBuilder()
                .build();
        MediaType mediaType = MediaType.parse("text/plain");
        RequestBody body = RequestBody.create(mediaType, "");
        Request request = new Request.Builder()
                .url("https://maps.googleapis.com/maps/api/distancematrix/json?origins="+address+"&destinations="+address1+"&units=imperial&key="+getString(R.string.API_KEY))
                .method("GET", body)
                .build();
        Response response = client.newCall(request).execute();
        Log.d("KINGSN", "distance: "+response);
    return response.toString();*/






    }

    private void vehiclelist() {

        custPrograssbar.prograssCreate(this);
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("uid", user.getId());
            jsonObject.put("lat", pickup.getLat());
            jsonObject.put("long", pickup.getLog());

        } catch (Exception e) {
            e.printStackTrace();
        }
        RequestBody bodyRequest = RequestBody.create(MediaType.parse("application/json"), jsonObject.toString());
        Call<JsonObject> call = APIClient.getInterface().vehiclelist(bodyRequest);
        GetResult getResult = new GetResult();
        getResult.setMyListener(this);
        getResult.callForLogin(call, "1");

    }

    private void placeOrder() {

        custPrograssbar.prograssCreate(this);
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("uid", user.getId());
            jsonObject.put("p_method_id", Utility.paymentId);
            jsonObject.put("pick_address", pickup.getAddress());
            jsonObject.put("pick_lat", pickup.getLat());
            jsonObject.put("pick_lng", pickup.getLog());
            jsonObject.put("drop_address", drop.getAddress());
            jsonObject.put("drop_lat", drop.getLat());
            jsonObject.put("drop_lng", drop.getLog());
            jsonObject.put("drop_name", drop.getRname());
            jsonObject.put("drop_mobile", drop.getRmobile());
            jsonObject.put("vehicleid", wheelitem.getId());
            jsonObject.put("cou_id", sessionManager.getIntData(couponid));
            jsonObject.put("cou_amt", sessionManager.getIntData(coupon));
            jsonObject.put("transaction_id", tragectionID);
            jsonObject.put("wall_amt", tWallet);
            jsonObject.put("o_total", totalprice);
            jsonObject.put("cat_id", wCategor.getId());
            jsonObject.put("subtotal", itmeprice);

        } catch (Exception e) {
            e.printStackTrace();
        }
        RequestBody bodyRequest = RequestBody.create(MediaType.parse("application/json"), jsonObject.toString());
        Call<JsonObject> call = APIClient.getInterface().orderNow(bodyRequest);
        GetResult getResult = new GetResult();
        getResult.setMyListener(this);
        getResult.callForLogin(call, "2");

    }

    @Override
    public void callback(JsonObject result, String callNo) {
        try {
            custPrograssbar.closePrograssBar();
            if (callNo.equalsIgnoreCase("1")) {
                Gson gson = new Gson();
                Wheele wheele = gson.fromJson(result.toString(), Wheele.class);
                Toast.makeText(BookWheelerActivity.this, wheele.getResponseMsg(), Toast.LENGTH_SHORT).show();
                if (wheele.getResult().equalsIgnoreCase("true")) {

                    List<Wheeleritem> list = wheele.getVehiclelist();
                    recycleviewWheele.setAdapter(new WheelerAdapter(this, list, this, dist));
                    paymentList = wheele.getPaymentItems();

                    if (wheele.getCategories().size() != 0) {
                        categoryList = wheele.getCategories();
                        wCategor = wheele.getCategories().get(0);
                    }
                    txtGoodtype.setText("" + wCategor.getCatName());


                }
            } else if (callNo.equalsIgnoreCase("2")) {

                Gson gson = new Gson();
                RestResponse restResponse = gson.fromJson(result.toString(), RestResponse.class);
                user.setWallet(restResponse.getWallet());
                sessionManager.setUserDetails(user);
                if (restResponse.getResult().equalsIgnoreCase("true")) {

                    sessionManager.setIntData(wallet, Integer.parseInt(restResponse.getWallet()));
                    Intent intent = new Intent(this, DriverSearchActivity.class).putExtra("oid", restResponse.getOrderId());
                    startActivity(intent);
                    finish();

                } else {
                    finish();
                    Toast.makeText(this, restResponse.getResponseMsg(), Toast.LENGTH_LONG).show();
                }
            }
        } catch (Exception e) {
            Log.e("Error", "-" + e.getMessage());

        }
    }


    public void bottonPaymentList() {
        BottomSheetDialog mBottomSheetDialog = new BottomSheetDialog(this);
        View sheetView = getLayoutInflater().inflate(R.layout.custome_payment, null);
        LinearLayout listView = sheetView.findViewById(R.id.lvl_list);
        TextView txtBooktrip = sheetView.findViewById(R.id.btn_booktrip);
        LinearLayout lvlWallat = sheetView.findViewById(R.id.lvl_wallat);
        if (Double.parseDouble(user.getWallet()) == 0) {
            lvlWallat.setVisibility(View.GONE);
        }
        Switch swich = sheetView.findViewById(R.id.swich);
        TextView txtTotal = sheetView.findViewById(R.id.txt_total);
        TextView txtWallet = sheetView.findViewById(R.id.txt_wallet);
        totalprice = itmeprice - sessionManager.getIntData(coupon);
        txtTotal.setText("Total " + sessionManager.getStringData(currency) + totalprice);
        txtWallet.setText("PocketPorter credit (" + sessionManager.getStringData(currency) + user.getWallet() + ")");
        txtBooktrip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Utility.paymentId = "5";
                placeOrder();
            }
        });
        swich.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    if (Double.parseDouble(user.getWallet()) < totalprice) {
                        double t = totalprice - Double.parseDouble(user.getWallet());
                        txtTotal.setText("item total " + sessionManager.getStringData(currency) + t);
                        tWallet = Double.parseDouble(user.getWallet());
                        txtBooktrip.setVisibility(View.GONE);

                    } else {
                        double t = Double.parseDouble(user.getWallet()) - totalprice;
                        txtWallet.setText("PocketPorter credit (" + sessionManager.getStringData(currency) + t + ")");
                        txtTotal.setText("item total " + sessionManager.getStringData(currency) + "0");
                        tWallet = totalprice;
                        listView.setVisibility(View.GONE);
                        txtBooktrip.setVisibility(View.VISIBLE);
                    }
                } else {
                    txtTotal.setText("item total " + sessionManager.getStringData(currency) + totalprice);
                    txtWallet.setText("PocketPorter credit (" + sessionManager.getStringData(currency) + user.getWallet() + ")");
                    listView.setVisibility(View.VISIBLE);
                    txtBooktrip.setVisibility(View.GONE);
                    tWallet = 0;
                }
            }
        });
        for (int i = 0; i < paymentList.size(); i++) {
            LayoutInflater inflater = LayoutInflater.from(BookWheelerActivity.this);
            PaymentItem paymentItem = paymentList.get(i);
            View view = inflater.inflate(R.layout.custome_paymentitem, null);
            ImageView imageView = view.findViewById(R.id.img_icon);
            TextView txtTitle = view.findViewById(R.id.txt_title);
            TextView txtSubtitel = view.findViewById(R.id.txt_subtitel);
            txtTitle.setText("" + paymentList.get(i).getmTitle());
            txtSubtitel.setText("" + paymentList.get(i).getSubtitle());
            Glide.with(BookWheelerActivity.this).load(APIClient.baseUrl + "/" + paymentList.get(i).getmImg()).thumbnail(Glide.with(BookWheelerActivity.this).load(R.drawable.emty)).into(imageView);
            int finalI = i;
            view.setOnClickListener(v -> {
                Utility.paymentId = paymentList.get(finalI).getmId();
                try {
                    switch (paymentList.get(finalI).getmTitle()) {
                        case "Razorpay":
                            int temtoal = (int) Math.round(totalprice);
                            mBottomSheetDialog.cancel();
                            startActivity(new Intent(BookWheelerActivity.this, RazerpayActivity.class).putExtra("amount", temtoal).putExtra("detail", paymentItem));
                            break;
                        case "Cash On Delivery":
//                          placeOrder();
                            Toast.makeText(this, "COD Not Available In Your Location", Toast.LENGTH_SHORT).show();
                           mBottomSheetDialog.cancel();
                           break;
                        case "Paypal":
                            mBottomSheetDialog.cancel();
                            startActivity(new Intent(BookWheelerActivity.this, PaypalActivity.class).putExtra("amount", totalprice).putExtra("detail", paymentItem));
                            break;
                        case "Stripe":
                            mBottomSheetDialog.cancel();
                            startActivity(new Intent(BookWheelerActivity.this, StripPaymentActivity.class).putExtra("amount", totalprice).putExtra("detail", paymentItem));
                            break;
                        case "FlutterWave":
                            mBottomSheetDialog.cancel();
                            startActivity(new Intent(BookWheelerActivity.this, FlutterwaveActivity.class).putExtra("amount", totalprice));
                            break;
                        case "Paytm":
                            mBottomSheetDialog.cancel();
                            startActivity(new Intent(BookWheelerActivity.this, PaytmActivity.class).putExtra("amount", totalprice));
                            break;
                        case "SenangPay":
                            mBottomSheetDialog.cancel();
                            startActivity(new Intent(BookWheelerActivity.this, SenangpayActivity.class).putExtra("amount", totalprice).putExtra("detail", paymentItem));
                            break;
                        case "PayStack":
                            int temtoal1 = (int) Math.round(totalprice);
                            mBottomSheetDialog.cancel();
                            startActivity(new Intent(BookWheelerActivity.this, PaystackActivity.class).putExtra("amount", temtoal1).putExtra("detail", paymentItem));
                            break;
                        default:
                            break;
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            listView.addView(view);
        }
        mBottomSheetDialog.setContentView(sheetView);
        mBottomSheetDialog.show();
    }

    public void bottonGoodsList() {
        BottomSheetDialog mBottomSheetDialog = new BottomSheetDialog(this);
        View sheetView = getLayoutInflater().inflate(R.layout.custome_goods, null);
        LinearLayout listView = sheetView.findViewById(R.id.lvl_list);
        for (int i = 0; i < categoryList.size(); i++) {
            LayoutInflater inflater = LayoutInflater.from(BookWheelerActivity.this);
            WCategory wCategory = categoryList.get(i);
            View view = inflater.inflate(R.layout.custome_goodsitem, null);
            ImageView imageView = view.findViewById(R.id.img_select);
            ImageView imgIcon = view.findViewById(R.id.img_icon);
            TextView txtTitle = view.findViewById(R.id.txt_title);
            Glide.with(this).load(APIClient.baseUrl + "/" + wCategory.getCatImg()).thumbnail(Glide.with(this).load(R.drawable.emty)).into(imgIcon);
            txtTitle.setText("" + wCategory.getCatName());
            if (wCategory.isSelected()) {
                imageView.setVisibility(View.VISIBLE);
            } else {
                imageView.setVisibility(View.GONE);
            }
            int finalI = i;
            view.setOnClickListener(v -> {
                for (int iq = 0; iq < categoryList.size(); iq++) {
                    if (finalI == iq) {
//                        Toast.makeText(this, "cat has data and selected", Toast.LENGTH_SHORT).show();
                        categoryList.get(iq).setSelected(true);
                        txtGoodtype.setText("" + categoryList.get(iq).getCatName());
                        wCategor = categoryList.get(iq);
                    } else {
//                        Toast.makeText(this, "cat not selected", Toast.LENGTH_SHORT).show();
                        categoryList.get(iq).setSelected(true);
                    }
                }
                imageView.setVisibility(View.VISIBLE);
                mBottomSheetDialog.cancel();
            });
            listView.addView(view);
        }

        mBottomSheetDialog.setContentView(sheetView);
        mBottomSheetDialog.show();
    }

    public void bottonInfo(Wheeleritem item) {
        BottomSheetDialog mBottomSheetDialog = new BottomSheetDialog(this);
        View sheetView = getLayoutInflater().inflate(R.layout.custome_info, null);
        ImageView imgIcon = sheetView.findViewById(R.id.img_icon);
        TextView txtTitle = sheetView.findViewById(R.id.txt_title);
        TextView txtCapcity = sheetView.findViewById(R.id.txt_capcity);
        TextView txtSize = sheetView.findViewById(R.id.txt_size);
        TextView txtDis = sheetView.findViewById(R.id.txt_dis);
        Glide.with(this).load(APIClient.baseUrl + "/" + item.getImg()).thumbnail(Glide.with(this).load(R.drawable.emty)).into(imgIcon);
        txtTitle.setText("" + item.getTitle());
        txtCapcity.setText("" + item.getCapcity());
        txtSize.setText("" + item.getSize());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            txtDis.setText(Html.fromHtml(item.getDescription(), Html.FROM_HTML_MODE_COMPACT));
        } else {
            txtDis.setText(Html.fromHtml(item.getDescription()));
        }
        mBottomSheetDialog.setContentView(sheetView);
        mBottomSheetDialog.show();
    }

    public void bottonConfirm() {
        BottomSheetDialog mBottomSheetDialog = new BottomSheetDialog(BookWheelerActivity.this);
        View sheetView = getLayoutInflater().inflate(R.layout.customeconfirmdetails, null);
        mBottomSheetDialog.setContentView(sheetView);
        CheckBox chUser = sheetView.findViewById(R.id.ch_user);
        EditText edName = sheetView.findViewById(R.id.ed_name);
        EditText edmobile = sheetView.findViewById(R.id.ed_mobile);
        TextView txtChoose = sheetView.findViewById(R.id.btn_send);
        edName.setText(drop.getRname());
        edmobile.setText(drop.getRmobile());
        chUser.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    edName.setText(user.getFname());
                    edmobile.setText(user.getMobile());
                } else {
                    edmobile.setText("");
                    edName.setText("");
                }
            }
        });
        txtChoose.setOnClickListener(v -> {
            if (!TextUtils.isEmpty(edmobile.getText().toString()) && !TextUtils.isEmpty(edName.getText().toString())) {
                mBottomSheetDialog.cancel();
                drop.setRname(edName.getText().toString());
                drop.setRmobile("" + edmobile.getText().toString());
                txtPickupcontect.setText("" + drop.getRname() + "-" + drop.getRmobile());

            }
        });
        mBottomSheetDialog.show();
    }

    @SuppressLint("NonConstantResourceId")
    @OnClick({R.id.btn_bookwheele, R.id.lvl_payment, R.id.txt_goodtype, R.id.txt_applycode, R.id.txt_pickupcontect})
    public void onBindClick(View view) {
        switch (view.getId()) {
            case R.id.btn_bookwheele:
                if (wheelitem != null) {
                    bottonPaymentList();
                } else {
                    Toast.makeText(this, "Select Vehicle first", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.txt_goodtype:
                bottonGoodsList();
                break;
            case R.id.lvl_payment:

                break;
            case R.id.txt_pickupcontect:
                bottonConfirm();
                break;
            case R.id.txt_applycode:
                if (sessionManager.getIntData(coupon) != 0) {
                    txtApplycode.setText("Apply Coupon");
                    sessionManager.setIntData(coupon, 0);
                } else {
                    int temp = (int) Math.round(itmeprice);
                    startActivity(new Intent(BookWheelerActivity.this, CoupunActivity.class).putExtra("amount", temp));
                }
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + view.getId());
        }
    }

    @Override
    public void onClickWheelerItem(Wheeleritem item, int position) {

        bottonInfo(item);

    }


    @Override
    public void onClickWheelerInfo(Wheeleritem item, int position) {
        sessionManager.setIntData(coupon, 0);
        txtApplycode.setText("Apply Coupon");
        wheelitem = item;
        if (dist <= item.getStartDistance()) {
            itmeprice = item.getStartPrice();
        } else {
            double km = dist - item.getStartDistance();
            itmeprice = item.getStartPrice() + (km * item.getAfterPrice());
        }
        time = dist * item.getTimeTaken();

    }


    @Override
    protected void onResume() {
        super.onResume();
        if (sessionManager.getIntData(coupon) != 0) {
            txtApplycode.setText("Save " + sessionManager.getStringData(currency) + sessionManager.getIntData(coupon));
        } else {
            txtApplycode.setText("Apply Coupon");
            sessionManager.setIntData(coupon, 0);
        }

        if (paymentsucsses == 1) {
            paymentsucsses = 0;
            placeOrder();
        }
    }


    @Override
    public void onTaskDone(Object... values) {

        if (currentPolyline != null)
            currentPolyline.remove();
        currentPolyline = mMap.addPolyline((PolylineOptions) values[0]);

    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {

        mMap = googleMap;
        MarkerOptions place2;
        MarkerOptions place1;
        place1 = new MarkerOptions().position(new LatLng(pickup.getLat(), pickup.getLog())).title("Path").icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_current_long));
        place2 = new MarkerOptions().position(new LatLng(drop.getLat(), drop.getLog())).title("f").icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_destination_long));
        new FetchURL(BookWheelerActivity.this).execute(getUrl(place1.getPosition(), place2.getPosition(), "driving"), "driving");

        mMap.addMarker(place1);
        mMap.addMarker(place2);


        LatLng coordinate = new LatLng(pickup.getLat(), pickup.getLog());
        CameraUpdate yourLocation = CameraUpdateFactory.newLatLngZoom(coordinate, 12);
        mMap.animateCamera(yourLocation);




    }

    private String getUrl(LatLng origin, LatLng dest, String directionMode) {
        // Origin of route
        String strOrigin = "origin=" + origin.latitude + "," + origin.longitude;
        // Destination of route
        String strDest = "destination=" + dest.latitude + "," + dest.longitude;
        // Mode
        String mode = "mode=" + directionMode;
        // Building the parameters to the web service
        String parameters = strOrigin + "&" + strDest + "&" + mode;
        // Output format
        String output = "json";
        // Building the url to the web service
        String url = "https://maps.googleapis.com/maps/api/directions/" + output + "?" + parameters + "&key=" + getString(R.string.API_KEY);
        return url;
    }

    private static double distance(double lat1, double lon1, double lat2, double lon2) {
        if ((lat1 == lat2) && (lon1 == lon2)) {
            return 0;
        } else {
            double theta = lon1 - lon2;
            double dist = Math.sin(Math.toRadians(lat1)) * Math.sin(Math.toRadians(lat2)) + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.cos(Math.toRadians(theta));
            dist = Math.acos(dist);
            dist = Math.toDegrees(dist);
            dist = dist * 60 * 1.1515;
            dist = dist * 2.609344;
            dist = (int) Math.round(dist);

            return (dist);
        }
    }

    public static boolean getDistanceDetails(String url, int timeout) {
        try{
            URL myUrl = new URL(url);
            URLConnection connection = myUrl.openConnection();
            connection.setConnectTimeout(timeout);
            connection.getContent();
            Log.d("KINGSN", "getDistanceDetails: "+connection.getContent());
            return true;
        } catch (Exception e) {
            // Handle your exceptions
            return false;
        }
    }



}