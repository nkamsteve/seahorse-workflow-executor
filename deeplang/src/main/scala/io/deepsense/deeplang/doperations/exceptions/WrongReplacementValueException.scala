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

package io.deepsense.deeplang.doperations.exceptions

import org.apache.spark.sql.types.StructField

case class WrongReplacementValueException(value: String, field: StructField)
  extends DOperationExecutionException(
    "Replacement value \"" + value + "\" can't be placed in column \"" + field.name + "\" " +
      "of type \"" + field.dataType.simpleString + "\"",
    None)
