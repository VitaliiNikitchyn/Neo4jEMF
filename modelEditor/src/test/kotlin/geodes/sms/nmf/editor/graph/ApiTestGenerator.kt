package geodes.sms.nmf.editor.graph

import java.io.File
import java.util.*

/*
G[n=""]
v: V[id=1], CV[id=2], V[id=3], CV[id=4]

V1
  e: V1, CV2

CV2
  sv: V[id=5], CV[id=6], V[id=7], CV[id=8]
  dv: V5
  e:V3

V3
  e: CV4, V1

CV4
  sv: V[id=9], CV[id=10], V[id=11], CV[id=12]
  dv: V9
  e:CV4

All other attributes the same to every V and CV

Parameters:
G= # graphs
S= # combinations V,CV,V,CV equivalent to width (include e, dv, sv)
V= # Vertex in S
CV= # CompositeVertex in S
D= max depth of sv from Graph to deepest S
E= # edges, randomly choose 2 V/CV and create an edge, could be: V1->V1, V1->V2, CV1->CV1, CV1->CV2, V1->CV2, CV1->V1

example:
Generate(G=5, S=18, V=500, CV=500, D=50, E=(500+500)^2 <-max 100,000)
Produce graph
Produce statistics on graph
# nodes, # edges, max depth of graph, max width of graph, average degree of node
*/

private class ApiTestGenerator(val g: Int, val s: Int, val v: Int, val cv: Int, val d: Int, val e: Int) {
    private var id = 1  //for Vertex and CompositeVertex
    private var graphID = 1
    private val cvAliasStack = LinkedList<String>()
    private val vAlias = mutableListOf<String>()    //all vertices aliases
    private val resWriter = File("./api.txt").bufferedWriter()

    fun addVertexToGraph(g: String) {
        resWriter.write("""
            val v$id = $g.addVertex()
            v$id.addEdge(v$id)
            v$id.setId($id)
        """.trimIndent())
        resWriter.newLine()
        vAlias.add("v${id++}")
    }

    fun addVertexToCompositeVertex(root: String) {
        resWriter.write("""
            val v$id = $root.addSubVertex()
            v$id.addEdge(v$id)
            v$id.setId($id)
        """.trimIndent())
        resWriter.newLine()
        vAlias.add("v${id++}")
    }

    fun addCompositeVertex(root: String) {
        resWriter.write("""
            val cv$id = $root.addCompositeVertex()
            cv$id.addEdge(cv$id)
            cv$id.setId($id)
            //cv$id.setCapacity(88)
        """.trimIndent())
        resWriter.newLine()
        vAlias.add("cv$id")
        cvAliasStack.add("cv${id++}")
    }

    fun genEdges() {
        for (i in 1..e)
            resWriter.write("${vAlias.random()}.addEdge(${vAlias.random()})\n")
    }

    fun genSForGraph(g: String) {
        for (i in 1..v)
            addVertexToGraph(g)

        for (i in 1..cv)
            addCompositeVertex(g)
    }

    fun genSForCompositeVertex(root: String) {
        for (i in 1..v)
            addVertexToCompositeVertex(root)

        for (i in 1..cv)
            addCompositeVertex(root)
    }

    fun genGraph() {
        val root = "g${graphID++}"
        resWriter.write("val $root = manager.createGraph()\n")
        for (i in 1..s)
            genSForGraph(root)

        for (i in 1 until d) {
            for (j in 1..cvAliasStack.size) {
                val r = cvAliasStack.pop()
                for (k in 1..s)
                    genSForCompositeVertex(r)
            }
        }
    }

    fun gen() {
        for (i in 1..g) {
            genGraph()
            cvAliasStack.clear()
            genEdges()
        }
        vAlias.clear()
        resWriter.close()
    }
}

fun main() {
    // cv on each depth level = (s * cv)^d
    // v on each depth level = (s * v)^d
    // all cv and v contains 1 self referenced edge

    //ApiTestGenerator(1, 2, 1, 2, 4,0).gen() //v=170 cv=340 total=510 edge=510
    //ApiTestGenerator(1, 2, 1, 2, 4,490).gen() //v=170 cv=340 total=510 edge=1000

    ApiTestGenerator(1, 2, 1, 2, 4,0).gen() //v=170 cv=340 total=510 edge=510
}

/* query for counts check
MATCH (v:Vertex) WITH count(v) AS v
MATCH (cv:CompositeVertex) WITH v, count(cv) AS cv
MATCH ()-[e:edge]->()
RETURN v,cv, v+cv+1 AS total, count(e) AS e
 */