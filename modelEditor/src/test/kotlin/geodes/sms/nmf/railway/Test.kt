package geodes.sms.nmf.railway

import geodes.sms.nmf.editor.railway.Region
import geodes.sms.nmf.editor.railway.Segment
import geodes.sms.nmf.editor.railway.Switch
import geodes.sms.nmf.editor.railway.neo4jImpl.ModelManagerImpl
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class Test {
    private val dbUri = "bolt://localhost:7687"
    private val username = "neo4j"
    private val password = "admin"
    private val manager = ModelManagerImpl(dbUri, username, password)

    @Test fun stableOperation() {
        //db cointains
        
        val rc = manager.loadRailwayContainerById(0)
        val routes = rc.loadRoutes(2000)

        for (rout in routes) {
            println(rout.getActive())
            //rout.removeFollows()
        }
    }

    @Test fun init() {
        val rc = manager.createRailwayContainer()
        val region = rc.addRegions()
        val sensor1 = region.addSensors()
        val sensor2 = region.addSensors()
        val segment1 = region.addSegmentElements()
        val segment2 = region.addSegmentElements()
        segment1.setMonitoredBy(sensor1)

        manager.saveChanges()
        rc.removeRegions(region)

        manager.saveChanges()
    }

    @Test fun loadTest() {
//        val rc = manager.createRailwayContainer()
//        val region = rc.addRegions()
//        val sensor1 = region.addSensors()
//        val sensor2 = region.addSensors()
//        val segment1 = region.addSegmentElements()
//        region.addSegmentElements()
//        region.addSegmentElements()
//        region.addSegmentElements()
//        segment1.setMonitoredBy(sensor1)
//        val switch = region.addSwitchElements()
//        switch.setCurrentPosition(Position.FAILURE)
//        region.addSwitchElements()

//        manager.saveChanges()
//        manager.clearCache()

        val regionLoaded = manager.loadRegionById(63)
        val elements = regionLoaded.loadElements()
        for (element in elements) {
            when (element) {
                is Switch -> println("${element.label} ${element._id} ${element.getCurrentPosition()}")
                is Segment -> println("${element.label} ${element._id} ${element.getLength()}")
                else -> println("NOT determined")
            }
        }
    }

    @Test fun removeContainmentTest() {
        val regions = LinkedList<Region>()
        val container = manager.createRailwayContainer()
        for (i in 1..10) {
            regions.add(container.addRegions())
        }
        manager.saveChanges()

        for (i in 1..4) {
            val region = regions.pop()
            println(region._id)
            container.removeRegions(region)
        }
        manager.saveChanges()

        println("Size: ${regions.size}")
    }

    @AfterAll fun close() {
        manager.close()
    }
}