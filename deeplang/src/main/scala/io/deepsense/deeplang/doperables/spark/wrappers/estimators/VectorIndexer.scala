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

package io.deepsense.deeplang.doperables.spark.wrappers.estimators

import org.apache.spark.ml.feature.{VectorIndexer => SparkVectorIndexer, VectorIndexerModel => SparkVectorIndexerModel}

import io.deepsense.deeplang.ExecutionContext
import io.deepsense.deeplang.doperables.spark.wrappers.models.VectorIndexerModel
import io.deepsense.deeplang.doperables.spark.wrappers.params.common._
import io.deepsense.deeplang.doperables.{Report, SparkEstimatorWrapper}
import io.deepsense.deeplang.params.Param

class VectorIndexer
  extends SparkEstimatorWrapper[SparkVectorIndexerModel, SparkVectorIndexer, VectorIndexerModel]
  with HasMaxCategoriesParam
  with HasInputColumn
  with HasOutputColumn {

  override def report(executionContext: ExecutionContext): Report = Report()

  override val params: Array[Param[_]] = declareParams(maxCategories, inputColumn, outputColumn)
}
