// Copyright (C) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License. See LICENSE in project root for information.

package com.microsoft.ml.spark.isolationforest

import org.apache.spark.ml.param.ParamMap
import org.apache.spark.ml.util._
import org.apache.spark.ml.{Estimator, Model}
import com.linkedin.relevance.isolationforest.{IsolationForestParams,
  IsolationForest => IsolationForestSource, IsolationForestModel => IsolationForestModelSource}
import com.microsoft.ml.spark.core.contracts.Wrappable
import org.apache.spark.sql.{DataFrame, Dataset}
import org.apache.spark.sql.types.StructType

object IsolationForest extends DefaultParamsReadable[IsolationForest]

class IsolationForest(override val uid: String, val that: IsolationForestSource)
  extends Estimator[IsolationForestModel]
  with IsolationForestParams with DefaultParamsWritable with Wrappable {

  def this(uid: String) = this(uid, new IsolationForestSource(uid))

  def this() = this(Identifiable.randomUID("IsolationForest"))

  override def copy(extra: ParamMap): IsolationForest =
    new IsolationForest(uid, that.copy(extra))

  override def fit(data: Dataset[_]): IsolationForestModel =
    new IsolationForestModel(uid, that.fit(data))

  override def transformSchema(schema: StructType): StructType =
    that.transformSchema(schema)
}

class IsolationForestModel(override val uid: String, val that: IsolationForestModelSource)
  extends Model[IsolationForestModel]
  with MLWritable {

  override def copy(extra: ParamMap): IsolationForestModel =
    new IsolationForestModel(uid, that.copy(extra))

  override def transform(data: Dataset[_]): DataFrame =
    that.transform(data)

  override def transformSchema(schema: StructType): StructType =
    that.transformSchema(schema)

  override def write: MLWriter = that.write
}

class IsolationForestModelReader extends MLReader[IsolationForestModel] with Serializable {
  override def load(path: String): IsolationForestModel = {
    val that = IsolationForestModelSource.load(path)

    new IsolationForestModel(that.uid, that)
  }
}
object IsolationForestModel extends MLReadable[IsolationForestModel] {
  override def read: MLReader[IsolationForestModel] = new IsolationForestModelReader
}
