package  com.example.fritheway1

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.fritheway1.R
import com.example.fritheway1.databinding.ActivityNavBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.Circle
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.GroundOverlay
import com.google.android.gms.maps.model.GroundOverlayOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.material.snackbar.Snackbar
import com.indooratlas.android.sdk.IALocation
import com.indooratlas.android.sdk.IALocationListener
import com.indooratlas.android.sdk.IALocationManager
import com.indooratlas.android.sdk.IALocationRequest
import com.indooratlas.android.sdk.IAOrientationListener
import com.indooratlas.android.sdk.IAOrientationRequest
import com.indooratlas.android.sdk.IAPOI
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


class NavActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMapClickListener {
    private val MAX_DIMENSION = 2048
    private val TAG = "Wayfinding"
    private lateinit var binding: ActivityNavBinding
    private val permCodes = 0
    private lateinit var mIALocationManager: IALocationManager
    private lateinit var mMap: GoogleMap
    private var mDestinationMarker : Marker? = null
    private var mWayFindingDestination: IAWayfindingRequest? = null
    private var mCurrentRoute: IARoute? = null
    private var mHeadingMarker: Marker? = null
    private var mGroundOverlay: GroundOverlay? = null
    private var mFloor = 0
    private var mPOIMarkers: MutableList<Marker> = mutableListOf()
    private var mPolyLines: MutableList<Polyline> = mutableListOf()
    private var mCameraPositionNeedUpdating = false
    private var mCircle : Circle? = null
    private var mOverLayFloorPlan: IARegion? = null
    private lateinit var mVenue:IAVenue
    private lateinit var mLoadTarget:Target


    private val mWayfindingListener = IAWayfindingListener { route ->
        mCurrentRoute = route
        if (hasArrivedToDestination(route)) {
            // stop wayfinding
            info("You're there!")
            mCurrentRoute = null
            mWayFindingDestination = null
            mIALocationManager.removeWayfindingUpdates()
        }
        updateRouteVisualization()
    }

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

