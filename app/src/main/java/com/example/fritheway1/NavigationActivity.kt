package com.example.fritheway1

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.location.GnssStatus
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.fritheway1.databinding.ActivityNavigationBinding
import com.google.android.gms.maps.model.Circle
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.snackbar.Snackbar
import com.indooratlas.android.sdk.IALocation
import com.indooratlas.android.sdk.IALocationListener
import com.indooratlas.android.sdk.IALocationManager
import com.indooratlas.android.sdk.IALocationRequest
import com.indooratlas.android.sdk.IAOrientationListener
import com.indooratlas.android.sdk.IAOrientationRequest
import com.indooratlas.android.sdk.IARegion
import com.indooratlas.android.sdk.IARoute
import com.indooratlas.android.sdk.IAWayfindingListener
import com.indooratlas.android.sdk.IAWayfindingRequest
import com.indooratlas.android.sdk.resources.IAFloorPlan
import com.indooratlas.android.sdk.resources.IALocationListenerSupport
import com.indooratlas.android.sdk.resources.IAVenue
import com.squareup.picasso.Picasso
import com.squareup.picasso.RequestCreator
import com.squareup.picasso.Target
//import es.dmoral.toasty.Toasty
import org.osmdroid.api.IMapController
import org.osmdroid.bonuspack.overlays.GroundOverlay
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
//import org.osmdroid.views.overlay.GroundOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import kotlin.math.floor

class NavigationActivity : AppCompatActivity(), MapListener, LocationListener {

    private val MAX_DIMENSION = 2048

    private lateinit var mMap: MapView
    private lateinit var controller: IMapController;
    private lateinit var mMyLocationOverlay: MyLocationNewOverlay;
    private lateinit var mGnssStatusCallback: GnssStatus.Callback
    private lateinit var mLocationManager: LocationManager
    private lateinit var binding: ActivityNavigationBinding
    private var mGroundOverlay: GroundOverlay? = null
    private var mFloor = 0
    private var mDestinationMarker : Marker? = null
    private var mWayFindingDestination: IAWayfindingRequest? = null
    private var mCurrentRoute: IARoute? = null
    private var mHeadingMarker: Marker? = null
    private var mPOIMarkers: MutableList<Marker> = mutableListOf()
    private var mPolyLines: MutableList<Polyline> = mutableListOf()
    private var mCameraPositionNeedUpdating = false
    private var mCircle : Circle? = null
    private var mOverLayFloorPlan: IARegion? = null
    private lateinit var mVenue: IAVenue
    private lateinit var mLoadTarget: Target

    private val TAG = "Navigation"
    private val venueId = "9560cf10-0f3f-11ee-a8d5-d94b27846395"
    private val permCodes = 0
    private lateinit var destinationLat: String
    private lateinit var destinationLong: String
    private lateinit var destinationFloor: String
    private lateinit var mIALocationManager: IALocationManager

    private val mRegionListener : IARegion.Listener =
        object : IARegion.Listener{
        override fun onEnterRegion(region: IARegion) {
            //fetch floorplan, set camera positions, set a groundOverlay
            if(region.type == IARegion.TYPE_FLOOR_PLAN){
                mCameraPositionNeedUpdating = true
                if(mGroundOverlay == null){
                    mMap.overlays.remove(mGroundOverlay)
                    mGroundOverlay = null
                }
                mOverLayFloorPlan = region
                fetchFloorPlanBitmap(region.floorPlan)

                if(mDestinationMarker == null){
                    setWayfindingTarget(LatLng(destinationLat.toDouble(),
                        destinationLong.toDouble()),
                        true)
                }
            }else if(region.type == IARegion.TYPE_VENUE){
                mVenue = region.venue
            }
        }
        override fun onExitRegion(region: IARegion?) {
            //maybe something should be here?
        }
    }

