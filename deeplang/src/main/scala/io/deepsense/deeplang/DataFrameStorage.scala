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

package io.deepsense.deeplang

import org.apache.spark.sql.{DataFrame => SparkDataFrame}

import io.deepsense.commons.models.Id
import io.deepsense.deeplang.DataFrameStorage._
import io.deepsense.deeplang.doperables.dataframe.DataFrame

trait ReadOnlyDataFrameStorage {

  /**
   * Returns stored dataframe.
   *
   * @param workflowId workflow id.
   * @param dataFrameName dataframe name.
   * @return stored dataframe.
   */
  def get(
      workflowId: Id,
      dataFrameName: DataFrameName): Option[DataFrame]

  /**
   * Lists dataframes stored for workflow with specified id.
   *
   * @param workflowId workflow id.
   * @return names of stored dataframes.
   */
  def listDataFrameNames(workflowId: Id): Iterable[DataFrameName]
}

trait CustomOperationDataFrameStorage {

  /**
   * Returns custom operation's input dataframe.
   * @param workflowId workflow id.
   * @param nodeId node id.
   * @return input dataframe of the operation.
   */
  def getInputDataFrame(workflowId: Id, nodeId: Id): Option[SparkDataFrame]

  /**
   * Sets custom operation's output dataframe.
   * @param workflowId workflow id.
   * @param nodeId node id.
   * @param dataFrame output dataframe of the operation.
   */
  def setOutputDataFrame(workflowId: Id, nodeId: Id, dataFrame: SparkDataFrame): Unit
}

trait DataFrameStorage
  extends ReadOnlyDataFrameStorage
  with CustomOperationDataFrameStorage {

  /**
   * Stores dataframe under specified name.
   *
   * @param workflowId workflow id.
   * @param dataFrameName dataframe name.
   * @param dataFrame dataframe.
   */
  def put(
      workflowId: Id,
      dataFrameName: DataFrameName,
      dataFrame: DataFrame): Unit

  /**
   * Sets custom operation's input dataframe.
   * @param workflowId workflow id.
   * @param nodeId node id.
   * @param dataFrame input dataframe of the operation.
   */
  def setInputDataFrame(workflowId: Id, nodeId: Id, dataFrame: SparkDataFrame): Unit

  /**
   * Returns custom operation's output dataframe.
   * @param workflowId workflow id.
   * @param nodeId node id.
   * @return output dataframe of the operation.
   */
  def getOutputDataFrame(workflowId: Id, nodeId: Id): Option[SparkDataFrame]
}



object DataFrameStorage {
  type DataFrameName = String
  type DataFrameId = (Id, DataFrameName)
}
