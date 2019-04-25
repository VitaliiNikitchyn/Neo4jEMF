package  geodes.sms.modeleditor.latexmetamodel.neo4jImpl

import geodes.sms.modeleditor.latexmetamodel.*
import geodes.sms.neo4jecore.Neo4jEObject
import geodes.sms.neo4jecore.Neo4jEObjectImpl
import org.neo4j.driver.v1.Session
import org.neo4j.driver.v1.Values

class TextContainerNeo4jImpl(override val dbSession: Session, override val id: Int, override val label: String) :TextContainer

, Neo4jEObject by Neo4jEObjectImpl(dbSession, id, label) {

        override fun setText(attrValue: String) : Boolean {
            return  try {
                dbSession.writeTransaction {it.run("MATCH (c:$label) WHERE ID(c)={nodeID}  SET c.text = {value}",
                Values.parameters("nodeID", this.id, "value", attrValue))}
                true
            } catch (e : Exception) {
                e.printStackTrace()
                false
            }
        }

        override fun getText() : String? {
            //return String
            return  try {
                val res = dbSession.readTransaction {it.run("MATCH (c:$label) WHERE ID(c) = {nodeID} RETURN c.text AS p",
                    Values.parameters("nodeID", this.id))}
                res.single()["p"].asString()
            } catch (e : Exception) {
                e.printStackTrace()
                null
            }
        }
        
}