    private fun fetchFloorPlanBitmap(floorPlan: IAFloorPlan?) {
        if(floorPlan == null){
            Log.d(TAG,"Null FLOORPLAN IN FETCHFLOORPLAN")
            return
        }
        val url = floorPlan.url
        Log.d(TAG, "Loading floorplan from $url")

        mLoadTarget = object :  Target{
            override fun onBitmapLoaded(bitmap: Bitmap?, from: Picasso.LoadedFrom?) {
                Log.d(TAG, "Bitmap loaded")
                if(mOverLayFloorPlan != null && floorPlan.id == mOverLayFloorPlan?.id){
                    Log.d(TAG,"Showing overlay, I HOPE")
                    setupGroundOverlay(floorPlan,bitmap)
                }
            }

            override fun onBitmapFailed(e: Exception?, errorDrawable: Drawable?) {
                info("Failed to load BITMAP")
                mOverLayFloorPlan = null
            }

            override fun onPrepareLoad(placeHolderDrawable: Drawable?) {
                // Nothing here i guess
            }
        }
        val request: RequestCreator = Picasso.get().load(url)
        if(floorPlan.bitmapHeight > MAX_DIMENSION){
            request.resize(0,MAX_DIMENSION)
        }else if(floorPlan.bitmapWidth >MAX_DIMENSION){
            request.resize(MAX_DIMENSION,0)
        }
        request.into(mLoadTarget)
    }
    private fun setupGroundOverlay(floorPlan: IAFloorPlan, bitmap: Bitmap?) {
        if(mGroundOverlay != null){
            mMap.overlays.remove(mGroundOverlay)
            mGroundOverlay = null
        }
        if(this::mMap.isInitialized){

            //val bb = BoundingBox(floorPlan.topLeft.latitude, floorPlan.topRight.longitude,floorPlan.bottomLeft.latitude,floorPlan.bottomRight.longitude)

            mGroundOverlay = GroundOverlay()

            mGroundOverlay!!.image = bitmap
            mGroundOverlay!!.bearing = floorPlan.bearing

            mGroundOverlay!!.setPositionFromBounds(GeoPoint(floorPlan.topLeft.latitude,floorPlan.topLeft.longitude),// zgleda kot da je topRight
                GeoPoint(floorPlan.topRight.latitude,floorPlan.topRight.longitude), //zgleda kot bottomright
                GeoPoint(floorPlan.bottomRight.latitude,floorPlan.bottomRight.longitude),
                GeoPoint(floorPlan.bottomLeft.latitude,floorPlan.bottomLeft.longitude), //izgleda kot top left
                //GeoPoint(floorPlan.topRight.latitude,floorPlan.topRight.longitude),
                //GeoPoint(floorPlan.topRight.latitude,floorPlan.topRight.longitude),)
            )
            mMap.overlays.add(0,mGroundOverlay )
        }

    }

