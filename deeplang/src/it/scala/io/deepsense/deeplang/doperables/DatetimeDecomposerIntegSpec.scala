/**
 * Copyright 2015, deepsense.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.deepsense.deeplang.doperables

import java.sql.Timestamp

import org.apache.spark.rdd.RDD
import org.apache.spark.sql.Row
import org.apache.spark.sql.catalyst.expressions.GenericRowWithSchema
import org.apache.spark.sql.types._
import org.joda.time.DateTime

import io.deepsense.deeplang.DeeplangIntegTestSupport
import io.deepsense.deeplang.doperables.dataframe.DataFrame
import io.deepsense.deeplang.doperations.exceptions.{ColumnDoesNotExistException, WrongColumnTypeException}
import io.deepsense.deeplang.params.selections.{IndexSingleColumnSelection, NameSingleColumnSelection}

class DatetimeDecomposerIntegSpec extends DeeplangIntegTestSupport {

  private[this] val timestampColumnName = "timestampColumn"
  private[this] val t1 = new DateTime(2015, 3, 30, 15, 25)

  "DatetimeDecomposer" should {
    "decompose timestamp column without prefix" in {
      val schema = createSchema
      val t2 = t1.plusDays(1)
      val data = createData(
        List(Some(new Timestamp(t1.getMillis)), Some(new Timestamp(t2.getMillis)))
      )
      val expectedData: Seq[Row] = Seq(
        createDecomposedTimestampRow(schema, 0, t1), createDecomposedTimestampRow(schema, 1, t2)
      )
      shouldDecomposeTimestamp(schema, data, expectedData, "")
    }

    "decompose timestamp column with prefix" in {
      val schema = createSchema
      val t2 = t1.plusDays(1)
      val data = createData(
        List(Some(new Timestamp(t1.getMillis)), Some(new Timestamp(t2.getMillis)))
      )
      val expectedData: Seq[Row] = Seq(
        createDecomposedTimestampRow(schema, 0, t1), createDecomposedTimestampRow(schema, 1, t2)
      )
      shouldDecomposeTimestamp(schema, data, expectedData, timestampColumnName + "_")
    }
  }

  it should {
    "transform schema without prefix" in {
      shouldTransformSchema(createSchema, "")
    }

    "transform schema with prefix" in {
      shouldTransformSchema(createSchema, timestampColumnName + "_")
    }
  }

  it should {
    "decompose null timestamp column" in {
      val schema = createSchema
      val data = createData(List(Some(new Timestamp(t1.getMillis)), None))
      val expectedData: Seq[Row] = Seq(
        createDecomposedTimestampRow(schema, 0, t1),
        new GenericRowWithSchema(Array(1, null, null, null, null, null, null, null),
          resultSchema(schema, ""))
      )

      shouldDecomposeTimestamp(schema, data, expectedData, "")
    }
  }

  it should {
    "throw an exception" when {
      "column selected by name does not exist" in {
        intercept[ColumnDoesNotExistException] {
          val operation = new DatetimeDecomposer()
            .setTimestampColumn(NameSingleColumnSelection("nonExistsingColumnName"))
            .setTimestampParts(partsFromStrings("year"))
            .setTimestampPrefix("")
          val dataFrame = createDataFrame(
            Seq.empty, StructType(List(StructField("id", DoubleType))))
          decomposeDatetime(operation, dataFrame)
        }
        ()
      }
      "column selected by index does not exist" in {
        intercept[ColumnDoesNotExistException] {
          val operation = new DatetimeDecomposer()
            .setTimestampColumn(IndexSingleColumnSelection(1))
            .setTimestampParts(partsFromStrings("year"))
            .setTimestampPrefix("")
          val dataFrame = createDataFrame(
            Seq.empty, StructType(List(StructField("id", DoubleType))))
          decomposeDatetime(operation, dataFrame)
        }
        ()
      }
      "selected column is not timestamp" in {
        intercept[WrongColumnTypeException] {
          val operation = new DatetimeDecomposer()
            .setTimestampColumn(IndexSingleColumnSelection(0))
            .setTimestampParts(partsFromStrings("year"))
            .setTimestampPrefix("")
          val dataFrame = createDataFrame(
            Seq.empty, StructType(List(StructField("id", DoubleType))))
          decomposeDatetime(operation, dataFrame)
        }
        ()
      }
    }
  }

  it should {
    "throw an exception in transform schema" when {
      "column selected by name does not exist" in {
        intercept[ColumnDoesNotExistException] {
          val operation = new DatetimeDecomposer()
            .setTimestampColumn(NameSingleColumnSelection("nonExistsingColumnName"))
            .setTimestampParts(partsFromStrings("year"))
            .setTimestampPrefix("")
          val schema = StructType(List(StructField("id", DoubleType)))
          operation._transformSchema(schema)
        }
        ()
      }
      "column selected by index does not exist" in {
        intercept[ColumnDoesNotExistException] {
          val operation = new DatetimeDecomposer()
            .setTimestampColumn(IndexSingleColumnSelection(1))
            .setTimestampParts(partsFromStrings("year"))
            .setTimestampPrefix("")
          val schema = StructType(List(StructField("id", DoubleType)))
          operation._transformSchema(schema)
        }
        ()
      }
      "selected column is not timestamp" in {
        intercept[WrongColumnTypeException] {
          val operation = new DatetimeDecomposer()
            .setTimestampColumn(IndexSingleColumnSelection(0))
            .setTimestampParts(partsFromStrings("year"))
            .setTimestampPrefix("")
          val schema = StructType(List(StructField("id", DoubleType)))
          operation._transformSchema(schema)
        }
        ()
      }
    }
  }

  private def shouldDecomposeTimestamp(
      schema: StructType, data: RDD[Row],
      expectedData: Seq[Row],
      prefix: String): Unit = {
    val operation: DatetimeDecomposer = operationWithParamsSet(prefix)
    val dataFrame = executionContext.dataFrameBuilder.buildDataFrame(schema, data)

    val resultDataFrame: DataFrame = decomposeDatetime(operation, dataFrame)

    val expectedSchema: StructType = resultSchema(schema, prefix)
    assert(expectedSchema == resultDataFrame.sparkDataFrame.schema)
    assert(expectedData.size == resultDataFrame.sparkDataFrame.count())
    val zipped = expectedData zip resultDataFrame.sparkDataFrame.rdd.collect()
    assert(zipped.forall(p => p._1 == p._2))
  }

  private def shouldTransformSchema(
      schema: StructType,
      prefix: String): Unit = {
    val operation: DatetimeDecomposer = operationWithParamsSet(prefix)
    val transformedSchema = operation._transformSchema(schema)

    val expectedSchema: StructType = resultSchema(schema, prefix)
    assert(expectedSchema == transformedSchema.get)
  }

  private def createDecomposedTimestampRow(schema: StructType, id: Int, t: DateTime): Row = {
    new GenericRowWithSchema(Array(id, new Timestamp(t.getMillis), t.getYear, t.getMonthOfYear,
      t.getDayOfMonth, t.getHourOfDay, t.getMinuteOfHour, t.getSecondOfMinute), schema)
  }

  private def resultSchema(originalSchema: StructType, prefix: String): StructType =
    StructType(originalSchema.fields ++ Array(
      StructField(prefix + "year", DoubleType),
      StructField(prefix + "month", DoubleType),
      StructField(prefix + "day", DoubleType),
      StructField(prefix + "hour", DoubleType),
      StructField(prefix + "minutes", DoubleType),
      StructField(prefix + "seconds", DoubleType)
    ))

  private def createData(timestamps: Seq[Option[Timestamp]]): RDD[Row] = {
    sparkContext.parallelize(timestamps.zipWithIndex.map(p => Row(p._2, p._1.orNull)))
  }

  private def createSchema: StructType = {
    StructType(List(
      StructField("id", IntegerType),
      StructField(timestampColumnName, TimestampType)
    ))
  }

  private def decomposeDatetime(
      decomposeDatetime: DatetimeDecomposer,
      dataFrame: DataFrame): DataFrame = {
    decomposeDatetime.transform.apply(executionContext)(())(dataFrame)
  }

  private def operationWithParamsSet(prefixParam: String): DatetimeDecomposer = {
    new DatetimeDecomposer()
      .setTimestampColumn(NameSingleColumnSelection(timestampColumnName))
      .setTimestampParts(partsFromStrings("year", "month", "day", "hour", "minutes", "seconds"))
      .setTimestampPrefix(prefixParam)
  }

  private def partsFromStrings(names: String*): Set[DatetimeDecomposer.TimestampPart] = {
    import DatetimeDecomposer.TimestampPart._
    val allParts = Set(Year(), Month(), Day(), Hour(), Minutes(), Seconds())
    names.map(name => allParts.filter(_.name == name).head).toSet
  }
}
