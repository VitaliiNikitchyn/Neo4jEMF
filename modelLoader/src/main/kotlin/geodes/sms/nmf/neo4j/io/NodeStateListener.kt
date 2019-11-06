package geodes.sms.nmf.neo4j.io

import org.neo4j.driver.Value

interface NodeStateListener {
    fun onMatch(node: Node)
    fun onUpdate(node: Node, props: Map<String, Value>)
    //fun onRemove(node: Node)
}