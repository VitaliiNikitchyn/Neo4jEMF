package geodes.sms.neo4j.io

import org.neo4j.driver.*
import org.neo4j.driver.internal.value.IntegerValue
import org.neo4j.driver.internal.value.MapValue
import org.neo4j.driver.internal.value.NullValue
import org.neo4j.driver.internal.value.StringValue
import org.neo4j.driver.types.Node
import java.util.*
import kotlin.collections.HashMap

class DBReader(val driver: Driver) {
    fun findNode(id: Long, label: String): Node {
        val session = driver.session()
        val node = session.readTransaction { tx ->
            val res = tx.run(Query("MATCH (node)" +
                    " WHERE ID(node)=\$id AND labels(node)[0]=\$label" +
                    " RETURN node",
                MapValue(mapOf("id" to IntegerValue(id), "label" to StringValue(label)))
            ))
            res.single()["node"].asNode()
        }
        session.close()
        return node
    }

    fun findNodeWithOutputsCount(id: Long, label: String): NodeResult {
        val session = driver.session()
        val nodeRes = session.readTransaction { tx ->
            val res = tx.run(Query("MATCH (node)" +
                    " WHERE ID(node)=\$id AND labels(node)[0]=\$label" +
                    " OPTIONAL MATCH (node)-[r]->()" +
                    " WITH ID(node) AS id, type(r) AS rType, count(r) AS count" +
                    " RETURN id, apoc.map.fromPairs(collect([rType, count])) AS count",
                MapValue(mapOf("id" to IntegerValue(id), "label" to StringValue(label)))
            ))
            val record = res.single()
            NodeResult(record["id"].asLong(), label,
                outRefCount = HashMap(record["count"].asMap { it.asInt() }))
        }
        session.close()
        return nodeRes
    }

    inline fun <R> findNodesByLabelWithOutputsCount(
        label: String,
        limit: Int = 100,
        crossinline mapFunction: (NodeResult) -> R
    ): List<R> {
        val params = MapValue(mapOf("limit" to IntegerValue(limit.toLong())))
        val session = driver.session()
        val data = session.readTransaction { tx ->
            val res = tx.run(Query("MATCH (node:$label)" +
                    " WITH node LIMIT \$limit" +
                    " OPTIONAL MATCH (node)-[r]->()" +
                    " WITH ID(node) AS id, labels(node)[0] AS label, type(r) AS rType, count(r) AS count" +
                    " RETURN id, label, apoc.map.fromPairs(collect([rType, count])) AS count", params)
            )
            val data = LinkedList<R>()
            for (record in res) {
                data.add(mapFunction(
                    NodeResult(
                        id = record["id"].asLong(),
                        label = record["label"].asString(),
                        outRefCount = HashMap(record["count"].asMap { it.asInt() }) )
                ))
            }
            data
        }
        session.close()
        return data
    }

    /** @return endNode plus count of output refs aggregated by count*/
    inline fun <R> findConnectedNodesWithOutputsCount(
        startID: Long, rType: String, endLabel: String?,
        filter: String = "",
        limit: Int = 100,
        crossinline mapFunction: (NodeResult) -> R
    ): List<R> {
        val params = MapValue(mapOf(
            "startID" to IntegerValue(startID),
            "labelFilter" to if (endLabel != null) StringValue("+$endLabel") else NullValue.NULL,
            "refPattern" to StringValue("$rType>"),
            "limit" to IntegerValue(limit.toLong())
        ))
        val session = driver.session()
        val data = session.readTransaction { tx ->
            val res = tx.run(Query("MATCH (start) WHERE ID(start)=\$startID" +
                    " CALL apoc.path.subgraphNodes(start, {" +
                    "  relationshipFilter: \$refPattern, labelFilter: \$labelFilter," +
                    "  minLevel:1,maxLevel:1}) YIELD node " +
                    filter +
                    " WITH node LIMIT \$limit" +
                    " OPTIONAL MATCH (node)-[r]->()" +
                    " WITH ID(node) AS id, labels(node)[0] AS label, type(r) AS rType, count(r) AS count" +
                    " RETURN id, label, apoc.map.fromPairs(collect([rType, count])) AS count", params)
            )
            val data = LinkedList<R>()
            for (record in res) {
                data.add(mapFunction(
                    NodeResult(
                        id = record["id"].asLong(),
                        label = record["label"].asString(),
                        outRefCount = HashMap(record["count"].asMap { it.asInt() }) )
                ))
            }
            data
        }
        /*
        val data = session.readTransaction { tx ->
            Sequence { res }.map { record ->
                mapFunction(NodeResult(record["id"].asLong(), record["count"].asMap { it.asInt() }))
            }.toList()

            mapFunction(Sequence { res }.map { record ->
                val nodeID = record["id"].asLong()
                val outputsCount = record["count"].asMap { it.asInt() }
                NodeResult(nodeID, outputsCount)
            })
        } */
        session.close()
        return data
    }

    fun getNodeCountWithProperty(label: String, propName: String, propValue: Value): Int {
        val session = driver.session()
        val count = session.readTransaction { tx ->
            val res = tx.run(Query("MATCH (n:$label) WHERE n[\$property]=\$value RETURN count(n) AS count",
                MapValue(mapOf("property" to StringValue(propName), "value" to propValue))
            ))
            res.single()["count"].asInt()
        }
        session.close()
        return count
    }

    fun readNodeProperty(id: Long, propName: String): Value {
        val session = driver.session()
        val propValue = session.readTransaction { tx ->
            val res = tx.run(Query("MATCH (n) WHERE ID(n)=\$id RETURN n[\$property] AS prop",
                MapValue(mapOf("id" to IntegerValue(id), "property" to StringValue(propName)))
            ))
            res.single()["prop"]
        }
        session.close()
        return propValue
    }
}