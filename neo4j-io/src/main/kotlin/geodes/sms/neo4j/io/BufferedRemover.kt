package geodes.sms.neo4j.io

import org.neo4j.driver.Query
import org.neo4j.driver.Session
import org.neo4j.driver.internal.value.IntegerValue
import org.neo4j.driver.internal.value.ListValue
import org.neo4j.driver.internal.value.MapValue
import org.neo4j.driver.internal.value.StringValue
import java.util.*


class BufferedRemover(val nodesBatchSize: Int = 20000, val refsBatchSize: Int = 50000) {

    /** endNode ID --> input ref type */
    private val nodesToRemove = hashSetOf<Long>()
    private val nodesToRemoveByHost = LinkedList<PathMatchParameter>()
    private val refsToRemoveByID = hashSetOf<Long>()
    private val refsToRemoveByHostNodes = hashSetOf<ReferenceMatchParameter>()

    fun removeNode(id: Long) {
        nodesToRemove.add(id)
    }

    fun removeChild(startID: Long, rType: String, endID: Long) {
        nodesToRemoveByHost.add(PathMatchParameter(startID, rType, endID))
    }

    fun removeRelationship(id: Long) {
        refsToRemoveByID.add(id)
    }

    fun removeRelationship(startID: Long, rType: String, endID: Long) {
        refsToRemoveByHostNodes.add(ReferenceMatchParameter(startID, rType, endID))
    }

//    fun popNodeRemove(id: Long) {
//        nodesToRemove.remove(id)
//    }
//
//    fun popRelationshipRemove(id: Long) {
//        refsToRemoveByID.remove(id)
//    }

    fun commitContainmentsRemove(session: Session, mapFunction: (Sequence<Long>) -> Unit) {
        val paramsIterator = nodesToRemoveByHost.asSequence().map { MapValue(mapOf(
            "startID" to IntegerValue(it.startID),
            "endID" to IntegerValue(it.endID),
            "rType" to StringValue(it.rType)))
        }.iterator()

        fun commit(batchSize: Int) {
            session.writeTransaction { tx ->
                val res = tx.run(Query("UNWIND \$batch AS row" +
                        " MATCH (start)-[r{containment:true}]->(end)" +
                        " WHERE ID(start)=row.startID AND type(r)=row.rType AND ID(end)=row.endID" +
                        " WITH end LIMIT 1" +
                        " MATCH (end)-[*0..{containment:true}]->(d)" +
                        " WITH d, ID(d) AS removedIDs" +
                        " DETACH DELETE d" +
                        " RETURN removedIDs",
                    MapValue(mapOf("batch" to ListValue(*Array(batchSize) { paramsIterator.next() })))
                ))
                mapFunction(Sequence { res }.map { it["removedIDs"].asLong() })
            }
        }

        for (i in 1..(nodesToRemoveByHost.size / nodesBatchSize)) {
            commit(nodesBatchSize)
        }
        val rem = nodesToRemoveByHost.size % nodesBatchSize
        if (rem > 0) commit(rem)
        nodesToRemoveByHost.clear()
    }

    // https://community.neo4j.com/t/parallel-deletes-with-apoc-periodic-iterate/6276
    fun commitRelationshipsRemoveByHost(session: Session) {
        val paramsIterator = refsToRemoveByHostNodes.asSequence().map { MapValue(mapOf(
            "startID" to IntegerValue(it.startID),
            "endID" to IntegerValue(it.endID),
            "rType" to StringValue(it.rType),
            "limit" to IntegerValue(it.limit)))
        }.iterator()

        fun commit(batchSize: Int) {
            session.writeTransaction { tx ->
                tx.run(Query("UNWIND \$batch as row" +
                        " MATCH (start) WHERE ID(start)=row.startID" +
                        " CALL apoc.cypher.doIt('" +
                        "  MATCH (start)-[r]->(end)" +
                        "  WHERE type(r)=rType AND ID(end)=endID" +
                        "  WITH r LIMIT \$l DELETE r'," +
                        " {start:start,rType:row.rType,endID:row.endID,l:row.limit}) YIELD value" +
                        " RETURN value", //return nothing
                    MapValue(mapOf("batch" to ListValue(*Array(batchSize) { paramsIterator.next() } )))
                ))
            }
        }

        for (i in 1..(refsToRemoveByHostNodes.size / refsBatchSize)) {
            commit(refsBatchSize)
        }
        val rem = refsToRemoveByHostNodes.size % refsBatchSize
        if (rem > 0) commit(rem)
        refsToRemoveByHostNodes.clear()
    }

    fun commitNodesRemoveByID() {

    }

    fun commitRelationshipsRemoveByID(session: Session): Sequence<Long> {
        if (refsToRemoveByID.isEmpty()) return emptySequence()

        val batchData = refsToRemoveByID.map { IntegerValue(it) }
        return session.writeTransaction { tx ->
            val res = tx.run(Query("UNWIND \$batch AS id" +
                    " MATCH ()-[r]->()" +
                    " WHERE ID(r)=id" +
                    " WITH ID(r) AS removedIDs" +
                    " DELETE d" +
                    " RETURN removedIDs",
                MapValue(mapOf("batch" to ListValue(*Array(batchData.size) {i -> batchData[i]} )))
            ))
            Sequence { res }.map { it["removedIDs"].asLong() }
        }
    }

    fun removeAll(session: Session) {
        session.writeTransaction { tx ->
            tx.run(Query("call apoc.periodic.iterate(" +
                    "\"MATCH (n) return n\", \"DETACH DELETE n\"," +
                    " {batchSize:10000, parallel:true})" +
                    " YIELD batches, total return batches, total"))
        }
    }
}