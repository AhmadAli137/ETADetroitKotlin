package com.riis.etaDetroitkotlin.database

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.room.Room
import com.riis.etaDetroitkotlin.model.Company
import com.riis.etaDetroitkotlin.model.RouteStopInfo
import com.riis.etaDetroitkotlin.model.Routes
import com.riis.etaDetroitkotlin.model.TripStops
import org.jetbrains.annotations.TestOnly

private const val DATABASE_NAME = "eta_detroit.sqlite"
private const val DATABASE_PATH = "databases/eta_detroit.sqlite"

class BusRepository private constructor(context: Context) {
    private var database: BusDatabase = Room.databaseBuilder(
        context.applicationContext,
        BusDatabase::class.java,
        DATABASE_NAME
    ).createFromAsset(DATABASE_PATH).build()

    private var busDao = database.busDao()

    fun getCompanies(): LiveData<List<Company>> = busDao.getCompanies()
    fun getRoutes(companyId: Int): LiveData<List<Routes>> = busDao.getRoutes(companyId)
    fun getStopsInfoOnRoute(routeId: Int): LiveData<List<RouteStopInfo>> =
        busDao.getStopsInfoOnRoute(routeId)

    fun getArrivalTimes(
        routeId: Int,
        directionId: Int,
        dayId: Int,
        stopId: Int
    ): LiveData<List<TripStops>> = busDao.getTripStops(routeId, directionId, dayId, stopId)

    companion object {
        private var INSTANCE: BusRepository? = null

        fun initialize(context: Context) {
            if (INSTANCE == null) {
                INSTANCE = BusRepository(context)
            }
        }

        fun get(): BusRepository {
            return INSTANCE ?: throw IllegalStateException("NOT INITIALIZED")
        }
    }

    @TestOnly
    fun setDatabase(busDatabase: BusDatabase) {
        database = busDatabase
    }

    @TestOnly
    fun setBusDao(dao: BusDao) {
        busDao = dao
    }
}