package org.example.dsl

class NodeDelegate<T : Node>(init: () -> T) {
    private var value: T? = init()

    operator fun getValue(thisRef: Any?, property: kotlin.reflect.KProperty<*>): T {
        return value ?: throw IllegalStateException("Node value has not been initialized. Property: ${property.name}")
    }

    operator fun setValue(thisRef: Any?, property: kotlin.reflect.KProperty<*>, value: T) {
        this.value = value
    }
}

// Sealed interface for nodes
sealed interface Node {
    val id: String
}

// First type of node: DataNode
class DataNode(
    override val id: String,
    val data: String
) : Node {
    override fun toString(): String = "DataNode(id='$id', data='$data')"
}

// Second type of node: ProcessNode
class ProcessNode(
    override val id: String,
    val operation: String
) : Node {
    override fun toString(): String = "ProcessNode(id='$id', operation='$operation')"
}


fun main() {
    var node by NodeDelegate { DataNode(id = "node1", data = "Sample data") }

    println("Node delegate created: $node")

    // Demonstrate with ProcessNode as well
    var processNode by NodeDelegate { ProcessNode(id = "process1", operation = "transform") }
    println("Process node: $processNode")
}
