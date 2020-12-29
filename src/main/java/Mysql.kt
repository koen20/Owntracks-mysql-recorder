import java.sql.*
import java.util.*
import kotlin.collections.ArrayList

class Mysql(configItem: ConfigItem) {
    companion object {
        lateinit var conn: Connection
        var stops: ArrayList<Stop> = ArrayList()
        lateinit var configItem: ConfigItem

        fun getStopsDb(): ArrayList<Stop> {
            stops.clear()
            try {
                conn.createStatement().use { stmt ->
                    stmt.executeQuery("SELECT * FROM stops").use { rs ->
                        while (rs.next()) {
                            val stop = Stop(
                                rs.getString("name"),
                                rs.getDouble("lat"),
                                rs.getDouble("lon"),
                                rs.getInt("radius"),
                                true
                            )
                            stops.add(stop)
                        }
                    }
                }
            } catch (e: SQLException) {
                e.printStackTrace()
            }
            return stops
        }
    }

    init {
        try {
            conn =
                DriverManager.getConnection(configItem.mysqlServer, configItem.mysqlUsername, configItem.mysqlPassword)
            stops = getStopsDb()
        } catch (e: SQLNonTransientConnectionException) {
            e.printStackTrace()
        }
        val updateTimer = Timer()
        updateTimer.scheduleAtFixedRate(checkMysqlConnection(), 2000, 60000)
        val updateTimerStops = Timer()
        updateTimerStops.scheduleAtFixedRate(updateTimerStops(), 120000, 21600000)
    }

    fun disconnect() {
        try {
            conn.close()
        } catch (e: Exception){

        }
    }

    fun AddStop(stop: Stop): Boolean {
        var added = false
        try {
            val insert = "INSERT INTO stops VALUES(NULL, ?, ?, ?, ?)"
            conn.prepareStatement(insert).use { ps ->
                ps.setString(1, stop.name)
                ps.setDouble(2, stop.lat)
                ps.setDouble(3, stop.lon)
                ps.setInt(4, stop.radius)
                ps.execute()
            }
            added = true
        } catch (exception: SQLException) {
            exception.printStackTrace()
        }
        return added
    }

    fun getData(startTime: Long, endTime: Long): ArrayList<LocationItem> {
        val data = ArrayList<LocationItem>()
        conn.createStatement().use { stmt ->
            stmt.executeQuery(
                "SELECT * FROM data WHERE date BETWEEN '${Mqtt.getMysqlDateString(startTime)}' AND '${
                    Mqtt.getMysqlDateString(
                        endTime
                    )
                }'"
            ).use { rs ->
                while (rs.next()) {
                    data.add(LocationItem(rs.getTimestamp("date"), rs.getDouble("lat"), rs.getDouble("lon")))
                }
            }
        }

        return data
    }

    fun updateStops() {
        stops = getStopsDb()
    }

    fun getStops(): ArrayList<Stop> {
        return stops
    }

    private class checkMysqlConnection : TimerTask() {
        override fun run() {
            try {
                if (!conn.isValid(3000)) {
                    conn.close()
                    conn = DriverManager.getConnection(
                        configItem.mysqlServer,
                        configItem.mysqlUsername, configItem.mysqlPassword
                    )
                }
            } catch (e: SQLException) {
                e.printStackTrace()
            }
        }
    }

    private class updateTimerStops : TimerTask() {
        override fun run() {
            stops = getStopsDb()
        }
    }
}