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

package io.deepsense.deeplang.doperations

import scala.collection.JavaConverters._

import org.apache.spark.rdd.RDD
import org.apache.spark.sql.Row
import org.apache.spark.sql.types.{IntegerType, StructField, StructType}
import org.scalatest.Matchers
import org.scalatest.prop.GeneratorDrivenPropertyChecks

import io.deepsense.deeplang._
import io.deepsense.deeplang.doperables.dataframe.DataFrame

class DataFrameSplitterIntegSpec
  extends DeeplangIntegTestSupport
  with GeneratorDrivenPropertyChecks
  with Matchers {

  "SplitDataFrame" should {
    "split one df into two df in given range" in {

      val input = Range(1, 100).toSeq

      val parameterPairs = List(
        (0.0, 0),
        (0.3, 1),
        (0.5, 2),
        (0.8, 3),
        (1.0, 4))

      for((splitRatio, seed) <- parameterPairs) {
        val rdd = createData(input)
        val df = executionContext.dataFrameBuilder.buildDataFrame(createSchema, rdd)
        val (df1, df2) = executeOperation(
          executionContext,
          new Split()
            .setSplitRatio(splitRatio)
            .setSeed(seed / 2))(df)
        val dfCount = df.sparkDataFrame.count()
        val df1Count = df1.sparkDataFrame.count()
        val df2Count = df2.sparkDataFrame.count()
        val rowsDf = df.sparkDataFrame.collectAsList().asScala
        val rowsDf1 = df1.sparkDataFrame.collectAsList().asScala
        val rowsDf2 = df2.sparkDataFrame.collectAsList().asScala
        val intersect = rowsDf1.intersect(rowsDf2)
        intersect.size shouldBe 0
        (df1Count + df2Count) shouldBe dfCount
        rowsDf.toSet shouldBe rowsDf1.toSet.union(rowsDf2.toSet)
      }
    }
  }

  private def createSchema: StructType = {
    StructType(List(
      StructField("value", IntegerType, nullable = false)
    ))
  }

  private def createData(data: Seq[Int]): RDD[Row] = {
    sparkContext.parallelize(data.map(Row(_)))
  }

  private def executeOperation(context: ExecutionContext, operation: DOperation)
                              (dataFrame: DataFrame): (DataFrame, DataFrame) = {
    val operationResult = operation.execute(context)(Vector[DOperable](dataFrame))
    val df1 = operationResult.head.asInstanceOf[DataFrame]
    val df2 = operationResult.last.asInstanceOf[DataFrame]
    (df1, df2)
  }
}