    /*private fun setupGroundOverlay(floorPlan: IAFloorPlan, bitmap: Bitmap?) {
        if(mGroundOverlay != null){
            mMap.overlays.remove(mGroundOverlay)
            mGroundOverlay = null
        }
        if(this::mMap.isInitialized){
            //val bitMapDescriptor = bitmap?.let {bitmapp -> BitmapDescriptorFactory.fromBitmap(bitmapp) }
            val iaLatLng = floorPlan.center
            val center = LatLng(iaLatLng.latitude,iaLatLng.longitude)
            *//*val floorPlanOverlay = bitMapDescriptor?.let {
                GroundOverlayOptions()
                    .image(it)
                    .zIndex(0.0f)
                    .position(center,floorPlan.widthMeters, floorPlan.heightMeters)
                    .bearing(floorPlan.bearing)
            }*//*
            mGroundOverlay = GroundOverlay()
            mGroundOverlay!!.setPosition(GeoPoint(floorPlan.topLeft.latitude,floorPlan.topLeft.longitude),// zgleda kot da je topRight
                //GeoPoint(floorPlan.topRight.latitude,floorPlan.topRight.longitude), zgleda kot bottomright
                GeoPoint(floorPlan.bottomRight.latitude,floorPlan.bottomRight.longitude),
                //GeoPoint(floorPlan.bottomLeft.latitude,floorPlan.bottomLeft.longitude), //izgleda kot top left
                //GeoPoint(floorPlan.topRight.latitude,floorPlan.topRight.longitude),
                //GeoPoint(floorPlan.topRight.latitude,floorPlan.topRight.longitude),
            )



            Log.e(TAG,GeoPoint(floorPlan.bottomLeft.latitude,floorPlan.bottomLeft.longitude).toString())

            //mGroundOverlay!!.bearing = floorPlan.bearing
            mGroundOverlay!!.image = Bitmap.createScaledBitmap(bitmap!!,floorPlan.bitmapWidth, floorPlan.bitmapHeight, false)
            //bitmap?.let { mGroundOverlay!!.image = Bitmap.createScaledBitmap(it.flipHorizontally(), floorPlan.bitmapWidth, floorPlan.bitmapHeight, false).rotate(-floorPlan.bearing)}
            mMap.overlays.add(0,mGroundOverlay )

            //here its wrapped in a let call so watch out for errors
        }
    }*/
    fun Bitmap.flipVertically(floorPlan: IAFloorPlan?): Bitmap {
        val matrix = Matrix().apply { postScale(1f, -1f, width / 1f, height / 1f) }
        return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
    }
    private fun Bitmap.flipHorizontally(): Bitmap {
        val matrix = Matrix().apply { postScale(-1f, 1f, width / 1f, height / 1f) }
        return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
    }
    private fun Bitmap.rotate(degrees: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degrees)
        return Bitmap.createBitmap(
            this, 0, 0, this.width, this.height, matrix, true
        )
    }

    //this should be done
    //camera positioning?
    private val mIALocationListener : IALocationListener = object : IALocationListenerSupport() {
        override fun onLocationChanged(location: IALocation) {
            super.onLocationChanged(location)
            Log.d(TAG, "New Location: " + location.latitude + " | " + location.longitude)
            if(!this@NavigationActivity::mMap.isInitialized){
                return
            }
            val center = LatLng(location.latitude,location.longitude)

            val newFloor = location.floorLevel
            if(mFloor != newFloor){
                //updateRouteVisualization
                updateRouteVisualization()
            }
            mFloor = newFloor

            showLocationCircle(center,location.accuracy.toDouble())

            if(mCameraPositionNeedUpdating){
                runOnUiThread {

                    Log.e(TAG,"${center.latitude}, ${center.longitude}")

                    controller.zoomTo(20.0)

                    controller.setCenter(GeoPoint(center.latitude,center.longitude))
                    controller.animateTo(GeoPoint(center.latitude,center.longitude))

                }
                mCameraPositionNeedUpdating = false

            }
        }

    }
