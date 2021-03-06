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

import org.apache.spark.sql.Row
import org.apache.spark.sql.types._

import io.deepsense.deeplang.DeeplangIntegTestSupport
import io.deepsense.deeplang.doperables.dataframe.DataFrame

class SqlExpressionIntegSpec extends DeeplangIntegTestSupport {

  val dataFrameId = "ThisIsAnId"
  val validExpression = s"select * from $dataFrameId"
  val invalidExpresion = "foobar"

  val firstColumn = "firstColumn"
  val secondColumn = "secondColumn"
  val thirdColumn = "thirdColumn"

  val schema = StructType(Seq(
    StructField(firstColumn, StringType),
    StructField(secondColumn, DoubleType),
    StructField(thirdColumn, BooleanType)
  ))

  val data = Seq(
    Row("c",  5.0,  true),
    Row("a",  5.0,  null),
    Row("b",  null, false),
    Row(null, 2.1,  true)
  )

  "SqlExpression" should {
    "allow to manipulate the input DataFrame using the specified name" in {
      val expression = s"select $secondColumn from $dataFrameId"
      val result = executeSqlExpression(expression, dataFrameId, sampleDataFrame)
      val selectedColumnsIndices = Seq(1) // 'secondColumn' index
      val expectedDataFrame = subsetDataFrame(selectedColumnsIndices)
      assertDataFramesEqual(result, expectedDataFrame)
      assertTableUnregistered()
    }
    "unregister the input DataFrame after execution" in {
      val dataFrame = sampleDataFrame
      executeSqlExpression(validExpression, dataFrameId, dataFrame)
      assertTableUnregistered()
    }
    "unregister the input DataFrame if execution failed" in {
      val dataFrame = sampleDataFrame
      a [RuntimeException] should be thrownBy {
        executeSqlExpression(invalidExpresion, dataFrameId, dataFrame)
      }
      assertTableUnregistered()
    }
  }

  def assertTableUnregistered(): Unit = {
    val exception = intercept[RuntimeException] {
      executionContext.sqlContext.table(dataFrameId)
    }
    exception.getMessage shouldBe s"Table Not Found: $dataFrameId"
  }

  def executeSqlExpression(expression: String, dataFrameId: String, input: DataFrame): DataFrame =
    new SqlExpression()
      .setExpression(expression)
      .setDataFrameId(dataFrameId)
      ._transform(executionContext, input)

  def sampleDataFrame: DataFrame = createDataFrame(data, schema)

  def subsetDataFrame(columns: Seq[Int]): DataFrame = {
    val subSchema = StructType(columns.map(schema))
    val subData = data.map { r => Row(columns.map(r.get): _*) }
    createDataFrame(subData, subSchema)
  }
}