    private val mIALocationListener : IALocationListener = object : IALocationListenerSupport() {
        override fun onLocationChanged(location: IALocation) {
            super.onLocationChanged(location)
            Log.d(TAG, "New Location: " + location.latitude + " | " + location.longitude)

            if(!this@NavActivity::mMap.isInitialized){
                return
            }

            val center = LatLng(location.latitude,location.longitude)

            val newFloor = location.floorLevel
            if(mFloor != newFloor){
                updateRouteVisualization()
            }
            mFloor = newFloor

            showLocationCircle(center,location.accuracy.toDouble())

            if(mCameraPositionNeedUpdating){
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(center,17.5f))
                mCameraPositionNeedUpdating = false
            }
        }

    }

    private fun showLocationCircle(center: LatLng, accuracy: Double) {
        if(mCircle == null){
            if(this::mMap.isInitialized){
                mCircle = mMap.addCircle(CircleOptions()
                    .center(center)
                    .radius(accuracy)
                    .fillColor(0x201681fb)
                    .strokeColor(0x500a78dd)
                    .zIndex(1.0f)
                    .visible(true)
                    .strokeWidth(5.0f))
                mHeadingMarker = mMap.addMarker(MarkerOptions()
                    .position(center)
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.map_blue_dot))
                    .anchor(0.5f,0.5f)
                    .flat(true))
            }
        }else{
            mCircle?.center = center
            mHeadingMarker?.position = center
            mCircle?.radius = accuracy
        }
    }

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
                val options: PolylineOptions = PolylineOptions()
                    .add(LatLng(leg.begin.latitude,leg.begin.longitude))
                    .add(LatLng(leg.end.latitude,leg.end.longitude))

                if(leg.begin.floor == mFloor && leg.end.floor  == mFloor){
                    options.color(getColor(R.color.black))
                }else{
                    options.color(getColor(R.color.white))
                }
                mPolyLines.add(mMap.addPolyline(options))
            }
        }
    }

    private fun clearRouteVisualization(){
        for(polyLine in mPolyLines){
            polyLine.remove()
        }
        mPolyLines.clear()
    }

    private fun hasArrivedToDestination(route: IARoute): Boolean {
        if(route.legs.size == 0){
            return false
        }

        val threshold  = 8.0
        var routeLength : Double = 0.0
        for(leg in route.legs) routeLength += leg.length

        return routeLength < threshold
    }

    private val mRegionListener : IARegion.Listener = object : IARegion.Listener{
        override fun onEnterRegion(region: IARegion) {
            if(region.type == IARegion.TYPE_FLOOR_PLAN){
                Log.d(TAG,"Entered Floor plan: " + region.id)

                mCameraPositionNeedUpdating = true;
                if(mGroundOverlay != null){
                    mGroundOverlay?.remove()
                    mGroundOverlay = null
                }
                mOverLayFloorPlan = region //this is now ?
                fetchFloorPlanBitmap(region.floorPlan)
                setupPOIs(mVenue.poIs, region.floorPlan.floorLevel)
            }else if( region.type == IARegion.TYPE_VENUE){
                mVenue = region.venue
            }
        }

        override fun onExitRegion(region: IARegion?) {
            //maybe something should be here?
        }

    }

    private fun setupPOIs(pois: List<IAPOI>, floorLevel: Int) {
        Log.d(TAG,pois.size.toString() + "POIS")
        for(m in mPOIMarkers){
            m.remove()
        }
        mPOIMarkers.clear()
        for(poi in pois){
            if(poi.floor == floorLevel){
                mPOIMarkers.add(mMap.addMarker(MarkerOptions()
                    .title(poi.name)
                    .position(LatLng(poi.location.latitude,poi.location.longitude))
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))) as Marker)
            }
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
            mGroundOverlay?.remove()
        }
        if(this::mMap.isInitialized){
            val bitMapDescriptor = bitmap?.let {bitmapp -> BitmapDescriptorFactory.fromBitmap(bitmapp) }
            val iaLatLng = floorPlan.center
            val center = LatLng(iaLatLng.latitude,iaLatLng.longitude)
            val floorPlanOverlay = bitMapDescriptor?.let {
                GroundOverlayOptions()
                    .image(it)
                    .zIndex(0.0f)
                    .position(center,floorPlan.widthMeters, floorPlan.heightMeters)
                    .bearing(floorPlan.bearing)
            }
            mGroundOverlay = floorPlanOverlay?.let { mMap.addGroundOverlay(it) }
            //here its wrapped in a let call so watch out for errors
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNavBinding.inflate(layoutInflater)
        setContentView(binding.root)


        val neededPermissions = arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_SCAN,
        )

        ActivityCompat.requestPermissions(this, neededPermissions, permCodes)

        mIALocationManager = IALocationManager.create(this)

        (supportFragmentManager
            .findFragmentById(R.id.map2) as SupportMapFragment)
            .getMapAsync(this)


        //this should be everything for onCreate
    }

    override fun onDestroy() {
        super.onDestroy()
        mIALocationManager.destroy()
    }

    override fun onPause() {
        super.onPause()
        //still need to add unregister for listeners
        mIALocationManager.removeLocationUpdates(mIALocationListener)
        mIALocationManager.unregisterRegionListener(mRegionListener)
        mIALocationManager.unregisterOrientationListener(mOrientationListener)

    }

    override fun onResume() {
        super.onResume()
        mIALocationManager.requestLocationUpdates(IALocationRequest.create(), mIALocationListener)
        mIALocationManager.registerRegionListener(mRegionListener)
        mIALocationManager.registerOrientationListener(
            IAOrientationRequest(1.0, 0.0),mOrientationListener
        )

        if (mWayFindingDestination != null) {
            mIALocationManager.requestWayfindingUpdates(mWayFindingDestination!!, mWayfindingListener)
        }
    }

    //from OnMapReadyCallback
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        /*if (!ListExamplesActivity.checkLocationPermissions(this)) {
            finish(); // Handle permission asking in ListExamplesActivity
            return;
        }*/
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            val neededPermissions = arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_SCAN,
            )

            ActivityCompat.requestPermissions(this, neededPermissions, permCodes)
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        Log.d(TAG,"reached onMapReady permissions")
        mMap.isMyLocationEnabled = false
        mMap.setOnMapClickListener(this)
        mMap.uiSettings.isMapToolbarEnabled = false
        mMap.setOnMarkerClickListener(OnMarkerClickListener { marker ->
            if (marker == mDestinationMarker) return@OnMarkerClickListener false

            setWayfindingTarget(marker.position, false)
            false
        })


        //there should be something about permissions here, but i think they were handled already
    }

    //from GoogleMap.OnMApClickListener
    override fun onMapClick(latLng: LatLng) {
        if(mPOIMarkers.isEmpty()){
            setWayfindingTarget(latLng,true)
        }

    }


    private fun setWayfindingTarget(latLng: LatLng, addMarker: Boolean){
        if(!this::mMap.isInitialized){
            Log.d(TAG,"Map not loaded yet")
            return
        }
        mWayFindingDestination = IAWayfindingRequest.Builder()
            .withLatitude(latLng.latitude)
            .withLongitude(latLng.longitude)
            .withFloor(mFloor)
            .build()

        //maybe error here because of !!
        mIALocationManager.requestWayfindingUpdates(mWayFindingDestination!!, mWayfindingListener)
        if(mDestinationMarker != null){
            mDestinationMarker?.remove()
            mDestinationMarker = null
        }
        if(addMarker){
            mDestinationMarker = mMap.addMarker(MarkerOptions()
                .position(latLng)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)))
        }
        Log.d(TAG, "Destination: " + mWayFindingDestination?.latitude + " | " +
                mWayFindingDestination?.longitude +
                ", FLoor" + mWayFindingDestination?.floor)
    }

    private fun info(text:String){
        val snack:Snackbar =
            Snackbar.make(binding.root,text,Snackbar.LENGTH_INDEFINITE)
        snack.setAction("Close", View.OnClickListener {
            snack.dismiss()
        }).show()
    }

}