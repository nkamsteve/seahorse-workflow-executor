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

import org.apache.spark.sql.types.StructType
import spray.json.{JsNumber, JsObject}

import io.deepsense.deeplang.doperables.dataframe.DataFrame
import io.deepsense.deeplang.doperations.MockDOperablesFactory._
import io.deepsense.deeplang.doperations.MockTransformers._
import io.deepsense.deeplang.doperations.exceptions.TooManyPossibleTypesException
import io.deepsense.deeplang.inference.{InferContext, InferenceWarnings}
import io.deepsense.deeplang.params.ParamsMatchers._
import io.deepsense.deeplang.{DKnowledge, DOperable, ExecutionContext, UnitSpec}

class TransformSpec extends UnitSpec {

  "Transform" should {

    "transform input Transformer on input DataFrame with proper parameters set" in {
      val transformer = new MockTransformer

      def testTransform(op: Transform, expectedDataFrame: DataFrame): Unit = {
        val Vector(outputDataFrame) = op.execute(mock[ExecutionContext])(
          Vector(mock[DataFrame], transformer))
        outputDataFrame shouldBe expectedDataFrame
      }

      val op1 = Transform()
      testTransform(op1, outputDataFrame1)

      val paramsForTransformer = JsObject(transformer.paramA.name -> JsNumber(2))
      val op2 = Transform().setTransformerParams(paramsForTransformer)
      testTransform(op2, outputDataFrame2)
    }

    "not modify params in input Transformer instance upon execution" in {
      val transformer = new MockTransformer
      val originalTransformer = transformer.replicate()

      val paramsForTransformer = JsObject(transformer.paramA.name -> JsNumber(2))
      val op = Transform().setTransformerParams(paramsForTransformer)
      op.execute(mock[ExecutionContext])(Vector(mock[DataFrame], transformer))

      transformer should have (theSameParamsAs (originalTransformer))
    }

    "infer knowledge from input Transformer on input DataFrame with proper parameters set" in {
      val transformer = new MockTransformer

      def testInference(op: Transform, expecteDataFrameKnowledge: DKnowledge[DataFrame]): Unit = {
        val inputDF = DataFrame.forInference(mock[StructType])
        val (knowledge, warnings) = op.inferKnowledge(mock[InferContext])(
          Vector(DKnowledge(inputDF), DKnowledge(transformer)))
        // Currently, InferenceWarnings are always empty.
        warnings shouldBe InferenceWarnings.empty
        val Vector(dataFrameKnowledge) = knowledge
        dataFrameKnowledge shouldBe expecteDataFrameKnowledge
      }

      val op1 = Transform()
      testInference(op1, dataFrameKnowledge(outputSchema1))

      val paramsForTransformer = JsObject(transformer.paramA.name -> JsNumber(2))
      val op2 = Transform().setTransformerParams(paramsForTransformer)
      testInference(op2, dataFrameKnowledge(outputSchema2))
    }

    "not modify params in input Transformer instance upon inference" in {
      val transformer = new MockTransformer
      val originalTransformer = transformer.replicate()

      val paramsForTransformer = JsObject(transformer.paramA.name -> JsNumber(2))
      val op = Transform().setTransformerParams(paramsForTransformer)
      val inputDF = DataFrame.forInference(mock[StructType])
      op.inferKnowledge(mock[InferContext])(Vector(DKnowledge(inputDF), DKnowledge(transformer)))

      transformer should have (theSameParamsAs (originalTransformer))
    }

    "throw Exception" when {
      "there is more than one Transformer in input Knowledge" in {
        val inputDF = DataFrame.forInference(mock[StructType])
        val transformers = Set[DOperable](new MockTransformer, new MockTransformer)

        val op = Transform()
        a [TooManyPossibleTypesException] shouldBe thrownBy {
          op.inferKnowledge(mock[InferContext])(
            Vector(DKnowledge(inputDF), DKnowledge(transformers)))
        }
      }
    }
  }
}
