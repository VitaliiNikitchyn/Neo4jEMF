package geodes.sms.nmf.loader.emf2neo4j

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import geodes.sms.nmf.neo4j.io.GraphBatchWriter
import org.eclipse.emf.common.util.URI
import org.eclipse.emf.ecore.resource.Resource
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl
import java.io.File
import kotlin.Exception


fun main(args: Array<String>) {

    try {
        val resource = getResource(args[0])
        println("Loading model: ${args[0]}")
        //val graph = Neo4jGraph.create(jacksonObjectMapper().readValue(File("Neo4jDBCredentials.json")))
        //val loader = ReflectiveLoader(graph)

        val graphWriter = GraphBatchWriter(jacksonObjectMapper().readValue(File("Neo4jDBCredentials.json")))
        val loader = ReflectiveBatchLoader(resource, graphWriter)
        loader.load()

        graphWriter.close()
        println("Loaded successfully")
    } catch (e: Exception) {
        println("Loading fail")
        e.printStackTrace()
    }

    /* Load multiple files
    GraphDatabase.driver(dbUri, AuthTokens.basic(username, password)).use { driver ->
        File(args[0]).walk()
            .filter { file -> file.extension in listOf("ecore", "xmi") }
            .forEach { file ->
                Neo4jBufferedWriter(driver).use { dbWriter ->
                    EmfModelLoader.createFromContent(file.path)
                        .load(dbWriter)
                    println("model loaded: ${file.name}")
                }
            }
    }
    println("Loading finished")*/
}

fun getResource(modelPath: String) : Resource {
    Resource.Factory.Registry.INSTANCE.extensionToFactoryMap.apply {
        put("ecore", EcoreResourceFactoryImpl())
        put("xmi", XMIResourceFactoryImpl())
    }

    //val adapter = ECrossReferenceAdapter()
    //val resourceSet = ResourceSetImpl()
    //resourceSet.eAdapters().add(adapter)

    // createFileURI method is able to locate metamodel by xsi:schemaLocation
    // absolute path is important here !!
    return ResourceSetImpl().getResource(URI.createFileURI(File(modelPath).absolutePath), true)
}