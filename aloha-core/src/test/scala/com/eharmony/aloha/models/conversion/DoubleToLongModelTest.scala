package com.eharmony.aloha.models.conversion

import com.eharmony.aloha.ModelSerializationTestHelper
import com.eharmony.aloha.factory.ModelFactory
import com.eharmony.aloha.id.ModelId
import com.eharmony.aloha.interop.LongFactoryInfo
import com.eharmony.aloha.models._
import com.eharmony.aloha.score.conversions.ScoreConverter.Implicits.LongScoreConverter
import com.eharmony.aloha.score.conversions.rich.RichScore
import com.eharmony.aloha.semantics.EmptySemantics
import org.junit.Assert._
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner
import spray.json.DefaultJsonProtocol.LongJsonFormat
import spray.json.DeserializationException

import scala.language.implicitConversions
import scala.util.Try

@RunWith(classOf[BlockJUnit4ClassRunner])
class DoubleToLongModelTest extends ModelSerializationTestHelper {
    import DoubleToLongModelTest._

    @Test def testSerialization(): Unit = {
        val sub = ErrorModel(ModelId(2, "abc"), Seq("def", "ghi"))
        val m = DoubleToJavaLongModel(ModelId(3, "jkl"), sub, 2.3, 3.4, 1, 4, round = true)
        val m1 = serializeDeserializeRoundTrip(m)
        assertEquals(m, m1)
    }

    @Test def testSubmodelClosed(): Unit = {
        val sub = new CloserTesterModel[Double]()
        DoubleToLongModel(ModelId.empty, sub).close()
        assertTrue(sub.isClosed)

        val sub1 = new CloserTesterModel[Double]()
        DoubleToJavaLongModel(ModelId.empty, sub1).close()
        assertTrue(sub1.isClosed)
    }

    @Test def test_value_1_5() {
        Try {
            scalaFactory.fromString(json(value = 1.5)).get
        }.map{
            m => assertEquals(1, m(null).get)
        }.get
    }

    @Test def test_value_2_5__lower_0__upper_1() {
        val upper = 1
        Try {
            scalaFactory.fromString(json(value = 1.5, clampLower = 0, clampUpper = upper)).get
        }.map{
            m => assertEquals(upper, m(null).get)
        }.get
    }

    @Test def test_value_0_5__lower_1__upper_2() {
        val lower = 1
        Try {
            scalaFactory.fromString(json(value = 0.5, clampLower = lower, clampUpper = 2)).get
        }.map{
            m => assertEquals(lower, m(null).get)
        }.get
    }

    @Test def test_value_2__scale_7__translation_11() {
        Try {
            scalaFactory.fromString(json(value = 2, scale = 7, translation = 11)).get
        }.map{
            m => assertEquals(25, m(null).get)
        }.get
    }

    /** Test that the model actually works
      *
      */
    @Test def testHappyPath() {
        val mTry = scalaFactory.fromString(goodJson)
        val m = mTry.get
        val s = m.score(null)
        assertEquals(1l, s.relaxed.asLong.get)
        assertFalse(s.hasError)
        assertEquals(1, s.getSubScoresCount)
        assertEquals(1.00000001, s.getSubScores(0).relaxed.asDouble.get, 0)
        assertFalse(s.getSubScores(0).hasError)
        assertTrue(s.getSubScores(0).hasScore)
        assertEquals(0, s.getSubScores(0).getSubScoresCount)
    }

    @Test def testStringInnerModel() {
        // Failed should throw if 'get' succeeded.
        Try { scalaFactory.fromString(stringInnerModelType).get }.failed.foreach {
            case e: DeserializationException =>
                assertEquals("""Expected Double as JsNumber, but got "1.00000001"""", e.getMessage)
            case e: Exception =>
                fail("Expected DeserializationException")
        }
    }

    @Test def testBooleanInnerModel() {
        // Failed should throw if 'get' succeeded.
        Try { scalaFactory.fromString(booleanInnerModelType).get }.failed.foreach {
            case e: DeserializationException =>
                assertEquals("Expected Double as JsNumber, but got true", e.getMessage)
            case e: Exception =>
                fail("Expected DeserializationException")
        }
    }
}

object DoubleToLongModelTest {
    private val semantics = EmptySemantics[Any]
    private val scalaFactory = ModelFactory(DoubleToLongModel.parser, ConstantModel.parser).toTypedFactory[Any, Long](semantics)
    private val javaFactory = ModelFactory(DoubleToLongModel.parser, ConstantModel.parser).toTypedFactory(semantics, new LongFactoryInfo[Any](classOf[Any]))

    def getScalaLongFactory = scalaFactory
    def getJavaLongFactory = javaFactory

    private implicit def intToOptLong(a: Int): Option[Long] = Option(a)
    private implicit def intToOptDouble(a: Int): Option[Double] = Option(a)

    def json(value: Double,
             scale: Option[Double] = None,
             translation: Option[Double] = None,
             clampLower: Option[Long] = None,
             clampUpper: Option[Long] = None,
             round: Option[Boolean] = None) = {

        val jsonPrefix =
            """
              |{
              |  "modelType": "DoubleToLong",
              |  "modelId": { "id": 2, "name": "outer" },
              |  "submodel": {
              |    "modelType": "Constant",
              |    "modelId": { "id": 1, "name": "inner" },
              |    "value": VALUE_HERE
              |  }
            """.stripMargin.replaceAll("VALUE_HERE", value.toString)

        val jsonString =
            jsonPrefix +
            (if (scale.nonEmpty)       ",\n  \"scale\": " + scale.get  else "") +
            (if (translation.nonEmpty) ",\n  \"translation\": " + translation.get else "") +
            (if (clampLower.nonEmpty)  ",\n  \"clampLower\": " + clampLower.get else "") +
            (if (clampUpper.nonEmpty)  ",\n  \"clampUpper\": " + clampUpper.get else "") +
            (if (round.nonEmpty)       ",\n  \"round\": " + round.get else "") +
            "\n}"

        jsonString
    }

    val goodJson = json(value = 1.00000001)

    val stringInnerModelType =
        """
          |{
          |  "modelType": "DoubleToLong",
          |  "modelId": { "id": 2, "name": "outer" },
          |  "submodel": {
          |    "modelType": "Constant",
          |    "modelId": { "id": 1, "name": "inner" },
          |    "value": "1.00000001"
          |  }
          |}
        """.stripMargin.trim

    val booleanInnerModelType =
        """
          |{
          |  "modelType": "DoubleToLong",
          |  "modelId": { "id": 2, "name": "outer" },
          |  "submodel": {
          |    "modelType": "Constant",
          |    "modelId": { "id": 1, "name": "inner" },
          |    "value": true
          |  }
          |}
        """.stripMargin.trim
}