//should be done
    private val mOrientationListener: IAOrientationListener = object : IAOrientationListener {
        override fun onHeadingChanged(p0: Long, p1: Double) {
            updateHeading(p1)
        }

        override fun onOrientationChange(p0: Long, p1: DoubleArray?) {
            //no need for full device orientation here
        }

    }

    private fun updateHeading(heading: Double) {

        if(mHeadingMarker != null){
            mHeadingMarker?.rotation = heading.toFloat()
        }

    }

    private val mWayfindingListener = IAWayfindingListener { route ->
        mCurrentRoute = route
        if (hasArrivedToDestination(route)) {
            // stop wayfinding

            mCurrentRoute = null
            mWayFindingDestination = null
            mIALocationManager.removeWayfindingUpdates()

            //Toasty.success(this, "Congrats, you got to your destination!", Toast.LENGTH_SHORT).show();
            info("Congrats, you got to your destination!")
            val handler = Handler()
            handler.postDelayed({ finish() }, 3000)

            //pop activity/go back and show message

        }
        updateRouteVisualization()
    }

    private fun hasArrivedToDestination(route: IARoute): Boolean {
        if(route.legs.size == 0){
            return false
        }

        val threshold  = 8.0
        var routeLength = 0.0
        for(leg in route.legs) routeLength += leg.length

        return routeLength < threshold
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNavigationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val extras = intent.extras!!

        destinationLat = extras.getString(RoomActivity.EXTRA_LAT).toString()
        destinationLong = extras.getString(RoomActivity.EXTRA_LONG).toString()
        destinationFloor = extras.getString(RoomActivity.EXTRA_FLOOR).toString()



        Configuration.getInstance().load(
            applicationContext,
            getSharedPreferences(getString(R.string.app_name), MODE_PRIVATE)
        )
        mMap = binding.osmmap
        mMap.setTileSource(TileSourceFactory.MAPNIK)
        mMap.mapCenter
        mMap.setMultiTouchControls(true)
        mMap.getLocalVisibleRect(Rect())

        mMyLocationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(this),mMap)
        controller = mMap.controller
        mMyLocationOverlay.disableMyLocation()
        mMap.setOnClickListener{

        }
        //mMyLocationOverlay.enableMyLocation()

        //mMyLocationOverlay.isDrawAccuracyEnabled= true
        mMyLocationOverlay.runOnFirstFix{

            //sets the destination marker as soon as the map loads

            runOnUiThread {
                controller.setCenter(mMyLocationOverlay.myLocation)
                controller.animateTo(mMyLocationOverlay.myLocation)
            }
        }
        controller.zoomTo(17.0)

        //Log.e("TAG", "onCreate:in ${controller.zoomIn()}")
        //og.e("TAG", "onCreate: out  ${controller.zoomOut()}")
        mMap.zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
        mMap.overlays.add(mMyLocationOverlay)

        mMap.addMapListener(this)

        mLocationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        mGnssStatusCallback = object : GnssStatus.Callback() {

        }

        mIALocationManager = IALocationManager.create(this)
    }

    override fun onResume() {
        super.onResume()
        mIALocationManager.requestLocationUpdates(IALocationRequest.create(),mIALocationListener)
        mIALocationManager.registerRegionListener(mRegionListener)

        mIALocationManager.registerOrientationListener(
            IAOrientationRequest(1.0, 0.0),mOrientationListener
        )

        if (mWayFindingDestination != null) {
            mIALocationManager.requestWayfindingUpdates(mWayFindingDestination!!, mWayfindingListener)
        }
    }

    override fun onPause() {
        //mLocationManager.removeUpdates(this);
        //mLocationManager.unregisterGnssStatusCallback(
        //    mGnssStatusCallback
        //)

        mIALocationManager.removeLocationUpdates(mIALocationListener)
        mIALocationManager.unregisterRegionListener(mRegionListener)
        mIALocationManager.unregisterOrientationListener(mOrientationListener)
        super.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mIALocationManager.destroy()
    }

    override fun onScroll(event: ScrollEvent?): Boolean {
        Log.e("TAG", "onCreate:la ${event?.source?.mapCenter?.latitude}")
        Log.e("TAG", "onCreate:lo ${event?.source?.mapCenter?.longitude}")
        //  Log.e("TAG", "onScroll   x: ${event?.x}  y: ${event?.y}", )
        return true
    }

    override fun onZoom(event: ZoomEvent?): Boolean {
        Log.e("TAG", "onZoom zoom level: ${event?.zoomLevel}   source:  ${event?.source}")
        return false;
    }

    override fun onLocationChanged(p0: Location) {
        Log.e("TAG", p0.toString())
    }

    private fun info(text:String){
        val snack: Snackbar =
            Snackbar.make(binding.root,text, Snackbar.LENGTH_INDEFINITE).setBackgroundTint(resources.getColor(R.color.yellow_ish))
        snack.setAction("Close", View.OnClickListener {
            snack.dismiss()
        }).show()
    }

    //I hope its done
    private fun updateRouteVisualization() {
        clearRouteVisualization()

        if(mCurrentRoute == null){
            return
        }
        //mutable property that could be null
        else {
            for (leg in mCurrentRoute?.legs!!){
                if(leg.edgeIndex == null){
                    //this meant this is either the first or the last leg of the route
                    continue
                }

                //val options: PolylineOptions = PolylineOptions()
                //    .add(LatLng(leg.begin.latitude,leg.begin.longitude))
                //    .add(LatLng(leg.end.latitude,leg.end.longitude))

                val mPolyline = Polyline()
                mPolyline.setPoints(mutableListOf(GeoPoint(
                    leg.begin.latitude,leg.begin.longitude),
                    GeoPoint(leg.end.latitude,leg.end.longitude)
                ))

                if(leg.begin.floor == mFloor && leg.end.floor  == mFloor){
                    //options.color(getColor(R.color.black))
                    mPolyline.color= getColor(R.color.blue_black)
                }else{
                    //options.color(getColor(R.color.white))
                    mPolyline.color= getColor(R.color.yellow_ish)
                }

                mPolyLines.add(mPolyline)
                mMap.overlays.add(mPolyline)
                //add the polylines to the map
                //mPolyLines.add(mMap.addPolyline(options))
            }
        }
    }


    //No accuracy cirlce around the marker :(, otherwise should work

    private fun showLocationCircle(center: LatLng, accuracy: Double) {
        if(mHeadingMarker == null){
            if(this::mMap.isInitialized){
                //accuracy circle

                /*
                mCircle = mMap.addCircle(CircleOptions()
                    .center(center)
                    .radius(accuracy)
                    .fillColor(0x201681fb)
                    .strokeColor(0x500a78dd)
                    .zIndex(1.0f)
                    .visible(true)
                    .strokeWidth(5.0f))*/
                runOnUiThread{
                    mHeadingMarker = Marker(mMap)
                    mHeadingMarker!!.position = GeoPoint(center.latitude,center.longitude)
                    mHeadingMarker!!.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    mHeadingMarker!!.icon = getDrawable(R.drawable.map_blue_dot)
                    mHeadingMarker!!.setAnchor(0.5f,0.5f)
                    mHeadingMarker!!.isFlat = true
                    mMap.overlays.add(mHeadingMarker)
                }

            }
        }else{
            //mCircle?.center = center
            runOnUiThread {
                mHeadingMarker?.position = GeoPoint(center.latitude,center.longitude)
            }
            //mCircle?.radius = accuracy
        }
    }

    private fun clearRouteVisualization(){
        for(polyLine in mPolyLines){
            mMap.overlays.remove(polyLine)
        }
        mPolyLines.clear()
    }

    private fun setWayfindingTarget(latLng: LatLng, addMarker: Boolean){
        if(!this::mMap.isInitialized){
            Log.d(TAG,"Map not loaded yet")
            return
        }
        mWayFindingDestination = IAWayfindingRequest.Builder()
            .withLatitude(destinationLat.toDouble())
            .withLongitude(destinationLong.toDouble())
            .withFloor(destinationFloor.toInt())
            .build()

        //maybe error here because of !!
        mIALocationManager.requestWayfindingUpdates(mWayFindingDestination!!, mWayfindingListener)
        if(mDestinationMarker != null){
            mDestinationMarker?.remove(mMap)
            mMap.overlays.remove(mDestinationMarker)
            mDestinationMarker = null
        }
        if(addMarker){

            mDestinationMarker = Marker(mMap)
            mDestinationMarker!!.position = GeoPoint(latLng.latitude,latLng.longitude)
            mDestinationMarker!!.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            mDestinationMarker!!.icon = getDrawable(org.osmdroid.bonuspack.R.drawable.marker_cluster)
            mDestinationMarker!!.setAnchor(0.5f,0.5f)
            mDestinationMarker!!.isFlat = true
            mMap.overlays.add(mDestinationMarker)

            /*mDestinationMarker = mMap.addMarker(MarkerOptions()
                .position(latLng)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)))*/
        }
        Log.d(TAG, "Destination: " + mWayFindingDestination?.latitude + " | " +
                mWayFindingDestination?.longitude +
                ", FLoor" + mWayFindingDestination?.floor)
    }
}