package geodes.sms.neo4j.io

import geodes.sms.neo4j.io.entity.INodeEntity
import org.neo4j.driver.Value

internal class NodeParameter(val alias: Long, val label: String, val props: Map<String, Value>)

internal class ReferenceParameter(
    val alias: Long,
    val type: String,
    val startNode: INodeEntity,
    val endNode: INodeEntity,
    val props: Map<String, Value>
)

internal class PathMatchParameter(
    val startID: Long,
    val rType: String,
    val endID: Long
) {
    override fun equals(other: Any?): Boolean {
        return other is PathMatchParameter && other.startID == startID && other.rType == rType && other.endID == endID
    }

    override fun hashCode(): Int {
        return startID.hashCode() + rType.hashCode() + endID.hashCode()
    }
}

internal class ReferenceMatchParameter(
    val startID: Long,
    val rType: String,
    val endID: Long,
    var limit: Long = 1
) {
    override fun equals(other: Any?): Boolean {
        return if (other is ReferenceMatchParameter && other.startID == startID
            && other.endID == endID && other.rType == rType
        ) {
            other.limit++
            true
        } else false
    }

    override fun hashCode(): Int {
        return startID.hashCode() + rType.hashCode() + endID.hashCode()
    }
}

data class NodeResult(
    val id: Long,
    val label: String,
    //val props: MutableMap<String, Any>,
    val outRefCount: HashMap<String, Int> = hashMapOf() //output ref type count
)