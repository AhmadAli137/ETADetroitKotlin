package com.riis.etaDetroitkotlin

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.graphics.toColorInt
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.riis.etaDetroitkotlin.model.RouteStopInfo
import java.util.*

private const val TAG = "StopsFragment"
private const val DAY_KEY = "day_key"
private const val DIRECTIONS_KEY = "directions_key"

//This fragment is a child fragment that will be displayed within the StopsFragmentParent's ViewPager.
//It contains a recycler view that displays a list of bus stops and their associated timings

class StopsFragmentChild : Fragment() {

    //CLASS VARIABLES
    //---------------
    private lateinit var stopsRecyclerView: RecyclerView
    private lateinit var adapter: StopAdapter
    private lateinit var directionFab: FloatingActionButton
    private lateinit var noRoutesTextView: TextView
    private var stopsVisibility: HashMap<Int, Int> = hashMapOf()
    private var tripStopsPositions: HashMap<Int, Int> = hashMapOf()
    private var day = 0
    private var directions: List<Int> = mutableListOf()
    private lateinit var routeStopsInfo: List<RouteStopInfo>

    //links the fragment to a viewModel shared with MainActivity and other fragments
    private val sharedViewModel: SharedViewModel by activityViewModels()

    //CREATING AND INITIALIZING THE FRAGMENT
    //--------------------------------------
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //retrieving the arguments from the bundle associated with DAY_KEY and DIRECTIONS_KEY keys
        day = arguments?.getInt(DAY_KEY)!! //retrieves the dayId provided by the StopsParentFragment
        directions = arguments?.getIntegerArrayList(DIRECTIONS_KEY)!! //retrieves the directions list provided by the StopsParentFragment

        //sharedViewModel.direction represents the directionId that dictates the arrow direction of the directionFab FloatingActionButton.
        //It is initially set to zero and acts as the index for the directions list
        sharedViewModel.direction = directions[sharedViewModel.directionCount] //sets sharedViewModel.direction to the first directionId in the directions list

