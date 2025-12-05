package org.example.dsl

import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

// Custom delegate for managing edges
class EdgeDelegate<T : Node>(private val initialEdges: MutableList<T> = mutableListOf()) :
    ReadWriteProperty<Any?, List<T>> {

    private var edges: MutableList<T> = initialEdges

    override fun getValue(thisRef: Any?, property: KProperty<*>): List<T> {
        println("Getting edges from ${property.name}: ${edges.map { it.id }}")
        return edges.toList()
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: List<T>) {
        println("Setting edges to ${property.name}: ${value.map { it.id }}")
        edges.clear()
        edges.addAll(value)
    }

    fun addEdge(node: T) {
        edges.add(node)
    }

    fun removeEdge(node: T) {
        edges.remove(node)
    }
}

// Sealed interface for nodes
sealed interface Node {
    val id: String
    val edges: List<Node>
}

// First type of node: DataNode
class DataNode(
    override val id: String,
    val data: String
) : Node {
    private val edgeDelegate = EdgeDelegate<Node>()
    override var edges: List<Node> by edgeDelegate

    fun connectTo(node: Node) {
        edgeDelegate.addEdge(node)
    }

    fun disconnect(node: Node) {
        edgeDelegate.removeEdge(node)
    }

    override fun toString(): String = "DataNode(id='$id', data='$data')"
}

// Second type of node: ProcessNode
class ProcessNode(
    override val id: String,
    val operation: String
) : Node {
    private val edgeDelegate = EdgeDelegate<Node>()
    override var edges: List<Node> by edgeDelegate

    fun connectTo(node: Node) {
        edgeDelegate.addEdge(node)
    }

    fun disconnect(node: Node) {
        edgeDelegate.removeEdge(node)
    }

    override fun toString(): String = "ProcessNode(id='$id', operation='$operation')"
}

// Node delegate provider for DataNode
class DataNodeDelegate(
    private val id: String,
    private val data: String
) : ReadOnlyProperty<Any?, DataNode> {
    private val node by lazy {
        println("Creating DataNode with id='$id'")
        DataNode(id, data)
    }

    override fun getValue(thisRef: Any?, property: KProperty<*>): DataNode {
        return node
    }
}

// Node delegate provider for ProcessNode
class ProcessNodeDelegate(
    private val id: String,
    private val operation: String
) : ReadOnlyProperty<Any?, ProcessNode> {
    private val node by lazy {
        println("Creating ProcessNode with id='$id'")
        ProcessNode(id, operation)
    }

    override fun getValue(thisRef: Any?, property: KProperty<*>): ProcessNode {
        return node
    }
}

// Helper functions to create node delegates
fun dataNode(id: String, data: String) = DataNodeDelegate(id, data)
fun processNode(id: String, operation: String) = ProcessNodeDelegate(id, operation)

// Graph class that uses the delegate
class Graph {
    private val nodesDelegate = EdgeDelegate<Node>()
    var nodes: List<Node> by nodesDelegate

    fun addNode(node: Node) {
        nodesDelegate.addEdge(node)
    }

    fun displayGraph() {
        println("\n=== Graph Structure ===")
        nodes.forEach { node ->
            when (node) {
                is DataNode -> println("DataNode[${node.id}](data='${node.data}') -> ${node.edges.map { it.id }}")
                is ProcessNode -> println("ProcessNode[${node.id}](op='${node.operation}') -> ${node.edges.map { it.id }}")
            }
        }
    }
}

fun main() {
    println("=== Creating nodes using delegates: val node by nodeDelegate ===\n")

    // Create nodes using delegate pattern: val node by nodeDelegate
    val inputNode by dataNode("input-1", "User Input Data")
    val processNode by processNode("process-1", "Transform")
    val outputNode by dataNode("output-1", "Processed Result")

    println("\n=== Creating edges ===")
    inputNode.connectTo(processNode)
    processNode.connectTo(outputNode)

    // Create graph
    val graph = Graph()
    graph.addNode(inputNode)
    graph.addNode(processNode)
    graph.addNode(outputNode)

    // Display graph
    graph.displayGraph()

    // Demonstrate delegate behavior
    println("\n=== Accessing edges ===")
    val edges = inputNode.edges
    println("Input node connected to: $edges")

    println("\n=== Accessing node again (should not recreate) ===")
    println("Input node: $inputNode")
}
