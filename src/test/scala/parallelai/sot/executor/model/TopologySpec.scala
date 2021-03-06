package parallelai.sot.executor.model

import org.scalatest.{ BeforeAndAfterAll, Matchers, WordSpec }
import parallelai.sot.executor.model.SOTMacroConfig.DAGMapping

import scala.util.Success

class TopologySpec extends WordSpec with Matchers with BeforeAndAfterAll {

  case class Vertex(label: String)

  case class Edge(from: Vertex, to: Vertex)
    extends Topology.Edge[Vertex]

  var top: Topology[Vertex, Edge] = _
  var topFromFile: Topology[String, DAGMapping] = _

  override def beforeAll(): Unit = {

    //Example topology
    // a -> b -> e
    // d ->   -> c
    //      x ->

    val vs = List(Vertex("a"), Vertex("b"), Vertex("c"), Vertex("d"), Vertex("e"), Vertex("x"), Vertex("un1"), Vertex("un2"))

    val es = List(
      Edge(Vertex("a"), Vertex("b")),
      Edge(Vertex("b"), Vertex("e")),
      Edge(Vertex("d"), Vertex("b")),
      Edge(Vertex("b"), Vertex("c")),
      Edge(Vertex("x"), Vertex("c"))
    )

    top = Topology.createTopology(vs, es)

    val config = SOTMacroJsonConfig("ps2ds-test.json")
    topFromFile = config.parseDAG()

  }

  "Topology" should {

    "check for unconnected nodes" in {

      top.unconnected should be(2)

    }

    "get source nodes" in {

      val sourceNodes = top.getSourceVertices()
      sourceNodes should be(Set(Vertex("a"), Vertex("d"), Vertex("x")))

    }

    "get sink nodes" in {

      val sinkNodes = top.getSinkVertices()
      sinkNodes should be(Set(Vertex("e"), Vertex("c")))

    }

    "check if any vertex has more than one incoming edges" in {

      val vertices = top.findWithIncomingEdges()
      vertices should be(Set(Vertex("b"), Vertex("c")))

    }

    "check if any vertex has more than one outgoing edges" in {

      val vertices = top.findWithOutgoingEdges()
      vertices should be(Set(Vertex("b")))

    }

    "parse topology from file" in {

      val expectedV = List("in", "filter", "mapper1", "sumByKey", "mapper2", "out")
      val expectedE = List(
        DAGMapping("mapper1", "sumByKey"),
        DAGMapping("in", "filter"),
        DAGMapping("sumByKey", "mapper2"),
        DAGMapping("filter", "mapper1"),
        DAGMapping("mapper2", "out")
      )
      val expectedTop = Topology.createTopology(expectedV, expectedE)

      topFromFile should be(expectedTop)

    }

    "check sinks from topology from file" in {

      val sinks = topFromFile.getSinkVertices()
      sinks should be(Set("out"))

    }

    "check sources from topology from file" in {

      val sources = topFromFile.getSourceVertices()
      sources should be(Set("in"))

    }

    "check if any vertex has more than one incoming edges from file" in {

      val incomingEdges = topFromFile.findWithIncomingEdges()
      incomingEdges should be(Set())

    }

    "check if any vertex has more than one outgoing edges from file" in {

      val outgoingEdges = topFromFile.findWithOutgoingEdges()
      outgoingEdges should be(Set())

    }

    "sort topoligically DAG" in {
      val sorted = topFromFile.topologicalSort()
      sorted._1 should ===(List("in", "filter", "mapper1", "sumByKey", "mapper2", "out"))
      sorted._2 should ===(List(("in", "filter"), ("filter", "mapper1"), ("mapper1", "sumByKey"), ("sumByKey", "mapper2"), ("mapper2", "out")))
    }

    "sort topoligically" in {
      val sorted = Topology.topologicalSort(Seq(("in1", "op1"), ("in", "op1"), ("op1", "out1"), ("op1", "out2")))
      sorted._1 should ===(Seq("in", "in1", "op1", "out1", "out2"))
      sorted._2 should ===(Seq(("in", "op1"), ("in1", "op1"), ("op1", "out1"), ("op1", "out2")))
    }

    "sort topoligically containing a cycle" in {
      assertThrows[RuntimeException] {
        Topology.topologicalSort(Seq(("in1", "op1"), ("in", "op1"), ("op1", "out1"), ("op1", "out2"), ("out2", "in1")))
      }
    }

  }

}