        //NOTE: In the case that there are no bus stops for the selected route, the parent fragment passes a value of zero for the dayId
        // ... and a directions list with a single value of zero. This results in day = 0 and sharedViewModel.direction = 0.
    }

    //CREATING THE FRAGMENT VIEW
    //--------------------------
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        //inflating the fragment_stops_child layout as the fragment view
        val view = inflater.inflate(R.layout.fragment_stops_child, container, false)

        //referencing layout views using their resource ids
        stopsRecyclerView = view.findViewById(R.id.stops_recycler_view)
        directionFab = view.findViewById(R.id.fab)
        noRoutesTextView = view.findViewById(R.id.NoRouteLbl)
        stopsRecyclerView.layoutManager = LinearLayoutManager(context)

        setDirectionImage() //update directionFab UI
        setUpAppBar() //setup the AppBar UI
        setHasOptionsMenu(true) //allows this fragment to be able to add its own menu options to the Main Activity's app bar

        return view
    }

    //UPDATING THE UI BASED ON NEW MODEL DATA
    //---------------------------------------
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //if there exists at least one bus stop for the selected route:
        if(day != 0 && sharedViewModel.direction != 0) {
            sharedViewModel.routeStopsInfoListLiveData.observe(
                viewLifecycleOwner,
                { routeStopsInfo ->
                    this.routeStopsInfo = routeStopsInfo
                    updateUI(routeStopsInfo.filter {
                        it.directionId == sharedViewModel.direction && it.dayId == day
                    })
                }
            )
        }else{ //if there are no bus stops for the selected route:
            stopsRecyclerView.visibility = View.GONE
            directionFab.visibility = View.GONE
            noRoutesTextView.visibility = View.VISIBLE
        }
    }

    override fun onStart() {
        super.onStart()
        directionFab.setOnClickListener {

            sharedViewModel.direction = if (sharedViewModel.directionCount + 1 < directions.size) {
                directions[++sharedViewModel.directionCount] //go to next direction in list if list hasn't been exhausted
            } else {
                sharedViewModel.directionCount =
                    0 //if list has been exhausted go back to first element
                directions[sharedViewModel.directionCount]
            }

            setDirectionImage()
            updateUI(this.routeStopsInfo.filter { it.directionId == sharedViewModel.direction && it.dayId == day })
        }
    }

    //ADDING MENU OPTIONS TO THE APP BAR PROVIDED BY MAIN ACTIVITY
    //------------------------------------------------------------
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(
            R.menu.search_menu,
            menu
        ) //search_menu.xml displays a search icon (magnifying glass) in the top right of the app bar

        //creating and configuring the search bar
        val searchIcon = menu.findItem(R.id.search_icon)
        val searchView =
            searchIcon?.actionView as SearchView //SearchView widget implements an action view for entering search queries
        searchView.imeOptions =
            EditorInfo.IME_ACTION_DONE //replaces the user's carriage return button in their on-screen keyboard
        // with a "Done" action button (may appear as a check mark)

        //handling interactions with the search bar
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            //when user clicks submit button after entering query...
            override fun onQueryTextSubmit(s: String): Boolean {
                return false //return false to let the SearchView handle the submission by launching any associated intent
            }

            //when the query text is changed by the user
            override fun onQueryTextChange(s: String): Boolean {
                //adapter?.filter?.filter(s)
                Log.d(TAG, s)
                return false
            }
        })

    }


    private fun setDirectionImage() {
        var drawable = when (sharedViewModel.direction) {
            1 -> ContextCompat.getDrawable(requireContext(), R.drawable.ic_arrow_down)
            2 -> ContextCompat.getDrawable(requireContext(), R.drawable.ic_arrow_up)
            3 -> ContextCompat.getDrawable(requireContext(), R.drawable.ic_arrow_left)
            4 -> ContextCompat.getDrawable(requireContext(), R.drawable.ic_arrow_right)
            else -> null
        }

        if (drawable != null) {
            drawable = DrawableCompat.wrap(drawable)
            DrawableCompat.setTint(
                drawable,
                ContextCompat.getColor(requireContext(), R.color.white)
            )
            directionFab.setImageDrawable(drawable)
        } else {
            directionFab.visibility = View.GONE
        }
    }

    private fun updateUI(routeStops: List<RouteStopInfo>) {
        adapter = StopAdapter(routeStops)
        stopsRecyclerView.adapter = adapter
    }

    private fun setUpAppBar() {
        (requireActivity() as AppCompatActivity).supportActionBar?.title =
            "${sharedViewModel.currentRoute?.name}"

        val appBarColor = ColorDrawable(sharedViewModel.currentCompany?.brandColor?.toColorInt()!!)
        (requireActivity() as AppCompatActivity).supportActionBar?.setBackgroundDrawable(appBarColor)
    }


    companion object {
        fun newInstance(day: Int, directions: ArrayList<Int>): StopsFragmentChild {
            val args = Bundle().apply {
                putInt(DAY_KEY, day)
                putIntegerArrayList(DIRECTIONS_KEY, directions)
            }
            return StopsFragmentChild().apply {
                arguments = args
            }
        }
    }

    private inner class StopHolder(view: View) : RecyclerView.ViewHolder(view),
        View.OnClickListener {

        private lateinit var routeStopInfoItem: RouteStopInfo
        private var allArrivalTimes: TextView = view.findViewById(R.id.all_arrival_times)

        private val stopName: TextView = view.findViewById(R.id.stop_name)
        private val currentTime: TextView = view.findViewById(R.id.current_time)
        private val arrivalTimeLabel: TextView = view.findViewById(R.id.arrival_time_label)

        private var dynamicLinearLayout =
            view.findViewById(R.id.dynamic_linear_layout) as LinearLayout

        init {
            itemView.setOnClickListener(this) //setting a click listener on each itemView
            dynamicLinearLayout.visibility = View.GONE
        }

        //binding the viewHolder's Stop object to date of another from the model layer
        fun bind(routeStopInfo: RouteStopInfo) {
            routeStopInfoItem = routeStopInfo
            stopName.text = routeStopInfoItem.name
            allArrivalTimes.text = null

            if (routeStopInfoItem.stopId !in stopsVisibility) {
                stopsVisibility[routeStopInfoItem.stopId] = View.GONE
            }
            dynamicLinearLayout.visibility = stopsVisibility[routeStopInfoItem.stopId]!!

            if (dynamicLinearLayout.visibility == View.VISIBLE) {
                setArrivalTimes()
            }

            sharedViewModel.getTripStops(routeStopInfoItem.stopId).observe(
                viewLifecycleOwner,
                { tripStop ->
                    val sortedTripStops = tripStop.sortedBy { it.arrivalTime }

                    if (sortedTripStops.size > 1) {
                        for (i in sortedTripStops.indices) {
                            val difference: Long =
                                sortedTripStops[i].arrivalTime?.time!! - Date(Calendar.getInstance().timeInMillis).time

                            if (difference > 0) {
                                val seconds = difference / 1000
                                val minutes = seconds / 60
                                currentTime.text = "(${
                                    sortedTripStops[i].arrivalTime.toString()
                                        .substring(11, 16)
                                })"

                                arrivalTimeLabel.text = "Next Stop: $minutes Minutes"
                                tripStopsPositions[routeStopInfoItem.stopId] = i
                                break
                            }

                        }
                    } else {
                        currentTime.text = ""
                        arrivalTimeLabel.text = "No Stop Times Found"
                    }
                }

            )
        }

        override fun onClick(view: View) {
            allArrivalTimes.text = null
            if (dynamicLinearLayout.visibility == View.GONE) {
                dynamicLinearLayout.visibility = View.VISIBLE
                setArrivalTimes()
            } else {
                dynamicLinearLayout.visibility = View.GONE
            }

            stopsVisibility[routeStopInfoItem.stopId] = dynamicLinearLayout.visibility
        }

        fun setArrivalTimes() {
            sharedViewModel.getTripStops(routeStopInfoItem.stopId).observe(
                viewLifecycleOwner,
                { tripStop ->
                    if (tripStop.size > 1) {
                        var tmp = ""
                        val sortedTripStops = tripStop.sortedBy { it.arrivalTime }
                        val tripStopsPosition = tripStopsPositions[routeStopInfoItem.stopId]

                        for (i in tripStopsPosition!!..(tripStopsPosition + 4)) {
                            val tmpTripStop = sortedTripStops[i % sortedTripStops.size]
                            tmp += "${
                                tmpTripStop.arrivalTime.toString().substring(11, 16)
                            }......${tmpTripStop.stopSequence}\n"
                        }
                        allArrivalTimes.text = tmp
                    } else {
                        allArrivalTimes.text = "No Stop Times Found"
                    }
                }
            )
        }

    }

    private inner class StopAdapter(var routeStopInfoList: List<RouteStopInfo>)//accepts a list of RouteStops objects from model layer
        : RecyclerView.Adapter<StopsFragmentChild.StopHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int)
                : StopsFragmentChild.StopHolder {
            val itemView = layoutInflater.inflate(R.layout.list_item_stop, parent, false)
            return StopHolder(itemView)
        }

        override fun getItemCount() = routeStopInfoList.size

        override fun onBindViewHolder(holder: StopsFragmentChild.StopHolder, position: Int) {
            val routeStop = routeStopInfoList[position]
            holder.bind(routeStop)
        }
    }
}
